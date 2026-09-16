/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
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
 * Heat exchanger recipe (M2.10 port of the legacy {@code buildcraft.lib.recipe.refinery} heat exchange family). The
 * legacy datagen used two serializer ids — {@code buildcraftfactory:heat_exchange/heatable} and
 * {@code buildcraftfactory:heat_exchange/coolable} (a registry name may contain a slash) — with identical
 * {@code in/out/heatFrom/heatTo} payloads, so this one class serves both serializers; the kind is injected by the
 * codec instance (the JSON itself does not carry it, exactly like 1.20.1). The JSONs are byte-identical copies.
 */
public final class BcHeatExchangeRecipe extends BcMachineRecipe implements Recipe<RecipeInput> {

    public enum Kind {
        HEATABLE, COOLABLE;

        public static final Codec<Kind> CODEC = Codec.STRING.comapFlatMap(
            name -> {
                try {
                    return DataResult.success(Kind.valueOf(name));
                } catch (IllegalArgumentException e) {
                    return DataResult.error(() -> "Unknown heat exchange kind '" + name + "'");
                }
            }, Kind::name);
    }

    public final Kind kind;
    public final BcFluidAmount in;
    public final BcFluidAmount out;
    public final int heatFrom;
    public final int heatTo;

    public BcHeatExchangeRecipe(Kind kind, BcFluidAmount in, BcFluidAmount out, int heatFrom, int heatTo) {
        this.kind = kind;
        this.in = in;
        this.out = out;
        this.heatFrom = heatFrom;
        this.heatTo = heatTo;
    }

    /** The kind-bound map codec: {@code HEATABLE_SERIALIZER} and {@code COOLABLE_SERIALIZER} each use their own. */
    public static MapCodec<BcHeatExchangeRecipe> mapCodec(Kind kind) {
        return RecordCodecBuilder.mapCodec(instance -> instance
            .group(
                BcFluidAmount.Codecs.MAP_CODEC.fieldOf("in").forGetter(recipe -> recipe.in),
                BcFluidAmount.Codecs.MAP_CODEC.fieldOf("out").forGetter(recipe -> recipe.out),
                Codec.INT.fieldOf("heatFrom").forGetter(recipe -> recipe.heatFrom),
                Codec.INT.fieldOf("heatTo").forGetter(recipe -> recipe.heatTo)
            ).apply(instance, (in, out, heatFrom, heatTo) -> new BcHeatExchangeRecipe(kind, in, out, heatFrom, heatTo)));
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, BcHeatExchangeRecipe> STREAM_CODEC = StreamCodec
        .composite(
            ByteBufCodecs.STRING_UTF8.map(Kind::valueOf, Kind::name), recipe -> recipe.kind,
            BcFluidAmount.Codecs.STREAM_CODEC, recipe -> recipe.in,
            BcFluidAmount.Codecs.STREAM_CODEC, recipe -> recipe.out,
            ByteBufCodecs.VAR_INT, recipe -> recipe.heatFrom,
            ByteBufCodecs.VAR_INT, recipe -> recipe.heatTo,
            BcHeatExchangeRecipe::new
        );

    @Override
    public RecipeSerializer<BcHeatExchangeRecipe> getSerializer() {
        return this.kind == Kind.HEATABLE ? BcFactoryRecipes.HEATABLE_SERIALIZER.get()
            : BcFactoryRecipes.COOLABLE_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<RecipeInput>> getType() {
        return BcFactoryRecipes.HEAT_EXCHANGE_TYPE.get();
    }
}
