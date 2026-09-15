/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.silicon;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import buildcraft.core.blockentity.PlaceholderBlockEntity;

/**
 * Central block entity type registration for buildcraftsilicon (task M2.4a skeleton, registry parity since M2.4b).
 * Every baseline block entity id registers as the shared {@link PlaceholderBlockEntity} bound to its (placeholder)
 * block; the real behaviour classes (legacy {@code Tile*}) migrate in M2.5+.
 */
public final class BcSiliconBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
            .create(BuiltInRegistries.BLOCK_ENTITY_TYPE, BuildCraftSilicon.MOD_ID);
    /** Placeholder for {@code buildcraftsilicon:advanced_crafting_table}; behaviour class migrates in M2.5+. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceholderBlockEntity>> ADVANCED_CRAFTING_TABLE = BLOCK_ENTITIES
            .register("advanced_crafting_table", () -> new BlockEntityType<>(
                    (pos, state) -> new PlaceholderBlockEntity(BcSiliconBlockEntities.ADVANCED_CRAFTING_TABLE.value(), pos, state),
                    BcSiliconBlocks.ADVANCED_CRAFTING_TABLE.value()));

    /** Placeholder for {@code buildcraftsilicon:assembly_table}; behaviour class migrates in M2.5+. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceholderBlockEntity>> ASSEMBLY_TABLE = BLOCK_ENTITIES
            .register("assembly_table", () -> new BlockEntityType<>(
                    (pos, state) -> new PlaceholderBlockEntity(BcSiliconBlockEntities.ASSEMBLY_TABLE.value(), pos, state),
                    BcSiliconBlocks.ASSEMBLY_TABLE.value()));

    /** Placeholder for {@code buildcraftsilicon:charging_table}; behaviour class migrates in M2.5+. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceholderBlockEntity>> CHARGING_TABLE = BLOCK_ENTITIES
            .register("charging_table", () -> new BlockEntityType<>(
                    (pos, state) -> new PlaceholderBlockEntity(BcSiliconBlockEntities.CHARGING_TABLE.value(), pos, state),
                    BcSiliconBlocks.CHARGING_TABLE.value()));

    /** Placeholder for {@code buildcraftsilicon:integration_table}; behaviour class migrates in M2.5+. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceholderBlockEntity>> INTEGRATION_TABLE = BLOCK_ENTITIES
            .register("integration_table", () -> new BlockEntityType<>(
                    (pos, state) -> new PlaceholderBlockEntity(BcSiliconBlockEntities.INTEGRATION_TABLE.value(), pos, state),
                    BcSiliconBlocks.INTEGRATION_TABLE.value()));

    /** Placeholder for {@code buildcraftsilicon:laser}; behaviour class migrates in M2.5+. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceholderBlockEntity>> LASER = BLOCK_ENTITIES
            .register("laser", () -> new BlockEntityType<>(
                    (pos, state) -> new PlaceholderBlockEntity(BcSiliconBlockEntities.LASER.value(), pos, state),
                    BcSiliconBlocks.LASER.value()));

    /** Placeholder for {@code buildcraftsilicon:programming_table}; behaviour class migrates in M2.5+. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceholderBlockEntity>> PROGRAMMING_TABLE = BLOCK_ENTITIES
            .register("programming_table", () -> new BlockEntityType<>(
                    (pos, state) -> new PlaceholderBlockEntity(BcSiliconBlockEntities.PROGRAMMING_TABLE.value(), pos, state),
                    BcSiliconBlocks.PROGRAMMING_TABLE.value()));


    private BcSiliconBlockEntities() {
    }
}
