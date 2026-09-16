/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.energy.recipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import buildcraft.energy.BuildCraftEnergy;

/**
 * Recipe serializer/type registration for buildcraftenergy (task M2.10): combustion fuels and cooling tower coolants.
 * Ids match the legacy {@code type} strings byte-for-byte; the 26.1.2 {@link RecipeSerializer} shape was verified
 * against the local 26.1.2.109 sources.
 */
public final class BcEnergyRecipes {

    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister
        .create(Registries.RECIPE_TYPE, BuildCraftEnergy.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister
        .create(Registries.RECIPE_SERIALIZER, BuildCraftEnergy.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<BcFuelRecipe>> FUEL_TYPE = TYPES
        .register("fuel", () -> new RecipeType<>() {});
    public static final DeferredHolder<RecipeType<?>, RecipeType<BcCoolantRecipe>> COOLANT_TYPE = TYPES
        .register("coolant", () -> new RecipeType<>() {});

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BcFuelRecipe>> FUEL_SERIALIZER = SERIALIZERS
        .register("fuel", () -> new RecipeSerializer<>(BcFuelRecipe.MAP_CODEC, BcFuelRecipe.STREAM_CODEC));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BcCoolantRecipe>> COOLANT_SERIALIZER = SERIALIZERS
        .register("coolant", () -> new RecipeSerializer<>(BcCoolantRecipe.MAP_CODEC, BcCoolantRecipe.STREAM_CODEC));

    private BcEnergyRecipes() {
    }
}
