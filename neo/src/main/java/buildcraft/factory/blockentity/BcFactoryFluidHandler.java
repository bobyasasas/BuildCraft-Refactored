/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory.blockentity;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

/**
 * M4.7: the single-slot fluid tank behind every factory machine block entity (the legacy
 * {@code buildcraft.lib.fluid.Tank} slot of {@code TileTank}, {@code TileDistiller_BC8}, {@code TileHeatExchange} and
 * {@code TilePump}). One slot at a fixed capacity; the owning block entity is notified on every change (the
 * {@code onContentsChanged} hook, which fires at the end of a transaction or immediately on {@code set}) so it can
 * mark itself dirty and push the new contents to clients.
 *
 * <p>Restrictions (fill-only or drain-only tanks) are expressed by subclasses overriding {@link #insert} or
 * {@link #extract} to return 0 &mdash; the machines' internal batch moves bypass both by writing through
 * {@link #set} directly, which is what keeps "players may only push into the input" and "the machine may still move
 * fluids internally" independent.
 *
 * <p>Contents live in exactly one place (the handler's own stack list); the block entities read them through
 * {@link #getFluidStack} and persist them through the stock {@link #serialize}/{@link #deserialize} pair.
 */
public class BcFactoryFluidHandler extends FluidStacksResourceHandler {

    /** The notification run by the owning block entity ({@code setChanged} + client push). */
    private final Runnable onChange;

    public BcFactoryFluidHandler(Runnable onChange, int capacity) {
        super(1, capacity);
        this.onChange = onChange;
    }

    @Override
    protected void onContentsChanged(int index, FluidStack previousContents) {
        this.onChange.run();
    }

    /** The current contents (an empty stack when the tank is empty) &mdash; the block entities' read view. */
    public FluidStack getFluidStack() {
        return this.getResource(0).toStack((int) this.getAmountAsLong(0));
    }

    /** Free mB left for the given (already matched or fresh) fluid, 0 for a full or foreign tank. */
    public long roomFor(FluidResource resource) {
        FluidStack current = this.getFluidStack();
        if (current.isEmpty()) {
            return this.capacity;
        }
        return FluidResource.of(current).equals(resource) ? this.capacity - current.getAmount() : 0;
    }
}
