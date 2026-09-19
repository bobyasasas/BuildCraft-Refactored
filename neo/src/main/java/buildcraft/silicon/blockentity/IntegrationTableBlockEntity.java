/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.silicon.blockentity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import buildcraft.silicon.BcSiliconBlockEntities;
import buildcraft.silicon.recipe.BcIntegrationRecipe;

/**
 * M4.16 integration table block entity (legacy counterpart:
 * {@code buildcraft.silicon.tile.TileIntegrationTable}, id {@code buildcraftsilicon:integration_table} unchanged).
 *
 * <p><b>Legacy audit and the slice mapping:</b>
 * <ul>
 * <li><b>Inventory</b> &mdash; legacy: three handlers ({@code invTarget} 1 slot, {@code invToIntegrate} 8 slots,
 * {@code invResult} 1 slot). Slice: one 10-slot {@link ItemStacksResourceHandler} with the legacy layout
 * ({@code [0] target, [1..8] toIntegrate, [9] result}); the result slot rejects external inserts
 * ({@code isValid}) while the machine writes it internally, everything else is insert+extract from every face
 * through the 26.1.2 item capability.</li>
 * <li><b>Recipe pick</b> &mdash; legacy: {@code IntegrationRecipeRegistry.getRecipeFor(target, stacks)} whenever the
 * previously picked recipe stops being craftable. Slice: the migrated {@link BcIntegrationRecipe}s are scanned per
 * tick (registration order like the registry's), the first one whose center matches slot 0, whose requirements are
 * fully draw-able from slots 1..8 <em>precisely</em> (the legacy {@code precise=true} rule that rejects unrelated
 * extras) and whose output has room.</li>
 * <li><b>Power + craft</b> &mdash; legacy: target = recipe's {@code requiredMicroJoules} while the output room
 * holds; craft consumes center + requirements and grows the result. Slice: the same rules through the base class on
 * the &times;10<sup>&minus;4</sup> scale, consumption and result write inside one transaction. The picked recipe id
 * is deliberately not persisted (the legacy {@code "recipe"} NBT) &mdash; the per-tick rescan re-derives it after a
 * reload, so the persisted state is inventory + power only.</li>
 * </ul>
 */
