/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders;

import buildcraft.datagen.BcDatagen;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * BuildCraft builders mod entry point for the NeoForge 26.1.2 port (task M2.4a eight-mod skeleton, M2.4c registry
 * parity). Legacy counterpart: {@code buildcraft.builders.BCBuilders}.
 */
// The value here should match the modId in META-INF/neoforge.mods.toml
@Mod(BuildCraftBuilders.MOD_ID)
public class BuildCraftBuilders {

    public static final String MOD_ID = "buildcraftbuilders";

    private static final Logger LOGGER = LogUtils.getLogger();

    public BuildCraftBuilders(IEventBus modEventBus) {
        LOGGER.info("BuildCraft builders (neo skeleton) loaded");
        BcBuildersBlocks.BLOCKS.register(modEventBus);
        BcBuildersItems.ITEMS.register(modEventBus);
        BcBuildersBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        // M4.8: menu types (the lib/gui framework's registry half).
        BcBuildersMenus.MENUS.register(modEventBus);
        // M3.4: datagen providers for this mod's namespace (item models, blockstates, lang placeholder).
        BcDatagen.register(modEventBus);
    }
}
