/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.client;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.slf4j.Logger;
import buildcraft.core.BcBlockEntities;
import buildcraft.core.BuildCraftCore;
import buildcraft.core.client.render.KinesisPipeBlockRenderer;
import buildcraft.core.client.render.StoneEngineBlockRenderer;

/**
 * Client-side entry point for the buildcraftcore mod (task M2.7b), mirroring
 * {@code buildcraft.energy.client.BuildCraftEnergyClient}: a second {@link Mod @Mod}-annotated class for the same
 * modid, restricted to {@link Dist#CLIENT} so FML filters it out during the bytecode scan on dedicated servers (the
 * class is also compiled out of the server runs entirely — it lives in the {@code client} source set, M2.7a).
 *
 * <p>Registers the mod's real block entity renderers (19 baseline renderers; the M2.7b slice ships the first two —
 * the stone engine and the wooden kinesis pipe — the rest migrate with their content in M2.8+/M2.9) through
 * {@link EntityRenderersEvent.RegisterRenderers}, the only legal registration entry on NeoForge (vanilla
 * {@code BlockEntityRenderers.register} is package private; the event fires on the mod event bus when the resource
 * manager builds the dispatcher's renderer table).
 */
// The value must match the main entry's modid; dist = CLIENT tells FML to only
// construct this class on the client dist (it is filtered out during scanning).
@Mod(value = BuildCraftCore.MOD_ID, dist = Dist.CLIENT)
public class BuildCraftCoreClient {

    private static final Logger LOGGER = LogUtils.getLogger();

    public BuildCraftCoreClient(IEventBus modEventBus) {
        modEventBus.addListener(BuildCraftCoreClient::onRegisterRenderers);
    }

    /** M2.7b: block entity renderer registration (double-generic {@code <T, S>} provider form of 26.1.2). */
    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BcBlockEntities.ENGINE_STONE.value(), StoneEngineBlockRenderer::new);
        event.registerBlockEntityRenderer(BcBlockEntities.PIPE_KINESIS_WOOD.value(), KinesisPipeBlockRenderer::new);
        LOGGER.info("BuildCraft core client renderers registered: {} -> {}, {} -> {}",
            BcBlockEntities.ENGINE_STONE.getId(), StoneEngineBlockRenderer.class.getSimpleName(),
            BcBlockEntities.PIPE_KINESIS_WOOD.getId(), KinesisPipeBlockRenderer.class.getSimpleName());
    }
}
