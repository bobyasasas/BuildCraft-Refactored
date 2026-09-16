/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.silicon.recipe;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
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
 * Assembly table recipe (M2.10 port of the legacy {@code buildcraft.lib.recipe.assembly} family, serializer
 * {@code buildcraftsilicon:assembly}; the 69 buildcraftsilicon + 16 buildcrafttransport JSONs share it).
 * Two sub-types exactly like the legacy {@code EnumAssemblyRecipeType}:
 * <ul>
 * <li>{@code BASIC}: inputs + micro-joule cost + one output stack (legacy NBT is dropped by the converter — the
 * 26.1.2 items are data-component placeholders);</li>
 * <li>{@code FACADE}: the single parameter-less facade recipe (legacy
 * {@code AssemblyRecipeRegistry.FACADE_ASSEMBLY_RECIPE}); real facade behaviour ports with the silicon machines.</li>
 * </ul>
 * JSON field names are byte-compatible with the 1.20.1 datagen output except the ingredient idioms.
 */
public final class BcAssemblyRecipe extends BcMachineRecipe implements Recipe<RecipeInput> {

    public enum SubType {
        BASIC, FACADE;

        public static final Codec<SubType> CODEC = Codec.STRING.comapFlatMap(
            name -> {
                try {
                    return DataResult.success(SubType.valueOf(name));
                } catch (IllegalArgumentException e) {
                    return DataResult.error(() -> "Unknown assembly subType '" + name + "'");
                }
            }, SubType::name);
    }

    public static final MapCodec<BcAssemblyRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance
        .group(
            SubType.CODEC.optionalFieldOf("subType", SubType.BASIC).forGetter(recipe -> recipe.subType),
            Codec.LONG.optionalFieldOf("requiredMicroJoules", 0L).forGetter(recipe -> recipe.requiredMicroJoules),
            BcIngredientStack.Codecs.CODEC.listOf().optionalFieldOf("requiredStacks", List.of())
                .forGetter(recipe -> recipe.requiredStacks),
            ItemStackTemplate.CODEC.optionalFieldOf("output").forGetter(recipe -> recipe.output)
        ).apply(instance, BcAssemblyRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BcAssemblyRecipe> STREAM_CODEC = StreamCodec
        .composite(
            ByteBufCodecs.STRING_UTF8.map(SubType::valueOf, SubType::name), recipe -> recipe.subType,
            ByteBufCodecs.VAR_LONG, recipe -> recipe.requiredMicroJoules,
            BcIngredientStack.Codecs.STREAM_CODEC.apply(ByteBufCodecs.list()), recipe -> recipe.requiredStacks,
            ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC), recipe -> recipe.output,
            BcAssemblyRecipe::new
        );

    public final SubType subType;
    public final long requiredMicroJoules;
    public final List<BcIngredientStack> requiredStacks;
    public final Optional<ItemStackTemplate> output;

    public BcAssemblyRecipe(SubType subType, long requiredMicroJoules, List<BcIngredientStack> requiredStacks,
            Optional<ItemStackTemplate> output) {
        this.subType = subType;
        this.requiredMicroJoules = requiredMicroJoules;
        this.requiredStacks = requiredStacks;
        this.output = output;
    }

    @Override
    public RecipeSerializer<BcAssemblyRecipe> getSerializer() {
        return BcSiliconRecipes.ASSEMBLY_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<RecipeInput>> getType() {
        return BcSiliconRecipes.ASSEMBLY_TYPE.get();
    }
}
