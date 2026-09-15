/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.energy;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import buildcraft.energy.client.BcEnergyFluidModels;

/**
 * BuildCraft energy mod entry point for the NeoForge 26.1.2 port (task M2.4a eight-mod skeleton, full registry parity
 * including fluids since M2.4b). Legacy counterpart: {@code buildcraft.energy.BCEnergy}. The registrations are
 * placeholders (see the {@code BcEnergy*} centres); real behaviour classes migrate in M2.5+.
 */
// The value here should match the modId in META-INF/neoforge.mods.toml
@Mod(BuildCraftEnergy.MOD_ID)
public class BuildCraftEnergy {

    public static final String MOD_ID = "buildcraftenergy";

    private static final Logger LOGGER = LogUtils.getLogger();

    public BuildCraftEnergy(IEventBus modEventBus) {
        LOGGER.info("BuildCraft energy (neo skeleton) loaded");
        BcEnergyFluidTypes.FLUID_TYPES.register(modEventBus);
        BcEnergyFluids.FLUIDS.register(modEventBus);
        BcEnergyBlocks.BLOCKS.register(modEventBus);
        BcEnergyItems.ITEMS.register(modEventBus);
        BcEnergyBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        if (FMLEnvironment.getDist().isClient()) {
            // Client-only hook: registers one FluidModel per placeholder fluid pair (avoids 60 Missing FluidModel
            // warnings). The method reference must stay behind this guard, the class loads client classes.
            modEventBus.addListener(BcEnergyFluidModels::onRegisterFluidModels);
        }
    }
}
