/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.energy;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * BuildCraft energy mod entry point for the NeoForge 26.1.2 port (task M2.4a eight-mod skeleton). Legacy
 * counterpart: {@code buildcraft.energy.BCEnergy}. Placeholder registrations (see the {@code BcEnergy*} centres)
 * arrive in M2.4b.
 */
// The value here should match the modId in META-INF/neoforge.mods.toml
@Mod(BuildCraftEnergy.MOD_ID)
public class BuildCraftEnergy {

    public static final String MOD_ID = "buildcraftenergy";

    private static final Logger LOGGER = LogUtils.getLogger();

    public BuildCraftEnergy(IEventBus modEventBus) {
        LOGGER.info("BuildCraft energy (neo skeleton) loaded");
        BcEnergyBlocks.BLOCKS.register(modEventBus);
        BcEnergyItems.ITEMS.register(modEventBus);
        BcEnergyBlockEntities.BLOCK_ENTITIES.register(modEventBus);
    }
}
