/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory.blockentity;

import java.util.function.IntFunction;

/**
 * M4.16 pure placement scan of the flood gate (a plain class on purpose, the {@code FactoryMachineLogicTest}
 * discipline: the block entity's class init drags vanilla's FML-bound statics into the JVM, which the FML-less unit
 * tests cannot bootstrap, so the testable half lives here). The v1 gate fills the straight column above itself (the
 * task's sanctioned simplification of the baseline {@code TileFloodGate} BFS over the open sides); the world-walking
 * half maps each column cell to a {@link Cell} and this scan picks where the next source block goes.
 */
public final class FloodGateLogic {

    /** The three world states a column cell can be in (the block entity maps {@code BlockState} to these). */
    public enum Cell {
        /** Air (or another replaceable, non-fluid state): a fluid source can be placed here. */
        PLACEABLE,
        /** Already the tank's fluid (source or flowing): hop over it and keep looking further up. */
        SAME_FLUID,
        /** Any other block or foreign fluid: the column is blocked. */
        BLOCKED
    }

    private FloodGateLogic() {
    }

    /**
     * Walks the column offsets {@code 1..limit} and returns the offset of the first placeable cell, hopping over cells
     * that already hold the gate's fluid, or {@code -1} when a foreign block stops the walk (or every cell up to the
     * limit already is the gate's fluid). Offsets are {@code 1 = directly above} upwards, mirroring the block entity's
     * {@code pos.above(offset)} mapping.
     */
    public static int findPlacementUp(int limit, IntFunction<Cell> cells) {
        for (int offset = 1; offset <= limit; offset++) {
            Cell cell = cells.apply(offset);
            if (cell == Cell.PLACEABLE) {
                return offset;
            }
            if (cell == Cell.BLOCKED) {
                return -1;
            }
        }
        return -1;
    }
}
