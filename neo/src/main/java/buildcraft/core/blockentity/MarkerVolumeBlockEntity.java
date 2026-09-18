/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.blockentity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import buildcraft.core.BcBlockEntities;
import buildcraft.core.BcBlocks;

/**
 * The volume/signal marker (M4.5 port of legacy {@code buildcraft.core.tile.TileMarkerVolume}, cache-less). Beyond the
 * shared connection bookkeeping of {@link MarkerBlockEntity} it owns the redstone-driven "signals" state: while a
 * redstone signal reaches the marker, {@link #isShowingSignals()} is true and the renderer draws the six axis lines
 * (legacy {@code BlockMarkerVolume#checkSignalState} + {@code RenderMarkerVolume}).
 */
public class MarkerVolumeBlockEntity extends MarkerBlockEntity {

    /** Legacy {@code TileMarkerVolume.showSignals}: the redstone-controlled signal lines. */
    private boolean showSignals;

    public MarkerVolumeBlockEntity(BlockPos pos, BlockState state) {
        super(BcBlockEntities.MARKER_VOLUME.value(), pos, state);
    }

    @Override
    protected Block markerBlock() {
        return BcBlocks.MARKER_VOLUME.value();
    }

    public boolean isShowingSignals() {
        return this.showSignals;
    }

    /** Redstone neighbour update (legacy {@code BlockMarkerVolume#checkSignalState}): the signal lines follow the
     * redstone input. */
    public void updateSignalState(boolean powered) {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        if (this.showSignals != powered) {
            this.showSignals = powered;
            this.syncToClients();
        }
    }

    @Override
    protected boolean canAddMember(BlockPos newPos) {
        // legacy VolumeConnection#canAddMarker: any not-yet-taken axis line, or a box corner
        java.util.EnumSet<Axis> taken = this.connectedAxis();
        for (BlockPos from : this.connectedPositions) {
            Direction direct = directFacingOffset(from, newPos);
            if (direct != null && !taken.contains(direct.getAxis())) {
                return true;
            }
        }
        return isCorner(newPos);
    }

    /** Legacy {@code buildcraft.lib.misc.data.Box#isCorner}: every coordinate matches the box min or max. */
    private boolean isCorner(BlockPos pos) {
        if (!this.isConnected()) {
            return false;
        }
        List<BlockPos> members = this.connectedPositions;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : members) {
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
            maxZ = Math.max(maxZ, p.getZ());
        }
        return (pos.getX() == minX || pos.getX() == maxX)
            && (pos.getY() == minY || pos.getY() == maxY)
            && (pos.getZ() == minZ || pos.getZ() == maxZ);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("bc_show_signals", this.showSignals);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.showSignals = input.getBooleanOr("bc_show_signals", false);
    }
}
