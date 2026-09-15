/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.energy.client;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import org.slf4j.Logger;
import buildcraft.energy.BuildCraftEnergy;
import buildcraft.lib.client.render.BcQuadSmoke;

/**
 * Client-side entry point for the buildcraftenergy mod (task M2.7a). NeoForge allows a second
 * {@link Mod @Mod}-annotated class per modid when it is restricted to {@link Dist#CLIENT}; FML decides from the
 * bytecode scan (no class loading) whether to construct it, so this class is never touched on dedicated servers.
 *
 * <p>This replaces the hand-written {@code FMLEnvironment.getDist().isClient()} guard that used to sit in
 * {@link BuildCraftEnergy#BuildCraftEnergy(IEventBus)}: with the client source set (M2.7a) the boundary is structural —
 * this class is compiled out of the server runs entirely, and every client-side listener registration starts from
 * here.
 */
// The value must match the main entry's modid; dist = CLIENT tells FML to only
// construct this class on the client dist (it is filtered out during scanning).
@Mod(value = BuildCraftEnergy.MOD_ID, dist = Dist.CLIENT)
public class BuildCraftEnergyClient {

    private static final Logger LOGGER = LogUtils.getLogger();

    public BuildCraftEnergyClient(IEventBus modEventBus) {
        // One FluidModel per placeholder fluid pair (avoids 60 Missing FluidModel warnings).
        modEventBus.addListener(BcEnergyFluidModels::onRegisterFluidModels);
        // M2.7a compile+startup smoke check for the new quad toolkit (real renderers come with M2.7b).
        LOGGER.info("BcQuad smoke check: {}", BcQuadSmoke.check() ? "PASS" : "FAIL");
    }
}
