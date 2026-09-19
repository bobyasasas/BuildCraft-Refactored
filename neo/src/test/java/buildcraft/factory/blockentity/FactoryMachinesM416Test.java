/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory.blockentity;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * M4.16 unit tests for the new factory machines' pure scan/pacing logic ({@link FloodGateLogic},
 * {@link MiningWellLogic} and {@link AutoworkbenchLogic}, plain classes on purpose: the block entity subclasses' class
 * init drags vanilla's FML-bound statics into the JVM, which the FML-less unit tests cannot bootstrap &mdash; the
 * {@code FactoryMachineLogicTest} discipline). The world-bound halves (fluid placement, block drops, recipe lookups)
 * are exercised by the in-world evidence run.
 */
public class FactoryMachinesM416Test {

    // ---------------------------------------------------------------- flood gate column scan

    @Test
    public void floodGatePlacesOnTheFirstAirCell() {
        assertEquals(1, FloodGateLogic.findPlacementUp(32, o -> o == 1
                ? FloodGateLogic.Cell.PLACEABLE
                : FloodGateLogic.Cell.BLOCKED));
    }

    @Test
    public void floodGateHopsOverItsOwnFluid() {
        // a 2-high water column already above the gate: the next source goes on top (offset 3)
        assertEquals(3, FloodGateLogic.findPlacementUp(32, o -> o <= 2
                ? FloodGateLogic.Cell.SAME_FLUID
                : FloodGateLogic.Cell.PLACEABLE));
    }

    @Test
    public void floodGateStopsAtForeignBlocks() {
        assertEquals(-1, FloodGateLogic.findPlacementUp(32, o -> FloodGateLogic.Cell.BLOCKED));
        assertEquals("a foreign block above same-fluid cells stops the hop", -1, FloodGateLogic.findPlacementUp(32,
                o -> o == 1 ? FloodGateLogic.Cell.SAME_FLUID : FloodGateLogic.Cell.BLOCKED));
    }

    @Test
    public void floodGateRespectsTheColumnLimit() {
        assertEquals("fully fluid columns up to the limit have no placement", -1, FloodGateLogic.findPlacementUp(4,
                o -> FloodGateLogic.Cell.SAME_FLUID));
        assertEquals("blocked columns up to the limit have no placement", -1, FloodGateLogic.findPlacementUp(4,
                o -> FloodGateLogic.Cell.BLOCKED));
        assertEquals("the limit never swallows the first air cell", 1, FloodGateLogic.findPlacementUp(4,
                o -> FloodGateLogic.Cell.PLACEABLE));
    }

    // ---------------------------------------------------------------- mining well scan

    @Test
    public void miningWellSkipsAirOntoTheFirstSolidBlock() {
        // two air cells, then stone: the target sits three blocks below the well
        assertEquals(3, MiningWellLogic.findTarget(512, o -> o <= 2
                ? MiningWellLogic.Cell.PASSABLE
                : MiningWellLogic.Cell.BREAKABLE));
    }

    @Test
    public void miningWellBreaksStraightDownWithoutGaps() {
        assertEquals(1, MiningWellLogic.findTarget(512, o -> MiningWellLogic.Cell.BREAKABLE));
    }

    @Test
    public void miningWellStopsAtBedrock() {
        assertEquals("bedrock right below: nothing to drill", -1, MiningWellLogic.findTarget(512,
                o -> MiningWellLogic.Cell.UNBREAKABLE));
        assertEquals("bedrock under an air gap: never past it", -1, MiningWellLogic.findTarget(512,
                o -> o <= 1 ? MiningWellLogic.Cell.PASSABLE : MiningWellLogic.Cell.UNBREAKABLE));
    }

    @Test
    public void miningWellGivesUpPastTheDepthLimit() {
        assertEquals(-1, MiningWellLogic.findTarget(4, o -> MiningWellLogic.Cell.PASSABLE));
    }

    @Test
    public void miningWellCostMatchesTheLegacyFormulaShape() {
        // legacy 16 MJ * (hardness + 1) * 2, scaled x10^-4: stone (1.5) = 8000 uMJ, dirt (0.5) = 4800 uMJ
        assertEquals(8_000L, MiningWellLogic.blockWorkCost(1.5F));
        assertEquals(4_800L, MiningWellLogic.blockWorkCost(0.5F));
        assertEquals("instabreak blocks still cost the base", 3_200L, MiningWellLogic.blockWorkCost(0.0F));
    }

    // ---------------------------------------------------------------- auto workbench pacing

    @Test
    public void workbenchCraftsOnlyWithEnoughStoredPower() {
        assertEquals(AutoworkbenchLogic.Action.CHARGE, AutoworkbenchLogic.plan(0, 4_000, true, true));
        assertEquals(AutoworkbenchLogic.Action.CHARGE, AutoworkbenchLogic.plan(3_999, 4_000, true, true));
        assertEquals(AutoworkbenchLogic.Action.CRAFT, AutoworkbenchLogic.plan(4_000, 4_000, true, true));
    }

    @Test
    public void workbenchWaitsWhileTheOutputIsBlocked() {
        assertEquals("a full output slot must not consume the grid", AutoworkbenchLogic.Action.IDLE,
                AutoworkbenchLogic.plan(4_000, 4_000, true, false));
    }

    @Test
    public void workbenchIdlesWithoutARecipe() {
        assertEquals(AutoworkbenchLogic.Action.IDLE, AutoworkbenchLogic.plan(0, 4_000, false, true));
        assertEquals(AutoworkbenchLogic.Action.IDLE, AutoworkbenchLogic.plan(4_000, 4_000, false, true));
    }

    @Test
    public void workbenchDrainClampsAtZero() {
        assertEquals(800L, AutoworkbenchLogic.drain(1_000, 200));
        assertEquals("the bleed never underflows", 0L, AutoworkbenchLogic.drain(100, 200));
        assertEquals(0L, AutoworkbenchLogic.drain(0, 200));
    }

    // ---------------------------------------------------------------- regression guard

    /** The cost base the two machines' slice economics hang on must not silently drift (task M4.16 economics). */
    @Test
    public void sliceEconomicsStayPut() {
        assertEquals(3_200L, MiningWellLogic.WORK_COST_BASE);
    }
}
