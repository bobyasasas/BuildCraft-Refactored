/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.robotics.blockentity;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Pure-logic tests for the M4.17 requester bookkeeping ({@link RequesterLogic}): the three-way slot state (the
 * count form of legacy {@code TileRequester#isFulfilled}), the outstanding-amount shrink (legacy
 * {@code TileRequester#getRequest}) and the deficit-capped pull plan (the v1 stand-in for the legacy robot
 * {@code offerItem} cap). Registry-free like the rest of the neo unit suite (plain int/long arithmetic).
 */
public class RequesterLogicTest {

    @Test
    public void emptyTemplateSlotsAreAlwaysFulfilled() {
        assertEquals(RequesterLogic.Fulfilment.EMPTY, RequesterLogic.classify(true, true, false, 0, 0));
        // even a full buffer behind a cleared template asks for nothing
        assertEquals(RequesterLogic.Fulfilment.EMPTY, RequesterLogic.classify(true, false, true, 64, 0));
    }

    @Test
    public void pendingUntilTheBufferReachesTheTemplateCount() {
        // empty buffer: pending regardless of the match
        assertEquals(RequesterLogic.Fulfilment.PENDING, RequesterLogic.classify(false, true, false, 0, 32));
        // matching but short
        assertEquals(RequesterLogic.Fulfilment.PENDING, RequesterLogic.classify(false, false, true, 31, 32));
        // exactly the template count: fulfilled (legacy: getCount() >= requested)
        assertEquals(RequesterLogic.Fulfilment.FULFILLED, RequesterLogic.classify(false, false, true, 32, 32));
        // over-filled (external automation may insert up to the slot cap): still fulfilled
        assertEquals(RequesterLogic.Fulfilment.FULFILLED, RequesterLogic.classify(false, false, true, 64, 32));
    }

    @Test
    public void nonMatchingBufferContentNeverCounts() {
        assertEquals(RequesterLogic.Fulfilment.PENDING, RequesterLogic.classify(false, false, false, 64, 1));
    }

    @Test
    public void deficitShrinksByWhatTheBufferHolds() {
        assertEquals(32, RequesterLogic.deficit(32, 0));
        assertEquals(12, RequesterLogic.deficit(32, 20));
        assertEquals(0, RequesterLogic.deficit(32, 32));
        assertEquals(0, RequesterLogic.deficit(32, 64));
        // legacy getRequest returned empty for empty templates: deficit 0
        assertEquals(0, RequesterLogic.deficit(0, 0));
        // defensive negatives
        assertEquals(32, RequesterLogic.deficit(32, -5));
        assertEquals(0, RequesterLogic.deficit(-1, 0));
    }

    @Test
    public void pullPlanCapsAtDeficitAndAvailability() {
        assertEquals(32, RequesterLogic.planPull(32, 64));
        assertEquals(12, RequesterLogic.planPull(32, 12));
        assertEquals(32, RequesterLogic.planPull(32, 32));
        // available caps the plan (the chest simply has less than the deficit asks for)
        assertEquals(3, RequesterLogic.planPull(7, 3));
        assertEquals(0, RequesterLogic.planPull(0, 64));
        assertEquals(0, RequesterLogic.planPull(5, 0));
        assertEquals(0, RequesterLogic.planPull(-3, 10));
    }
}
