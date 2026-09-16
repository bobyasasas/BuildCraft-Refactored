/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.datafixers.util.Either;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Sized ingredient for the BuildCraft machine recipes migrated in M2.10 (legacy counterpart:
 * {@code buildcraft.api.recipes.IngredientStack}). JSON: a plain ingredient ("item-id" / "#tag" / list, exactly the
 * 26.1.2 vanilla {@link Ingredient} idioms) when {@code count} is 1, otherwise
 * {@code {"ingredient": <ingredient>, "count": n}}.
 */
public record BcIngredientStack(Ingredient ingredient, int count) {

    /** Mirrors the {@code count in [1, 99]} bound of {@code Codecs.CODEC} for programmatic construction too. */
    public BcIngredientStack {
        if (count < 1 || count > 99) {
            throw new IllegalArgumentException("ingredient count must be in [1, 99], got " + count);
        }
    }

    /**
     * The codecs live in a lazy holder: class-init of this record (plain data carrier, unit-testable without vanilla
     * registries) must not force {@code BuiltInRegistries} access before bootstrap. The sized form is declared as its
     * own field inside the holder because nesting {@code RecordCodecBuilder.create(...)} directly inside
     * {@link Codec#either} breaks javac's overload resolution against the 26.1.2 DFU (9.0.19).
     */
    public static final class Codecs {

        private static final Codec<BcIngredientStack> EXPLICIT = RecordCodecBuilder.create(instance -> instance
            .group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(BcIngredientStack::ingredient),
                Codec.intRange(1, 99).optionalFieldOf("count", 1).forGetter(BcIngredientStack::count)
            ).apply(instance, BcIngredientStack::new));

        public static final Codec<BcIngredientStack> CODEC = Codec
            .either(
                Ingredient.CODEC,
                EXPLICIT
            ).xmap(
                either -> either.map(ingredient -> new BcIngredientStack(ingredient, 1), stack -> stack),
                stack -> stack.count == 1 ? Either.left(stack.ingredient) : Either.right(stack)
            );

        public static final StreamCodec<RegistryFriendlyByteBuf, BcIngredientStack> STREAM_CODEC = StreamCodec
            .composite(
                Ingredient.CONTENTS_STREAM_CODEC, BcIngredientStack::ingredient,
                ByteBufCodecs.VAR_INT, BcIngredientStack::count,
                BcIngredientStack::new
            );

        private Codecs() {
        }
    }
}
