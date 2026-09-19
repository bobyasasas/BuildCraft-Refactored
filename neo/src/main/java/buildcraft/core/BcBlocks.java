/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import buildcraft.lib.BcLangKeys;
import buildcraft.core.block.EnergyMeterBlock;
import buildcraft.core.block.EngineBlock;
import buildcraft.core.block.KinesisPipeBlock;
import buildcraft.core.block.MarkerPathBlock;
import buildcraft.core.block.MarkerVolumeBlock;
import buildcraft.core.block.PowerTesterBlock;
import buildcraft.core.block.SpringOilBlock;
import buildcraft.core.block.StoneEngineBlock;

/**
 * Central block registration for buildcraftcore (task M2.2a, M2.4a registry parity). Every block this mod registers
 * gets a constant {@link DeferredBlock} field here, registered through the single {@link #BLOCKS} holder on the mod
 * event bus (see {@link BuildCraftCore#BuildCraftCore(net.neoforged.bus.api.IEventBus)}).
 *
 * <p>Since M2.4a this class carries every block id the 1.20.1 registry baseline (see
 * {@code migration/snapshots/registry-baseline.json}) attributes to {@code buildcraftcore}: blocks without a migrated
 * behaviour class register as plain {@link Block} placeholders and get replaced by the real classes in M2.5+.
 */
