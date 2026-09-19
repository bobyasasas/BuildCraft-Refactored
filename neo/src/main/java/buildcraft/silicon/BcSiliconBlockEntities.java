/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.silicon;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import buildcraft.silicon.blockentity.AdvancedCraftingTableBlockEntity;
import buildcraft.silicon.blockentity.AssemblyTableBlockEntity;
import buildcraft.silicon.blockentity.ChargingTableBlockEntity;
import buildcraft.silicon.blockentity.IntegrationTableBlockEntity;
import buildcraft.silicon.blockentity.LaserBlockEntity;
import buildcraft.silicon.blockentity.ProgrammingTableBlockEntity;

/**
 * Central block entity type registration for buildcraftsilicon (task M2.4a skeleton, registry parity since M2.4b).
 * Since M4.16 every baseline silicon machine id (laser + five tables) carries its real behaviour class (the M4.7
 * factory precedent) bound to its real block &mdash; the ids are unchanged, so the registry parity gate is
 * unaffected.
 */
public final class BcSiliconBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
            .create(BuiltInRegistries.BLOCK_ENTITY_TYPE, BuildCraftSilicon.MOD_ID);
    /** {@code buildcraftsilicon:advanced_crafting_table}; real class since M4.16 ({@link AdvancedCraftingTableBlockEntity}). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AdvancedCraftingTableBlockEntity>> ADVANCED_CRAFTING_TABLE = BLOCK_ENTITIES
            .register("advanced_crafting_table", () -> new BlockEntityType<>(
                    AdvancedCraftingTableBlockEntity::new, BcSiliconBlocks.ADVANCED_CRAFTING_TABLE.value()));

    /** {@code buildcraftsilicon:assembly_table}; real class since M4.16 ({@link AssemblyTableBlockEntity}). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AssemblyTableBlockEntity>> ASSEMBLY_TABLE = BLOCK_ENTITIES
            .register("assembly_table", () -> new BlockEntityType<>(
                    AssemblyTableBlockEntity::new, BcSiliconBlocks.ASSEMBLY_TABLE.value()));

    /** {@code buildcraftsilicon:charging_table}; real class since M4.16 ({@link ChargingTableBlockEntity}). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChargingTableBlockEntity>> CHARGING_TABLE = BLOCK_ENTITIES
            .register("charging_table", () -> new BlockEntityType<>(
                    ChargingTableBlockEntity::new, BcSiliconBlocks.CHARGING_TABLE.value()));

    /** {@code buildcraftsilicon:integration_table}; real class since M4.16 ({@link IntegrationTableBlockEntity}). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IntegrationTableBlockEntity>> INTEGRATION_TABLE = BLOCK_ENTITIES
            .register("integration_table", () -> new BlockEntityType<>(
                    IntegrationTableBlockEntity::new, BcSiliconBlocks.INTEGRATION_TABLE.value()));

    /** {@code buildcraftsilicon:laser}; real class since M4.16 ({@link LaserBlockEntity}). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LaserBlockEntity>> LASER = BLOCK_ENTITIES
            .register("laser", () -> new BlockEntityType<>(
                    LaserBlockEntity::new, BcSiliconBlocks.LASER.value()));

    /** {@code buildcraftsilicon:programming_table}; real class since M4.16 ({@link ProgrammingTableBlockEntity}, real recipe loop since M4.17). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProgrammingTableBlockEntity>> PROGRAMMING_TABLE = BLOCK_ENTITIES
            .register("programming_table", () -> new BlockEntityType<>(
                    ProgrammingTableBlockEntity::new, BcSiliconBlocks.PROGRAMMING_TABLE.value()));


    private BcSiliconBlockEntities() {
    }
}
