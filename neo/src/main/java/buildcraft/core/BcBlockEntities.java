/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import buildcraft.core.blockentity.EnergyMeterBlockEntity;
import buildcraft.core.blockentity.EngineBlockEntity;
import buildcraft.core.blockentity.KinesisPipeBlockEntity;
import buildcraft.core.blockentity.MarkerPathBlockEntity;
import buildcraft.core.blockentity.MarkerVolumeBlockEntity;
import buildcraft.core.blockentity.PlaceholderBlockEntity;
import buildcraft.core.blockentity.StoneEngineBlockEntity;

/**
 * Central block entity type registration for buildcraftcore (task M2.2a, first entry M2.2b, M2.4a registry parity).
 * Every block entity type this mod registers gets a constant {@link DeferredHolder} field here, registered through the
 * single {@link #BLOCK_ENTITIES} holder on the mod event bus (see
 * {@link BuildCraftCore#BuildCraftCore(net.neoforged.bus.api.IEventBus)}).
 *
 * <p>NeoForge 26.1.2 removed {@code BlockEntityType.Builder}; the block entity type constructor is public instead and
 * the supplier receives only the position and state (the type is resolved by the behaviour class):
 *
 * <pre>{@code
 * public static final Supplier<BlockEntityType<TileEngine>> ENGINE = BLOCK_ENTITIES.register("engine",
 *         () -> new BlockEntityType<>(TileEngine::new, BcBlocks.ENGINE.value()));
 * }</pre>
 *
 * <p>Since M2.4a this class carries every block entity type id the 1.20.1 registry baseline attributes to
 * {@code buildcraftcore}: types without a migrated behaviour class use {@link PlaceholderBlockEntity} and get replaced
 * in M2.5+.
 */
public final class BcBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
            .create(BuiltInRegistries.BLOCK_ENTITY_TYPE, BuildCraftCore.MOD_ID);

    /**
     * M2.2b stone engine slice block entity type. The varargs constructor is a NeoForge convenience
     * ({@code BlockEntityType#BlockEntityType(BlockEntitySupplier, Block...)}) over the vanilla
     * {@code Set<Block>} one. Referencing {@link BcBlocks#ENGINE_STONE} inside the supplier is safe: blocks register
     * before block entity types.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StoneEngineBlockEntity>> ENGINE_STONE = BLOCK_ENTITIES
            .register("engine_stone", () -> new BlockEntityType<>(StoneEngineBlockEntity::new, BcBlocks.ENGINE_STONE.value()));

    /** M2.2c wooden kinesis pipe slice block entity type (see {@link KinesisPipeBlockEntity}). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KinesisPipeBlockEntity>> PIPE_KINESIS_WOOD = BLOCK_ENTITIES
            .register("pipe_kinesis_wood", () -> new BlockEntityType<>(KinesisPipeBlockEntity::new, BcBlocks.PIPE_KINESIS_WOOD.value()));

    /** M2.2c slice-only measurement block entity type (see {@link EnergyMeterBlockEntity}). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyMeterBlockEntity>> ENERGY_METER = BLOCK_ENTITIES
            .register("energy_meter", () -> new BlockEntityType<>(EnergyMeterBlockEntity::new, BcBlocks.ENERGY_METER.value()));

    // -------------------------------------------------------------------------
    // M2.4a registry parity placeholders: every remaining baseline buildcraftcore
    // block entity id, each bound to its (placeholder) block through the shared
    // PlaceholderBlockEntity. All placeholder, behaviour classes migrate in M2.5+.
    // -------------------------------------------------------------------------

    /** M4.4: the real wooden engine block entity (render slice of the legacy {@code TileEngineRedstone_BC8}). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EngineBlockEntity>> ENGINE_WOOD = BLOCK_ENTITIES
            .register("engine_wood", () -> new BlockEntityType<>(
                    (pos, state) -> new EngineBlockEntity(BcBlockEntities.ENGINE_WOOD.value(), pos, state, false), BcBlocks.ENGINE_WOOD.value()));

    /** M4.4: the real creative engine block entity (render slice of the legacy {@code TileEngineCreative}). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EngineBlockEntity>> ENGINE_CREATIVE = BLOCK_ENTITIES
            .register("engine_creative", () -> new BlockEntityType<>(
                    (pos, state) -> new EngineBlockEntity(BcBlockEntities.ENGINE_CREATIVE.value(), pos, state, true), BcBlocks.ENGINE_CREATIVE.value()));

    /** M4.4: the real iron engine block entity (render slice of the legacy iron {@code TileEngineIron_BC8}). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EngineBlockEntity>> ENGINE_IRON = BLOCK_ENTITIES
            .register("engine_iron", () -> new BlockEntityType<>(
                    (pos, state) -> new EngineBlockEntity(BcBlockEntities.ENGINE_IRON.value(), pos, state, false), BcBlocks.ENGINE_IRON.value()));

    /** M4.4: the real rf engine block entity (render slice of the legacy rf {@code TileEngineRf_BC8}). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EngineBlockEntity>> ENGINE_RF = BLOCK_ENTITIES
            .register("engine_rf", () -> new BlockEntityType<>(
                    (pos, state) -> new EngineBlockEntity(BcBlockEntities.ENGINE_RF.value(), pos, state, false), BcBlocks.ENGINE_RF.value()));

    /** M4.5: the real path marker (legacy {@code TileMarkerPath}), replacing the M2.4a placeholder. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MarkerPathBlockEntity>> MARKER_PATH = BLOCK_ENTITIES
            .register("marker_path", () -> new BlockEntityType<>(MarkerPathBlockEntity::new, BcBlocks.MARKER_PATH.value()));

    /** M4.5: the real volume marker (legacy {@code TileMarkerVolume}), replacing the M2.4a placeholder. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MarkerVolumeBlockEntity>> MARKER_VOLUME = BLOCK_ENTITIES
            .register("marker_volume", () -> new BlockEntityType<>(MarkerVolumeBlockEntity::new, BcBlocks.MARKER_VOLUME.value()));


    /** Placeholder for {@code buildcraftcore:power_tester}; behaviour class migrates in M2.5+. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceholderBlockEntity>> POWER_TESTER = BLOCK_ENTITIES
            .register("power_tester", () -> new BlockEntityType<>(
                    (pos, state) -> new PlaceholderBlockEntity(BcBlockEntities.POWER_TESTER.value(), pos, state), BcBlocks.POWER_TESTER.value()));

    /** Placeholder for {@code buildcraftcore:spring_oil}; behaviour class migrates in M2.5+. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceholderBlockEntity>> SPRING_OIL = BLOCK_ENTITIES
            .register("spring_oil", () -> new BlockEntityType<>(
                    (pos, state) -> new PlaceholderBlockEntity(BcBlockEntities.SPRING_OIL.value(), pos, state), BcBlocks.SPRING_OIL.value()));

    private BcBlockEntities() {
    }
}
