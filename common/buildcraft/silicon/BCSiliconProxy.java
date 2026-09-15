/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon;

import net.minecraftforge.fml.loading.FMLLoader;

//public abstract class BCSiliconProxy implements IGuiHandler
public abstract class BCSiliconProxy {
    private static BCSiliconProxy proxy;

    public static BCSiliconProxy getProxy() {
        if (proxy == null) {
            switch (FMLLoader.getDist()) {
                case CLIENT:
                    proxy = new BCSiliconProxy.ClientProxy();
                    break;
                case DEDICATED_SERVER:
                    proxy = new BCSiliconProxy.ServerProxy();
                    break;
            }
        }
        return proxy;
    }

//    @Override

//    @Override

    public void fmlPreInit() {
    }

    public void fmlInit() {
    }

    public void fmlPostInit() {
    }

    @SuppressWarnings("unused")
    public static class ServerProxy extends BCSiliconProxy {
    }

    @SuppressWarnings("unused")
    public static class ClientProxy extends BCSiliconProxy {

        @Override
        public void fmlPreInit() {
            super.fmlPreInit();
            BCSiliconSprites.fmlPreInit();
            BCSiliconModels.fmlPreInit();
        }

        @Override
        public void fmlInit() {
            super.fmlInit();
            BCSiliconModels.fmlInit();
        }

        @Override
        public void fmlPostInit() {
            super.fmlPostInit();
            BCSiliconModels.fmlPostInit();
        }

//        @Override
    }
}
