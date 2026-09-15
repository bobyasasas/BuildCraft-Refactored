/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy;

import buildcraft.energy.event.ChristmasHandler;
import net.minecraftforge.fml.loading.FMLLoader;

//public abstract class BCEnergyProxy implements IGuiHandler
public abstract class BCEnergyProxy {
    private static BCEnergyProxy proxy;

    public static BCEnergyProxy getProxy() {
        if (proxy == null) {
            switch (FMLLoader.getDist()) {
                case CLIENT:
                    proxy = new BCEnergyProxy.ClientProxy();
                    break;
                case DEDICATED_SERVER:
                    proxy = new BCEnergyProxy.ServerProxy();
                    break;
            }
        }
        return proxy;
    }

    public void fmlPreInit() {
    }

    public void fmlInit() {
    }

    public void fmlPostInit() {
    }

//    @Override

//    @Override
//            case ENGINE_STONE:
//            case ENGINE_IRON:
//            default:

    public static class ServerProxy extends BCEnergyProxy {
        @Override
        public void fmlPreInit() {
            super.fmlPreInit();
            ChristmasHandler.fmlPreInitDedicatedServer();
        }
    }

    public static class ClientProxy extends BCEnergyProxy {
        @Override
        public void fmlPreInit() {
            super.fmlPreInit();
            ChristmasHandler.fmlPreInitClient();
            BCEnergyModels.fmlPreInit();
            BCEnergySprites.fmlPreInit();
        }

        @Override
        public void fmlInit() {
            super.fmlInit();
            // moved to BCEnergyModels#onTesrReg
        }

//        @Override
//                case ENGINE_STONE:
//                case ENGINE_IRON:
//                default:
    }
}
