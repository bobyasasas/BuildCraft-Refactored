/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib;

import buildcraft.api.registry.EventBuildCraftReload;
import buildcraft.lib.client.guide.GuideManager;
import buildcraft.lib.client.model.ModelHolderRegistry;
import buildcraft.lib.client.reload.ReloadManager;
import buildcraft.lib.client.render.fluid.FluidRenderer;
import buildcraft.lib.client.render.laser.LaserRenderer_BC8;
import buildcraft.lib.client.sprite.SpriteHolderRegistry;
import buildcraft.lib.misc.data.ModelVariableData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.function.Consumer;

public enum BCLibEventDistModBus {
    INSTANCE;

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void modelBake(ModelEvent.BakingCompleted event) {
        SpriteHolderRegistry.exportTextureMap((TextureAtlas) Minecraft.getInstance().textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS));
        SpriteHolderRegistry.exportTextureMap(FluidRenderer.FROZEN_ATLAS);
        LaserRenderer_BC8.clearModels();
        ModelHolderRegistry.onModelBake();
        ModelVariableData.onModelBake();
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void textureStitchPre(ModelEvent.ModifyBakingResult event) {
        ReloadManager.INSTANCE.preReloadResources();
        SpriteHolderRegistry.onTextureStitchPre();
        ModelHolderRegistry.onTextureStitchPre();
    }


    public static void onDatagenTextureRegister(Consumer<ResourceLocation> consumer, ExistingFileHelper fileHelper) {
        SpriteHolderRegistry.onDatagenTextureRegister(consumer, fileHelper);
        ModelHolderRegistry.onDatagenTextureRegister(consumer, fileHelper);
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void textureStitchPost(TextureStitchEvent.Post event) {

        // or the engine texture will not be loaded
        SpriteHolderRegistry.onTextureStitchPost(event);
        FluidRenderer.onTextureStitchPost(event);
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onReloadFinish(EventBuildCraftReload.FinishLoad event) {
        // Note: when you need to add server-side listeners the client listeners need to be moved to BCLibProxy
        GuideManager.INSTANCE.onRegistryReload(event);
    }
}
