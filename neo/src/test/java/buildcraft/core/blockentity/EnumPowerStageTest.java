/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.blockentity;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * M4.4 unit tests for the ported {@link EnumPowerStage}: the legacy {@code TileEngineBase_BC8#computePowerStage}
 * threshold table, the legacy {@code getPistonSpeed} table and the (jsonbc-facing) lowercase serialised names.
 */
public class EnumPowerStageTest {

    /** The legacy computePowerStage thresholds: &lt;0.25 BLUE, &lt;0.5 GREEN, &lt;0.75 YELLOW, &lt;0.85 RED, else OVERHEAT. */
    @Test
    public void fromLevelThresholdTable() {
        assertEquals(EnumPowerStage.BLUE, EnumPowerStage.fromLevel(0.0));
        assertEquals(EnumPowerStage.BLUE, EnumPowerStage.fromLevel(0.1));
        assertEquals(EnumPowerStage.BLUE, EnumPowerStage.fromLevel(0.249999));
        assertEquals(EnumPowerStage.GREEN, EnumPowerStage.fromLevel(0.25));
        assertEquals(EnumPowerStage.GREEN, EnumPowerStage.fromLevel(0.499999));
        assertEquals(EnumPowerStage.YELLOW, EnumPowerStage.fromLevel(0.5));
        assertEquals(EnumPowerStage.YELLOW, EnumPowerStage.fromLevel(0.749999));
        assertEquals(EnumPowerStage.RED, EnumPowerStage.fromLevel(0.75));
        assertEquals(EnumPowerStage.RED, EnumPowerStage.fromLevel(0.849999));
        assertEquals(EnumPowerStage.OVERHEAT, EnumPowerStage.fromLevel(0.85));
        assertEquals(EnumPowerStage.OVERHEAT, EnumPowerStage.fromLevel(1.0));
    }

    /** The legacy getPistonSpeed table (progress gained per tick per stage; the creative/overheat stage animates
     * through its own path, 0 here). */
    @Test
    public void pistonSpeedTable() {
        assertEquals(0.02, EnumPowerStage.BLUE.getPistonSpeed(), 1e-12);
        assertEquals(0.04, EnumPowerStage.GREEN.getPistonSpeed(), 1e-12);
        assertEquals(0.08, EnumPowerStage.YELLOW.getPistonSpeed(), 1e-12);
        assertEquals(0.12, EnumPowerStage.RED.getPistonSpeed(), 1e-12);
        assertEquals(0.0, EnumPowerStage.OVERHEAT.getPistonSpeed(), 1e-12);
        assertEquals(0.0, EnumPowerStage.BLACK.getPistonSpeed(), 1e-12);
    }

    /** The jsonbc texture references ({@code '#trunk_' + stage}) are built from the lowercase names. */
    @Test
    public void serialisedNamesAreLowercase() {
        assertEquals("blue", EnumPowerStage.BLUE.getSerializedName());
        assertEquals("green", EnumPowerStage.GREEN.getSerializedName());
        assertEquals("yellow", EnumPowerStage.YELLOW.getSerializedName());
        assertEquals("red", EnumPowerStage.RED.getSerializedName());
        assertEquals("overheat", EnumPowerStage.OVERHEAT.getSerializedName());
        assertEquals("black", EnumPowerStage.BLACK.getSerializedName());
    }
}
