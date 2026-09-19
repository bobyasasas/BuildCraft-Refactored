/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.silicon.blockentity;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * M4.16 unit tests for the silicon laser family's pure decision logic ({@link BcSiliconMachineLogic}, a plain class
 * on purpose: the block entity subclasses' class init drags vanilla's FML-bound statics into the JVM, which the
 * FML-less unit tests cannot bootstrap). Covers the &times;10<sup>&minus;4</sup> cost scale, the laser's per-tick
 * push formula (the baseline {@code TileLaser#update} clamps) and the legacy {@code TileLaserTableBase#extract}
 * allocation planner (simulate/precise). The registry-bound halves (the recipe codecs, the ingredient tests) are
 * exercised by the in-world evidence run.
 */
public class BcSiliconMachineLogicTest {

    // ---------------------------------------------------------------- slice scale

    @Test
    public void sliceCostScalesLegacyMicroJoulesDown() {
        // the migrated redstone chipset JSON carries the legacy 10,000 MJ = 10^10 µJ cost
        assertEquals(1_000_000, BcSiliconMachineLogic.sliceCost(10_000_000_000L));
        // legacy integration boards cost 5,000 MJ
        assertEquals(500_000, BcSiliconMachineLogic.sliceCost(5_000_000_000L));
        assertEquals(0, BcSiliconMachineLogic.sliceCost(0));
        assertEquals(9_999, BcSiliconMachineLogic.sliceCost(99_999_999));
    }

    @Test
    public void sliceConstantsMatchTheLegacyNumbers() {
        // legacy TileLaser#getMaxPowerPerTick = 4 MJ -> 400 µMJ on the slice scale
        assertEquals(400, BcSiliconMachineLogic.LASER_MAX_PUSH_PER_TICK);
        // legacy TileLaser battery = 1024 MJ -> 102,400 µMJ
        assertEquals(102_400, BcSiliconMachineLogic.LASER_BATTERY_CAPACITY);
        // legacy TileAdvancedCraftingTable#POWER_REQ = 500 MJ -> 50,000 µMJ
        assertEquals(50_000, BcSiliconMachineLogic.ADVANCED_CRAFTING_POWER_REQ);
    }

    // ---------------------------------------------------------------- laser push

    @Test
    public void laserPushesNothingWithoutWorkOrEnergy() {
        assertEquals(0, BcSiliconMachineLogic.laserPushThisTick(1000, 100_000, 400, 0));
        assertEquals(0, BcSiliconMachineLogic.laserPushThisTick(0, 100_000, 400, 1000));
    }

    @Test
    public void laserFullBatteryPushesAtTheCap() {
        // full battery: the fill-scaled term exceeds the cap, so the cap and the requirement bound the push
        assertEquals(400, BcSiliconMachineLogic.laserPushThisTick(102_400, 102_400, 400, 10_000));
        // a half-filled battery is exactly the cap's knee: still full rate
        assertEquals(400, BcSiliconMachineLogic.laserPushThisTick(51_200, 102_400, 400, 10_000));
    }

    @Test
    public void laserRateScalesDownWithTheBattery() {
        // a quarter battery scales the push proportionally (the legacy linear fill term):
        // 400 * (25,600 + 400) / 51,200 = 203
        long push = BcSiliconMachineLogic.laserPushThisTick(25_600, 102_400, 400, 10_000);
        assertEquals(203, push);
        // and the push never exceeds what the battery actually holds:
        // 400 * (50 + 400) / 51,200 = 3
        assertEquals(3, BcSiliconMachineLogic.laserPushThisTick(50, 102_400, 400, 10_000));
    }

    @Test
    public void laserNeverOverfillsTheTarget() {
        // required below the rate: push exactly the requirement (the legacy min(min(...), required) tail)
        assertEquals(150, BcSiliconMachineLogic.laserPushThisTick(102_400, 102_400, 400, 150));
        assertEquals(1, BcSiliconMachineLogic.laserPushThisTick(102_400, 102_400, 400, 1));
    }

    // ---------------------------------------------------------------- extract planner

    /** Matcher keyed by item group: slot holds group {@code g} when {@code items[slot] == g + 1}. */
    private static BcSiliconMachineLogic.SlotMatcher byGroup(int[] items) {
        return (slot, group) -> items[slot] == group + 1;
    }

    @Test
    public void extractSatisfiesEveryGroup() {
        // group 0 needs 3, group 1 needs 2; slots: 3x g0, 2x g1
        int[] amounts = {3, 2};
        assertTrue(BcSiliconMachineLogic.extract(amounts, new int[] {3, 2}, byGroup(new int[] {1, 2}), true,
            false, null));
    }

    @Test
    public void extractDrawsAcrossSlotsWhenOneRunsOut() {
        // group 0 split 1 + 2 across two slots
        int[] amounts = {1, 2, 0};
        assertTrue(BcSiliconMachineLogic.extract(amounts, new int[] {3}, byGroup(new int[] {1, 1, 0}), true,
            false, null));
    }

    @Test
    public void extractFailsWhenAGroupRunsShort() {
        int[] amounts = {2, 2};
        assertFalse(BcSiliconMachineLogic.extract(amounts, new int[] {3, 2}, byGroup(new int[] {1, 2}), true,
            false, null));
    }

    @Test
    public void simulateLeavesTheSlotsUntouched() {
        int[] amounts = {3, 2};
        assertTrue(BcSiliconMachineLogic.extract(amounts, new int[] {3, 2}, byGroup(new int[] {1, 2}), true,
            false, null));
        assertArrayEquals(new int[] {3, 2}, amounts);
    }

    @Test
    public void consumeAppliesThePlan() {
        int[] amounts = {3, 2};
        int[] taken = new int[2];
        assertTrue(BcSiliconMachineLogic.extract(amounts, new int[] {3, 2}, byGroup(new int[] {1, 2}), false,
            false, taken));
        assertArrayEquals(new int[] {0, 0}, amounts);
        assertArrayEquals(new int[] {3, 2}, taken);
    }

    @Test
    public void failedPlanConsumesNothing() {
        // group 1 runs short: the whole plan aborts without touching the slots (the M4.16 fix over the legacy
        // partial-consumption bug, see the planner javadoc)
        int[] amounts = {3, 1};
        assertFalse(BcSiliconMachineLogic.extract(amounts, new int[] {3, 2}, byGroup(new int[] {1, 2}), false,
            false, null));
        assertArrayEquals(new int[] {3, 1}, amounts);
    }

    @Test
    public void preciseRejectsLeftoverStacks() {
        // the non-matching extra slot 2 blocks the precise (integration-table) mode
        int[] amounts = {1, 1, 4};
        assertFalse(BcSiliconMachineLogic.extract(amounts, new int[] {1, 1}, byGroup(new int[] {1, 2, 3}), true,
            true, null));
        // and disappears once consumed too
        int[] amounts2 = {1, 1, 0};
        assertTrue(BcSiliconMachineLogic.extract(amounts2, new int[] {1, 1}, byGroup(new int[] {1, 2, 3}), true,
            true, null));
    }

    @Test
    public void nonPreciseToleratesLeftoverStacks() {
        // the same layout passes the assembly-table (non-precise) mode
        int[] amounts = {1, 1, 4};
        assertTrue(BcSiliconMachineLogic.extract(amounts, new int[] {1, 1}, byGroup(new int[] {1, 2, 3}), true,
            false, null));
    }

    @Test
    public void oneStackCannotServeTwoDifferentGroups() {
        // two groups, one slot holding only 1: the first group draws it, the second runs short
        int[] amounts = {1};
        assertFalse(BcSiliconMachineLogic.extract(amounts, new int[] {1, 1}, (slot, group) -> slot == 0, true,
            false, null));
        // but a count of 2 can serve two groups of 1 (cross-group draw from the same slot)
        int[] amounts2 = {2};
        assertTrue(BcSiliconMachineLogic.extract(amounts2, new int[] {1, 1}, (slot, group) -> slot == 0, true,
            false, null));
    }

    @Test
    public void planningPassReportsTheTakenPlan() {
        // the craft path plans with simulate=true and replays taken[] inside its own transaction: the reported plan
        // must arrive even though nothing was consumed yet (the M4.16 evidence-run lesson)
        int[] amounts = {3};
        int[] taken = new int[1];
        assertTrue(BcSiliconMachineLogic.extract(amounts, new int[] {3}, (slot, group) -> slot == 0, true, false,
            taken));
        assertEquals(3, taken[0]);
        assertEquals("a simulate plan leaves the slot untouched", 3, amounts[0]);
    }
}
