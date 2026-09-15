/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * BuildCraft factory mod entry point for the NeoForge 26.1.2 port (task M2.4a eight-mod skeleton). Legacy
 * counterpart: {@code buildcraft.factory.BCFactory}. Placeholder registrations (see the {@code BcFactory*}
 * centres) arrive in M2.4b.
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
    }
}
