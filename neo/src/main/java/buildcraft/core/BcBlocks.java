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
import buildcraft.core.block.KinesisPipeBlock;
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

    /** Placeholder for {@code buildcraftcore:spring_oil} (legacy {@code BlockSpring}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> SPRING_OIL = BcLangKeys.simpleBlock(BLOCKS, "spring_oil",
            properties -> properties.strength(0.5F));

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

    /** Placeholder for {@code buildcraftcore:engine_wood} (legacy {@code BlockEngine_BC8}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> ENGINE_WOOD = BcLangKeys.simpleBlock(BLOCKS, "engine_wood",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftcore:engine_creative} (legacy {@code BlockEngine_BC8}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> ENGINE_CREATIVE = BcLangKeys.simpleBlock(BLOCKS, "engine_creative",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftcore:engine_iron} (legacy {@code BlockEngine_BC8}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> ENGINE_IRON = BcLangKeys.simpleBlock(BLOCKS, "engine_iron",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftcore:engine_rf} (legacy {@code BlockEngine_BC8}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> ENGINE_RF = BcLangKeys.simpleBlock(BLOCKS, "engine_rf",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftcore:marker_volume} (legacy {@code BlockMarkerVolume}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> MARKER_VOLUME = BcLangKeys.simpleBlock(BLOCKS, "marker_volume",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftcore:marker_path} (legacy {@code BlockMarkerPath}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> MARKER_PATH = BcLangKeys.simpleBlock(BLOCKS, "marker_path",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftcore:power_tester} (legacy {@code BlockPowerConsumerTester}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> POWER_TESTER = BcLangKeys.simpleBlock(BLOCKS, "power_tester",
            properties -> properties.strength(0.5F));

    private BcBlocks() {
    }
}
