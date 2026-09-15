/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport;

import buildcraft.api.BCModules;
import buildcraft.api.transport.pipe.PipeApiClient;
import buildcraft.lib.net.MessageManager;
import buildcraft.transport.client.PipeRegistryClient;
import buildcraft.transport.client.render.PipeWireRenderer;
import buildcraft.transport.net.MessageMultiPipeItem;
import buildcraft.transport.wire.MessageWireSystems;
import buildcraft.transport.wire.MessageWireSystemsPowered;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;

//public abstract class BCTransportProxy implements IGuiHandler
public abstract class BCTransportProxy {
    private static BCTransportProxy proxy;

    public static BCTransportProxy getProxy() {
        if (proxy == null) {
            switch (FMLLoader.getDist()) {
                case CLIENT:
                    proxy = new BCTransportProxy.ClientProxy();
                    break;
                case DEDICATED_SERVER:
                    proxy = new BCTransportProxy.ServerProxy();
                    break;
            }
        }
        return proxy;
    }

//    @Override

//    @Override

    public void fmlPreInit() {
        MessageManager.registerMessageClass(BCModules.TRANSPORT, MessageWireSystems.class, Dist.CLIENT);
        MessageManager.registerMessageClass(BCModules.TRANSPORT, MessageWireSystemsPowered.class, Dist.CLIENT);
        MessageManager.registerMessageClass(BCModules.TRANSPORT, MessageMultiPipeItem.class, Dist.CLIENT);
    }

    public void fmlInit() {
    }

    public void fmlPostInit() {
    }

    @SuppressWarnings("unused")
    public static class ServerProxy extends BCTransportProxy {
    }

    @SuppressWarnings("unused")
    public static class ClientProxy extends BCTransportProxy {
        @Override
        public void fmlPreInit() {
            super.fmlPreInit();
            BCTransportSprites.fmlPreInit();
            BCTransportModels.fmlPreInit();
            PipeApiClient.registry = PipeRegistryClient.INSTANCE;
            PipeWireRenderer.init();

            MessageManager.setHandler(MessageWireSystems.class, MessageWireSystems.HANDLER, Dist.CLIENT);
            MessageManager.setHandler(MessageWireSystemsPowered.class, MessageWireSystemsPowered.HANDLER, Dist.CLIENT);
            MessageManager.setHandler(MessageMultiPipeItem.class, MessageMultiPipeItem.HANDLER, Dist.CLIENT);
        }

        @Override
        public void fmlInit() {
            super.fmlInit();
            BCTransportModels.fmlInit();
        }

        @Override
        public void fmlPostInit() {
            super.fmlPostInit();
            BCTransportModels.fmlPostInit();
        }

//        @Override
    }
}
