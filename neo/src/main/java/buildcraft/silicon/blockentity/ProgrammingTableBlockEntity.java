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
import buildcraft.silicon.recipe.BcProgrammingRecipe;

/**
 * M4.17 programming table block entity (legacy counterpart:
 * {@code buildcraft.silicon.tile.TileProgrammingTable_Neptune}, id {@code buildcraftsilicon:programming_table}
 * unchanged) &mdash; replaces the M4.16 v2 placeholder with the minimal faithful programming loop through the shared
 * laser-table economics:
 * <ul>
 * <li><b>Inventory</b> &mdash; legacy: a 1-slot {@code input} (insert+extract) and a 1-slot {@code output}
 * (extract-only). Slice: one 2-slot {@link ItemStacksResourceHandler}, slot 0 input, slot 1 output; the output slot
 * rejects external inserts ({@code isValid}, the legacy {@code EnumAccess.EXTRACT}) and everything is reachable
 * through the 26.1.2 item capability on every face.</li>
 * <li><b>Recipes</b> &mdash; legacy: {@code findRecipe()} collects every registered {@code IProgrammingRecipe} whose
 * input matches the slot stack into {@code availableRecipeIds}; a GUI lets the player pick {@code optionId}
 * (defaulting to -1 = no work). Slice: the migrated {@link BcProgrammingRecipe}s are scanned per tick exactly like
 * the legacy scan (via the {@code RecipeManager}, the M4.16 assembly/integration precedent); the GUI pick is
 * auto-resolved to the <em>first matching recipe by recipe id</em> (the deterministic stand-in for the legacy manual
 * selection, same auto-pick direction the assembly table took) and is deliberately not persisted &mdash; the per-tick
 * scan re-derives it after a reload, so the persisted state is inventory + power only.</li>
 * <li><b>Power + craft</b> &mdash; legacy: target = the picked recipe's {@code energyCost} while the output slot is
 * empty ({@code hasWork}); at {@code power >= target} the craft consumes the input, resets the power to 0 and pushes
 * the output into the output slot. Slice: the same rules through the base class on the &times;10<sup>&minus;4</sup>
 * scale ({@link BcSiliconMachineLogic#sliceCost}, {@link BcSiliconMachineLogic#programmingTarget}), the consumption
 * and the output write happening in one transaction so a blocked output consumes nothing.</li>
 * </ul>
 */
public class ProgrammingTableBlockEntity extends LaserTableBaseBlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** The legacy {@code input} slot (insert + extract). */
    public static final int SLOT_INPUT = 0;
    /** The legacy {@code output} slot (machine-write, externally extract-only). */
    public static final int SLOT_OUTPUT = 1;

    /** The combined 2-slot inventory (legacy two handlers, see class javadoc). */
    private final ItemStacksResourceHandler inv = new ItemStacksResourceHandler(2) {
        @Override
        public boolean isValid(int index, ItemResource resource) {
            // the output slot never takes external inserts (legacy EnumAccess.EXTRACT semantics)
            return index == SLOT_INPUT;
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previous) {
            ProgrammingTableBlockEntity.this.setChanged();
        }
    };

    /** The recipe auto-picked by this tick's scan (the legacy {@code optionRecipes[optionId]}), null while nothing matches. */
    @Nullable
    private RecipeHolder<BcProgrammingRecipe> active;

    public ProgrammingTableBlockEntity(BlockPos pos, BlockState state) {
        super(BcSiliconBlockEntities.PROGRAMMING_TABLE.value(), pos, state);
    }

    public ItemStacksResourceHandler getInv() {
        return this.inv;
    }

    /** The capability view: every side reaches both slots (the output only for extraction, see {@code isValid}). */
    public ResourceHandler<ItemResource> getItemHandler(@Nullable Direction side) {
        return this.inv;
    }

    @Nullable
    public RecipeHolder<BcProgrammingRecipe> getActive() {
        return this.active;
    }

    /** Rescans for this tick's auto-pick (the legacy {@code findRecipe}; first match by recipe id, see class javadoc). */
    private void refresh(RecipeManager recipes) {
        RecipeHolder<BcProgrammingRecipe> best = null;
        ItemStack input = this.inv.getResource(SLOT_INPUT).toStack((int) this.inv.getAmountAsLong(SLOT_INPUT));
        if (!input.isEmpty()) {
            for (RecipeHolder<?> holder : recipes.getRecipes()) {
                if (holder.value() instanceof BcProgrammingRecipe recipe
                    && recipe.input.ingredient().test(input)
                    && (best == null || holder.id().identifier().compareTo(best.id().identifier()) < 0)) {
                    best = typed(holder);
                }
            }
        }
        this.active = best;
    }

    /** Narrows a scanned holder after the {@code instanceof} filter above (value checked non-null there). */
    @SuppressWarnings("unchecked")
    private static RecipeHolder<BcProgrammingRecipe> typed(RecipeHolder<?> holder) {
        return (RecipeHolder<BcProgrammingRecipe>) holder;
    }

    @Override
    protected long currentTarget() {
        boolean outputEmpty = this.inv.getAmountAsLong(SLOT_OUTPUT) == 0;
        return this.active == null ? 0 : BcSiliconMachineLogic.programmingTarget(true, outputEmpty,
            BcSiliconMachineLogic.sliceCost(this.active.value().energyCost));
    }

    @Override
    protected void craft(ServerLevel level) {
        RecipeHolder<BcProgrammingRecipe> holder = this.active;
        if (holder == null) {
            return;
        }
        BcProgrammingRecipe recipe = holder.value();
        ItemStack out = recipe.output.create();
        // the legacy craft: consume the input board (the legacy extracted output-count boards), write the programmed
        // output, reset the power; the output write is machine-internal (the slot's isValid only gates inserts)
        try (Transaction transaction = Transaction.openRoot()) {
            long available = this.inv.getAmountAsLong(SLOT_INPUT);
            if (available < out.getCount()) {
                return; // not enough input stacks to consume: nothing happens this tick (defensive, boards are 1)
            }
            this.inv.extract(SLOT_INPUT, this.inv.getResource(SLOT_INPUT), out.getCount(), transaction);
            if (this.inv.getAmountAsLong(SLOT_OUTPUT) != 0) {
                return; // the output got occupied since the target was computed: retry once it frees up
            }
            transaction.commit();
        }
        this.inv.set(SLOT_OUTPUT, ItemResource.of(out), out.getCount());
        this.power = 0;
        LOGGER.info("[M417] programming craft @ {}: recipe={} cost={}µJ (slice {}µMJ) input {} -> output {} x{},"
            + " power reset to 0µMJ", this.worldPosition, holder.id().identifier(), recipe.energyCost,
            BcSiliconMachineLogic.sliceCost(recipe.energyCost), recipe.input.ingredient(), out.getItem(), out.getCount());
        this.syncToClients();
    }

    /** Per-tick driver wired through {@code ProgrammingTableBlock#getTicker}. */
    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state,
        ProgrammingTableBlockEntity table) {
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
