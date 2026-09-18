/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.slf4j.Logger;
import buildcraft.factory.client.render.DistillerBlockRenderer;
import buildcraft.factory.client.render.HeatExchangeBlockRenderer;
import buildcraft.factory.client.render.TankBlockRenderer;

/**
 * Client-side entry point for the buildcraftfactory mod (M4.7), the same {@code @Mod(modid, dist = CLIENT)} pattern as
 * {@code buildcraft.transport.BuildCraftTransportClient}: a second mod class for the same modid that FML only
 * constructs on the client dist (and which lives in the {@code client} source set, compiled out of dedicated
 * servers). Registers the M4.7 machine renderers: the tank's liquid column, the distiller's three tank windows and
 * the heat exchanger's jsonbc geometry + two windows. The pump needs no renderer (its cube model is static).
 */
// The value must match the main entry's modid; dist = CLIENT tells FML to only
// construct this class on the client dist (it is filtered out during scanning).
@Mod(value = BuildCraftFactory.MOD_ID, dist = Dist.CLIENT)
public class BuildCraftFactoryClient {

    private static final Logger LOGGER = LogUtils.getLogger();

    public BuildCraftFactoryClient(IEventBus modEventBus) {
        modEventBus.addListener(BuildCraftFactoryClient::onRegisterRenderers);
    }

    /** M4.7: the machine block entity renderers (fluid windows + the heat exchange jsonbc geometry). */
    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BcFactoryBlockEntities.TANK.value(), TankBlockRenderer::new);
        event.registerBlockEntityRenderer(BcFactoryBlockEntities.DISTILLER.value(), DistillerBlockRenderer::new);
        event.registerBlockEntityRenderer(BcFactoryBlockEntities.HEAT_EXCHANGE.value(), HeatExchangeBlockRenderer::new);
        LOGGER.info("BuildCraft factory client renderers registered: tank, distiller, heat_exchange");
    }
}
