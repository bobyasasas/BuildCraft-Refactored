/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.misc;

import static org.junit.Assert.assertEquals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import org.junit.Test;

/** M3.4: covers the M2.8 {@link VecUtil} port (pure axis maths, legacy semantics — previously untested). */
public class VecUtilTest {

    @Test
    public void getValueReadsTheRequestedAxis() {
        Vec3i pos = new Vec3i(3, -5, 7);
        assertEquals(3, VecUtil.getValue(pos, Axis.X));
        assertEquals(-5, VecUtil.getValue(pos, Axis.Y));
        assertEquals(7, VecUtil.getValue(pos, Axis.Z));
    }

    @Test
    public void replaceValueSwapsOnlyTheRequestedAxis() {
        Vec3i old = new Vec3i(3, -5, 7);
        assertEquals(new BlockPos(9, -5, 7), VecUtil.replaceValue(old, Axis.X, 9));
        assertEquals(new BlockPos(3, 9, 7), VecUtil.replaceValue(old, Axis.Y, 9));
        assertEquals(new BlockPos(3, -5, 9), VecUtil.replaceValue(old, Axis.Z, 9));
    }

    @Test
    public void distanceSqMatchesLegacyCornerToCornerSemantics() {
        assertEquals(0.0, VecUtil.distanceSq(BlockPos.ZERO, BlockPos.ZERO), 0.0);
        assertEquals(1.0, VecUtil.distanceSq(BlockPos.ZERO, new BlockPos(1, 0, 0)), 0.0);
        // 3-4-5 corner triangle: 9 + 16 = 25
        assertEquals(25.0, VecUtil.distanceSq(BlockPos.ZERO, new BlockPos(3, 4, 0)), 1e-9);
        assertEquals(6.0, VecUtil.distanceSq(new BlockPos(1, 1, 1), new BlockPos(2, 2, 3)), 1e-9);
    }

    @Test
    public void minMaxAreComponentWise() {
        BlockPos a = new BlockPos(3, -5, 7);
        BlockPos b = new BlockPos(-2, 4, 7);
        assertEquals(new BlockPos(-2, -5, 7), VecUtil.min(a, b));
        assertEquals(new BlockPos(3, 4, 7), VecUtil.max(a, b));
    }

    @Test
    public void posOneConstantIsUnitCube() {
        assertEquals(new BlockPos(1, 1, 1), VecUtil.POS_ONE);
    }
}
