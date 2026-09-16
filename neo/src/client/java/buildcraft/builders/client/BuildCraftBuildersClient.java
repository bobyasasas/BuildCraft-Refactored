/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders.client;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.slf4j.Logger;
import buildcraft.builders.BuildCraftBuilders;
import buildcraft.builders.BcBuildersBlockEntities;
import buildcraft.builders.client.render.FillerBlockRenderer;
import buildcraft.builders.client.render.QuarryBlockRenderer;

/**
 * Client-side entry point for the buildcraftbuilders mod (task M2.12), mirroring
 * {@code buildcraft.core.client.BuildCraftCoreClient}: a second {@link Mod @Mod}-annotated class for the same modid,
 * restricted to {@link Dist#CLIENT} so FML filters it out during the bytecode scan on dedicated servers (the class is
 * also compiled out of the server runs entirely &mdash; it lives in the {@code client} source set, M2.7a).
 *
 * <p>Registers the mod's first real block entity renderers (the M2.12 filler and quarry slice; the remaining baseline
 * renderers migrate with their content) through {@link EntityRenderersEvent.RegisterRenderers}, the only legal
 * registration entry on NeoForge (vanilla {@code BlockEntityRenderers.register} is package private; the event fires
 * on the mod event bus when the resource manager builds the dispatcher's renderer table).
 */
// The value must match the main entry's modid; dist = CLIENT tells FML to only
// construct this class on the client dist (it is filtered out during scanning).
@Mod(value = BuildCraftBuilders.MOD_ID, dist = Dist.CLIENT)
public class BuildCraftBuildersClient {

    private static final Logger LOGGER = LogUtils.getLogger();

    public BuildCraftBuildersClient(IEventBus modEventBus) {
        modEventBus.addListener(BuildCraftBuildersClient::onRegisterRenderers);
    }

    /** M2.12: block entity renderer registration (double-generic {@code <T, S>} provider form of 26.1.2). */
    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BcBuildersBlockEntities.FILLER.value(), FillerBlockRenderer::new);
        event.registerBlockEntityRenderer(BcBuildersBlockEntities.QUARRY.value(), QuarryBlockRenderer::new);
        LOGGER.info("BuildCraft builders client renderers registered: {} -> {}, {} -> {}",
            BcBuildersBlockEntities.FILLER.getId(), FillerBlockRenderer.class.getSimpleName(),
            BcBuildersBlockEntities.QUARRY.getId(), QuarryBlockRenderer.class.getSimpleName());
    }
}
