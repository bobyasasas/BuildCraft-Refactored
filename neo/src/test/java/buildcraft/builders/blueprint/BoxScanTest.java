/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders.blueprint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.junit.Test;

/**
 * Unit coverage for {@link BoxScan} &mdash; the box-walk order the replacer's replaceable-cell search and the
 * placement-decision loops consume (M4.17; the {@code QuarryScanOrderTest} precedent). Covers the ascending
 * x&rarr;z&rarr;y order, the terminal null at the box's max corner (a walk that stays inside the box at all times),
 * the resume-from-cursor semantics and the stale-cursor clamp.
 */
public class BoxScanTest {

    private static final BlockPos MIN = new BlockPos(0, 64, 0);
    private static final BlockPos MAX = new BlockPos(2, 65, 1); // 3 x 2 x 2 = 12 cells

    /** Walks the whole box in the nextMatch order, collecting the cells that pass the filter. */
    private static List<BlockPos> walkAll(BlockPos min, BlockPos max) {
        List<BlockPos> visited = new ArrayList<>();
        BlockPos pos = BoxScan.nextMatch(min, min, max, cell -> true);
        while (pos != null) {
            visited.add(pos);
            // the resume step must use advance (an offset(1,0,0) start past the box would clamp back into it)
            pos = BoxScan.nextMatch(BoxScan.advance(pos, min, max), min, max, cell -> true);
        }
        return visited;
    }

    @Test
    public void walkCoversEveryCellExactlyOnce() {
        List<BlockPos> visited = walkAll(MIN, MAX);
        assertEquals(12, visited.size());
        for (int y = 64; y <= 65; y++) {
            for (int z = 0; z <= 1; z++) {
                for (int x = 0; x <= 2; x++) {
                    assertTrue("cell " + x + "," + y + "," + z + " visited once", visited.remove(new BlockPos(x, y, z)));
                }
            }
        }
        assertTrue(visited.isEmpty());
    }

    @Test
    public void walkOrderIsXThenZThenY() {
        List<BlockPos> visited = walkAll(MIN, MAX);
        // x ascends inside a z row
        assertEquals(new BlockPos(0, 64, 0), visited.get(0));
        assertEquals(new BlockPos(1, 64, 0), visited.get(1));
        assertEquals(new BlockPos(2, 64, 0), visited.get(2));
        // then z wraps back to min x
        assertEquals(new BlockPos(0, 64, 1), visited.get(3));
        // then y starts a new layer at the min corner
        assertEquals(new BlockPos(0, 65, 0), visited.get(6));
        // and the max corner is the very last cell
        assertEquals(MAX, visited.get(11));
    }

    @Test
    public void walkTerminatesInsideTheBox() {
        // the regression guard: walking past the max corner must yield null, never a position outside the box
        // (the replacer would otherwise test and replace a block one cell past the box face)
        BlockPos past = BoxScan.advance(MAX, MIN, MAX);
        assertNull(past);
        assertNull(BoxScan.advance(MAX.offset(1, 1, 1), MIN, MAX)); // already outside: stays outside
        List<BlockPos> visited = walkAll(MIN, MAX);
        for (BlockPos cell : visited) {
            assertTrue("cell " + cell + " left the box", BoxScan.contains(cell, MIN, MAX));
        }
    }

    @Test
    public void nextMatchSkipsNonMatchingCells() {
        // only x=1 cells match (the replacer's "block kind" filter shape)
        BlockPos found = BoxScan.nextMatch(MIN, MIN, MAX, cell -> cell.getX() == 1);
        assertEquals(new BlockPos(1, 64, 0), found);
        // resuming from just past that hit lands on the next matching cell in the next z row
        BlockPos second = BoxScan.nextMatch(found.offset(1, 0, 0), MIN, MAX, cell -> cell.getX() == 1);
        assertEquals(new BlockPos(1, 64, 1), second);
        // and from there only the y=65 layer remains
        BlockPos third = BoxScan.nextMatch(second.offset(1, 0, 0), MIN, MAX, cell -> cell.getX() == 1);
        assertEquals(new BlockPos(1, 65, 0), third);
        BlockPos last = BoxScan.nextMatch(third.offset(1, 0, 0), MIN, MAX, cell -> cell.getX() == 1);
        assertEquals(new BlockPos(1, 65, 1), last);
        // exhausted: the walk is done
        assertNull(BoxScan.nextMatch(last.offset(1, 0, 0), MIN, MAX, cell -> cell.getX() == 1));
    }

    @Test
    public void nextMatchReturnsNullOnNothingToMatch() {
        assertNull(BoxScan.nextMatch(MIN, MIN, MAX, cell -> false));
        // a degenerate 1x1x1 box still works
        assertEquals(MIN, BoxScan.nextMatch(MIN, MIN, MIN, cell -> true));
    }

    @Test
    public void clampPullsStaleCursorsBackIntoTheBox() {
        assertEquals(new BlockPos(0, 64, 0), BoxScan.clamp(new BlockPos(-5, 0, -7), MIN, MAX));
        assertEquals(MAX, BoxScan.clamp(new BlockPos(9, 99, 9), MIN, MAX));
        assertEquals(new BlockPos(1, 65, 1), BoxScan.clamp(new BlockPos(1, 99, 1), MIN, MAX));
        // a cursor already inside stays put
        assertEquals(new BlockPos(2, 64, 0), BoxScan.clamp(new BlockPos(2, 64, 0), MIN, MAX));
    }

    @Test
    public void containsAndAdvanceAgree() {
        // every in-box cell advances to another in-box cell or null; every out-of-box cell refuses to advance
        BlockPos pos = MIN;
        while (pos != null) {
            assertTrue(BoxScan.contains(pos, MIN, MAX));
            BlockPos next = BoxScan.advance(pos, MIN, MAX);
            if (next != null) {
                assertTrue(BoxScan.contains(next, MIN, MAX));
            }
            pos = next;
        }
        assertFalse(BoxScan.contains(new BlockPos(3, 64, 0), MIN, MAX));
    }
}
