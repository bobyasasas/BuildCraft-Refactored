/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders.blueprint;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

/**
 * Pure box-walk math for the M4.17 C route machines (the replacer's replaceable-cell search; the architect scan uses
 * the {@link BlueprintData} cell order instead). Deterministic ascending order: x, then z, then y (bottom-up layers,
 * the filler slice's scan order, legacy {@code BoxIterator} with {@code EnumAxisOrder.XZY}); split out of the block
 * entity classes so it stays unit testable (the {@code MarkerLines} precedent).
 */
public final class BoxScan {

    private BoxScan() {
    }

    /**
     * The first cell of the box at or after {@code start} (inclusive) matching {@code match}, or null when the walk
     * runs off the {@code max} corner (the caller's "nothing left" signal).
     */
    public static @Nullable BlockPos nextMatch(BlockPos start, BlockPos min, BlockPos max, Predicate<BlockPos> match) {
        if (start == null) {
            return null; // an exhausted walk stays exhausted
        }
        BlockPos pos = clamp(start, min, max);
        while (pos != null) {
            if (match.test(pos)) {
                return pos;
            }
            pos = advance(pos, min, max);
        }
        return null;
    }

    /** The next box cell in x&rarr;z&rarr;y ascending order, or null past the {@code max} corner (the next position is
     * never returned out of the box: {@link #nextMatch} tests every non-null return against {@code match} directly). */
    public static @Nullable BlockPos advance(BlockPos pos, BlockPos min, BlockPos max) {
        if (min == null || max == null || !contains(pos, min, max)) {
            return null;
        }
        if (pos.getX() < max.getX()) {
            return pos.offset(1, 0, 0);
        }
        if (pos.getZ() < max.getZ()) {
            return new BlockPos(min.getX(), pos.getY(), pos.getZ() + 1);
        }
        if (pos.getY() < max.getY()) {
            return new BlockPos(min.getX(), pos.getY() + 1, min.getZ());
        }
        return null; // the max corner was the last cell of the box
    }

    /** True while {@code pos} still addresses a cell of the box. */
    public static boolean contains(BlockPos pos, BlockPos min, BlockPos max) {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX()
            && pos.getY() >= min.getY() && pos.getY() <= max.getY()
            && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    /** Clamps a (possibly stale, persisted) cursor back into the box. */
    public static BlockPos clamp(BlockPos pos, BlockPos min, BlockPos max) {
        return new BlockPos(
            Math.min(Math.max(pos.getX(), min.getX()), max.getX()),
            Math.min(Math.max(pos.getY(), min.getY()), max.getY()),
            Math.min(Math.max(pos.getZ(), min.getZ()), max.getZ()));
    }
}
