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
import buildcraft.silicon.recipe.BcAssemblyRecipe;

/**
 * M4.16 assembly table block entity (legacy counterpart:
 * {@code buildcraft.silicon.tile.TileAssemblyTable}, id {@code buildcraftsilicon:assembly_table} unchanged).
 *
 * <p><b>Legacy audit and the slice mapping:</b>
 * <ul>
 * <li><b>Inventory</b> &mdash; legacy: {@code ItemHandlerSimple("inv", 3 * 4)} on {@code EnumAccess.BOTH} from every
 * pipe part. Slice: a 12-slot {@link ItemStacksResourceHandler} exposed through the 26.1.2 item capability on every
 * face (insert and extract).</li>
 * <li><b>Recipes</b> &mdash; legacy: the {@code AssemblyRecipeRegistry} scanned per tick, per-output instruction
 * states ({@code POSSIBLE/SAVED/SAVED_ENOUGH[_ACTIVE]}) with a GUI to pick the active one. Slice: the migrated
 * {@link BcAssemblyRecipe}s ({@code SubType.BASIC} with an output) are scanned per tick exactly like the legacy
 * {@code updateRecipes}; the "active" instruction is the first recipe whose full input set sits in the inventory
 * (the legacy {@code SAVED_ENOUGH} rule) and whose output has room &mdash; the legacy auto-pick that
 * {@code activateNextRecipe} performs when no user selection exists. The per-output state machine and its GUI
 * packet are second-batch work; the FACADE sub-type (parameter-less facade recipe) stays inert until facades
 * migrate.</li>
 * <li><b>Power</b> &mdash; legacy: {@code getTarget()} = active recipe's {@code requiredMicroJoules}; craft at
 * {@code power >= target}. Slice: the same rule through the base class, with the JSON cost on the
 * &times;10<sup>&minus;4</sup> scale ({@link BcSiliconMachineLogic#sliceCost}).</li>
 * <li><b>Craft</b> &mdash; legacy: non-precise {@code extract} of the inputs, output pushed to the best acceptor.
 * Slice: input consumption + output insertion run in one transaction against the own inventory (if the output does
 * not fit, nothing is consumed and the recipe simply does not run), keeping the crafted chips reachable through the
 * same capability they were fed through.</li>
 * </ul>
 *
 * <p>Crafts are logged with the recipe economics ({@code [M416] assembly craft ...}) as the milestone's server-side
 * evidence trail.
 */
public class AssemblyTableBlockEntity extends LaserTableBaseBlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** The legacy {@code TileAssemblyTable} inventory size (3 &times; 4). */
    public static final int INV_SLOTS = 3 * 4;

    /** The one input/output inventory (legacy {@code inv}, see class javadoc). */
    private final ItemStacksResourceHandler inv = new ItemStacksResourceHandler(INV_SLOTS) {
        @Override
        protected void onContentsChanged(int index, ItemStack previous) {
            AssemblyTableBlockEntity.this.syncToClients();
        }
    };

    /** The recipe picked by this tick's scan (the legacy active instruction), null while nothing matches. */
    @Nullable
    private RecipeHolder<BcAssemblyRecipe> active;

    public AssemblyTableBlockEntity(BlockPos pos, BlockState state) {
        super(BcSiliconBlockEntities.ASSEMBLY_TABLE.value(), pos, state);
    }

    public ItemStacksResourceHandler getInv() {
        return this.inv;
    }

    /** The capability view: every side reaches the whole inventory (legacy {@code EnumPipePart.VALUES} routing). */
    public ResourceHandler<ItemResource> getItemHandler(@Nullable Direction side) {
        return this.inv;
    }

    @Nullable
    public RecipeHolder<BcAssemblyRecipe> getActive() {
        return this.active;
    }

    /** Rescans the recipes for this tick's active pick (the legacy {@code updateRecipes}, auto-pick form). */
    private void refresh(RecipeManager recipes) {
        this.active = null;
        for (RecipeHolder<?> holder : recipes.getRecipes()) {
            if (holder.value() instanceof BcAssemblyRecipe recipe
                && recipe.subType == BcAssemblyRecipe.SubType.BASIC
                && recipe.output.isPresent()
                && this.inputsAvailable(recipe)
                && this.outputFits(recipe)) {
                this.active = typed(holder);
                return;
            }
        }
    }

    /** Narrows a scanned holder after the {@code instanceof} filter above (value checked non-null there). */
    @SuppressWarnings("unchecked")
    private static RecipeHolder<BcAssemblyRecipe> typed(RecipeHolder<?> holder) {
        return (RecipeHolder<BcAssemblyRecipe>) holder;
    }

    /** The legacy non-precise rule: every requirement draws its count from the inventory (extras allowed). */
    private boolean inputsAvailable(BcAssemblyRecipe recipe) {
        return BcSiliconMachineLogic.extract(slotAmounts(), counts(recipe),
            (slot, group) -> recipe.requiredStacks.get(group).ingredient()
                .test(this.inv.getResource(slot).toStack(1)),
            true, false, null);
    }

    /** The output must fit the inventory (slice stand-in for the legacy best-acceptor push). */
    private boolean outputFits(BcAssemblyRecipe recipe) {
        ItemStack out = recipe.output.get().create();
        ItemResource resource = ItemResource.of(out);
        try (Transaction transaction = Transaction.openRoot()) {
            int room = 0;
            for (int slot = 0; slot < this.inv.size(); slot++) {
                room += this.inv.insert(slot, resource, out.getCount(), transaction);
            }
            return room >= out.getCount();
        }
    }

    private static int[] counts(BcAssemblyRecipe recipe) {
        int[] counts = new int[recipe.requiredStacks.size()];
        for (int i = 0; i < counts.length; i++) {
            counts[i] = recipe.requiredStacks.get(i).count();
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
        RecipeHolder<BcAssemblyRecipe> holder = this.active;
        if (holder == null) {
            return;
        }
        BcAssemblyRecipe recipe = holder.value();
        ItemStack out = recipe.output.get().create();
        int[] taken = new int[this.inv.size()];
        if (!BcSiliconMachineLogic.extract(slotAmounts(), counts(recipe),
            (slot, group) -> recipe.requiredStacks.get(group).ingredient()
                .test(this.inv.getResource(slot).toStack(1)),
            true, false, taken)) {
            return;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            for (int slot = 0; slot < this.inv.size(); slot++) {
                if (taken[slot] > 0) {
                    this.inv.extract(slot, this.inv.getResource(slot), taken[slot], transaction);
                }
            }
            int inserted = 0;
            for (int slot = 0; slot < this.inv.size() && inserted < out.getCount(); slot++) {
                inserted += this.inv.insert(slot, ItemResource.of(out), out.getCount() - inserted, transaction);
            }
            if (inserted < out.getCount()) {
                return; // no room after all: leave everything untouched, try again next tick
            }
            transaction.commit();
        }
        long cost = currentTarget();
        this.power -= cost;
        LOGGER.info("[M416] assembly craft @ {}: recipe={} target={}µJ (slice {}µMJ) output={} x{}, power left {}µMJ",
            this.worldPosition, holder.id().identifier(), recipe.requiredMicroJoules, cost,
            out.getItem(), out.getCount(), this.power);
        this.syncToClients();
    }

    /** Per-tick driver wired through {@code AssemblyTableBlock#getTicker}. */
    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, AssemblyTableBlockEntity table) {
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
