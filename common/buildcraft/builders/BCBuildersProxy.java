/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.builders;

import buildcraft.api.BCModules;
import buildcraft.builders.client.render.RenderArchitectTables;
import buildcraft.builders.client.render.RenderQuarry;
import buildcraft.builders.snapshot.MessageSnapshotRequest;
import buildcraft.builders.snapshot.MessageSnapshotResponse;
import buildcraft.lib.client.render.DetachedRenderer;
import buildcraft.lib.net.MessageManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;

//public abstract class BCBuildersProxy implements IGuiHandler
public abstract class BCBuildersProxy {
    // @SidedProxy
    private static BCBuildersProxy proxy;

    public static BCBuildersProxy getProxy() {
        if (proxy == null) {
            switch (FMLLoader.getDist()) {
                case CLIENT:
                    proxy = new BCBuildersProxy.ClientProxy();
                    break;
                case DEDICATED_SERVER:
                    proxy = new BCBuildersProxy.ServerProxy();
                    break;
            }
        }
        return proxy;
    }

//    @Override

//    @Override

    public void fmlPreInit() {
        MessageManager.registerMessageClass(BCModules.BUILDERS, MessageSnapshotRequest.class, MessageSnapshotRequest.HANDLER, Dist.DEDICATED_SERVER);
        MessageManager.registerMessageClass(BCModules.BUILDERS, MessageSnapshotResponse.class, Dist.CLIENT);
    }

    public void fmlInit() {
    }

    public void fmlPostInit() {
    }

    @SuppressWarnings("unused")
    public static class ServerProxy extends BCBuildersProxy {
    }

    @SuppressWarnings("unused")
    public static class ClientProxy extends BCBuildersProxy {
//        @Override

        @Override
        public void fmlPreInit() {
            super.fmlPreInit();
            // ...
            // ...
            BCBuildersSprites.fmlPreInit();
            RenderQuarry.init();

            MessageManager.setHandler(MessageSnapshotResponse.class, MessageSnapshotResponse.HANDLER, Dist.CLIENT);
        }

        @Override
        public void fmlInit() {
            super.fmlInit();
            DetachedRenderer.INSTANCE.addRenderer(DetachedRenderer.RenderMatrixType.FROM_WORLD_ORIGIN, RenderArchitectTables.INSTANCE);
        }
    }
}
