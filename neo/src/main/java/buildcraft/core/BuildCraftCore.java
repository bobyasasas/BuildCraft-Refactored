/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core;

import buildcraft.datagen.BcDatagen;
import buildcraft.core.gametest.BcGameTests;
import buildcraft.core.gametest.RegistryDumpProbe;
import com.mojang.logging.LogUtils;
import buildcraft.lib.expression.DefaultContexts;
import buildcraft.lib.expression.GenericExpressionCompiler;
import buildcraft.lib.expression.api.InvalidExpressionException;
import buildcraft.lib.expression.api.IExpressionNode.INodeLong;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * BuildCraft core mod entry point for the NeoForge 26.1.2 port (toolchain proven in M2.1, registry parity since
 * M2.4a). Real behaviour classes migrate in later Phase 2 tasks.
 */
// The value here should match the modId in META-INF/neoforge.mods.toml
@Mod(BuildCraftCore.MOD_ID)
public class BuildCraftCore {

    public static final String MOD_ID = "buildcraftcore";

    private static final Logger LOGGER = LogUtils.getLogger();

    public BuildCraftCore(IEventBus modEventBus) {
        LOGGER.info("BuildCraft core (neo skeleton) loaded");
        expressionSmokeTest();

        // M2.2a registration centres, carrying the full 1.20.1 baseline parity set since M2.4a.
        BcBlocks.BLOCKS.register(modEventBus);
        BcItems.ITEMS.register(modEventBus);
        BcBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        BcCreativeTabs.TABS.register(modEventBus);

        // M3.4: datagen providers for this mod's namespace (item models, blockstates, frozen lang).
        BcDatagen.register(modEventBus);

        modEventBus.addListener(BcGameTests::onRegisterGameTests);
        modEventBus.addListener(BuildCraftCore::onCommonSetup);

        // M3.3: gated runtime registry dump (see RegistryDumpProbe) — writes the 26.1.2 registry dump only when
        // buildcraft.registrydump.path is set to a non-empty path; off (empty) in every normal run.
        NeoForge.EVENT_BUS.addListener(RegistryDumpProbe::onServerStarted);
    }

    /**
     * M2.2a: runtime proof that the marker placeholder block/item really bound to the frozen registries. Since M2.2b
     * it also covers the stone engine slice. Runs in every dist, so the same log line doubles as evidence for
     * runClient and the headless gametest server.
     */
    private static void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("BuildCraft core registration smoke: block {} ({}) with item {} registered",
                BcBlocks.MARKER.getId(), BcBlocks.MARKER.value().getClass().getSimpleName(), BcItems.MARKER.getId());
        LOGGER.info("BuildCraft core registration smoke: block {} ({}) with item {} and block entity type {} registered",
                BcBlocks.ENGINE_STONE.getId(), BcBlocks.ENGINE_STONE.value().getClass().getSimpleName(),
                BcItems.ENGINE_STONE.getId(), BcBlockEntities.ENGINE_STONE.getId());
        LOGGER.info("BuildCraft core registration smoke: block {} ({}) with item {} and block entity type {} registered",
                BcBlocks.PIPE_KINESIS_WOOD.getId(), BcBlocks.PIPE_KINESIS_WOOD.value().getClass().getSimpleName(),
                BcItems.PIPE_KINESIS_WOOD.getId(), BcBlockEntities.PIPE_KINESIS_WOOD.getId());
        LOGGER.info("BuildCraft core registration smoke: block {} ({}) with item {} and block entity type {} registered",
                BcBlocks.ENERGY_METER.getId(), BcBlocks.ENERGY_METER.value().getClass().getSimpleName(),
                BcItems.ENERGY_METER.getId(), BcBlockEntities.ENERGY_METER.getId());
    }

    /**
     * M2.3: runtime proof that the expression library shared from
     * sub_projects/expression is really on the mod classpath, by compiling and
     * evaluating a constant expression through the real compiler.
     */
    private static void expressionSmokeTest() {
        try {
            INodeLong node = GenericExpressionCompiler.compileExpressionLong("1+2*3", DefaultContexts.createWithAll());
            LOGGER.info("BuildCraft expression smoke: 1+2*3 = " + node.evaluate());
        } catch (InvalidExpressionException e) {
            throw new IllegalStateException("BuildCraft expression smoke test failed to compile", e);
        }
    }
}
