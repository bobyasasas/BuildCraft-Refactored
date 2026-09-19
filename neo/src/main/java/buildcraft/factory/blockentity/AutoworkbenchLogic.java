/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory.blockentity;

/**
 * M4.16 pure decision logic of the item auto workbench (a plain class on purpose, the
 * {@code FactoryMachineLogicTest} discipline: the block entity's class init drags vanilla's FML-bound statics into the
 * JVM, which the FML-less unit tests cannot bootstrap, so the testable half lives here). Port of the
 * {@code TileAutoWorkbenchBase#update} pacing:
 * <ul>
 * <li>recipe ready and the output has room: craft once the stored power reaches the requirement, otherwise charge at
 * the passive rate (legacy gains {@code POWER_GEN_PASSIVE} per tick while {@code canCraft()});</li>
 * <li>no usable recipe (or the output is blocked): bleed the stored power away at the legacy loss rate
 * ({@code powerStored -= POWER_LOST}, clamped at zero) so a stale charge does not survive forever.</li>
 * </ul>
 */
public final class AutoworkbenchLogic {

    /** What one workbench tick should do (see the class javadoc). */
    public enum Action {
        /** Consume the grid and produce the recipe result (the caller has verified the room). */
        CRAFT,
        /** Store another passive-rate chunk towards the requirement. */
        CHARGE,
        /** Nothing usable to craft: bleed stored power away. */
        IDLE
    }

    private AutoworkbenchLogic() {
    }

    /**
     * The tick decision (see the class javadoc). {@code recipeReady} means a crafting recipe matches the current grid;
     * {@code outputHasRoom} means the (already assembled) result would fit the output slot.
     */
    public static Action plan(long stored, long required, boolean recipeReady, boolean outputHasRoom) {
        if (recipeReady && outputHasRoom) {
            return stored >= required ? Action.CRAFT : Action.CHARGE;
        }
        return Action.IDLE;
    }

    /** The legacy loss step ({@code >= POWER_LOST ? stored - POWER_LOST : 0}), clamped at zero. */
    public static long drain(long stored, long lost) {
        return Math.max(0, stored - lost);
    }
}
