/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.silicon.recipe;

import java.util.List;

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
 * Integration table recipe (M2.10 port of the legacy {@code buildcraft.lib.recipe.integration} family, serializer
 * {@code buildcraftsilicon:integration}): one center stack, surrounding requirement stacks, micro-joule cost and the
 * output (robot boards/plugs).
 */
public final class BcIntegrationRecipe extends BcMachineRecipe implements Recipe<RecipeInput> {

    public static final MapCodec<BcIntegrationRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance
        .group(
            BcIngredientStack.Codecs.CODEC.fieldOf("centerStack").forGetter(recipe -> recipe.centerStack),
            BcIngredientStack.Codecs.CODEC.listOf().optionalFieldOf("requirements", List.of())
                .forGetter(recipe -> recipe.requirements),
            ItemStackTemplate.CODEC.fieldOf("output").forGetter(recipe -> recipe.output),
            Codec.LONG.fieldOf("requiredMicroJoules").forGetter(recipe -> recipe.requiredMicroJoules)
        ).apply(instance, BcIntegrationRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BcIntegrationRecipe> STREAM_CODEC = StreamCodec
        .composite(
            BcIngredientStack.Codecs.STREAM_CODEC, recipe -> recipe.centerStack,
            BcIngredientStack.Codecs.STREAM_CODEC.apply(ByteBufCodecs.list()), recipe -> recipe.requirements,
            ItemStackTemplate.STREAM_CODEC, recipe -> recipe.output,
            ByteBufCodecs.VAR_LONG, recipe -> recipe.requiredMicroJoules,
            BcIntegrationRecipe::new
        );

    public final BcIngredientStack centerStack;
    public final List<BcIngredientStack> requirements;
    public final ItemStackTemplate output;
    public final long requiredMicroJoules;

    public BcIntegrationRecipe(BcIngredientStack centerStack, List<BcIngredientStack> requirements,
            ItemStackTemplate output, long requiredMicroJoules) {
        this.centerStack = centerStack;
        this.requirements = requirements;
        this.output = output;
        this.requiredMicroJoules = requiredMicroJoules;
    }

    @Override
    public RecipeSerializer<BcIntegrationRecipe> getSerializer() {
        return BcSiliconRecipes.INTEGRATION_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<RecipeInput>> getType() {
        return BcSiliconRecipes.INTEGRATION_TYPE.get();
    }
}
