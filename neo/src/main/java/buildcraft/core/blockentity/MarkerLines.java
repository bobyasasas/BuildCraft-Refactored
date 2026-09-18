/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/**
 * The marker connection geometry (M4.5): pure position math, split out of {@link MarkerBlockEntity} so it stays unit
 * testable (block entity classes pull in the FML bootstrap through their parent class chain and cannot initialize in
 * a plain JUnit run).
 */
public final class MarkerLines {

    /** Legacy {@code PositionUtil.getDirectFacingOffset}: the axis direction from {@code from} to {@code to} when
     * both share the other two coordinates, otherwise null. */
    @Nullable
    public static Direction directFacingOffset(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dz = to.getZ() - from.getZ();
        if (dx != 0 && dy == 0 && dz == 0) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        }
        if (dy != 0 && dx == 0 && dz == 0) {
            return dy > 0 ? Direction.UP : Direction.DOWN;
        }
        if (dz != 0 && dx == 0 && dy == 0) {
            return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        }
        return null;
    }

    private MarkerLines() {
    }
}
