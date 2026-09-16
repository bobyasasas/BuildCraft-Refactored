/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory.recipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import buildcraft.factory.BuildCraftFactory;

/**
 * Recipe serializer/type registration for buildcraftfactory (task M2.10): the heat exchange pair
 * ({@code heat_exchange/heatable} + {@code heat_exchange/coolable}, slash-bearing ids exactly like the legacy
 * {@code type} strings in the JSONs) and the distiller. 26.1.2 {@link RecipeSerializer} shape verified against the
 * local 26.1.2.109 sources; the heatable/coolable pair shares one recipe class with kind-bound codecs (see
 * {@link BcHeatExchangeRecipe#mapCodec}).
 */
public final class BcFactoryRecipes {

    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister
        .create(Registries.RECIPE_TYPE, BuildCraftFactory.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister
        .create(Registries.RECIPE_SERIALIZER, BuildCraftFactory.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<BcHeatExchangeRecipe>> HEAT_EXCHANGE_TYPE = TYPES
        .register("heat_exchange", () -> new RecipeType<>() {});
    public static final DeferredHolder<RecipeType<?>, RecipeType<BcDistillationRecipe>> DISTILLATION_TYPE = TYPES
        .register("distillation", () -> new RecipeType<>() {});

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BcHeatExchangeRecipe>> HEATABLE_SERIALIZER = SERIALIZERS
        .register("heat_exchange/heatable",
            () -> new RecipeSerializer<>(BcHeatExchangeRecipe.mapCodec(BcHeatExchangeRecipe.Kind.HEATABLE),
                BcHeatExchangeRecipe.STREAM_CODEC));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BcHeatExchangeRecipe>> COOLABLE_SERIALIZER = SERIALIZERS
        .register("heat_exchange/coolable",
            () -> new RecipeSerializer<>(BcHeatExchangeRecipe.mapCodec(BcHeatExchangeRecipe.Kind.COOLABLE),
                BcHeatExchangeRecipe.STREAM_CODEC));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BcDistillationRecipe>> DISTILLATION_SERIALIZER = SERIALIZERS
        .register("distillation",
            () -> new RecipeSerializer<>(BcDistillationRecipe.MAP_CODEC, BcDistillationRecipe.STREAM_CODEC));

    private BcFactoryRecipes() {
    }
}
