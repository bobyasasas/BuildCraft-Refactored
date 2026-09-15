/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.silicon;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * BuildCraft silicon mod entry point for the NeoForge 26.1.2 port (task M2.4a eight-mod skeleton, full registry
 * parity since M2.4b). Legacy counterpart: {@code buildcraft.silicon.BCSilicon}. The registrations are
 * placeholders (see the {@code BcSilicon*} centres); real behaviour classes migrate in M2.5+.
 */
// The value here should match the modId in META-INF/neoforge.mods.toml
@Mod(BuildCraftSilicon.MOD_ID)
public class BuildCraftSilicon {

    public static final String MOD_ID = "buildcraftsilicon";

    private static final Logger LOGGER = LogUtils.getLogger();

    public BuildCraftSilicon(IEventBus modEventBus) {
        LOGGER.info("BuildCraft silicon (neo skeleton) loaded");
        BcSiliconBlocks.BLOCKS.register(modEventBus);
        BcSiliconItems.ITEMS.register(modEventBus);
        BcSiliconBlockEntities.BLOCK_ENTITIES.register(modEventBus);
    }
}
