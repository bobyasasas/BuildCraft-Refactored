/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;

/**
 * Minimal M2.8 port of legacy {@code buildcraft.lib.misc.VecUtil}: only the members the oil field worldgen
 * ({@code buildcraft.energy.generation.structure}) needs. The full legacy utility migrates with its consumers in
 * later Phase 2/3 tasks — extend this class, do not duplicate it.
 */
public final class VecUtil {

    public static final BlockPos POS_ONE = new BlockPos(1, 1, 1);

    private VecUtil() {}

    public static int getValue(Vec3i from, Axis axis) {
        return switch (axis) {
            case X -> from.getX();
            case Y -> from.getY();
            case Z -> from.getZ();
        };
    }

    public static BlockPos replaceValue(Vec3i old, Axis axis, int with) {
        return switch (axis) {
            case X -> new BlockPos(with, old.getY(), old.getZ());
            case Y -> new BlockPos(old.getX(), with, old.getZ());
            case Z -> new BlockPos(old.getX(), old.getY(), with);
        };
    }

    /** Distance in block units, from corner to corner (legacy semantics, see the legacy javadoc rant about Mojang). */
    public static double distanceSq(Vec3i pos1, Vec3i pos2) {
        double d1 = (double) pos1.getX() - pos2.getX();
        double d2 = (double) pos1.getY() - pos2.getY();
        double d3 = (double) pos1.getZ() - pos2.getZ();
        return d1 * d1 + d2 * d2 + d3 * d3;
    }

    public static BlockPos min(BlockPos a, BlockPos b) {
        return new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
    }

    public static BlockPos max(BlockPos a, BlockPos b) {
        return new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
    }
}
