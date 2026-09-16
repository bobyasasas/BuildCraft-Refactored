/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.silicon.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import buildcraft.lib.recipe.BcIngredientStack;
import buildcraft.lib.recipe.BcMachineRecipe;

/**
 * Programmer recipe (M2.10 port of the legacy {@code buildcraft.lib.recipe.programming} family, serializer
 * {@code buildcraftsilicon:programming}; the 17 JSONs live under the buildcraftrobotics namespace, exactly like
 * 1.20.1). One board input + energy cost + the programmed board output.
 */
public final class BcProgrammingRecipe extends BcMachineRecipe implements Recipe<RecipeInput> {

    public static final MapCodec<BcProgrammingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance
        .group(
            Codec.LONG.fieldOf("energyCost").forGetter(recipe -> recipe.energyCost),
            BcIngredientStack.Codecs.CODEC.fieldOf("input").forGetter(recipe -> recipe.input),
            ItemStackTemplate.CODEC.fieldOf("output").forGetter(recipe -> recipe.output)
        ).apply(instance, BcProgrammingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BcProgrammingRecipe> STREAM_CODEC = StreamCodec
        .composite(
            ByteBufCodecs.VAR_LONG, recipe -> recipe.energyCost,
            BcIngredientStack.Codecs.STREAM_CODEC, recipe -> recipe.input,
            ItemStackTemplate.STREAM_CODEC, recipe -> recipe.output,
            BcProgrammingRecipe::new
        );

    public final long energyCost;
    public final BcIngredientStack input;
    public final ItemStackTemplate output;

    public BcProgrammingRecipe(long energyCost, BcIngredientStack input, ItemStackTemplate output) {
        this.energyCost = energyCost;
        this.input = input;
        this.output = output;
    }

    @Override
    public RecipeSerializer<BcProgrammingRecipe> getSerializer() {
        return BcSiliconRecipes.PROGRAMMING_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<RecipeInput>> getType() {
        return BcSiliconRecipes.PROGRAMMING_TYPE.get();
    }
}
