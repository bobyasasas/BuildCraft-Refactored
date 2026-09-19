/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.robotics.blockentity;

/**
 * M4.17 pure request bookkeeping of the requester (a plain class on purpose, the {@code FloodGateLogic}/
 * {@code AutoworkbenchLogic} discipline: the block entity's class init drags vanilla's FML-bound statics into the JVM,
 * which the FML-less unit tests cannot bootstrap, so the testable half lives here). The decisions mirror the frozen
 * 1.20.1 {@code TileRequester} semantics on counts:
 * <ul>
 * <li>{@code isFulfilled(i)}: an empty template slot asks for nothing; a slot is fulfilled only when the buffer holds
 * at least the template count of a stack matching the template;</li>
 * <li>{@code getRequest(i)}'s shrink step: the outstanding amount of a request is {@code template - buffered}, never
 * negative;</li>
 * <li>the v1 pull replaces the legacy robot delivery ({@code IRequestProvider#getRequest}/{@code offerItem}) with a
 * direct neighbour pull, one deficit-capped plan per matching neighbour stack.</li>
 * </ul>
 */
public final class RequesterLogic {

    /** The three states of one request slot ({@code TileRequester#isFulfilled} compressed to a decision). */
    public enum Fulfilment {
        /** No template in the slot: nothing is requested, nothing counts as shortfall. */
        EMPTY,
        /** A template exists and the buffer has not reached its count yet (the v1 pull keeps working on it). */
        PENDING,
        /** The buffer holds at least the template count of a matching stack. */
        FULFILLED
    }

    private RequesterLogic() {
    }

    /**
     * The state of one request slot, pure form of {@code TileRequester#isFulfilled(i)}: the identity match between the
     * buffer content and the template is handed in as {@code bufferMatches} (identity checks need the registry world,
     * counts do not).
     */
    public static Fulfilment classify(boolean templateEmpty, boolean bufferEmpty, boolean bufferMatches,
        int bufferedCount, int templateCount) {
        if (templateEmpty) {
            return Fulfilment.EMPTY;
        }
        if (bufferEmpty || !bufferMatches) {
            return Fulfilment.PENDING;
        }
        return bufferedCount >= templateCount ? Fulfilment.FULFILLED : Fulfilment.PENDING;
    }

    /** {@code TileRequester#getRequest(i)}'s shrink step: how many items slot {@code i} still wants (never negative). */
    public static int deficit(int templateCount, int bufferedCount) {
        if (templateCount <= 0) {
            return 0;
        }
        return Math.max(0, templateCount - Math.max(0, bufferedCount));
    }

    /**
     * The v1 pull plan for one matching neighbour stack: never negative and capped by both the slot's deficit and what
     * the source actually holds (the legacy robot {@code offerItem} cap, applied at plan time instead of at delivery).
     */
    public static int planPull(int deficit, long available) {
        if (deficit <= 0 || available <= 0) {
            return 0;
        }
        return (int) Math.min((long) deficit, available);
    }
}
