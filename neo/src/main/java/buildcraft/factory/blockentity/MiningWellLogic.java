/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory.blockentity;

import java.util.function.IntFunction;

/**
 * M4.16 pure scan and pricing of the mining well (a plain class on purpose, the {@code FactoryMachineLogicTest}
 * discipline: the block entity's class init drags vanilla's FML-bound statics into the JVM, which the FML-less unit
 * tests cannot bootstrap, so the testable half lives here). Port of the legacy {@code TileMiningWell#nextPos} column
 * walk and {@code BlockUtil#computeBlockBreakPower} on the M2.2 slice micro-MJ economy (the same &times;10&#8315;&#8308;
 * scale the builders-slice quarry uses).
 */
public final class MiningWellLogic {

    /** The three world states a column cell can be in (the block entity maps {@code BlockState} to these). */
    public enum Cell {
        /** Air or a fluid: not a target, but the scan keeps walking through it (legacy {@code isEmptyBlock} branch). */
        PASSABLE,
        /** A breakable solid block: this is the drill's next target. */
        BREAKABLE,
        /** An unbreakable block (bedrock, negative destroy speed): the drill is finished. */
        UNBREAKABLE
    }

    /**
     * Slice per-block work cost base in &micro;MJ: legacy {@code BlockUtil.computeBlockBreakPower} charges
     * {@code 16 MJ * (hardness + 1) * 2} per broken block; scaled &times;10&#8315;&#8308; that is
     * {@code 3,200 * (hardness + 1)} &micro;MJ (identical to the builders-slice {@code QuarryBlockEntity#WORK_COST_BASE},
     * cross-referenced there).
     */
    public static final long WORK_COST_BASE = 3_200;

    private MiningWellLogic() {
    }

    /**
     * Walks the column offsets {@code 1..maxDepth} and returns the offset of the first breakable cell, passing through
     * passable (air/fluid) cells, or {@code -1} when an unbreakable block stops the walk (or nothing breakable exists
     * down to the limit). Offsets are {@code 1 = directly below} downwards, mirroring the block entity's
     * {@code pos.below(offset)} mapping.
     */
    public static int findTarget(int maxDepth, IntFunction<Cell> cells) {
        for (int offset = 1; offset <= maxDepth; offset++) {
            Cell cell = cells.apply(offset);
            if (cell == Cell.BREAKABLE) {
                return offset;
            }
            if (cell == Cell.UNBREAKABLE) {
                return -1;
            }
        }
        return -1;
    }

    /**
     * The slice power cost of breaking one block, in &micro;MJ (see {@link #WORK_COST_BASE}): the legacy formula's
     * {@code *2} is already folded into the base (1,600 &times; 2 = 3,200 on the &times;10&#8315;&#8308; scale).
     */
    public static long blockWorkCost(float hardness) {
        return (long) Math.floor(WORK_COST_BASE * (hardness + 1));
    }
}
