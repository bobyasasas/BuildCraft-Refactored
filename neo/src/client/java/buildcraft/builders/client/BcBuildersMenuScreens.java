/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders.client;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.slf4j.Logger;
import buildcraft.builders.BcBuildersMenus;
import buildcraft.builders.BuildCraftBuilders;
import buildcraft.builders.client.gui.FillerScreen;

/**
 * Client-side menu screen registration for buildcraftbuilders (task M4.8, the lib/gui framework's client half): wires
 * the filler menu type to its screen through {@link RegisterMenuScreensEvent}, the 26.1.2 replacement for the legacy
 * {@code MenuScreens.register} call in {@code BCBuildersGuis}. Lives in the client source set and is dist-gated to
 * {@link Dist#CLIENT}, so dedicated servers never load it; the mod-bus event is picked up by the
 * {@link EventBusSubscriber} scan.
 */
@EventBusSubscriber(modid = BuildCraftBuilders.MOD_ID, value = Dist.CLIENT)
public final class BcBuildersMenuScreens {

    private static final Logger LOGGER = LogUtils.getLogger();

    private BcBuildersMenuScreens() {
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(BcBuildersMenus.FILLER.value(), FillerScreen::new);
        LOGGER.info("BuildCraft builders menu screens registered: {} -> {}",
                BcBuildersMenus.FILLER.getId(), FillerScreen.class.getSimpleName());
    }
}
