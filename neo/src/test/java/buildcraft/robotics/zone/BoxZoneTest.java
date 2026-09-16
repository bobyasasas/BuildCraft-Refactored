/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.robotics.zone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.junit.Test;

/**
 * Pure-logic tests for the M2.13 robot work zone ({@link BoxZone}): containment (both the block-cell and the
 * {@code IZone#contains(Vec3)} float forms, the latter matching legacy {@code buildcraft.api.core.IZone}), the
 * deterministic scan order the robot's search walks, and the random-cell helper (legacy
 * {@code IZone#getRandomBlockPos(RandomSource)}). Registry-free like the rest of the neo unit suite (BlockPos/Vec3/
 * RandomSource are plain math classes).
 */
public class BoxZoneTest {

    @Test
    public void containsBlockCellsAndPoints() {
        BoxZone zone = new BoxZone(new BlockPos(4, 1, 1), new BlockPos(5, 1, 2));
        assertTrue(zone.contains(new BlockPos(4, 1, 1)));
        assertTrue(zone.contains(new BlockPos(5, 1, 2)));
        assertTrue(zone.contains(new BlockPos(4, 1, 2)));
        assertFalse(zone.contains(new BlockPos(3, 1, 1)));
        assertFalse(zone.contains(new BlockPos(5, 2, 2)));
        assertFalse(zone.contains(new BlockPos(5, 1, 3)));

        // IZone#contains(Vec3): inclusive of the max face (block coords span [min .. max+1) in world space)
        assertTrue(zone.contains(new Vec3(4.0, 1.0, 1.0)));
        assertTrue(zone.contains(new Vec3(6.0, 2.0, 3.0)));
        assertFalse(zone.contains(new Vec3(6.5, 1.5, 1.5)));
        assertFalse(zone.contains(new Vec3(3.9, 1.0, 1.0)));
    }

    @Test
    public void constructorNormalizesSwappedCorners() {
        BoxZone zone = new BoxZone(new BlockPos(5, 2, 2), new BlockPos(4, 1, 1));
        assertEquals(new BlockPos(4, 1, 1), zone.min());
        assertEquals(new BlockPos(5, 2, 2), zone.max());
    }

    @Test
    public void scanWalksEveryCellExactlyOnceInFixedOrder() {
        BoxZone zone = new BoxZone(new BlockPos(4, 1, 1), new BlockPos(5, 2, 2));
        BlockPos[] expected = {
                new BlockPos(4, 1, 1), new BlockPos(5, 1, 1),
                new BlockPos(4, 1, 2), new BlockPos(5, 1, 2),
                new BlockPos(4, 2, 1), new BlockPos(5, 2, 1),
                new BlockPos(4, 2, 2), new BlockPos(5, 2, 2) };
        BlockPos cursor = null;
        for (BlockPos cell : expected) {
            cursor = zone.advanceScan(cursor);
            assertEquals(cell, cursor);
        }
        assertNull(zone.advanceScan(cursor));
        // and the scan restarts from the beginning when handed a null cursor again
        assertEquals(new BlockPos(4, 1, 1), zone.advanceScan(null));
    }

    @Test
    public void singleCellZoneScansToItselfThenEnds() {
        BoxZone zone = new BoxZone(new BlockPos(7, 3, -2), new BlockPos(7, 3, -2));
        assertEquals(new BlockPos(7, 3, -2), zone.advanceScan(null));
        assertNull(zone.advanceScan(new BlockPos(7, 3, -2)));
    }

    @Test
    public void randomBlockPosStaysInside() {
        BoxZone zone = new BoxZone(new BlockPos(-3, -5, 9), new BlockPos(2, 0, 14));
        RandomSource rand = RandomSource.create(12345L);
        for (int i = 0; i < 256; i++) {
            BlockPos pos = zone.getRandomBlockPos(rand);
            assertTrue("random pos outside the zone: " + pos, zone.contains(pos));
        }
        // determinism: the same seed replays the same sequence
        RandomSource replay = RandomSource.create(12345L);
        assertEquals(zone.getRandomBlockPos(replay), zone.getRandomBlockPos(RandomSource.create(12345L)));
    }
}
