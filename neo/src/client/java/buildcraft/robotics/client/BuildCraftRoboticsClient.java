/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.robotics.client;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.slf4j.Logger;
import buildcraft.robotics.BcRoboticsEntities;
import buildcraft.robotics.BuildCraftRobotics;
import buildcraft.robotics.client.render.RobotEntityRenderer;

/**
 * Client-side entry point for the buildcraftrobotics mod (task M2.13), mirroring
 * {@code buildcraft.builders.client.BuildCraftBuildersClient}: a second {@link Mod @Mod}-annotated class for the same
 * modid, restricted to {@link Dist#CLIENT} so FML filters it out during the bytecode scan on dedicated servers (the
 * class also only exists in the {@code client} source set, M2.7a).
 *
 * <p>Registers the tree's first real entity renderer (the M2.13 robot_miner slice) through
 * {@link EntityRenderersEvent.RegisterRenderers#registerEntityRenderer}, the only legal registration entry on
 * NeoForge (vanilla {@code EntityRenderers.register} is package private); the event fires on the mod event bus when
 * the resource manager builds the dispatcher's renderer table.
 */
// The value must match the main entry's modid; dist = CLIENT tells FML to only
// construct this class on the client dist (it is filtered out during scanning).
@Mod(value = BuildCraftRobotics.MOD_ID, dist = Dist.CLIENT)
public class BuildCraftRoboticsClient {

    private static final Logger LOGGER = LogUtils.getLogger();

    public BuildCraftRoboticsClient(IEventBus modEventBus) {
        modEventBus.addListener(BuildCraftRoboticsClient::onRegisterRenderers);
    }

    /** M2.13: entity renderer registration (the {@code EntityRendererProvider} lambda form of 26.1.2). */
    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BcRoboticsEntities.ROBOT_MINER.value(), RobotEntityRenderer::new);
        LOGGER.info("BuildCraft robotics client renderers registered: {} -> {}",
            BcRoboticsEntities.ROBOT_MINER.getId(), RobotEntityRenderer.class.getSimpleName());
    }
}
