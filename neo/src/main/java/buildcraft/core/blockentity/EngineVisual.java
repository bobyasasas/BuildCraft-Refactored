/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.blockentity;

import net.minecraft.core.Direction;

/**
 * M4.4: the render-facing view of one engine, shared by the {@link StoneEngineBlockEntity} (M2.2b slice) and the
 * {@link EngineBlockEntity} family so the single jsonbc-driven engine BER can drive both. Mirrors the three model
 * variables the legacy {@code BCCoreModels}/@code BCEnergyModels} wired into their jsonbc contexts
 * ({@code progress}/{@code stage}/{@code direction}) plus the pumping flag.
 */
public interface EngineVisual {

    /** Legacy {@code isPumping}: the piston is currently cycling. */
    boolean isPumping();

    /** Legacy {@code getPowerStage}: which trunk texture/light stage the engine shows. */
    EnumPowerStage getPowerStage();

    /** Legacy {@code getProgressClient}: the piston position (0..1) interpolated for the given partial ticks. */
    float getProgressClient(float partialTicks);

    /** Legacy {@code getCurrentFacing}: where the piston points (the output face; the block state facing here). */
    Direction getOutputFacing();

    /**
     * The pure math of the legacy {@code getProgressClient} (the client advances the piston outside the tick loop, so
     * the stroke interpolates between the last two synced ticks): past the halfway point of a full stroke the "now"
     * position counts as one stroke further on, and the result wraps back into 0..1. Static so the animation contract
     * stays testable without a block entity (a plain-JUnit JVM cannot load vanilla
     * {@code BlockEntity}'s {@code AttachmentHolder} supertype).
     */
    static float interpolateClientProgress(float lastProgress, float progress, float partialTicks) {
        float now = progress;
        if (lastProgress > 0.5 && now < 0.5) {
            // we just returned
            now += 1;
        }
        float interp = lastProgress * (1 - partialTicks) + now * partialTicks;
        return interp % 1;
    }

    /**
     * The pure math of the legacy client-side piston counter: the position gains the piston speed while the engine
     * pumps (wrapping 1 &rarr; 0 for the next stroke) and decays by {@code retractSpeed} otherwise, never below 0.
     */
    static float advanceClientProgress(float progress, boolean pumping, double pistonSpeed, float retractSpeed) {
        if (pumping) {
            progress += pistonSpeed;
            if (progress >= 1) {
                progress = 0;
            }
        } else if (progress > 0) {
            progress -= retractSpeed;
            if (progress < 0) {
                progress = 0;
            }
        }
        return progress;
    }
}
