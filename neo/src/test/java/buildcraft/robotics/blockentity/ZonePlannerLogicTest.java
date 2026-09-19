/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.robotics.blockentity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.minecraft.core.BlockPos;
import org.junit.Test;
import buildcraft.robotics.zone.BoxZone;

/**
 * Pure-logic tests for the M4.17 zone planner decisions ({@link ZonePlannerLogic}): the single-radius definition form
 * (with its clamping), the null-safe containment (an undefined zone contains nothing) and the cell volume for the
 * {@code [M417]} bounds log. Registry-free like the rest of the neo unit suite (BlockPos is a plain math class, the
 * {@code BoxZoneTest} discipline).
 */
public class ZonePlannerLogicTest {

    @Test
    public void radiusZeroIsThePlannerCellItself() {
        BlockPos center = new BlockPos(10, -3, 7);
        BoxZone zone = ZonePlannerLogic.around(center, 0);
        assertEquals(new BoxZone(center, center), zone);
        assertTrue(ZonePlannerLogic.contains(zone, center));
        assertFalse(ZonePlannerLogic.contains(zone, center.east()));
    }

    @Test
    public void radiusSpansEveryAxisSymmetrically() {
        BlockPos center = new BlockPos(0, 64, 0);
        BoxZone zone = ZonePlannerLogic.around(center, 3);
        assertEquals(new BlockPos(-3, 61, -3), zone.min());
        assertEquals(new BlockPos(3, 67, 3), zone.max());
        assertEquals(7 * 7 * 7, ZonePlannerLogic.volume(zone));
        assertTrue(ZonePlannerLogic.contains(zone, new BlockPos(-3, 61, -3)));
        assertTrue(ZonePlannerLogic.contains(zone, new BlockPos(3, 67, 3)));
        assertFalse(ZonePlannerLogic.contains(zone, new BlockPos(4, 64, 0)));
        assertFalse(ZonePlannerLogic.contains(zone, new BlockPos(0, 68, 0)));
    }

    @Test
    public void negativeRadiusClampsToZero() {
        BlockPos center = new BlockPos(5, 5, 5);
        assertEquals(new BoxZone(center, center), ZonePlannerLogic.around(center, -4));
    }

    @Test
    public void oversizedRadiusClampsToTheSanityCap() {
        BlockPos center = new BlockPos(0, 0, 0);
        BoxZone zone = ZonePlannerLogic.around(center, 10_000);
        assertEquals(new BlockPos(-ZonePlannerLogic.MAX_RADIUS, -ZonePlannerLogic.MAX_RADIUS,
            -ZonePlannerLogic.MAX_RADIUS), zone.min());
        assertEquals(new BlockPos(ZonePlannerLogic.MAX_RADIUS, ZonePlannerLogic.MAX_RADIUS,
            ZonePlannerLogic.MAX_RADIUS), zone.max());
    }

    @Test
    public void undefinedZoneContainsNothingAndHasNoVolume() {
        assertFalse(ZonePlannerLogic.contains(null, BlockPos.ZERO));
        assertEquals(0, ZonePlannerLogic.volume(null));
    }
}
