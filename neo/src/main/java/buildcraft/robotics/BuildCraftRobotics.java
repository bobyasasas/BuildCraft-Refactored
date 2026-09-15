/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.robotics;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * BuildCraft robotics mod entry point for the NeoForge 26.1.2 port (task M2.4a eight-mod skeleton). Legacy
 * counterpart: {@code buildcraft.robotics.BCRobotics}. Placeholder registrations (see the {@code BcRobotics*}
 * centres) arrive in M2.4c.
 */
// The value here should match the modId in META-INF/neoforge.mods.toml
@Mod(BuildCraftRobotics.MOD_ID)
public class BuildCraftRobotics {

    public static final String MOD_ID = "buildcraftrobotics";

    private static final Logger LOGGER = LogUtils.getLogger();

    public BuildCraftRobotics(IEventBus modEventBus) {
        LOGGER.info("BuildCraft robotics (neo skeleton) loaded");
        BcRoboticsBlocks.BLOCKS.register(modEventBus);
        BcRoboticsItems.ITEMS.register(modEventBus);
        BcRoboticsBlockEntities.BLOCK_ENTITIES.register(modEventBus);
    }
}
