/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory.blockentity;

import buildcraft.lib.recipe.BcFluidAmount;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * M4.7: the pure value logic shared by the factory machine block entities &mdash; deliberately a plain class reading
 * nothing but {@link BcFluidAmount#amount()}, so the FML-less unit-test JVM can run it (a block entity subclass or a
 * real {@code Fluid} instance would drag vanilla's bootstrap-guarded statics in). The recipes' {@code minecraft:empty}
 * payload outputs (the heatable water/lava sinks) carry {@code amount: 0} in the JSONs, so "produces nothing" is
 * simply a zero amount: zero amounts always fit and write nothing. The registry-bound halves (the recipe lookups and
 * the fluid identity of a payload) sit in the block entities.
 */
public final class BcMachineFluids {

    /** True when one recipe output fits into {@code room} mB of free space (a zero-amount output always fits). */
    public static boolean fits(long room, BcFluidAmount out) {
        return room >= out.amount();
    }

    /** The fluid fill level of a tank, clamped to 0..1 &mdash; the renderer's liquid column height share. */
    public static float fillFraction(long amount, int capacity) {
        if (capacity <= 0) {
            return 0.0F;
        }
        return Math.min(1.0F, Math.max(0.0F, amount / (float) capacity));
    }

    /**
     * The distiller's batch decision (pure, unit-tested): the input tank holds at least the recipe's input and both
     * outputs fit their tanks (zero-amount outputs always do).
     */
    public static boolean canDistill(long inAmount, BcFluidAmount in, long gasRoom, BcFluidAmount outGas,
        long liquidRoom, BcFluidAmount outLiquid) {
        return inAmount >= in.amount()
            && fits(gasRoom, outGas)
            && fits(liquidRoom, outLiquid);
    }

    /**
     * The heat exchanger's batch decision (pure, unit-tested): the input tank holds at least the recipe's input and
     * the output fits the output tank (zero-amount outputs always do).
     */
    public static boolean canConvert(long inAmount, BcFluidAmount in, long outRoom, BcFluidAmount out) {
        return inAmount >= in.amount() && fits(outRoom, out);
    }

    /** The pump's operation gate (pure, unit-tested): one source-block pickup only starts when a full bucket fits. */
    public static boolean hasBucketRoom(long currentAmount, int capacity) {
        return capacity - currentAmount >= FluidType.BUCKET_VOLUME;
    }

    private BcMachineFluids() {
    }
}
