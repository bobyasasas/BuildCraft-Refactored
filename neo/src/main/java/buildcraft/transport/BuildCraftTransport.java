/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.transport;

import buildcraft.datagen.BcDatagen;
import buildcraft.transport.net.PipeItemMessageQueue;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * BuildCraft transport mod entry point for the NeoForge 26.1.2 port (task M2.4a eight-mod skeleton, M2.4c registry
 * parity). Legacy counterpart: {@code buildcraft.transport.BCTransport}.
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
        // M3.4: datagen providers for this mod's namespace (item models, blockstates, lang placeholder).
        BcDatagen.register(modEventBus);
        // M4.6: expose the pipe's per-face item inboxes to automation (hoppers etc. push stacks into pipes through
        // the 26.1.2 item capability; vanilla containers are bridged on the other side by NeoForge's own hooks).
        modEventBus.addListener(this::onRegisterCapabilities);
        // M4.6: flush the travelling-item sync batch at the end of every server tick (legacy PipeItemMessageQueue).
        NeoForge.EVENT_BUS.addListener(PipeItemMessageQueue::onServerTickPost);
    }

    private void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            Capabilities.Item.BLOCK,
            BcTransportBlockEntities.PIPE_HOLDER.value(),
            (blockEntity, side) -> blockEntity.getInbox(side));
    }
}