public class IntegrationTableBlockEntity extends LaserTableBaseBlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** The legacy {@code invTarget} slot: the recipe's center stack. */
    public static final int SLOT_TARGET = 0;
    /** The legacy {@code invToIntegrate} slots (3 &times; 3 - 1). */
    public static final int SLOTS_TO_INTEGRATE_FIRST = 1;
    public static final int SLOTS_TO_INTEGRATE_LAST = 8;
    /** The legacy {@code invResult} slot (machine-write only, externally extract-only). */
    public static final int SLOT_RESULT = 9;

    /** The combined 10-slot inventory (legacy three handlers, see class javadoc). */
    private final ItemStacksResourceHandler inv = new ItemStacksResourceHandler(10) {
        @Override
        public boolean isValid(int index, ItemResource resource) {
            // the result slot never takes external inserts (legacy EnumAccess result semantics, sane form)
            return index != SLOT_RESULT;
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previous) {
            IntegrationTableBlockEntity.this.syncToClients();
        }
    };

    /** The recipe picked by this tick's scan (the legacy {@code recipe} field), null while nothing matches. */
    @Nullable
    private RecipeHolder<BcIntegrationRecipe> active;

    public IntegrationTableBlockEntity(BlockPos pos, BlockState state) {
        super(BcSiliconBlockEntities.INTEGRATION_TABLE.value(), pos, state);
    }

    public ItemStacksResourceHandler getInv() {
        return this.inv;
    }

    /** The capability view: every side reaches the whole combined inventory. */
    public ResourceHandler<ItemResource> getItemHandler(@Nullable Direction side) {
        return this.inv;
    }

    @Nullable
    public RecipeHolder<BcIntegrationRecipe> getActive() {
        return this.active;
    }

    /** Rescans for this tick's recipe (the legacy {@code updateRecipe}). */
    private void refresh(RecipeManager recipes) {
        this.active = null;
        for (RecipeHolder<?> holder : recipes.getRecipes()) {
            if (holder.value() instanceof BcIntegrationRecipe recipe && this.craftable(recipe)) {
                this.active = typed(holder);
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static RecipeHolder<BcIntegrationRecipe> typed(RecipeHolder<?> holder) {
        return (RecipeHolder<BcIntegrationRecipe>) holder;
    }

    /** Center in slot 0 + precise requirements in slots 1..8 + output room (the legacy craft gate). */
    private boolean craftable(BcIntegrationRecipe recipe) {
        ItemStack center = this.inv.getResource(SLOT_TARGET).toStack(1);
        if ((int) this.inv.getAmountAsLong(SLOT_TARGET) < recipe.centerStack.count()
            || !recipe.centerStack.ingredient().test(center)) {
            return false;
        }
        if (!this.outputFits(recipe)) {
            return false;
        }
        // the plan itself runs non-precise because the planner's precise rule would also condemn slot 0 — but the
        // center is consumed by the craft itself, outside the requirement groups; the legacy precision (every
        // non-empty slot among 1..8 must serve the recipe, extras reject it) is checked right here instead
        int[] amounts = slotAmounts();
        int[] taken = new int[this.inv.size()];
        if (!BcSiliconMachineLogic.extract(amounts, requirementCounts(recipe), this::requirementMatcher, true,
            false, taken)) {
            return false;
        }
        for (int slot = SLOTS_TO_INTEGRATE_FIRST; slot <= SLOTS_TO_INTEGRATE_LAST; slot++) {
            if (amounts[slot] > 0 && taken[slot] == 0) {
                return false;
            }
        }
        return true;
    }

    /** Requirements may draw from any of the 8 surrounding slots (the legacy cross-slot extract); the precise rule
     * in {@link BcSiliconMachineLogic#extract} rejects the extras. */
    private boolean requirementMatcher(int slot, int group) {
        return slot >= SLOTS_TO_INTEGRATE_FIRST && slot <= SLOTS_TO_INTEGRATE_LAST;
    }

    private boolean outputFits(BcIntegrationRecipe recipe) {
        // a raw capacity check on purpose: the result slot rejects every external insert ({@code isValid}), and the
        // transactional insert honours that gate even for this machine's own room probe — the craft itself writes the
        // result through the machine-internal {@code set} bypass. The cap is the plain item stack size for the same
        // isValid reason (getCapacityAsLong would report zero for the gated slot).
        ItemStack out = recipe.output.create();
        ItemResource resource = ItemResource.of(out);
        long current = this.inv.getAmountAsLong(SLOT_RESULT);
        if (current == 0) {
            return true;
        }
        return this.inv.getResource(SLOT_RESULT).equals(resource)
            && current + out.getCount() <= out.getMaxStackSize();
    }

    private static int[] requirementCounts(BcIntegrationRecipe recipe) {
        int[] counts = new int[recipe.requirements.size()];
        for (int i = 0; i < counts.length; i++) {
            counts[i] = recipe.requirements.get(i).count();
        }
        return counts;
    }

    private int[] slotAmounts() {
        int[] amounts = new int[this.inv.size()];
        for (int i = 0; i < amounts.length; i++) {
            amounts[i] = (int) this.inv.getAmountAsLong(i);
        }
        return amounts;
    }

    @Override
    protected long currentTarget() {
        return this.active == null ? 0
            : BcSiliconMachineLogic.sliceCost(this.active.value().requiredMicroJoules);
    }

    @Override
    protected void craft(ServerLevel level) {
        RecipeHolder<BcIntegrationRecipe> holder = this.active;
        if (holder == null) {
            return;
        }
        BcIntegrationRecipe recipe = holder.value();
        ItemStack out = recipe.output.create();
        int[] taken = new int[this.inv.size()];
        // non-precise plan: the precision gate (every non-empty slot in 1..8 serves the recipe) already ran in
        // {@link #craftable} this tick, and the center slot is consumed right below, outside the groups
        if (!BcSiliconMachineLogic.extract(slotAmounts(), requirementCounts(recipe),
            this::requirementMatcher, true, false, taken)) {
            return;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            // center stack (slot 0)
            this.inv.extract(SLOT_TARGET, this.inv.getResource(SLOT_TARGET), recipe.centerStack.count(), transaction);
            for (int slot = 0; slot < this.inv.size(); slot++) {
                if (taken[slot] > 0) {
                    this.inv.extract(slot, this.inv.getResource(slot), taken[slot], transaction);
                }
            }
            // the machine-internal result write (the result slot's isValid gate only rejects external inserts —
            // which is also why the room check reads the plain item stack cap, not getCapacityAsLong: that helper
            // honours isValid and would report zero for the result slot)
            ItemResource outResource = ItemResource.of(out);
            long resultNow = this.inv.getAmountAsLong(SLOT_RESULT);
            if (!(resultNow == 0 || this.inv.getResource(SLOT_RESULT).equals(outResource))
                || resultNow + out.getCount() > out.getMaxStackSize()) {
                return; // no room after all: leave everything untouched, try again next tick
            }
            transaction.commit();
            this.inv.set(SLOT_RESULT, outResource, (int) resultNow + out.getCount());
        }
        long cost = currentTarget();
        this.power -= cost;
        LOGGER.info("[M416] integration craft @ {}: recipe={} target={}µJ (slice {}µMJ) output={} x{}, power left {}µMJ",
            this.worldPosition, holder.id().identifier(), recipe.requiredMicroJoules, cost,
            out.getItem(), out.getCount(), this.power);
        this.syncToClients();
    }

    /** Per-tick driver wired through {@code IntegrationTableBlock#getTicker}. */
    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, IntegrationTableBlockEntity table) {
        table.refresh(level.recipeAccess());
        table.tickPower(level);
    }

    // ---------------------------------------------------------------------
    // Persistence
    // ---------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.inv.serialize(output.child("bc_inv"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.inv.deserialize(input.childOrEmpty("bc_inv"));
    }
}
