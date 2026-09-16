/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.silicon.recipe;

import com.mojang.serialization.MapCodec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import buildcraft.lib.recipe.BcMachineRecipe;

/**
 * Facade swap recipe (M2.10 port of the legacy {@code buildcraft.silicon.recipe.FacadeSwapRecipe} singleton,
 * serializer {@code buildcraftsilicon:facade_swap}, the single JSON {@code buildcraftsilicon:recipe/facade_swap/
 * facade_swap.json} is {@code {"type": ...}} only). The 26.1.2 port keeps the parameter-less singleton; the actual
 * swap logic ports with the silicon machines (it needs the facade state manager).
 */
public final class BcFacadeSwapRecipe extends BcMachineRecipe implements Recipe<RecipeInput> {

    public static final BcFacadeSwapRecipe INSTANCE = new BcFacadeSwapRecipe();

    /** The JSON has no fields besides {@code type}; both codecs ignore any remaining content. */
    public static final MapCodec<BcFacadeSwapRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);

    public static final StreamCodec<RegistryFriendlyByteBuf, BcFacadeSwapRecipe> STREAM_CODEC = StreamCodec
        .unit(INSTANCE);

    private BcFacadeSwapRecipe() {
    }

    @Override
    public RecipeSerializer<BcFacadeSwapRecipe> getSerializer() {
        return BcSiliconRecipes.FACADE_SWAP_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<RecipeInput>> getType() {
        return BcSiliconRecipes.FACADE_SWAP_TYPE.get();
    }
}
