/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.silicon.blockentity;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import buildcraft.lib.charge.BcChargeableItem;

/**
 * M4.17 unit tests for the silicon charge/program pure decision logic ({@code BcSiliconMachineLogic} style: plain
 * values only, the FML-less runner cannot bootstrap vanilla). Covers the {@link BcChargeableItem#clampReceive} clamp
 * (the one pacing rule shared by the chargeable items and the charging table's per-tick dump), the full-charge cycle
 * it produces against the legacy-paced laser feed, and the programming table's craft gate
 * ({@link BcSiliconMachineLogic#programmingTarget}).
 */
public class BcSiliconChargeProgramLogicTest {

    private static final long ROBOT_BATTERY = 500_000;
    private static final long LASER_RATE = BcSiliconMachineLogic.LASER_MAX_PUSH_PER_TICK;

    // ---------------------------------------------------------------- receive clamp (the interface's pacing)

    @Test
    public void emptyItemTakesTheWholeOffer() {
        assertEquals(400, BcChargeableItem.clampReceive(0, ROBOT_BATTERY, 400));
    }

    @Test
    public void partialItemTakesOnlyTheRoom() {
        // 10 µMJ of room cut the 400 µMJ offer down to 10 (the legacy receivePower min-clamp)
        assertEquals(10, BcChargeableItem.clampReceive(ROBOT_BATTERY - 10, ROBOT_BATTERY, 400));
    }

    @Test
    public void fullItemTakesNothing() {
        assertEquals(0, BcChargeableItem.clampReceive(ROBOT_BATTERY, ROBOT_BATTERY, 400));
    }

    @Test
    public void noOfferTakesNothing() {
        assertEquals(0, BcChargeableItem.clampReceive(0, ROBOT_BATTERY, 0));
    }

    @Test
    public void clampNeverReturnsNegative() {
        // defensive: a stored-over-max reading must not turn the clamp into a drain
        assertEquals(0, BcChargeableItem.clampReceive(ROBOT_BATTERY + 5, ROBOT_BATTERY, 400));
    }

    // ---------------------------------------------------------------- the full-charge cycle

    @Test
    public void legacyPacedChargeTakesTheLegacy1250Ticks() {
        // the robot battery (500,000 µMJ) at the laser cap (400 µMJ/tick): the charging table dumps the whole buffer
        // into the item every tick, so the item gains exactly one laser push per tick until it is full
        long stored = 0;
        int ticks = 0;
        while (stored < ROBOT_BATTERY) {
            stored += BcChargeableItem.clampReceive(stored, ROBOT_BATTERY, LASER_RATE);
            ticks++;
        }
        assertEquals(1250, ticks);
        assertEquals(ROBOT_BATTERY, stored);
    }

    @Test
    public void lastChargeTickLandsExactlyOnFull() {
        // 499,900 stored: the dump covers the remaining 100 even though the buffer offered 400 (no overfill)
        assertEquals(100, BcChargeableItem.clampReceive(499_900, ROBOT_BATTERY, 400));
    }

    // ---------------------------------------------------------------- programming craft gate

    @Test
    public void programmingTargetIsTheRecipeCostWhenFree() {
        // board programming recipe board_robot_bomber: 800,000,000 µJ -> 80,000 µMJ slice cost
        long cost = BcSiliconMachineLogic.sliceCost(800_000_000L);
        assertEquals(80_000, BcSiliconMachineLogic.programmingTarget(true, true, cost));
    }

    @Test
    public void programmingTargetIsZeroWhileTheOutputIsOccupied() {
        long cost = BcSiliconMachineLogic.sliceCost(800_000_000L);
        assertEquals(0, BcSiliconMachineLogic.programmingTarget(true, false, cost));
    }

    @Test
    public void programmingTargetIsZeroWithoutAMatchingRecipe() {
        assertEquals(0, BcSiliconMachineLogic.programmingTarget(false, true, 80_000));
    }

    @Test
    public void programmingCraftGateMirrorsTheTargetRule() {
        // the craft only fires at power >= target with the same conditions that produced the target
        long cost = BcSiliconMachineLogic.sliceCost(800_000_000L);
        long target = BcSiliconMachineLogic.programmingTarget(true, true, cost);
        boolean ready = target > 0 && 80_000 >= target;
        assertEquals(true, ready);
        assertEquals(false, 79_999 >= target);
    }
}
