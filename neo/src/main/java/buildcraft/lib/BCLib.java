/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib;

import buildcraft.lib.net.MessageManager;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * BuildCraft lib mod entry point for the NeoForge 26.1.2 port (task M2.4a eight-mod skeleton). Legacy counterpart:
 * {@code buildcraft.lib.BCLib}. All BuildCraft mods depend on this one, so FML constructs it first.
 */
// The value here should match the modId in META-INF/neoforge.mods.toml
@Mod(BCLib.MOD_ID)
public class BCLib {

    public static final String MOD_ID = "buildcraftlib";

    private static final Logger LOGGER = LogUtils.getLogger();

    public BCLib(IEventBus modEventBus) {
        LOGGER.info("BuildCraft lib (neo skeleton) loaded");
        BcLibItems.ITEMS.register(modEventBus);

        // M2.5: the whole BuildCraft message set registers under this mod's network namespace (legacy registered the
        // same set through the lib-owned MessageManager).
        modEventBus.addListener(MessageManager::onRegisterPayloadHandlers);
    }
}
