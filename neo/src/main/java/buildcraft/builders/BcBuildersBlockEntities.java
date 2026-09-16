/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders;

import buildcraft.builders.blockentity.FillerBlockEntity;
import buildcraft.builders.blockentity.QuarryBlockEntity;
import buildcraft.core.blockentity.PlaceholderBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central block entity type registration for buildcraftbuilders (task M2.4c registry parity). Every block entity id
 * the 1.20.1 registry baseline attributes to {@code buildcraftbuilders} registers here, bound to its block through the
 * shared {@link PlaceholderBlockEntity}. All placeholder, behaviour classes (legacy
 * {@code TileArchitect}, {@code TileBuilder}, {@code TileFiller}, {@code TileLibrary}, {@code TileMarker},
 * {@code TileQuarry}, {@code TileReplacer}) migrate in M2.5+ &mdash; M2.12 swapped the first two (filler, quarry) from
 * the placeholder to their real behaviour classes under the unchanged ids.
 */
public final class BcBuildersBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
            .create(BuiltInRegistries.BLOCK_ENTITY_TYPE, BuildCraftBuilders.MOD_ID);

    /** Placeholder for {@code buildcraftbuilders:architect}; behaviour class migrates in M2.5+. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceholderBlockEntity>> ARCHITECT = BLOCK_ENTITIES
            .register("architect", () -> new BlockEntityType<>(
                    (pos, state) -> new PlaceholderBlockEntity(BcBuildersBlockEntities.ARCHITECT.value(), pos, state),
                    BcBuildersBlocks.ARCHITECT.value()));

    /** Placeholder for {@code buildcraftbuilders:builder}; behaviour class migrates in M2.5+. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceholderBlockEntity>> BUILDER = BLOCK_ENTITIES
            .register("builder", () -> new BlockEntityType<>(
                    (pos, state) -> new PlaceholderBlockEntity(BcBuildersBlockEntities.BUILDER.value(), pos, state),
                    BcBuildersBlocks.BUILDER.value()));

    /** {@code buildcraftbuilders:filler}; M2.12 replaced the placeholder behaviour with the real {@link FillerBlockEntity} (same id). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FillerBlockEntity>> FILLER = BLOCK_ENTITIES
            .register("filler", () -> new BlockEntityType<>(
                    FillerBlockEntity::new,
                    BcBuildersBlocks.FILLER.value()));

    /** Placeholder for {@code buildcraftbuilders:library}; behaviour class migrates in M2.5+. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceholderBlockEntity>> LIBRARY = BLOCK_ENTITIES
            .register("library", () -> new BlockEntityType<>(
                    (pos, state) -> new PlaceholderBlockEntity(BcBuildersBlockEntities.LIBRARY.value(), pos, state),
                    BcBuildersBlocks.LIBRARY.value()));

    /** Placeholder for {@code buildcraftbuilders:marker_construction}; behaviour class migrates in M2.5+. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceholderBlockEntity>> MARKER_CONSTRUCTION = BLOCK_ENTITIES
            .register("marker_construction", () -> new BlockEntityType<>(
                    (pos, state) -> new PlaceholderBlockEntity(BcBuildersBlockEntities.MARKER_CONSTRUCTION.value(), pos, state),
                    BcBuildersBlocks.MARKER_CONSTRUCTION.value()));

    /** {@code buildcraftbuilders:quarry}; M2.12 replaced the placeholder behaviour with the real {@link QuarryBlockEntity} (same id). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<QuarryBlockEntity>> QUARRY = BLOCK_ENTITIES
            .register("quarry", () -> new BlockEntityType<>(
                    QuarryBlockEntity::new,
                    BcBuildersBlocks.QUARRY.value()));

    /** Placeholder for {@code buildcraftbuilders:replacer}; behaviour class migrates in M2.5+. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceholderBlockEntity>> REPLACER = BLOCK_ENTITIES
            .register("replacer", () -> new BlockEntityType<>(
                    (pos, state) -> new PlaceholderBlockEntity(BcBuildersBlockEntities.REPLACER.value(), pos, state),
                    BcBuildersBlocks.REPLACER.value()));

    private BcBuildersBlockEntities() {
    }
}
