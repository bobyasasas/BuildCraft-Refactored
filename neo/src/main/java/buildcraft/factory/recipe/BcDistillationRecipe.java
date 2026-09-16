/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory.recipe;

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
 * Distillation (refinery) recipe (M2.10 port of the legacy {@code buildcraft.lib.recipe.refinery} distillation family,
 * serializer {@code buildcraftfactory:distillation}; the 10 JSONs live under the buildcraftenergy namespace, exactly
 * like 1.20.1). One fluid input split into gas + liquid outputs against a micro-joule cost; byte-identical JSONs.
 */
public final class BcDistillationRecipe extends BcMachineRecipe implements Recipe<RecipeInput> {

    public static final MapCodec<BcDistillationRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance
        .group(
            Codec.LONG.fieldOf("powerRequired").forGetter(recipe -> recipe.powerRequired),
            BcFluidAmount.Codecs.MAP_CODEC.fieldOf("in").forGetter(recipe -> recipe.in),
            BcFluidAmount.Codecs.MAP_CODEC.fieldOf("outGas").forGetter(recipe -> recipe.outGas),
            BcFluidAmount.Codecs.MAP_CODEC.fieldOf("outLiquid").forGetter(recipe -> recipe.outLiquid)
        ).apply(instance, BcDistillationRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BcDistillationRecipe> STREAM_CODEC = StreamCodec
        .composite(
            ByteBufCodecs.VAR_LONG, recipe -> recipe.powerRequired,
            BcFluidAmount.Codecs.STREAM_CODEC, recipe -> recipe.in,
            BcFluidAmount.Codecs.STREAM_CODEC, recipe -> recipe.outGas,
            BcFluidAmount.Codecs.STREAM_CODEC, recipe -> recipe.outLiquid,
            BcDistillationRecipe::new
        );

    public final long powerRequired;
    public final BcFluidAmount in;
    public final BcFluidAmount outGas;
    public final BcFluidAmount outLiquid;

    public BcDistillationRecipe(long powerRequired, BcFluidAmount in, BcFluidAmount outGas, BcFluidAmount outLiquid) {
        this.powerRequired = powerRequired;
        this.in = in;
        this.outGas = outGas;
        this.outLiquid = outLiquid;
    }

    @Override
    public RecipeSerializer<BcDistillationRecipe> getSerializer() {
        return BcFactoryRecipes.DISTILLATION_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<RecipeInput>> getType() {
        return BcFactoryRecipes.DISTILLATION_TYPE.get();
    }
}
