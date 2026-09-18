/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidType;
import org.jspecify.annotations.Nullable;
import buildcraft.factory.BcFactoryBlockEntities;
import buildcraft.factory.recipe.BcDistillationRecipe;
import buildcraft.lib.recipe.BcFluidAmount;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * M4.7 distiller block entity: a minimal but usable port of the legacy {@code TileDistiller_BC8} &mdash; an input tank
 * plus the gas and liquid output tanks (each 4 buckets, the legacy sizes), refilled by one
 * {@link BcDistillationRecipe} batch per {@link #PROCESS_INTERVAL} ticks whenever the input holds a distillable fluid
 * and both outputs have room.
 *
 * <p>Deliberate v1 trims (both inherited from the recipe JSONs, which carry the legacy economics): the legacy machine
 * charges micro-joules per batch ({@code powerRequired}); this slice runs unpowered &mdash; no MJ battery exists to
 * charge yet. There is no GUI and no power gauge; the three tank windows are the whole interface (the M4.7 renderer).
 *
 * <p>Side layout (the legacy per-side capability routing): horizontal sides and the null side reach the input tank
 * (fill only), the top face the gas output and the bottom face the liquid output (drain only) &mdash; buckets work
 * through {@code DistillerBlock}'s right-click interaction.
 */
public class DistillerBlockEntity extends BlockEntity {

    /** One tank's size: the legacy {@code TileDistiller_BC8} 4-bucket tanks. */
    public static final int TANK_CAPACITY = 4 * FluidType.BUCKET_VOLUME;
    /** Ticks between batch attempts (the v1 pacing stand-in for the legacy MJ draw: the legacy machine ran one small
     * recipe batch every couple of ticks while powered, so the unpowered v1 keeps that rate without a battery). */
    public static final int PROCESS_INTERVAL = 5;

    /** The input tank: external access is fill-only (the machine drains it internally). */
    private final BcFactoryFluidHandler tankIn = new BcFactoryFluidHandler(this::pushToClients, TANK_CAPACITY) {
        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return 0;
        }
    };
    /** The gas output: external access is drain-only. */
    private final BcFactoryFluidHandler tankGasOut = new BcFactoryFluidHandler(this::pushToClients, TANK_CAPACITY) {
        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return 0;
        }
    };
    /** The liquid output: external access is drain-only. */
    private final BcFactoryFluidHandler tankLiquidOut = new BcFactoryFluidHandler(this::pushToClients, TANK_CAPACITY) {
        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return 0;
        }
    };

    /** Ticks until the next batch attempt (0 = attempt now). */
    private int cooldown;

    public DistillerBlockEntity(BlockPos pos, BlockState state) {
        super(BcFactoryBlockEntities.DISTILLER.value(), pos, state);
    }

    public BcFactoryFluidHandler getTankIn() {
        return this.tankIn;
    }

    public BcFactoryFluidHandler getTankGasOut() {
        return this.tankGasOut;
    }

    public BcFactoryFluidHandler getTankLiquidOut() {
        return this.tankLiquidOut;
    }

    /** The capability view: sides reach exactly one tank each (see class javadoc). */
    public BcFactoryFluidHandler getFluidHandler(@Nullable Direction side) {
        if (side == Direction.UP) {
            return this.tankGasOut;
        }
        if (side == Direction.DOWN) {
            return this.tankLiquidOut;
        }
        return this.tankIn;
    }

    /** The per-tick batch driver (wired through {@code DistillerBlock#getTicker}). */
    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, DistillerBlockEntity distiller) {
        if (distiller.cooldown > 0) {
            distiller.cooldown--;
        } else if (distiller.processBatch(level.recipeAccess())) {
            distiller.cooldown = PROCESS_INTERVAL;
        }
    }

    /**
     * Runs one recipe batch if the input and both output tanks allow it; the move itself writes straight through
     * {@code set} (the {@code BcMachineFluids#canDistill} predicate guarantees every write fits, so no transaction rollback is
     * needed) and the handler's change hook pushes the new contents to clients.
     *
     * @return true when a batch ran.
     */
    private boolean processBatch(RecipeManager recipes) {
        FluidStack inFluid = this.tankIn.getFluidStack();
        if (inFluid.isEmpty()) {
            return false;
        }
        BcDistillationRecipe recipe = findRecipe(recipes, inFluid.getFluid());
        if (recipe == null) {
            return false;
        }
        long inAmount = this.tankIn.getAmountAsLong(0);
        if (!BcMachineFluids.canDistill(inAmount, recipe.in,//
            this.tankGasOut.roomFor(fluidResource(recipe.outGas)), recipe.outGas,//
            this.tankLiquidOut.roomFor(fluidResource(recipe.outLiquid)), recipe.outLiquid)) {
            return false;
        }
        this.tankIn.set(0, this.tankIn.getResource(0), (int) (inAmount - recipe.in.amount()));
        produce(this.tankGasOut, recipe.outGas);
        produce(this.tankLiquidOut, recipe.outLiquid);
        return true;
    }

    /**
     * Writes one recipe output into an output tank (the caller guarantees the room); zero-amount outputs (the
     * {@code minecraft:empty} payloads) produce nothing.
     */
    private static void produce(BcFactoryFluidHandler tank, BcFluidAmount out) {
        if (out.amount() == 0) {
            return;
        }
        FluidResource current = tank.getResource(0);
        FluidResource next = current.isEmpty() ? FluidResource.of(out.fluid()) : current;
        tank.set(0, next, (int) (tank.getAmountAsLong(0) + out.amount()));
    }

    /** The recipes' fluid payload as a handler lookup resource ({@code minecraft:empty} comes back empty). */
    private static FluidResource fluidResource(BcFluidAmount out) {
        return FluidResource.of(out.fluid());
    }

    /** The distillation recipe for one input fluid, or null ({@code RecipeManager#byType} without an input wrapper). */
    public static @Nullable BcDistillationRecipe findRecipe(RecipeManager recipes, Fluid fluid) {
        for (RecipeHolder<?> holder : recipes.getRecipes()) {
            if (holder.value() instanceof BcDistillationRecipe recipe && recipe.in.fluid() == fluid) {
                return recipe;
            }
        }
        return null;
    }

    /** Marks the state dirty and pushes it to clients (Beacon-pattern update tag, see class javadoc). */
    private void pushToClients() {
        this.setChanged();
        if (this.level instanceof ServerLevel serverLevel) {
            BlockState state = this.getBlockState();
            serverLevel.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    // ---------------------------------------------------------------------
    // Persistence + client sync (Beacon pattern)
    // ---------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.tankIn.serialize(output.child("bc_tank_in"));
        this.tankGasOut.serialize(output.child("bc_tank_gas_out"));
        this.tankLiquidOut.serialize(output.child("bc_tank_liquid_out"));
        output.putInt("bc_cooldown", this.cooldown);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.tankIn.deserialize(input.childOrEmpty("bc_tank_in"));
        this.tankGasOut.deserialize(input.childOrEmpty("bc_tank_gas_out"));
        this.tankLiquidOut.deserialize(input.childOrEmpty("bc_tank_liquid_out"));
        this.cooldown = input.getIntOr("bc_cooldown", 0);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
