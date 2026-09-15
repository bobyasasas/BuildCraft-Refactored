/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 * <p/>
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.builders.client.render;

import buildcraft.builders.tile.TileMarkerConstruction;
import buildcraft.core.BCCoreConfig;
import buildcraft.core.client.BuildCraftLaserManager;
import buildcraft.lib.client.render.ItemRenderUtil;
import buildcraft.lib.client.render.laser.LaserBoxRenderer;
import buildcraft.lib.client.render.laser.LaserData_BC8;
import buildcraft.lib.client.render.laser.LaserRenderer_BC8;
import buildcraft.lib.misc.RenderUtil;
import buildcraft.lib.misc.data.Box;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public class RenderMarkerConstruction implements BlockEntityRenderer<TileMarkerConstruction> {

    public RenderMarkerConstruction(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TileMarkerConstruction marker, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
        renderBox(marker, poseStack, bufferSource);

        if (marker != null) {
            poseStack.pushPose();

            poseStack.translate(-marker.getBlockPos().getX(), -marker.getBlockPos().getY(), -marker.getBlockPos().getZ());

            if (marker.laser != null) {
                poseStack.pushPose();
                VertexConsumer buffer = bufferSource.getBuffer(RenderType.translucent());
                LaserData_BC8 laser = new LaserData_BC8(BuildCraftLaserManager.STRIPES_WRITE, marker.laser.getFirst(), marker.laser.getSecond(), 0.5 / 16.0);
                LaserRenderer_BC8.renderLaserStatic(laser, poseStack.last());
                poseStack.popPose();
            }

            if (!marker.itemBlueprint.isEmpty()) {
                VertexConsumer buffer = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());
                doRenderItem(
                        poseStack,
                        buffer,
                        marker.itemBlueprint,
                        marker.getBlockPos().getX() + 0.5F,
                        marker.getBlockPos().getY() + 0.2F,
                        marker.getBlockPos().getZ() + 0.5F,
                        RenderUtil.getCombinedLight(marker.getLevel(), new BlockPos(marker.getBlockPos())),
                        marker.direction
                );
            }

            poseStack.popPose();

            if (marker.bluePrintBuilder != null) {
                VertexConsumer buffer = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());
                RenderSnapshotBuilder.render(marker.bluePrintBuilder, marker.getLevel(), marker.getBlockPos(), partialTicks, poseStack, buffer);
            }
        }
    }

    public void doRenderItem(PoseStack poseStack, VertexConsumer buffer, ItemStack stack, double x, double y, double z, int combinedLight, Direction facing) {
        if (stack == null) {
            return;
        }

        float renderScale = 1.5f;
        poseStack.pushPose();
        poseStack.translate((float) x, (float) y, (float) z);
        poseStack.translate(0, 0.25F, 0);
        poseStack.scale(renderScale, renderScale, renderScale);
        ItemRenderUtil.renderItemStack(stack, combinedLight, facing, poseStack, buffer);

        poseStack.popPose();
    }

    // @Override
    public void renderBox(TileMarkerConstruction tileentity, PoseStack poseStack, MultiBufferSource bufferSource) {
        VertexConsumer buffer = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());

        poseStack.pushPose();
        poseStack.translate(-tileentity.getBlockPos().getX(), -tileentity.getBlockPos().getY(), -tileentity.getBlockPos().getZ());

        Box box = tileentity.box;
        LaserBoxRenderer.renderLaserBoxStatic(box, BuildCraftLaserManager.STRIPES_WRITE, poseStack.last(), true);

        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(TileMarkerConstruction te) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return BCCoreConfig.markerMaxDistance * 2;
    }
}
