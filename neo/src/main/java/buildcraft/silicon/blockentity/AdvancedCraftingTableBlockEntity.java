/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.silicon.blockentity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
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

/**
 * M4.16 advanced crafting table block entity (legacy counterpart:
 * {@code buildcraft.silicon.tile.TileAdvancedCraftingTable}, id {@code buildcraftsilicon:advanced_crafting_table}
 * unchanged): an auto workbench fed by the laser family &mdash; a 3&times;3 crafting grid plus a result buffer;
 * whenever the grid matches a vanilla {@link CraftingRecipe} and the laser energy covers the flat
 * {@link BcSiliconMachineLogic#ADVANCED_CRAFTING_POWER_REQ} (legacy {@code POWER_REQ = 500 MJ} on the slice scale),
 * one craft runs: ingredients consumed (container-item remainders restored like the vanilla {@code ResultSlot}
 * path), the result lands in the buffer.
 *
 * <p>v1 trims (no GUI in this batch &mdash; the phantom blueprint is a GUI concept): the legacy
 * {@code invBlueprint} 3&times;3 phantom pattern and its {@code WorkbenchCrafting} bookkeeping are replaced by
 * direct vanilla-recipe matching over whatever sits in the grid; the 5&times;3 materials store and the 3&times;3
 * result store collapse into grid (slots 0-8) + 3 result buffer slots (9-11, externally extract-only). Power
 * economics (target = {@code canCraft ? POWER_REQ : 0}, the legacy drain-when-idle rule) run through the shared
 * laser-table base.
 */
public class AdvancedCraftingTableBlockEntity extends LaserTableBaseBlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** The 3 &times; 3 crafting grid (legacy {@code invMaterials} feed surface). */
    public static final int GRID_SLOTS = 9;
    /** The result buffer (legacy {@code invResults}, trimmed to 3 slots). */
    public static final int RESULT_SLOTS = 3;
    public static final int SLOT_RESULT_FIRST = GRID_SLOTS;
    public static final int SLOT_RESULT_LAST = GRID_SLOTS + RESULT_SLOTS - 1;

    /** Grid + result buffer in one handler; the result slots reject external inserts (machine writes only). */
    private final ItemStacksResourceHandler inv = new ItemStacksResourceHandler(GRID_SLOTS + RESULT_SLOTS) {
        @Override
        public boolean isValid(int index, ItemResource resource) {
            return index < SLOT_RESULT_FIRST;
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previous) {
            AdvancedCraftingTableBlockEntity.this.invChanged = true;
            AdvancedCraftingTableBlockEntity.this.syncToClients();
        }
    };

    /** Set on inventory changes so the recipe scan re-runs instead of every tick. */
    private boolean invChanged = true;
    /** The cached recipe the current grid matches, null while none does. */
    @Nullable
    private RecipeHolder<CraftingRecipe> cachedRecipe;

    public AdvancedCraftingTableBlockEntity(BlockPos pos, BlockState state) {
        super(BcSiliconBlockEntities.ADVANCED_CRAFTING_TABLE.value(), pos, state);
    }

    public ItemStacksResourceHandler getInv() {
        return this.inv;
    }

    /** The capability view: every side reaches grid + result buffer (result inserts rejected via {@code isValid}). */
    public ResourceHandler<ItemResource> getItemHandler(@Nullable Direction side) {
        return this.inv;
    }

    @Nullable
    public RecipeHolder<CraftingRecipe> getCachedRecipe() {
        return this.cachedRecipe;
    }

    /** Re-matches the grid against the vanilla crafting recipes when it changed (the legacy canCraft check). */
    private void refresh(ServerLevel level) {
        if (!this.invChanged) {
            return;
        }
        this.invChanged = false;
        this.cachedRecipe = null;
        CraftingInput input = CraftingInput.of(3, 3, this.gridStacks());
        if (input.isEmpty()) {
            return;
        }
        this.cachedRecipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level).orElse(null);
    }

    private NonNullList<ItemStack> gridStacks() {
        NonNullList<ItemStack> stacks = NonNullList.withSize(GRID_SLOTS, ItemStack.EMPTY);
        for (int slot = 0; slot < GRID_SLOTS; slot++) {
            stacks.set(slot, this.inv.getResource(slot).toStack((int) this.inv.getAmountAsLong(slot)));
        }
        return stacks;
    }

    @Override
    protected long currentTarget() {
        return this.cachedRecipe == null ? 0 : BcSiliconMachineLogic.ADVANCED_CRAFTING_POWER_REQ;
    }

    @Override
    protected void craft(ServerLevel level) {
        RecipeHolder<CraftingRecipe> holder = this.cachedRecipe;
        if (holder == null) {
            return;
        }
        CraftingInput input = CraftingInput.of(3, 3, this.gridStacks());
        ItemStack out = holder.value().assemble(input);
        if (out.isEmpty()) {
            return;
        }
        NonNullList<ItemStack> remainders = holder.value().getRemainingItems(input);
        try (Transaction transaction = Transaction.openRoot()) {
            for (int slot = 0; slot < GRID_SLOTS; slot++) {
                ItemResource resource = this.inv.getResource(slot);
                if (!resource.isEmpty() && this.inv.getAmountAsLong(slot) > 0) {
                    this.inv.extract(slot, resource, 1, transaction);
                }
            }
            // container-item remainders (the vanilla ResultSlot rule: remainder wins the slot once it freed up)
            for (int slot = 0; slot < GRID_SLOTS && slot < remainders.size(); slot++) {
                ItemStack remainder = remainders.get(slot);
                if (!remainder.isEmpty()) {
                    this.inv.insert(slot, ItemResource.of(remainder), remainder.getCount(), transaction);
                }
            }
            // the machine-internal result write (the result slots' isValid gate only rejects external inserts); one
            // craft's output always fits a single slot, so the first empty-or-matching slot takes the whole stack
            ItemResource outResource = ItemResource.of(out);
            int targetSlot = -1;
            long existing = 0;
            for (int slot = SLOT_RESULT_FIRST; slot <= SLOT_RESULT_LAST; slot++) {
                long now = this.inv.getAmountAsLong(slot);
                if (now == 0 || (this.inv.getResource(slot).equals(outResource)
                    && now + out.getCount() <= out.getMaxStackSize())) {
                    targetSlot = slot;
                    existing = now;
                    break;
                }
            }
            if (targetSlot < 0) {
                return; // buffer full: leave everything untouched, retry next tick
            }
            transaction.commit();
            this.inv.set(targetSlot, outResource, (int) existing + out.getCount());
        }
        this.invChanged = true;
        LOGGER.info("[M416] advanced craft @ {}: recipe={} cost={}µMJ output={} x{}, grid consumed, power left {}µMJ",
            this.worldPosition, holder.id().identifier(), BcSiliconMachineLogic.ADVANCED_CRAFTING_POWER_REQ,
            out.getItem(), out.getCount(), this.power - BcSiliconMachineLogic.ADVANCED_CRAFTING_POWER_REQ);
        this.power -= BcSiliconMachineLogic.ADVANCED_CRAFTING_POWER_REQ;
        this.syncToClients();
    }

    /** Per-tick driver wired through {@code AdvancedCraftingTableBlock#getTicker}. */
    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state,
            AdvancedCraftingTableBlockEntity table) {
        table.refresh(level);
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
        this.invChanged = true;
    }

    /** Diagnostics anchor (the recipe id type reaches the log line through {@link Identifier}). */
    @Nullable
    public Identifier cachedRecipeId() {
        return this.cachedRecipe == null ? null : this.cachedRecipe.id().identifier();
    }
}
