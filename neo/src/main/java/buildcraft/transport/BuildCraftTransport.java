/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.transport;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * BuildCraft transport mod entry point for the NeoForge 26.1.2 port (task M2.4a eight-mod skeleton). Legacy
 * counterpart: {@code buildcraft.transport.BCTransport}. Placeholder registrations (see the {@code BcTransport*}
 * centres) arrive in M2.4c.
 */
// The value here should match the modId in META-INF/neoforge.mods.toml
@Mod(BuildCraftTransport.MOD_ID)
public class BuildCraftTransport {

    public static final String MOD_ID = "buildcrafttransport";

    private static final Logger LOGGER = LogUtils.getLogger();

    public BuildCraftTransport(IEventBus modEventBus) {
        LOGGER.info("BuildCraft transport (neo skeleton) loaded");
        BcTransportBlocks.BLOCKS.register(modEventBus);
        BcTransportItems.ITEMS.register(modEventBus);
        BcTransportBlockEntities.BLOCK_ENTITIES.register(modEventBus);
    }
}
