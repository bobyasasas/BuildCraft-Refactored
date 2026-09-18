/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.transport;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.slf4j.Logger;
import buildcraft.transport.client.PipeItemFlowClient;
import buildcraft.transport.client.render.PipeHolderBlockRenderer;
import buildcraft.transport.net.MessageMultiPipeItem;

/**
 * Client-side entry point for the buildcrafttransport mod (M4.6), the same {@code @Mod(modid, dist = CLIENT)} pattern
 * as {@code buildcraft.core.client.BuildCraftCoreClient}: a second mod class for the same modid that FML only
 * constructs on the client dist (and which lives in the {@code client} source set, compiled out of dedicated servers).
 *
 * <p>Registers the shared pipe block renderer and installs the {@link MessageMultiPipeItem#clientHandler} hook (the
 * main source set cannot reference client classes, so the travelling-item batches are routed through this
 * indirection, mirroring the legacy {@code PipeFlowItems#handleClientReceviedItems} entry).
 */
// The value must match the main entry's modid; dist = CLIENT tells FML to only
// construct this class on the client dist (it is filtered out during scanning).
@Mod(value = BuildCraftTransport.MOD_ID, dist = Dist.CLIENT)
public class BuildCraftTransportClient {

    private static final Logger LOGGER = LogUtils.getLogger();

    public BuildCraftTransportClient(IEventBus modEventBus) {
        modEventBus.addListener(BuildCraftTransportClient::onRegisterRenderers);
        MessageMultiPipeItem.clientHandler = BuildCraftTransportClient::handlePipeItems;
    }

    /** M4.6: the shared pipe block renderer (body/arms/plugs/fluid/power/items, all custom geometry). */
    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BcTransportBlockEntities.PIPE_HOLDER.value(), PipeHolderBlockRenderer::new);
        LOGGER.info("BuildCraft transport client renderers registered: {} -> {}",
            BcTransportBlockEntities.PIPE_HOLDER.getId(), PipeHolderBlockRenderer.class.getSimpleName());
    }

    /** The travelling-item batch entry point (client thread, main-source-set safe via the static hook). */
    private static void handlePipeItems(MessageMultiPipeItem message) {
        ClientLevel level = Minecraft.getInstance().level;
        PipeItemFlowClient.handle(message, level);
    }
}
