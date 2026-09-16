/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.silicon.recipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import buildcraft.silicon.BuildCraftSilicon;

/**
 * Recipe serializer/type registration for buildcraftsilicon (task M2.10). The 1.20.1 counterparts are the custom
 * serializer singletons of the assembly/programming/integration/facade-swap families ({@code AssemblyRecipeSerializer}
 * etc.); the 26.1.2 {@link RecipeSerializer} is a record of a {@link com.mojang.serialization.MapCodec} plus the
 * (deprecated but mandatory) {@link net.minecraft.network.codec.StreamCodec}, verified against the local 26.1.2.109
 * sources. IDs match the legacy {@code type} strings in the migrated JSONs byte-for-byte; note the serializers of the
 * programming recipes are also used by the buildcraftrobotics-namespace JSONs, exactly like 1.20.1.
 */
public final class BcSiliconRecipes {

    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister
        .create(Registries.RECIPE_TYPE, BuildCraftSilicon.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister
        .create(Registries.RECIPE_SERIALIZER, BuildCraftSilicon.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<BcAssemblyRecipe>> ASSEMBLY_TYPE = TYPES
        .register("assembly", () -> new RecipeType<>() {});
    public static final DeferredHolder<RecipeType<?>, RecipeType<BcProgrammingRecipe>> PROGRAMMING_TYPE = TYPES
        .register("programming", () -> new RecipeType<>() {});
    public static final DeferredHolder<RecipeType<?>, RecipeType<BcIntegrationRecipe>> INTEGRATION_TYPE = TYPES
        .register("integration", () -> new RecipeType<>() {});
    public static final DeferredHolder<RecipeType<?>, RecipeType<BcFacadeSwapRecipe>> FACADE_SWAP_TYPE = TYPES
        .register("facade_swap", () -> new RecipeType<>() {});

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BcAssemblyRecipe>> ASSEMBLY_SERIALIZER = SERIALIZERS
        .register("assembly", () -> new RecipeSerializer<>(BcAssemblyRecipe.MAP_CODEC, BcAssemblyRecipe.STREAM_CODEC));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BcProgrammingRecipe>> PROGRAMMING_SERIALIZER = SERIALIZERS
        .register("programming",
            () -> new RecipeSerializer<>(BcProgrammingRecipe.MAP_CODEC, BcProgrammingRecipe.STREAM_CODEC));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BcIntegrationRecipe>> INTEGRATION_SERIALIZER = SERIALIZERS
        .register("integration",
            () -> new RecipeSerializer<>(BcIntegrationRecipe.MAP_CODEC, BcIntegrationRecipe.STREAM_CODEC));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BcFacadeSwapRecipe>> FACADE_SWAP_SERIALIZER = SERIALIZERS
        .register("facade_swap",
            () -> new RecipeSerializer<>(BcFacadeSwapRecipe.MAP_CODEC, BcFacadeSwapRecipe.STREAM_CODEC));

    private BcSiliconRecipes() {
    }
}
