/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;

/**
 * Fluid amount for the BuildCraft machine recipes migrated in M2.10 (legacy counterpart:
 * {@code buildcraft.lib.fluid.FluidStack} JSON shape). JSON: {@code {"fluid": <fluid-id>, "amount": <mb>}} — the
 * exact field names the 1.20.1 legacy datagen emitted (heat exchange, distillation, fuel and coolant recipes are
 * byte-identical copies, see neo/tools/convert_recipes_m210.py).
 */
public record BcFluidAmount(Fluid fluid, int amount) {

    /** Mirrors the {@code amount >= 0} bound of {@code Codecs.MAP_CODEC} for programmatic construction too. */
    public BcFluidAmount {
        if (amount < 0) {
            throw new IllegalArgumentException("fluid amount must be >= 0 mb, got " + amount);
        }
    }

    /**
     * The codecs live in a lazy holder: class-init of this record (plain data carrier, unit-testable without vanilla
     * registries) must not force {@code BuiltInRegistries} access before bootstrap.
     */
    public static final class Codecs {

        public static final MapCodec<BcFluidAmount> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(
                BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(BcFluidAmount::fluid),
                Codec.intRange(0, Integer.MAX_VALUE).fieldOf("amount").forGetter(BcFluidAmount::amount)
            ).apply(instance, BcFluidAmount::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, BcFluidAmount> STREAM_CODEC = StreamCodec
            .composite(
                ByteBufCodecs.<Fluid>holderRegistry(Registries.FLUID)
                    .map(Holder::value, Fluid::builtInRegistryHolder),
                BcFluidAmount::fluid,
                ByteBufCodecs.VAR_INT, BcFluidAmount::amount,
                BcFluidAmount::new
            );

        private Codecs() {
        }
    }
}
