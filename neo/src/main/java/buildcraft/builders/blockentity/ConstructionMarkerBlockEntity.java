/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders.blockentity;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import buildcraft.builders.BcBuildersBlockEntities;
import buildcraft.builders.BcBuildersBlocks;
import buildcraft.builders.marker.MarkerPair;

/**
 * The M4.17 construction marker block entity (C route, replaces the {@code PlaceholderBlockEntity} under the unchanged
 * id {@code buildcraftbuilders:marker_construction}): two markers form a pair and the pair defines the box the
 * architect scans and the replacer works in. Legacy counterpart: {@code buildcraft.builders.tile.TileMarkerConstruction}
 * (frozen 1.20.1 tree, the robot building-site form) plus the pair/box semantics of the core volume markers
 * ({@code buildcraft.core.marker.VolumeConnection}, {@code currentMarkers} static registry and all).
 *
 * <p><b>v1 reductions (flagged for the full builders migration):</b>
 * <ul>
 * <li>The frozen tree's construction marker is a robot build-site (blueprint item slot, launched building items,
 * robots as the resource/power source); none of that ships here &mdash; the builders builder machine is
 * {@link BuilderBlockEntity}, and this marker only carries the shared box definition.</li>
 * <li>Box shape: the legacy volume-marker pair connects only along one axis line and needs a third/fourth marker
 * (axis or corner extension) before the box spans all three dimensions. v1 pairs connect within
 * {@link #PAIR_SCAN_RADIUS} in any direction (or on an axis line up to {@link #MAX_MARKER_DISTANCE}, the legacy
 * {@code BCCoreConfig.markerMaxDistance} default) and the box is always the corner-normalised bounding box of both
 * cells ({@link MarkerPair}) &mdash; one pair, one box, nothing else to place.</li>
 * <li>Marker cells inside a box are scaffolding, not content: the architect scan and the replacer walk skip them (see
 * those classes), so a box drawn tightly around a structure does not capture its markers.</li>
 * </ul>
 *
 * <p>Client sync is the quarry's Beacon pattern: {@link #getUpdateTag()} = {@link #saveAdditional}, so the renderer
 * sees the peer and the box frame through {@code loadAdditional}.
 */
public class ConstructionMarkerBlockEntity extends BlockEntity {

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    /** Legacy {@code BCCoreConfig.markerMaxDistance} default: the axis-line pairing range. */
    public static final int MAX_MARKER_DISTANCE = 64;
    /** The v1 pairing range for pairs that do not share two coordinates ({@code 2*r+1} cells probed per axis, a
     * click/load-time cube scan). */
    public static final int PAIR_SCAN_RADIUS = 16;

    /**
     * The server-side registry of markers with a live pair (legacy {@code TileMarkerConstruction#currentMarkers}); the
     * architect and replacer find their box through {@link #findNearestPair}. Entries leave in {@link #setRemoved()}.
     */
    private static final Set<ConstructionMarkerBlockEntity> ACTIVE = new HashSet<>();

    /** The paired marker's position, null while unpaired. */
    @Nullable
    private BlockPos peerPos;
    /** The pair box (both cells included), derived from {@link #peerPos}; null while unpaired. */
    @Nullable
    private MarkerPair pair;

    public ConstructionMarkerBlockEntity(BlockPos pos, BlockState state) {
        super(BcBuildersBlockEntities.MARKER_CONSTRUCTION.value(), pos, state);
    }

