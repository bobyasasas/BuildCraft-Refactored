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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import buildcraft.lib.recipe.BcFluidAmount;
import buildcraft.lib.recipe.BcMachineRecipe;

/**
 * Cooling tower coolant recipe (M2.10 port of the legacy {@code buildcraft.lib.recipe.coolant} family, serializer
 * {@code buildcraftenergy:coolant}). Two kinds, exactly like the legacy {@code EnumCoolantType}: a fluid coolant with
 * its {@code degreesCoolingPerMb}, or a solid coolant ({@code solid} item ingredient — converted to the 26.1.2
 * ingredient string idioms by the M2.10 converter) with its per-item {@code multiplier}.
 */
public final class BcCoolantRecipe extends BcMachineRecipe implements Recipe<RecipeInput> {

    public static final MapCodec<BcCoolantRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance
        .group(
            Codec.STRING.fieldOf("coolantType").forGetter(recipe -> recipe.coolantType),
            BcFluidAmount.Codecs.MAP_CODEC.fieldOf("fluid").forGetter(recipe -> recipe.fluid),
            Codec.FLOAT.optionalFieldOf("degreesCoolingPerMb").forGetter(recipe -> recipe.degreesCoolingPerMb),
            Codec.FLOAT.optionalFieldOf("multiplier").forGetter(recipe -> recipe.multiplier),
            Ingredient.CODEC.optionalFieldOf("solid").forGetter(recipe -> recipe.solid)
        ).apply(instance, BcCoolantRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BcCoolantRecipe> STREAM_CODEC = StreamCodec
        .composite(
            ByteBufCodecs.STRING_UTF8, recipe -> recipe.coolantType,
            BcFluidAmount.Codecs.STREAM_CODEC, recipe -> recipe.fluid,
            ByteBufCodecs.optional(ByteBufCodecs.FLOAT), recipe -> recipe.degreesCoolingPerMb,
            ByteBufCodecs.optional(ByteBufCodecs.FLOAT), recipe -> recipe.multiplier,
            ByteBufCodecs.optional(Ingredient.CONTENTS_STREAM_CODEC), recipe -> recipe.solid,
            BcCoolantRecipe::new
        );

    public final String coolantType;
    public final BcFluidAmount fluid;
    public final Optional<Float> degreesCoolingPerMb;
    public final Optional<Float> multiplier;
    public final Optional<Ingredient> solid;

    public BcCoolantRecipe(String coolantType, BcFluidAmount fluid, Optional<Float> degreesCoolingPerMb,
            Optional<Float> multiplier, Optional<Ingredient> solid) {
        this.coolantType = coolantType;
        this.fluid = fluid;
        this.degreesCoolingPerMb = degreesCoolingPerMb;
        this.multiplier = multiplier;
        this.solid = solid;
    }

    @Override
    public RecipeSerializer<BcCoolantRecipe> getSerializer() {
        return BcEnergyRecipes.COOLANT_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<RecipeInput>> getType() {
        return BcEnergyRecipes.COOLANT_TYPE.get();
    }
}
