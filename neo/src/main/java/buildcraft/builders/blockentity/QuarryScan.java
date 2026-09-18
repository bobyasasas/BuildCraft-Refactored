/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders.blockentity;

import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

/**
 * The quarry's deterministic area scan (M4.5 close-out): pure position math, split out of {@link QuarryBlockEntity}
 * so it stays unit testable (block entity classes pull in the FML bootstrap through their parent class chain and
 * cannot initialize in a plain JUnit run — the {@code MarkerLines} precedent).
 *
 * <p>Order: x ascending, then z ascending within one layer, then one layer down &mdash; the drill works the area
 * top-down (the legacy {@code BoxIterator} randomises XZY/ZXY and x/z inversions per session, but always descends in
 * y). The pre-fix scan started at {@code areaMin} (the bottom layer) and stepped y <em>below</em> the area on the
 * first wrap, so only the bottom layer was ever reachable.
 */
public final class QuarryScan {

    /** The scan's first cell: the top layer's {@code (min.x, max.y, min.z)} corner. */
    public static BlockPos scanStart(BlockPos min, BlockPos max) {
        return new BlockPos(min.getX(), max.getY(), min.getZ());
    }

    /** The cell after {@code pos} in scan order, or null once the bottom layer is exhausted. */
    @Nullable
    public static BlockPos nextScanPos(BlockPos pos, BlockPos min, BlockPos max) {
        if (pos.getX() < max.getX()) {
            return pos.offset(1, 0, 0);
        }
        if (pos.getZ() < max.getZ()) {
            return new BlockPos(min.getX(), pos.getY(), pos.getZ() + 1);
        }
        if (pos.getY() > min.getY()) {
            return new BlockPos(min.getX(), pos.getY() - 1, min.getZ());
        }
        return null;
    }

    private QuarryScan() {
    }
}
