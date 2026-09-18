/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.blockentity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.Test;

/**
 * Unit coverage for {@link MarkerLines#directFacingOffset} &mdash; the axis-line test every marker scan and the
 * volume {@code connectedAxis} set is built on (M4.5). Pure geometry: {@code BlockPos}/{@code Direction} initialize
 * without the game bootstrap, unlike the block entity classes.
 */
public class MarkerLinesTest {

    private static final BlockPos ORIGIN = new BlockPos(10, 64, 10);

    @Test
    public void directFacingOffsetOnXAxis() {
        assertEquals(Direction.EAST, MarkerLines.directFacingOffset(ORIGIN, ORIGIN.offset(5, 0, 0)));
        assertEquals(Direction.WEST, MarkerLines.directFacingOffset(ORIGIN, ORIGIN.offset(-5, 0, 0)));
    }

    @Test
    public void directFacingOffsetOnYAxis() {
        assertEquals(Direction.UP, MarkerLines.directFacingOffset(ORIGIN, ORIGIN.offset(0, 3, 0)));
        assertEquals(Direction.DOWN, MarkerLines.directFacingOffset(ORIGIN, ORIGIN.offset(0, -3, 0)));
    }

    @Test
    public void directFacingOffsetOnZAxis() {
        assertEquals(Direction.SOUTH, MarkerLines.directFacingOffset(ORIGIN, ORIGIN.offset(0, 0, 7)));
        assertEquals(Direction.NORTH, MarkerLines.directFacingOffset(ORIGIN, ORIGIN.offset(0, 0, -7)));
    }

    @Test
    public void directFacingOffsetRejectsOffAxisPairs() {
        // one block off the line: not a direct neighbour line
        assertNull(MarkerLines.directFacingOffset(ORIGIN, ORIGIN.offset(5, 1, 0)));
        // diagonals are never a single-axis line (legacy getDirectFacingOffset)
        assertNull(MarkerLines.directFacingOffset(ORIGIN, ORIGIN.offset(1, 1, 1)));
        assertNull(MarkerLines.directFacingOffset(ORIGIN, ORIGIN.offset(2, 0, 2)));
        // the same block is not a direction either
        assertNull(MarkerLines.directFacingOffset(ORIGIN, ORIGIN));
    }
}
