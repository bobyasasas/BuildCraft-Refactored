/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.factory;

import buildcraft.factory.client.render.RenderMiningWell;
import buildcraft.factory.client.render.RenderPump;
import net.minecraftforge.fml.loading.FMLLoader;

//public abstract class BCFactoryProxy implements IGuiHandler
public abstract class BCFactoryProxy {
    private static BCFactoryProxy proxy;

    public static BCFactoryProxy getProxy() {
        if (proxy == null) {
            switch (FMLLoader.getDist()) {
                case CLIENT:
                    proxy = new BCFactoryProxy.ClientProxy();
                    break;
                case DEDICATED_SERVER:
                    proxy = new BCFactoryProxy.ServerProxy();
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
    public static class ServerProxy extends BCFactoryProxy {
    }

    @SuppressWarnings("unused")
    public static class ClientProxy extends BCFactoryProxy {
//        @Override

        @Override
        public void fmlPreInit() {
            super.fmlPreInit();
            RenderPump.init();
            RenderMiningWell.init();
            BCFactoryModels.fmlPreInit();
        }

        @Override
        public void fmlInit() {
            super.fmlInit();
        }
    }
}
