/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.blockentity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import buildcraft.core.block.MarkerPathBlock;
import buildcraft.core.block.MarkerVolumeBlock;

/**
 * Common behaviour of the volume and path markers (M4.5 port of legacy {@code buildcraft.lib.tile.TileMarker} and its
 * marker caches, reduced to what the laser visuals need). Every marker stores the full member list of the connection
 * it belongs to ({@code connectedPositions}, this marker included); there is no server-global cache object &mdash;
 * the group is replicated into every loaded member instead, which keeps the renderer and the persistence self
 * contained. Connection discovery mirrors the legacy caches: on load a marker scans the six axis lines
 * ({@code MAX_MARKER_DISTANCE} blocks, legacy {@code BCCoreConfig.markerMaxDistance} default) for the first marker
 * block; a same-type marker forms a connection, a different marker type blocks that line.
 *
 * <p>Client sync uses the same channel as the M2.12 quarry (update tag = {@link #saveAdditional}): positions and the
 * connection reach the renderer through {@code loadAdditional}, and every change pushes
 * {@code ClientboundBlockEntityDataPacket} via {@link #syncToClients()}.
 */
public abstract class MarkerBlockEntity extends BlockEntity {

    /** Legacy {@code BCCoreConfig.markerMaxDistance} default (16..256, default 64). */
    public static final int MAX_MARKER_DISTANCE = 64;

    /**
     * The members of this marker's connection, this position always included. Empty means unconnected; two or more
     * entries is a live connection. Ordered for the path markers ({@link MarkerPathBlockEntity} chains along one
     * axis); the volume markers only care about the set.
     */
    protected final List<BlockPos> connectedPositions = new ArrayList<>();

    protected MarkerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ---------------------------------------------------------------------
    // Renderer-facing queries (safe on both sides)
    // ---------------------------------------------------------------------

    /** True while this marker belongs to a connection of two or more markers. */
    public boolean isConnected() {
        return this.connectedPositions.size() >= 2;
    }

    /** Read-only view of the connection members (this position included). */
    public List<BlockPos> getConnectedPositions() {
        return List.copyOf(this.connectedPositions);
    }

    // ---------------------------------------------------------------------
    // Server-side connection management
    // ---------------------------------------------------------------------

