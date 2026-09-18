/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.blockentity;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * M4.4 unit tests for the engine BER's animation inputs — the two pure piston functions of {@link EngineVisual} that
 * {@code EngineBlockEntity#getProgressClient(float)}/{@code clientTick} delegate to (the legacy
 * {@code TileEngineBase_BC8} client counter). They live as interface statics precisely so they are testable here: a
 * plain-JUnit JVM cannot load vanilla {@code BlockEntity} (its {@code AttachmentHolder} supertype boots FML), which is
 * the same reason the project's tests stay registry-free.
 */
public class EngineVisualTest {

    private static final float RETRACT = EngineBlockEntity.RETRACT_SPEED; // 0.01 per idle tick

    /** Plain interpolation between the last two synced positions (stage BLUE = 0.02/tick). */
    @Test
    public void progressInterpolatesBetweenTicks() {
        assertEquals(0.1f, EngineVisual.interpolateClientProgress(0.1f, 0.12f, 0.0f), 1e-6f);
        assertEquals(0.11f, EngineVisual.interpolateClientProgress(0.1f, 0.12f, 0.5f), 1e-6f);
        assertEquals(0.12f, EngineVisual.interpolateClientProgress(0.1f, 0.12f, 1.0f), 1e-6f);
    }

    /** The wrap: last 0.99 &rarr; now 0 reads as "now = 1", so the stroke stays continuous through the return. */
    @Test
    public void progressWrapsContinuously() {
        assertEquals(0.995f, EngineVisual.interpolateClientProgress(0.99f, 0.0f, 0.5f), 1e-6f);
        // exactly one partial tick later the position has come back around to the small values
        assertEquals(0.0f, EngineVisual.interpolateClientProgress(0.99f, 0.0f, 1.0f), 1e-6f);
        // and just before the wrap there is no special casing (0.4 is below the 0.5 threshold)
        assertEquals(0.945f, EngineVisual.interpolateClientProgress(0.99f, 0.9f, 0.5f), 1e-6f);
    }

    /** While pumping the position gains the piston speed and wraps 1 &rarr; 0 (the legacy client counter). */
    @Test
    public void pumpingAdvancesAndWraps() {
        assertEquals(0.02f, EngineVisual.advanceClientProgress(0.0f, true, 0.02, RETRACT), 1e-6f);
        assertEquals(0.94f, EngineVisual.advanceClientProgress(0.92f, true, 0.02, RETRACT), 1e-6f);
        // 0.99 + 0.02 crosses the stroke end and restarts from 0
        assertEquals(0.0f, EngineVisual.advanceClientProgress(0.99f, true, 0.02, RETRACT), 1e-6f);
        // exactly 1 wraps too
        assertEquals(0.0f, EngineVisual.advanceClientProgress(0.98f, true, 0.02, RETRACT), 1e-6f);
    }

    /** Once stopped the position decays by 0.01/tick down to exactly 0 (no negative overshoot). */
    @Test
    public void retractionDecaysToZero() {
        assertEquals(0.49f, EngineVisual.advanceClientProgress(0.5f, false, 0.02, RETRACT), 1e-6f);
        // 0.005 - 0.01 clamps to 0 instead of going negative
        assertEquals(0.0f, EngineVisual.advanceClientProgress(0.005f, false, 0.02, RETRACT), 1e-6f);
        // already retracted stays retracted
        assertEquals(0.0f, EngineVisual.advanceClientProgress(0.0f, false, 0.02, RETRACT), 1e-6f);
    }
}
