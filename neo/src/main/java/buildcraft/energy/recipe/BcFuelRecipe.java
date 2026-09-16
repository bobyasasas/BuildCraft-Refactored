/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.energy.recipe;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import buildcraft.lib.recipe.BcFluidAmount;
import buildcraft.lib.recipe.BcMachineRecipe;

/**
 * Combustion engine fuel recipe (M2.10 port of the legacy {@code buildcraft.lib.recipe.fuel} family, serializer
 * {@code buildcraftenergy:fuel}). A fluid with its power-per-cycle and burn length; dirty fuels additionally carry the
 * residue fluid they degrade into. Byte-identical JSONs (the 9 files carry no item references).
 */
public final class BcFuelRecipe extends BcMachineRecipe implements Recipe<RecipeInput> {

    public static final MapCodec<BcFuelRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance
        .group(
            BcFluidAmount.Codecs.MAP_CODEC.fieldOf("fluid").forGetter(recipe -> recipe.fluid),
            Codec.LONG.fieldOf("powerPerCycle").forGetter(recipe -> recipe.powerPerCycle),
            Codec.INT.fieldOf("totalBurningTime").forGetter(recipe -> recipe.totalBurningTime),
            Codec.BOOL.optionalFieldOf("dirty", false).forGetter(recipe -> recipe.dirty),
            BcFluidAmount.Codecs.MAP_CODEC.codec().optionalFieldOf("residue").forGetter(recipe -> recipe.residue)
        ).apply(instance, BcFuelRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BcFuelRecipe> STREAM_CODEC = StreamCodec
        .composite(
            BcFluidAmount.Codecs.STREAM_CODEC, recipe -> recipe.fluid,
            ByteBufCodecs.VAR_LONG, recipe -> recipe.powerPerCycle,
            ByteBufCodecs.VAR_INT, recipe -> recipe.totalBurningTime,
            ByteBufCodecs.BOOL, recipe -> recipe.dirty,
            ByteBufCodecs.optional(BcFluidAmount.Codecs.STREAM_CODEC), recipe -> recipe.residue,
            BcFuelRecipe::new
        );

    public final BcFluidAmount fluid;
    public final long powerPerCycle;
    public final int totalBurningTime;
    public final boolean dirty;
    public final Optional<BcFluidAmount> residue;

    public BcFuelRecipe(BcFluidAmount fluid, long powerPerCycle, int totalBurningTime, boolean dirty,
            Optional<BcFluidAmount> residue) {
        this.fluid = fluid;
        this.powerPerCycle = powerPerCycle;
        this.totalBurningTime = totalBurningTime;
        this.dirty = dirty;
        this.residue = residue;
    }

    @Override
    public RecipeSerializer<BcFuelRecipe> getSerializer() {
        return BcEnergyRecipes.FUEL_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<RecipeInput>> getType() {
        return BcEnergyRecipes.FUEL_TYPE.get();
    }
}
