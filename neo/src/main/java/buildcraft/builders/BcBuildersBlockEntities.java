/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders;

import buildcraft.builders.blockentity.ArchitectBlockEntity;
import buildcraft.builders.blockentity.BuilderBlockEntity;
import buildcraft.builders.blockentity.ConstructionMarkerBlockEntity;
import buildcraft.builders.blockentity.FillerBlockEntity;
import buildcraft.builders.blockentity.LibraryBlockEntity;
import buildcraft.builders.blockentity.QuarryBlockEntity;
import buildcraft.builders.blockentity.ReplacerBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central block entity type registration for buildcraftbuilders (task M2.4c registry parity). Every block entity id
 * the 1.20.1 registry baseline attributes to {@code buildcraftbuilders} registers here, bound to its block. The
 * placeholder registrations ({@code PlaceholderBlockEntity}) migrate to their real behaviour classes under the
 * unchanged ids as the content lands: M2.12 swapped filler and quarry, M4.17 swapped architect, builder, library,
 * marker_construction and replacer &mdash; the builders module carries no placeholder any more.
 */
public final class BcBuildersBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
            .create(BuiltInRegistries.BLOCK_ENTITY_TYPE, BuildCraftBuilders.MOD_ID);

    /** {@code buildcraftbuilders:architect}; M4.17 replaced the placeholder behaviour with the real {@link ArchitectBlockEntity} (same id). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArchitectBlockEntity>> ARCHITECT = BLOCK_ENTITIES
            .register("architect", () -> new BlockEntityType<>(
                    ArchitectBlockEntity::new,
                    BcBuildersBlocks.ARCHITECT.value()));

    /** {@code buildcraftbuilders:builder}; M4.17 replaced the placeholder behaviour with the real {@link BuilderBlockEntity} (same id). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BuilderBlockEntity>> BUILDER = BLOCK_ENTITIES
            .register("builder", () -> new BlockEntityType<>(
                    BuilderBlockEntity::new,
                    BcBuildersBlocks.BUILDER.value()));

    /** {@code buildcraftbuilders:filler}; M2.12 replaced the placeholder behaviour with the real {@link FillerBlockEntity} (same id). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FillerBlockEntity>> FILLER = BLOCK_ENTITIES
            .register("filler", () -> new BlockEntityType<>(
                    FillerBlockEntity::new,
                    BcBuildersBlocks.FILLER.value()));

    /** {@code buildcraftbuilders:library}; M4.17 replaced the placeholder behaviour with the real {@link LibraryBlockEntity} (same id). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LibraryBlockEntity>> LIBRARY = BLOCK_ENTITIES
            .register("library", () -> new BlockEntityType<>(
                    LibraryBlockEntity::new,
                    BcBuildersBlocks.LIBRARY.value()));

    /** {@code buildcraftbuilders:marker_construction}; M4.17 replaced the placeholder behaviour with the real {@link ConstructionMarkerBlockEntity} (same id). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConstructionMarkerBlockEntity>> MARKER_CONSTRUCTION = BLOCK_ENTITIES
            .register("marker_construction", () -> new BlockEntityType<>(
                    ConstructionMarkerBlockEntity::new,
                    BcBuildersBlocks.MARKER_CONSTRUCTION.value()));

    /** {@code buildcraftbuilders:quarry}; M2.12 replaced the placeholder behaviour with the real {@link QuarryBlockEntity} (same id). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<QuarryBlockEntity>> QUARRY = BLOCK_ENTITIES
            .register("quarry", () -> new BlockEntityType<>(
                    QuarryBlockEntity::new,
                    BcBuildersBlocks.QUARRY.value()));

    /** {@code buildcraftbuilders:replacer}; M4.17 replaced the placeholder behaviour with the real {@link ReplacerBlockEntity} (same id). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReplacerBlockEntity>> REPLACER = BLOCK_ENTITIES
            .register("replacer", () -> new BlockEntityType<>(
                    ReplacerBlockEntity::new,
                    BcBuildersBlocks.REPLACER.value()));

    private BcBuildersBlockEntities() {
    }
}