public final class BcBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BuildCraftCore.MOD_ID);

    /**
     * M2.2a placeholder block: a plain {@link Block} with no behaviour, existing purely as compile- and runtime proof
     * that the registration pipeline works. M2.2c will add the pipe blocks next to it.
     */
    public static final DeferredBlock<Block> MARKER = BcLangKeys.simpleBlock(BLOCKS, "marker",
            properties -> properties.strength(0.5F));

    /**
     * M2.2b stone engine slice block (see {@link StoneEngineBlock}). Placeholder placement for the M2.2 slice only:
     * the real engine registry migration (legacy {@code BlockRegistry} + engine module) is M2.4/M2.9.
     */
    public static final DeferredBlock<StoneEngineBlock> ENGINE_STONE = BcLangKeys.block(BLOCKS, "engine_stone",
            StoneEngineBlock::new, () -> BlockBehaviour.Properties.of().strength(3.5F));

    /**
     * M2.2c wooden kinesis pipe slice block (see {@link KinesisPipeBlock}). Placeholder placement for the M2.2 slice
     * only: the real transport module registry migration (legacy {@code BlockGenericPipe}) is M2.4/M2.9.
     */
    public static final DeferredBlock<KinesisPipeBlock> PIPE_KINESIS_WOOD = BcLangKeys.block(BLOCKS, "pipe_kinesis_wood",
            KinesisPipeBlock::new, () -> BlockBehaviour.Properties.of().strength(0.5F));

    /**
     * M2.2c slice-only measurement block (see {@link EnergyMeterBlock}); not final content, exists for the
     * {@code kinesis_chain_transfers_power} game test.
     */
    public static final DeferredBlock<EnergyMeterBlock> ENERGY_METER = BcLangKeys.block(BLOCKS, "energy_meter",
            EnergyMeterBlock::new, () -> BlockBehaviour.Properties.of().strength(1.0F));

    // -------------------------------------------------------------------------
    // M2.4a registry parity placeholders (legacy BCCoreBlocks; the engines were
    // registered by BCEnergyBlocks, but under the buildcraftcore mod id). All
    // placeholder, behaviour classes migrate in M2.5+.
    // -------------------------------------------------------------------------

    /** Placeholder for {@code buildcraftcore:spring_water} (legacy {@code BlockSpring}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> SPRING_WATER = BcLangKeys.simpleBlock(BLOCKS, "spring_water",
            properties -> properties.strength(0.5F));

    /**
     * M4.16: the real oil spring (legacy {@code BlockSpring} for {@code EnumSpring.OIL} + the {@code TileSpringOil}
     * slice), replacing the M2.4a placeholder; the generation loop ticks in the block entity (see
     * {@link buildcraft.core.block.SpringOilBlock} and {@link buildcraft.core.blockentity.SpringOilBlockEntity}).
     */
    public static final DeferredBlock<SpringOilBlock> SPRING_OIL = BcLangKeys.block(BLOCKS, "spring_oil",
            SpringOilBlock::new, () -> BlockBehaviour.Properties.of().strength(0.5F));

    /** Placeholder for {@code buildcraftcore:decorated_blueprint} (legacy {@code BlockDecoration}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> DECORATED_BLUEPRINT = BcLangKeys.simpleBlock(BLOCKS, "decorated_blueprint",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftcore:decorated_destroy} (legacy {@code BlockDecoration}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> DECORATED_DESTROY = BcLangKeys.simpleBlock(BLOCKS, "decorated_destroy",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftcore:decorated_laser_back} (legacy {@code BlockDecoration}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> DECORATED_LASER_BACK = BcLangKeys.simpleBlock(BLOCKS, "decorated_laser_back",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftcore:decorated_leather} (legacy {@code BlockDecoration}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> DECORATED_LEATHER = BcLangKeys.simpleBlock(BLOCKS, "decorated_leather",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftcore:decorated_paper} (legacy {@code BlockDecoration}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> DECORATED_PAPER = BcLangKeys.simpleBlock(BLOCKS, "decorated_paper",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftcore:decorated_template} (legacy {@code BlockDecoration}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> DECORATED_TEMPLATE = BcLangKeys.simpleBlock(BLOCKS, "decorated_template",
            properties -> properties.strength(0.5F));

    /**
     * M4.4: the real wooden engine (legacy {@code BlockEngine_BC8} + {@code TileEngineRedstone_BC8}), replacing the
     * M2.4a placeholder; rendering slice — the block entity carries the fuel/buffer state the engine BER animates
     * (see {@link EngineBlock} and {@link buildcraft.core.blockentity.EngineBlockEntity}).
     */
    public static final DeferredBlock<EngineBlock> ENGINE_WOOD = BcLangKeys.block(BLOCKS, "engine_wood",
            properties -> new EngineBlock(properties, () -> BcBlockEntities.ENGINE_WOOD.value(), false),
            properties -> properties.strength(0.5F));

    /**
     * M4.4: the real creative engine (legacy {@code BlockEngine_BC8} + {@code TileEngineCreative}), replacing the
     * M2.4a placeholder; redstone-driven, never burns (see {@link EngineBlock}).
     */
    public static final DeferredBlock<EngineBlock> ENGINE_CREATIVE = BcLangKeys.block(BLOCKS, "engine_creative",
            properties -> new EngineBlock(properties, () -> BcBlockEntities.ENGINE_CREATIVE.value(), true),
            properties -> properties.strength(0.5F));

    /** M4.4: the real iron engine (legacy {@code BlockEngine_BC8}); rendering slice, see {@link #ENGINE_WOOD}. */
    public static final DeferredBlock<EngineBlock> ENGINE_IRON = BcLangKeys.block(BLOCKS, "engine_iron",
            properties -> new EngineBlock(properties, () -> BcBlockEntities.ENGINE_IRON.value(), false),
            properties -> properties.strength(0.5F));

    /** M4.4: the real rf engine (legacy {@code BlockEngine_BC8}); rendering slice, see {@link #ENGINE_WOOD}. */
    public static final DeferredBlock<EngineBlock> ENGINE_RF = BcLangKeys.block(BLOCKS, "engine_rf",
            properties -> new EngineBlock(properties, () -> BcBlockEntities.ENGINE_RF.value(), false),
            properties -> properties.strength(0.5F));

    /**
     * M4.5: the real volume marker (legacy {@code BlockMarkerVolume}), replacing the M2.4a placeholder. The block
     * entity carries the marker connection bookkeeping and the redstone-driven signal lines (see
     * {@link MarkerVolumeBlock} and {@link buildcraft.core.blockentity.MarkerVolumeBlockEntity}).
     */
    public static final DeferredBlock<MarkerVolumeBlock> MARKER_VOLUME = BcLangKeys.block(BLOCKS, "marker_volume",
            MarkerVolumeBlock::new, () -> BlockBehaviour.Properties.of().strength(0.5F));

    /**
     * M4.5: the real path marker (legacy {@code BlockMarkerPath}), replacing the M2.4a placeholder; connections are
     * ordered chains along one axis (see {@link MarkerPathBlock} and
     * {@link buildcraft.core.blockentity.MarkerPathBlockEntity}).
     */
    public static final DeferredBlock<MarkerPathBlock> MARKER_PATH = BcLangKeys.block(BLOCKS, "marker_path",
            MarkerPathBlock::new, () -> BlockBehaviour.Properties.of().strength(0.5F));


    /**
     * M4.16: the real power consumer tester (legacy {@code BlockPowerConsumerTester} +
     * {@code TilePowerConsumerTester}), replacing the M2.4a placeholder; the always-on MJ sink lives in the block
     * entity (see {@link buildcraft.core.block.PowerTesterBlock} and
     * {@link buildcraft.core.blockentity.PowerTesterBlockEntity}).
     */
    public static final DeferredBlock<PowerTesterBlock> POWER_TESTER = BcLangKeys.block(BLOCKS, "power_tester",
            PowerTesterBlock::new, () -> BlockBehaviour.Properties.of().strength(0.5F));

    private BcBlocks() {
    }
}
