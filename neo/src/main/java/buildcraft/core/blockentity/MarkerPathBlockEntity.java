/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.blockentity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import buildcraft.core.BcBlockEntities;
import buildcraft.core.BcBlocks;

/**
 * The path marker (M4.5 port of legacy {@code buildcraft.core.tile.TileMarkerPath}, cache-less). Connections are
 * ordered chains along one axis ({@code connectedPositions} keeps the chain order, see
 * {@code PathConnection#positions}): a new marker extends either chain end, and the renderer draws one
 * {@code MARKER_PATH_CONNECTED} laser per chain hop.
 */
public class MarkerPathBlockEntity extends MarkerBlockEntity {

    public MarkerPathBlockEntity(BlockPos pos, BlockState state) {
        super(BcBlockEntities.MARKER_PATH.value(), pos, state);
    }

    @Override
    protected Block markerBlock() {
        return BcBlocks.MARKER_PATH.value();
    }

    @Override
    protected boolean canAddMember(BlockPos newPos) {
        if (!this.isConnected()) {
            return false;
        }
        // The chain runs along one axis; a new member must line up with the first or last member on that same axis
        // (legacy PathConnection#addMarker extends the chain ends).
        List<BlockPos> chain = this.connectedPositions;
        Direction.Axis chainAxis = null;
        Direction head = directFacingOffset(chain.get(0), chain.get(chain.size() - 1));
        if (head != null) {
            chainAxis = head.getAxis();
        }
        Direction fromLast = directFacingOffset(chain.get(chain.size() - 1), newPos);
        if (fromLast != null && (chainAxis == null || fromLast.getAxis() == chainAxis)) {
            return true;
        }
        Direction fromFirst = directFacingOffset(chain.get(0), newPos);
        return fromFirst != null && (chainAxis == null || fromFirst.getAxis() == chainAxis);
    }

    @Override
    public boolean addMember(BlockPos newPos) {
        if (this.level == null || this.level.isClientSide() || !this.isConnected()
            || this.connectedPositions.contains(newPos) || !this.canAddMember(newPos)) {
            return false;
        }
        List<BlockPos> chain = new ArrayList<>(this.connectedPositions);
        BlockPos last = chain.get(chain.size() - 1);
        BlockPos first = chain.get(0);
        Direction fromLast = directFacingOffset(last, newPos);
        Direction fromFirst = directFacingOffset(first, newPos);
        if (fromLast != null) {
            chain.add(newPos);
        } else if (fromFirst != null) {
            chain.add(0, newPos);
        } else {
            return false;
        }
        for (BlockPos member : chain) {
            if (this.level.getBlockEntity(member) instanceof MarkerBlockEntity marker) {
                marker.applyConnection(chain);
            }
        }
        return true;
    }
}
