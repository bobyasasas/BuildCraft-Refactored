/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory.blockentity;

import java.util.Collection;
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
import buildcraft.factory.recipe.BcHeatExchangeRecipe;
import buildcraft.lib.recipe.BcFluidAmount;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * M4.7 heat exchanger block entity: a single-block stand-in for the legacy multi-block {@code TileHeatExchange} with
 * the same two 2-bucket tanks and the same recipe families &mdash; one {@link BcHeatExchangeRecipe} batch (heatable
 * or coolable, whichever matches the input tank) moves {@code in.amount} mB from the input to the output tank every
 * {@link #PROCESS_INTERVAL} ticks.
 *
 * <p>Deliberate v1 trims: the legacy exchanger is a 3-block tower (start/middle/end sections, synced as one machine);
 * this slice is one block whose renderer draws the {@code heat_exchange_static.jsonbc} geometry (MIDDLE section)
 * instead. The heat values ({@code heatFrom}/{@code heatTo}) select the recipe but drive no heat simulation yet.
 * Recipes whose output is {@code minecraft:empty} (the water/lava sinks) consume their input and produce nothing.
 *
 * <p>Side layout: the bottom face reaches the input tank (fill only), the top face the output tank (drain only);
 * buckets work through {@code HeatExchangeBlock}'s right-click interaction.
 */
public class HeatExchangeBlockEntity extends BlockEntity {

    /** One tank's size: the legacy {@code TileHeatExchange} 2-bucket tanks. */
    public static final int TANK_CAPACITY = 2 * FluidType.BUCKET_VOLUME;
    /** Ticks between batch attempts (the v1 pacing stand-in for the legacy exchange delay, which moved one small
     * recipe batch every couple of ticks). */
    public static final int PROCESS_INTERVAL = 5;

    /** The input tank: external access is fill-only (the machine drains it internally). */
    private final BcFactoryFluidHandler tankIn = new BcFactoryFluidHandler(this::pushToClients, TANK_CAPACITY) {
        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return 0;
        }
    };
    /** The output tank: external access is drain-only. */
    private final BcFactoryFluidHandler tankOut = new BcFactoryFluidHandler(this::pushToClients, TANK_CAPACITY) {
        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return 0;
        }
    };

    /** Ticks until the next batch attempt (0 = attempt now). */
    private int cooldown;

    public HeatExchangeBlockEntity(BlockPos pos, BlockState state) {
        super(BcFactoryBlockEntities.HEAT_EXCHANGE.value(), pos, state);
    }

    public BcFactoryFluidHandler getTankIn() {
        return this.tankIn;
    }

    public BcFactoryFluidHandler getTankOut() {
        return this.tankOut;
    }

    /** The capability view: bottom fills, top drains. */
    public @Nullable BcFactoryFluidHandler getFluidHandler(@Nullable Direction side) {
        if (side == Direction.UP) {
            return this.tankOut;
        }
        if (side == Direction.DOWN) {
            return this.tankIn;
        }
        return null;
    }

    /** The per-tick batch driver (wired through {@code HeatExchangeBlock#getTicker}). */
    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state,
        HeatExchangeBlockEntity exchanger) {
        if (exchanger.cooldown > 0) {
            exchanger.cooldown--;
        } else if (exchanger.processBatch(level.recipeAccess())) {
            exchanger.cooldown = PROCESS_INTERVAL;
        }
    }

    /**
     * Runs one heatable-or-coolable batch if the tanks allow it; the move writes straight through {@code set} (the
     * {@code BcMachineFluids#canConvert} predicate guarantees it fits) and the handler's change hook pushes the contents to clients.
     *
     * @return true when a batch ran.
     */
    private boolean processBatch(RecipeManager recipes) {
        FluidResource inResource = this.tankIn.getResource(0);
        if (inResource.isEmpty()) {
            return false;
        }
        BcHeatExchangeRecipe recipe = findRecipe(recipes, inResource.getFluid());
        if (recipe == null) {
            return false;
        }
        long inAmount = this.tankIn.getAmountAsLong(0);
        if (!BcMachineFluids.canConvert(inAmount, recipe.in, this.tankOut.roomFor(fluidResource(recipe.out)), recipe.out)) {
            return false;
        }
        this.tankIn.set(0, inResource, (int) (inAmount - recipe.in.amount()));
        if (recipe.out.amount() > 0) {
            FluidResource current = this.tankOut.getResource(0);
            FluidResource next = current.isEmpty() ? FluidResource.of(recipe.out.fluid()) : current;
            this.tankOut.set(0, next, (int) (this.tankOut.getAmountAsLong(0) + recipe.out.amount()));
        }
        return true;
    }

    /** All heat exchange recipes for one input fluid, both directions considered (heatable wins ties). */
    public static @Nullable BcHeatExchangeRecipe findRecipe(RecipeManager recipes, Fluid fluid) {
        Collection<RecipeHolder<?>> holders = recipes.getRecipes();
        BcHeatExchangeRecipe coolable = null;
        for (RecipeHolder<?> holder : holders) {
            if (holder.value() instanceof BcHeatExchangeRecipe recipe && recipe.in.fluid() == fluid) {
                if (recipe.kind == BcHeatExchangeRecipe.Kind.HEATABLE) {
                    return recipe;
                }
                coolable = recipe;
            }
        }
        return coolable;
    }

    /** The recipes' fluid payload as a handler lookup resource ({@code minecraft:empty} comes back empty). */
    private static FluidResource fluidResource(BcFluidAmount out) {
        return FluidResource.of(out.fluid());
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
        this.tankOut.serialize(output.child("bc_tank_out"));
        output.putInt("bc_cooldown", this.cooldown);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.tankIn.deserialize(input.childOrEmpty("bc_tank_in"));
        this.tankOut.deserialize(input.childOrEmpty("bc_tank_out"));
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
