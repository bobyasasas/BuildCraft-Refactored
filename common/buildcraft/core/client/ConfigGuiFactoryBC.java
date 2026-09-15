/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.client;

import buildcraft.core.BCCoreConfig;
import buildcraft.lib.config.ConfigCategory;
import net.minecraft.client.gui.screens.Screen;

//public class ConfigGuiFactoryBC implements ModGuiFactory
public class ConfigGuiFactoryBC {
    // public static class GuiConfigManager extends GuiConfig
    public static class GuiConfigManager {
        public GuiConfigManager(Screen parentScreen) {

            for (ConfigCategory<?> s : BCCoreConfig.config.getAll()) {
                if (s.getFullPath().split("\\.").length == 1) {
                }
            }

            for (ConfigCategory<?> s : BCCoreConfig.objConfig.getAll()) {
                if (s.getFullPath().split("\\.").length == 1) {
                }
            }
        }
    }

    /**
     * Needed for forge IModGuiFactory
     */
    public ConfigGuiFactoryBC() {
    }

//    @Override
//        // We don't need to do anything


//    @Override


//    @Override

//    @Override
}
