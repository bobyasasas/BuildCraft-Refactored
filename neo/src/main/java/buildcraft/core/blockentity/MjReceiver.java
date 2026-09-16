/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.blockentity;

/**
 * Minimal MJ power receiver port for the M2.12 filler/quarry slice, mirroring the semantics of legacy
 * {@code buildcraft.api.mj.IMjReceiver} (BuildCraftAPI, read-only reference): a machine that can accept micro-MJ from a
 * neighbouring power pipe.
 * <ul>
 * <li>{@link #getPowerRequested()} &mdash; legacy {@code IMjReceiver#getPowerRequested}: "the number of microjoules
 * that this receiver currently wants, and can accept". Implementations return 0 when they have no work (legacy: "refuse
 * all power (if you have no more work to do or your battery is full)"). The legacy {@code MjBatteryReceiver}
 * implementation returns {@code capacity - stored}.</li>
 * <li>{@link #receivePower(long, boolean)} &mdash; legacy {@code IMjReceiver#receivePower}: accepts up to the requested
 * amount into the battery and returns the <em>excess</em> (the part of {@code microJoules} that was NOT accepted), so
 * callers can deduct {@code microJoules - excess} from their own buffer.</li>
 * </ul>
 *
 * <p>This interface lives in {@code buildcraft.core.blockentity} next to {@link KinesisPipeBlockEntity} so the slice
 * pipe can push into machines without the core layering a dependency on content modules (the M2.11 precedent: the gate
 * slice classes also live in core even though gates are silicon content). It is a plain Java interface on purpose
 * &mdash; the NeoForge capability integration stays deferred (see the {@link KinesisPipeBlockEntity} slice notes), and
 * it intentionally does NOT register anywhere (zero new registry ids).
 */
public interface MjReceiver {

    /**
     * The number of &micro;MJ this receiver currently wants and can accept (legacy
     * {@code IMjReceiver#getPowerRequested}); 0 means "no work / battery full &mdash; refuse power".
     */
    long getPowerRequested();

    /**
     * Accepts up to the requested amount of &micro;J into the battery, mirroring legacy
     * {@code IMjReceiver#receivePower}: with {@code simulate} the internal state is untouched. Returns the excess
     * &micro;MJ (the portion of {@code microJoules} that was NOT accepted).
     */
    long receivePower(long microJoules, boolean simulate);
}
