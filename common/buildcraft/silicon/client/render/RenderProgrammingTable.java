/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.client.render;

import buildcraft.silicon.tile.TileProgrammingTable_Neptune;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.LazyLoadedValue;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RenderProgrammingTable implements BlockEntityRenderer<TileProgrammingTable_Neptune> {
    private final LazyLoadedValue<TextureAtlasSprite> glass_white = new LazyLoadedValue<>(() ->
            Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(new ResourceLocation("block/white_stained_glass"))
    );

    public RenderProgrammingTable(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TileProgrammingTable_Neptune tile, float partial, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
        Minecraft.getInstance().getProfiler().push("bc");
        Minecraft.getInstance().getProfiler().push("table");
        Minecraft.getInstance().getProfiler().push("programming");

        poseStack.pushPose();
        TextureAtlasSprite sprite = glass_white.get();
        int light1 = combinedLight >> 16 & 65535;
        int light2 = combinedLight & 65535;

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.translucent());

        PoseStack.Pose pose = poseStack.last();


        poseStack.popPose();

        Minecraft.getInstance().getProfiler().pop();
        Minecraft.getInstance().getProfiler().pop();
        Minecraft.getInstance().getProfiler().pop();
    }
}
