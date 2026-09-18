/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders.blockentity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import org.junit.Test;

/**
 * Unit coverage for the quarry's deterministic area scan ({@link QuarryScan#scanStart} +
 * {@link QuarryScan#nextScanPos}, M4.5 close-out regression): the drill must work the area top-down
 * layer by layer. The pre-fix scan started at {@code areaMin} (the bottom layer) and stepped y <em>below</em> the
 * area on the first wrap, so only the bottom layer was ever reachable; these tests pin the fixed order. Pure
 * geometry: {@code BlockPos} initializes without the game bootstrap (the {@code MarkerLinesTest} precedent).
 */
public class QuarryScanOrderTest {

    /** Walks the full scan of one area and collects every visited cell. */
    private static List<BlockPos> walkAll(BlockPos min, BlockPos max) {
        List<BlockPos> walk = new ArrayList<>();
        BlockPos pos = QuarryScan.scanStart(min, max);
        while (pos != null) {
            walk.add(pos);
            pos = QuarryScan.nextScanPos(pos, min, max);
        }
        return walk;
    }

    @Test
    public void scanStartsAtTopLayer() {
        BlockPos min = new BlockPos(4, 40, 6);
        BlockPos max = new BlockPos(9, 47, 12);
        assertEquals(new BlockPos(4, 47, 6), QuarryScan.scanStart(min, max));
    }

    @Test
    public void multilayerVolumeReachesSecondLayer() {
        // the M4.5 regression: a 2x2x2 area whose first (top) layer is exhausted must still yield the bottom layer
        BlockPos min = new BlockPos(10, 64, 10);
        BlockPos max = new BlockPos(11, 65, 11);
        List<BlockPos> walk = walkAll(min, max);
        assertEquals(8, walk.size());
        // the 5th cell (first after the top layer) is on the bottom layer...
        assertEquals(64, walk.get(4).getY());
        // ...and the descent re-enters at the layer's (min.x, min.z) corner
        assertEquals(new BlockPos(10, 64, 10), walk.get(4));
    }

    @Test
    public void layersAreWorkedTopDownFullyBeforeDropping() {
        BlockPos min = new BlockPos(0, 10, 0);
        BlockPos max = new BlockPos(2, 12, 2);
        List<BlockPos> walk = walkAll(min, max);
        // every layer appears as one contiguous block of the walk, layers ordered max.y -> min.y
        int index = 0;
        for (int y = max.getY(); y >= min.getY(); y--) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    BlockPos cell = walk.get(index++);
                    assertTrue("walk index " + (index - 1) + " left the layer " + y,
                            cell.getY() == y && cell.getZ() == z && cell.getX() == x);
                }
            }
        }
        assertEquals(27, index);
    }

    @Test
    public void scanCoversEveryCellExactlyOnce() {
        BlockPos min = new BlockPos(-2, 30, 7);
        BlockPos max = new BlockPos(3, 33, 9);
        List<BlockPos> walk = walkAll(min, max);
        Set<BlockPos> unique = new HashSet<>(walk);
        assertEquals((max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1),
                walk.size());
        assertEquals(walk.size(), unique.size());
        for (BlockPos pos : walk) {
            assertFalse("x out of area: " + pos, pos.getX() < min.getX() || pos.getX() > max.getX());
            assertFalse("y out of area: " + pos, pos.getY() < min.getY() || pos.getY() > max.getY());
            assertFalse("z out of area: " + pos, pos.getZ() < min.getZ() || pos.getZ() > max.getZ());
        }
    }

    @Test
    public void singleLayerVolumeTerminates() {
        // 2x2x1: one layer only, no descent below areaMin (the pre-fix behaviour, pinned here as the shape of the
        // smallest area the gametest quarry_cycle uses)
        BlockPos min = new BlockPos(4, 41, 1);
        BlockPos max = new BlockPos(5, 41, 2);
        List<BlockPos> walk = walkAll(min, max);
        assertEquals(4, walk.size());
        for (BlockPos pos : walk) {
            assertEquals(41, pos.getY());
        }
        assertNull(QuarryScan.nextScanPos(walk.get(3), min, max));
        assertNotNull(QuarryScan.nextScanPos(walk.get(2), min, max));
    }
}