    // ----------------------------------------------------------------- pairing (server side)

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (this.level != null && !this.level.isClientSide()) {
            ACTIVE.add(this);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide()) {
            ACTIVE.add(this);
            this.revalidatePair();
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null && !this.level.isClientSide()) {
            ACTIVE.remove(this);
            if (this.peerPos != null && this.level.getBlockEntity(this.peerPos)
                instanceof ConstructionMarkerBlockEntity peer) {
                peer.breakPair();
            }
            this.breakPair();
        }
    }

    /** Right-click entry point: (re-)tries to pair, then logs the current state (the [M417] evidence line). */
    public void onManualConnectionAttempt() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        this.tryConnect();
        this.logStatus();
    }

    /** Finds the nearest other construction marker and forms the pair. An existing pair is kept. */
    public void tryConnect() {
        if (this.level == null || this.level.isClientSide() || this.peerPos != null) {
            return;
        }
        BlockPos nearest = this.findNearestMarker();
        if (nearest != null && this.level.getBlockEntity(nearest)
            instanceof ConstructionMarkerBlockEntity peer && peer.peerPos == null) {
            this.applyPair(nearest);
            peer.applyPair(this.worldPosition);
        }
    }

    /** The v1 pairing search: the six axis lines first (legacy {@code PositionUtil#getDirectFacingOffset} walk), then
     * the bounded cube for off-axis partners. Returns the closest candidate, null when none is in range. */
    private @Nullable BlockPos findNearestMarker() {
        Level level = this.level;
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Direction direction : Direction.values()) {
            BlockPos pos = this.worldPosition;
            for (int i = 1; i <= MAX_MARKER_DISTANCE; i++) {
                pos = pos.relative(direction);
                if (level.isOutsideBuildHeight(pos)) {
                    break;
                }
                if (level.getBlockState(pos).getBlock() == BcBuildersBlocks.MARKER_CONSTRUCTION.value()) {
                    double distance = pos.distSqr(this.worldPosition);
                    if (distance < bestDistance) {
                        best = pos;
                        bestDistance = distance;
                    }
                    break; // the line walk stops at the first marker either way
                }
            }
        }
        int r = PAIR_SCAN_RADIUS;
        for (BlockPos pos : BlockPos.betweenClosed(
            this.worldPosition.offset(-r, -r, -r), this.worldPosition.offset(r, r, r))) {
            if (pos.equals(this.worldPosition)
                || level.getBlockState(pos).getBlock() != BcBuildersBlocks.MARKER_CONSTRUCTION.value()) {
                continue;
            }
            double distance = pos.distSqr(this.worldPosition);
            if (distance < bestDistance) {
                best = pos.immutable();
                bestDistance = distance;
            }
        }
        return best;
    }

    private void applyPair(BlockPos peerPos) {
        this.peerPos = peerPos;
        this.pair = MarkerPair.of(this.worldPosition, peerPos);
        this.syncToClients();
    }

    /** Drops the pair (peer gone / marker removed). */
    public void breakPair() {
        if (this.peerPos == null) {
            return;
        }
        this.peerPos = null;
        this.pair = null;
        if (this.level != null && !this.level.isClientSide() && !this.isRemoved()) {
            this.syncToClients();
        }
    }

    /** Re-checks the stored pair after a chunk reload; drops it when the peer block is gone. */
    public void revalidatePair() {
        if (this.peerPos != null
            && (this.level == null || this.level.getBlockState(this.peerPos).getBlock()
                != BcBuildersBlocks.MARKER_CONSTRUCTION.value())) {
            this.breakPair();
        }
    }

    // ----------------------------------------------------------------- queries

    public boolean isConnected() {
        return this.peerPos != null && this.pair != null;
    }

    public @Nullable BlockPos getPeerPos() {
        return this.peerPos;
    }

    /** The pair box (both marker cells included), null while unpaired. */
    public @Nullable MarkerPair getPair() {
        return this.pair;
    }

    /** The [M417] status line (right-click + probe evidence). */
    public void logStatus() {
        if (this.pair != null) {
            LOGGER.info("[M417] marker_construction at {} paired with {}, box {} ({} cells)",//
                this.worldPosition, this.peerPos, this.pair.sizeSummary(), this.pair.volume());
        } else {
            LOGGER.info("[M417] marker_construction at {} unpaired (no partner in range)",//
                this.worldPosition);
        }
    }

    /**
     * The machine box-lookup radius: a paired marker further than this from the machine is another rig's business, not
     * a candidate box (keeps independent builds on one map from stealing each other's machines).
     */
    public static final int MACHINE_LOOKUP_RADIUS = 64;

    /**
     * The architect/replacer box lookup: the nearest paired marker of {@code level} within {@link #MACHINE_LOOKUP_RADIUS}
     * of {@code from} whose box does not contain the machine itself (machines are scaffolding, never content), null when
     * none is in range. Server side only.
     */
    public static @Nullable ConstructionMarkerBlockEntity findNearestPairedMarker(ServerLevel level, BlockPos from) {
        ConstructionMarkerBlockEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        double radiusSquared = (double) MACHINE_LOOKUP_RADIUS * MACHINE_LOOKUP_RADIUS;
        for (ConstructionMarkerBlockEntity marker : ACTIVE) {
            if (marker.isRemoved() || !marker.isConnected() || marker.getLevel() != level) {
                continue;
            }
            double distance = marker.worldPosition.distSqr(from);
            if (distance > radiusSquared) {
                continue; // out of range: not this machine's box
            }
            if (marker.pair != null && marker.pair.contains(from)) {
                continue;
            }
            if (distance < bestDistance) {
                best = marker;
                bestDistance = distance;
            }
        }
        return best;
    }

    // ----------------------------------------------------------------- persistence + client sync

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.peerPos != null) {
            output.store("bc_peer", BlockPos.CODEC, this.peerPos);
            if (this.pair != null) {
                output.store("bc_box", CompoundTag.CODEC, this.pair.writeTo(new CompoundTag()));
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.peerPos = input.read("bc_peer", BlockPos.CODEC).orElse(null);
        this.pair = input.read("bc_box", CompoundTag.CODEC).map(MarkerPair::from).orElse(null);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void syncToClients() {
        this.setChanged();
        if (this.level instanceof ServerLevel serverLevel) {
            BlockState state = this.getBlockState();
            serverLevel.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }
}
