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
 * BuildCraft energy mod entry point for the NeoForge 26.1.2 port (task M2.4a eight-mod skeleton, full registry parity
 * including fluids since M2.4b). Legacy counterpart: {@code buildcraft.energy.BCEnergy}. The registrations are
 * placeholders (see the {@code BcEnergy*} centres); real behaviour classes migrate in M2.5+.
 *
 * <p><b>Source set boundary (M2.7a):</b> this class (and everything under {@code src/main/java}) must never reference
 * anything from the {@code client} source set — the main compile classpath does not contain it. Client-only listener
 * registration lives in the client entry point {@code buildcraft.energy.client.BuildCraftEnergyClient}
 * ({@code @Mod(dist = Dist.CLIENT)}), which replaces the former {@code FMLEnvironment} dist guard here.
 */
// The value here should match the modId in META-INF/neoforge.mods.toml.
// (buildcraft.energy.client.BuildCraftEnergyClient adds a second @Mod entry for
// the same modid restricted to Dist.CLIENT — the documented way to get a
// client-only constructor without dist guards.)
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
    }
}