    /** The block class this marker type connects to. */
    protected abstract Block markerBlock();

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide()) {
            this.revalidateConnection();
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null && !this.level.isClientSide()) {
            this.leaveConnection();
        }
    }

    /** Right-click entry point (legacy {@code TileMarkerVolume#onManualConnectionAttempt}): lonely markers try to
     * connect along their six lines; connected markers additionally try to pull nearby markers into their group. */
    public void onManualConnectionAttempt() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        for (Direction dir : Direction.values()) {
            BlockPos other = this.findMarkerAlong(dir);
            if (other == null) {
                continue;
            }
            if (this.level.getBlockEntity(other) instanceof MarkerBlockEntity marker) {
                this.connectWith(marker);
            }
        }
    }

    /**
     * Re-checks the stored connection (legacy cache bookkeeping): drops members whose marker is gone, dissolves
     * groups smaller than two and auto-connects markers that are still lonely.
     */
    public void revalidateConnection() {
        boolean changed = this.connectedPositions
            .removeIf(p -> !p.equals(this.worldPosition) && !this.isChunkLoaded(p));
        changed |= this.connectedPositions.removeIf(p -> !p.equals(this.worldPosition) && !this.isMarkerBlockAt(p));
        if (!this.connectedPositions.isEmpty() && this.connectedPositions.size() < 2) {
            this.connectedPositions.clear();
            changed = true;
        }
        if (changed) {
            this.syncToClients();
        }
        if (!this.isConnected()) {
            this.tryConnect();
        }
    }

    /** First (auto-)connection attempt: scan the six axis lines. */
    public void tryConnect() {
        if (this.level == null || this.level.isClientSide() || this.isConnected()) {
            return;
        }
        for (Direction dir : Direction.values()) {
            BlockPos other = this.findMarkerAlong(dir);
            if (other == null) {
                continue;
            }
            if (this.level.getBlockEntity(other) instanceof MarkerBlockEntity marker) {
                if (this.connectWith(marker)) {
                    return;
                }
            }
        }
    }

    /** Joins this marker with the other one: an already connected counterpart tries to absorb this marker
     * (legacy {@code VolumeConnection#addMarker} / {@code PathConnection#addMarker}), two lonely markers form a new
     * pair connection (legacy {@code tryCreateConnection}). */
    protected boolean connectWith(MarkerBlockEntity other) {
        if (other.isConnected()) {
            return other.addMember(this.worldPosition);
        }
        if (this.isConnected()) {
            return this.addMember(other.worldPosition);
        }
        List<BlockPos> makeup = new ArrayList<>();
        makeup.add(this.worldPosition);
        makeup.add(other.worldPosition);
        this.applyConnection(makeup);
        other.applyConnection(makeup);
        return true;
    }

    /**
     * Type-specific absorption rule for pulling {@code newPos} into this marker's existing connection. Volume
     * markers accept any marker on a not-yet-taken axis line or on a box corner (legacy
     * {@code VolumeConnection#canAddMarker}); path markers extend the chain ends along the chain axis.
     */
    protected abstract boolean canAddMember(BlockPos newPos);

    /** Pulls {@code newPos} into this marker's connection, replicating the new member list to every loaded member. */
    public boolean addMember(BlockPos newPos) {
        if (this.level == null || this.level.isClientSide() || !this.isConnected()
            || this.connectedPositions.contains(newPos) || !this.canAddMember(newPos)) {
            return false;
        }
        List<BlockPos> newMakeup = new ArrayList<>(this.connectedPositions);
        newMakeup.add(newPos);
        for (BlockPos member : newMakeup) {
            if (this.level.getBlockEntity(member) instanceof MarkerBlockEntity marker) {
                marker.applyConnection(newMakeup);
            }
        }
        return true;
    }

    /** Replaces this marker's connection membership and re-syncs (the connection update fan-out calls this on every
     * loaded member). */
    protected void applyConnection(List<BlockPos> makeup) {
        if (this.connectedPositions.equals(makeup)) {
            return;
        }
        this.connectedPositions.clear();
        this.connectedPositions.addAll(makeup);
        this.syncToClients();
    }

    /** Removes this marker from its group and notifies the other members (legacy
     * {@code MarkerConnection#removeMarker}). */
    private void leaveConnection() {
        if (!this.isConnected()) {
            this.connectedPositions.clear();
            return;
        }
        List<BlockPos> others = new ArrayList<>(this.connectedPositions);
        others.remove(this.worldPosition);
        this.connectedPositions.clear();
        for (BlockPos other : others) {
            if (this.level.getBlockEntity(other) instanceof MarkerBlockEntity marker) {
                marker.memberRemoved(this.worldPosition);
            }
        }
    }

    /** A group member vanished: drop it; dissolved groups may re-connect on their own. */
    private void memberRemoved(BlockPos gone) {
        if (!this.isConnected() || !this.connectedPositions.remove(gone)) {
            return;
        }
        if (this.connectedPositions.size() < 2) {
            this.connectedPositions.clear();
        }
        this.syncToClients();
        if (!this.isConnected()) {
            this.tryConnect();
        }
    }

    // ---------------------------------------------------------------------
    // Line scanning (legacy PositionUtil.getDirectFacingOffset + cache lookups)
    // ---------------------------------------------------------------------

    /** Walks one axis line up to {@link #MAX_MARKER_DISTANCE}: returns the first same-type marker position, or null
     * when a different marker type blocks the line first (legacy {@code canCreateConnection}). */
    @Nullable
    protected BlockPos findMarkerAlong(Direction dir) {
        BlockPos pos = this.worldPosition;
        for (int i = 1; i <= MAX_MARKER_DISTANCE; i++) {
            pos = pos.relative(dir);
            if (this.level == null || this.level.isOutsideBuildHeight(pos) || !this.isChunkLoaded(pos)) {
                return null;
            }
            BlockState state = this.level.getBlockState(pos);
            if (state.getBlock() == this.markerBlock()) {
                return pos;
            }
            if (isMarkerBlock(state)) {
                return null;
            }
        }
        return null;
    }

    /** True for any marker block (of either type) &mdash; the line blocker of {@link #findMarkerAlong}. */
    public static boolean isMarkerBlock(BlockState state) {
        Block block = state.getBlock();
        return block instanceof MarkerVolumeBlock || block instanceof MarkerPathBlock;
    }

    private boolean isMarkerBlockAt(BlockPos pos) {
        return this.level != null && this.level.getBlockState(pos).getBlock() == this.markerBlock();
    }

    private boolean isChunkLoaded(BlockPos pos) {
        if (this.level == null) {
            return false;
        }
        return !(this.level instanceof ServerLevel serverLevel)
            || serverLevel.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    /** Legacy {@code PositionUtil.getDirectFacingOffset}: the axis direction from {@code from} to {@code to} when
     * both share the other two coordinates, otherwise null. Delegates to the testable {@link MarkerLines} math. */
    @Nullable
    public static Direction directFacingOffset(BlockPos from, BlockPos to) {
        return MarkerLines.directFacingOffset(from, to);
    }

    /** The axes on which two of this connection's members line up (legacy
     * {@code VolumeConnection#getConnectedAxis}). */
    protected java.util.EnumSet<Axis> connectedAxis() {
        java.util.EnumSet<Axis> taken = java.util.EnumSet.noneOf(Axis.class);
        for (BlockPos a : this.connectedPositions) {
            for (BlockPos b : this.connectedPositions) {
                Direction offset = directFacingOffset(a, b);
                if (offset != null) {
                    taken.add(offset.getAxis());
                }
            }
        }
        return taken;
    }

    // ---------------------------------------------------------------------
    // Persistence + client sync (quarry pattern, see the class javadoc)
    // ---------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.connectedPositions.isEmpty()) {
            output.store("bc_makeup", BlockPos.CODEC.listOf(), this.connectedPositions);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.connectedPositions.clear();
        input.read("bc_makeup", BlockPos.CODEC.listOf()).ifPresent(this.connectedPositions::addAll);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Marks the state dirty and pushes it to clients. */
    protected void syncToClients() {
        this.setChanged();
        if (this.level instanceof ServerLevel serverLevel) {
            BlockState state = this.getBlockState();
            serverLevel.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }
}
