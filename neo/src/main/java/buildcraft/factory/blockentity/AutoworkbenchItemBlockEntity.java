/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory.blockentity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import buildcraft.core.blockentity.MjReceiver;
import buildcraft.factory.BcFactoryBlockEntities;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * M4.16 item auto workbench block entity: a minimal port of the legacy {@code TileAutoWorkbenchBase} +
 * {@code TileAutoWorkbenchItems} pair under the unchanged id {@code buildcraftfactory:autoworkbench_item}. The machine
 * holds the legacy 3x3 crafting grid plus one result slot; every tick it matches the grid contents against the vanilla
 * crafting recipes and, when a recipe fits and the stored power covers it, consumes one item of each occupied grid slot
 * and moves the result into the output slot (legacy {@code WorkbenchCrafting#craft} behaviour).
 *
 * <p><b>Power (the legacy engine economics on the M2.2 slice &micro;MJ scale, &times;10&#8315;&#8308;):</b> the
 * workbench is a real {@link MjReceiver} (the {@code TileAutoWorkbenchBase} MJ redstone receiver): each craft costs
 * {@link #POWER_REQUIRED} &micro;MJ (legacy 40&nbsp;MJ), it self-charges at {@link #POWER_GEN_PASSIVE} &micro;MJ/tick
 * while a recipe waits (the legacy passive trickle that made an unpowered workbench finish a craft in 10 seconds) and
 * bleeds charge away while no recipe fits (the legacy {@code POWER_LOST} decay). A kinesis pipe can feed it through the
 * receiver, exactly like legacy. The pacing decisions live in the testable {@link AutoworkbenchLogic}.
 *
 * <p><b>Items:</b> automation reaches all ten slots through the item capability from every side: the nine grid slots
 * accept insertion (the legacy {@code invMaterials} {@code EnumAccess.INSERT} view), the output slot is extract-only
 * (the legacy {@code invResult} {@code EXTRACT} view). There is no GUI and no blueprint/phantom-filter grid yet: the
 * grid itself is the recipe input (v1 trim, the legacy phantom {@code invBlueprint} + {@code createFilters} balancing
 * is a GUI-era feature).
 *
 * <p><b>v1 trims (both javadoc-tracked):</b> no player advancement trigger (the legacy
 * {@code lazy_crafting} advancement), and container leftovers (e.g. the bucket of a cake recipe) are not returned
 * &mdash; standard 26.1.2 recipes consumed here are ingredient-only.
 *
 * <p><b>Client sync</b> is the milestone's {@code BeaconBlockEntity} pattern (the {@code TankBlockEntity} house
 * style): {@link #getUpdateTag} returns {@link #saveCustomOnly} and the handler's {@code onContentsChanged} pushes it
 * with {@code sendBlockUpdated}.
 */
public class AutoworkbenchItemBlockEntity extends BlockEntity implements MjReceiver {

    /** The legacy {@code TileAutoWorkbenchItems} grid: 3x3. */
    public static final int GRID_SLOTS = 9;
    /** The one result slot behind the grid (legacy {@code invResult}). */
    public static final int OUTPUT_SLOT = GRID_SLOTS;
    public static final int SLOTS = GRID_SLOTS + 1;

    /**
     * Power per craft in &micro;MJ: legacy {@code POWER_REQUIRED} = {@code (MjAPI.MJ / 5) * 20 * 10} = 40&nbsp;MJ,
     * scaled &times;10&#8315;&#8308; (see the class javadoc).
     */
    public static final long POWER_REQUIRED = 4_000;
    /**
     * Passive self-charge while a recipe waits, in &micro;MJ/tick: legacy {@code POWER_GEN_PASSIVE} =
     * {@code MjAPI.MJ / 5}, scaled &times;10&#8315;&#8308; &mdash; an unpowered craft still finishes in the legacy
     * 200 ticks.
     */
    public static final long POWER_GEN_PASSIVE = 20;
    /** Charge bleed per recipe-less tick in &micro;MJ: legacy {@code POWER_LOST} = {@code 10 * POWER_GEN_PASSIVE}, scaled. */
    public static final long POWER_LOST = 200;
    /** Every slot holds up to one stack (the legacy {@code ItemHandlerSimple} 64-slot capacities). */
    public static final int SLOT_CAPACITY = 64;

    /**
     * The ten-slot inventory: grid slots accept inserts, the output slot only extracts (see the class javadoc). The
     * machine's own craft writes through {@link #set}, which bypasses both restrictions exactly like the legacy
     * internal handler moves.
     */
    private final ItemStacksResourceHandler inv = new ItemStacksResourceHandler(SLOTS) {
        @Override
        protected int getCapacity(int index, ItemResource resource) {
            return SLOT_CAPACITY;
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            // external inserts land in the grid only; the output slot is extract-only
            return index < GRID_SLOTS;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return index >= OUTPUT_SLOT ? super.extract(index, resource, amount, transaction) : 0;
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            AutoworkbenchItemBlockEntity.this.pushToClients();
        }
    };

    /** Stored craft power, &micro;MJ (legacy {@code powerStored}). */
    private long powerStored;

    public AutoworkbenchItemBlockEntity(BlockPos pos, BlockState state) {
        super(BcFactoryBlockEntities.AUTOWORKBENCH_ITEM.value(), pos, state);
    }

    /** The full ten-slot view (capability target and evidence-rig read side). */
    public ItemStacksResourceHandler getInv() {
        return this.inv;
    }

    public long getPowerStored() {
        return this.powerStored;
    }

    /** The per-tick craft driver (wired through {@code AutoworkbenchItemBlock#getTicker}). */
    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, AutoworkbenchItemBlockEntity bench) {
        bench.tickCraft(level);
    }

    /**
     * One legacy {@code TileAutoWorkbenchBase#update} step: match the grid, then either craft, charge towards the
     * requirement, or bleed the stored charge (see {@link AutoworkbenchLogic}).
     */
    private void tickCraft(ServerLevel level) {
        CraftingInput input = this.buildInput();
        RecipeHolder<CraftingRecipe> holder = level.recipeAccess()
                .getRecipeFor(RecipeType.CRAFTING, input, level).orElse(null);
        if (holder == null) {
            this.powerStored = AutoworkbenchLogic.drain(this.powerStored, POWER_LOST);
            return;
        }
        CraftingRecipe recipe = holder.value();
        boolean outputRoom = this.fitsOutput(recipe.assemble(input));
        switch (AutoworkbenchLogic.plan(this.powerStored, POWER_REQUIRED, true, outputRoom)) {
            case CRAFT -> {
                if (this.craft(recipe, input)) {
                    this.powerStored -= POWER_REQUIRED;
                }
            }
            case CHARGE -> this.powerStored += POWER_GEN_PASSIVE;
            case IDLE -> this.powerStored = AutoworkbenchLogic.drain(this.powerStored, POWER_LOST);
        }
    }

    /** The grid contents as a 3x3 crafting input (live stacks, read-only for the recipe match). */
    private CraftingInput buildInput() {
        List<ItemStack> grid = new ArrayList<>(GRID_SLOTS);
        for (int i = 0; i < GRID_SLOTS; i++) {
            grid.add(this.inv.getResource(i).toStack((int) this.inv.getAmountAsLong(i)));
        }
        return CraftingInput.of(3, 3, grid);
    }

    /** True when {@code result} would fit the output slot (empty slot, or same item with room). */
    private boolean fitsOutput(ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }
        long current = this.inv.getAmountAsLong(OUTPUT_SLOT);
        if (current == 0) {
            return true;
        }
        ItemResource resource = ItemResource.of(result);
        return this.inv.matches(this.inv.getResource(OUTPUT_SLOT).toStack((int) current), resource)
                && current + result.getCount() <= SLOT_CAPACITY;
    }

    /**
     * Runs one craft: consumes one item of each occupied grid slot (the vanilla crafting consume shape) and moves the
     * assembled result into the output slot. The caller has verified the room, so the guarded re-check only guards
     * against a vanished result.
     */
    private boolean craft(CraftingRecipe recipe, CraftingInput input) {
        ItemStack result = recipe.assemble(input);
        if (!this.fitsOutput(result)) {
            return false;
        }
        for (int i = 0; i < GRID_SLOTS; i++) {
            long count = this.inv.getAmountAsLong(i);
            if (count > 0) {
                this.inv.set(i, this.inv.getResource(i), (int) count - 1);
            }
        }
        ItemResource outResource = ItemResource.of(result);
        long outCount = this.inv.getAmountAsLong(OUTPUT_SLOT);
        this.inv.set(OUTPUT_SLOT, outResource, (int) outCount + result.getCount());
        return true;
    }

    // ---------------------------------------------------------------------
    // MjReceiver (legacy IMjRedstoneReceiver semantics, see the interface javadoc)
    // ---------------------------------------------------------------------

    @Override
    public long getPowerRequested() {
        return Math.max(0, POWER_REQUIRED - this.powerStored);
    }

    @Override
    public long receivePower(long microJoules, boolean simulate) {
        long accepted = Math.min(microJoules, Math.max(0, POWER_REQUIRED - this.powerStored));
        if (!simulate && accepted > 0) {
            this.powerStored += accepted;
        }
        return microJoules - accepted;
    }

    // ---------------------------------------------------------------------
    // Persistence + client sync (Beacon pattern)
    // ---------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.inv.serialize(output.child("bc_inv"));
        output.putLong("bc_power", this.powerStored);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.inv.deserialize(input.childOrEmpty("bc_inv"));
        this.powerStored = input.getLongOr("bc_power", 0L);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Marks the state dirty and pushes it to clients (Beacon-pattern update tag, see class javadoc). */
    private void pushToClients() {
        this.setChanged();
        if (this.level instanceof ServerLevel serverLevel) {
            BlockState state = this.getBlockState();
            serverLevel.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }
}
