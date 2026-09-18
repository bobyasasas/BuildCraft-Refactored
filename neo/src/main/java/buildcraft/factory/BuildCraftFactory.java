/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory;

import com.mojang.logging.LogUtils;
import buildcraft.datagen.BcDatagen;
import buildcraft.factory.recipe.BcFactoryRecipes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.slf4j.Logger;

/**
 * BuildCraft factory mod entry point for the NeoForge 26.1.2 port (task M2.4a eight-mod skeleton, full registry
 * parity since M2.4b). Legacy counterpart: {@code buildcraft.factory.BCFactory}. Most registrations are
 * placeholders (see the {@code BcFactory*} centres); the M4.7 machines (tank, distiller, heat exchange, pump) carry
 * their real behaviour classes plus their fluid capabilities.
 */
// The value here should match the modId in META-INF/neoforge.mods.toml
@Mod(BuildCraftFactory.MOD_ID)
public class BuildCraftFactory {

    public static final String MOD_ID = "buildcraftfactory";

    private static final Logger LOGGER = LogUtils.getLogger();

    public BuildCraftFactory(IEventBus modEventBus) {
        LOGGER.info("BuildCraft factory (neo skeleton) loaded");
        BcFactoryBlocks.BLOCKS.register(modEventBus);
        BcFactoryItems.ITEMS.register(modEventBus);
        BcFactoryBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        // M3.4: datagen providers for this mod's namespace (item models, blockstates, lang placeholder).
        BcDatagen.register(modEventBus);
        // M2.10: heat exchange (heatable/coolable) + distillation recipe serializers.
        BcFactoryRecipes.TYPES.register(modEventBus);
        BcFactoryRecipes.SERIALIZERS.register(modEventBus);
        // M4.7: expose the four machines' fluid tanks to buckets and automation through the 26.1.2 fluid capability;
        // each block entity routes the queried side to exactly one tank (see the per-class side layouts).
        modEventBus.addListener(this::onRegisterCapabilities);
    }

    private void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, BcFactoryBlockEntities.TANK.value(),
            (blockEntity, side) -> blockEntity.getFluidHandler(side));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, BcFactoryBlockEntities.DISTILLER.value(),
            (blockEntity, side) -> blockEntity.getFluidHandler(side));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, BcFactoryBlockEntities.HEAT_EXCHANGE.value(),
            (blockEntity, side) -> blockEntity.getFluidHandler(side));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, BcFactoryBlockEntities.PUMP.value(),
            (blockEntity, side) -> blockEntity.getFluidHandler(side));
    }
}
