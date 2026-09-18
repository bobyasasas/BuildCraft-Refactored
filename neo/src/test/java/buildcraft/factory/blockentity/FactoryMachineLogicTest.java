/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory.blockentity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import buildcraft.lib.recipe.BcFluidAmount;

/**
 * M4.7 unit tests for the factory machines' pure batch logic ({@link BcMachineFluids}, a plain class on purpose:
 * the block entity subclasses' class init drags vanilla's FML-bound statics into the JVM, which the FML-less unit
 * tests cannot bootstrap). Covers the shared output-room and fill-fraction helpers, the distiller's and heat
 * exchanger's batch decisions and the pump's operation gate. The fixtures carry a {@code null} fluid on purpose
 * (the predicates read nothing but the amounts); the registry-bound halves (the recipe lookups, the fluid identity)
 * are exercised by the in-world evidence run.
 */
public class FactoryMachineLogicTest {

    private static final BcFluidAmount IN = new BcFluidAmount(null, 8);
    /** A payload that produces something (a positive amount). */
    private static final BcFluidAmount REAL_OUT = new BcFluidAmount(null, 30);
    /** The {@code minecraft:empty} payloads (the heatable water/lava sinks) carry amount 0: produces nothing. */
    private static final BcFluidAmount EMPTY_OUT = new BcFluidAmount(null, 0);

    // ---------------------------------------------------------------- shared helpers

    @Test
    public void realOutputNeedsItsRoom() {
        assertTrue(BcMachineFluids.fits(30, REAL_OUT));
        assertFalse(BcMachineFluids.fits(29, REAL_OUT));
        assertTrue(BcMachineFluids.fits(Integer.MAX_VALUE, REAL_OUT));
    }

    @Test
    public void emptyPayloadConsumesNoRoom() {
        assertTrue("an empty payload produces nothing, so zero room fits", BcMachineFluids.fits(0, EMPTY_OUT));
        assertFalse("a non-empty payload still needs its room", BcMachineFluids.fits(0, new BcFluidAmount(null, 1000)));
    }

    @Test
    public void fillFractionClampsToUnitInterval() {
        assertEquals(0.0f, BcMachineFluids.fillFraction(0, 100), 1e-6f);
        assertEquals(0.25f, BcMachineFluids.fillFraction(2500, 10_000), 1e-6f);
        assertEquals(1.0f, BcMachineFluids.fillFraction(16_000, 16_000), 1e-6f);
        assertEquals("overflow clamps at full", 1.0f, BcMachineFluids.fillFraction(17_000, 16_000), 1e-6f);
        assertEquals("negative amounts clamp at empty", 0.0f, BcMachineFluids.fillFraction(-5, 100), 1e-6f);
        assertEquals("a zero-capacity tank never fills", 0.0f, BcMachineFluids.fillFraction(50, 0), 1e-6f);
    }

    // ---------------------------------------------------------------- distiller

    @Test
    public void distillerRunsAWellFittedBatch() {
        assertTrue(BcMachineFluids.canDistill(8, IN, 100, REAL_OUT, 100, REAL_OUT));
        assertTrue("exact inputs and exact rooms still fit", BcMachineFluids.canDistill(8, IN, 30, REAL_OUT, 30,
            REAL_OUT));
    }

    @Test
    public void distillerNeedsTheFullInput() {
        assertFalse(BcMachineFluids.canDistill(7, IN, 100, REAL_OUT, 100, REAL_OUT));
    }

    @Test
    public void distillerNeedsBothOutputRooms() {
        assertFalse("no gas room blocks the batch", BcMachineFluids.canDistill(8, IN, 29, REAL_OUT, 100, REAL_OUT));
        assertFalse("no liquid room blocks the batch", BcMachineFluids.canDistill(8, IN, 100, REAL_OUT, 0, REAL_OUT));
    }

    @Test
    public void distillerEmptyOutputsSkipTheRoomCheck() {
        assertTrue("an empty gas output never blocks", BcMachineFluids.canDistill(8, IN, 0, EMPTY_OUT, 100,
            REAL_OUT));
        assertTrue("an empty liquid output never blocks", BcMachineFluids.canDistill(8, IN, 100, REAL_OUT, 0,
            EMPTY_OUT));
    }

    // ---------------------------------------------------------------- heat exchange

    @Test
    public void exchangerConvertsAWellFittedBatch() {
        assertTrue(BcMachineFluids.canConvert(10, new BcFluidAmount(null, 10), 30, REAL_OUT));
        assertTrue(BcMachineFluids.canConvert(2000, new BcFluidAmount(null, 10), 2000, REAL_OUT));
    }

    @Test
    public void exchangerNeedsInputAndRoom() {
        assertFalse(BcMachineFluids.canConvert(9, new BcFluidAmount(null, 10), 10, REAL_OUT));
        assertFalse(BcMachineFluids.canConvert(10, new BcFluidAmount(null, 10), 9, REAL_OUT));
    }

    @Test
    public void exchangerEmptyOutputConsumesWithoutProducing() {
        assertTrue("the water/lava sink recipes run with a full output tank", BcMachineFluids.canConvert(10,
            new BcFluidAmount(null, 10), 0, EMPTY_OUT));
    }

    // ---------------------------------------------------------------- pump

    /** The pump tank size (mirrors {@code PumpBlockEntity.CAPACITY} as a literal: touching the block entity class
     * would bootstrap vanilla statics the plain JVM cannot load). */
    private static final int PUMP_CAPACITY = 16_000;

    @Test
    public void pumpStartsOnAnEmptyTank() {
        assertTrue(BcMachineFluids.hasBucketRoom(0, PUMP_CAPACITY));
    }

    @Test
    public void pumpNeedsARoomBucket() {
        assertTrue(BcMachineFluids.hasBucketRoom(PUMP_CAPACITY - 1000, PUMP_CAPACITY));
        assertFalse("less than a bucket of room blocks the operation", BcMachineFluids.hasBucketRoom(
            PUMP_CAPACITY - 999, PUMP_CAPACITY));
        assertFalse(BcMachineFluids.hasBucketRoom(PUMP_CAPACITY, PUMP_CAPACITY));
    }
}
