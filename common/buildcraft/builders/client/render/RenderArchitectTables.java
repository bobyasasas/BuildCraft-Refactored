/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.client.render;

import buildcraft.builders.BCBuildersConfig;
import buildcraft.builders.BCBuildersSprites;
import buildcraft.builders.client.ClientArchitectTables;
import buildcraft.lib.client.model.ModelUtil;
import buildcraft.lib.client.render.DetachedRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public enum RenderArchitectTables implements DetachedRenderer.IDetachedRenderer {
    INSTANCE;

    @Override
    public void render(Player player, float partialTicks, PoseStack poseStack) {
        List<AABB> boxes = new ArrayList<>(ClientArchitectTables.BOXES.keySet());
        boxes.sort(
                Comparator.<AABB>comparingDouble(bb ->
                                bb.getCenter().distanceTo(player.position())
                ).reversed()
        );
        List<BlockPos> poses = new ArrayList<>(ClientArchitectTables.SCANNED_BLOCKS.keySet());
        poses.sort(
                Comparator.<BlockPos>comparingDouble(pos ->
                                Vec3.atLowerCornerOf(pos).distanceTo(player.position())
                ).reversed()
        );

        final boolean __STENCIL = BCBuildersConfig.enableStencil;

        for (AABB bb : boxes) {
            MultiBufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            VertexConsumer buffer = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());
//            Minecraft.getMinecraft().renderEngine.bindTexture(
//                            "buildcraftbuilders",
//                            "textures/blocks/scan.png"
//            RenderSystem.setShaderTexture(
//                    0, new ResourceLocation(
//                            "buildcraftbuilders",
//                            "textures/blocks/scan.png"
            TextureAtlasSprite scan = BCBuildersSprites.ARCHITECT_SCAN.getSprite();
            float u0 = scan.getU0(), u1 = scan.getU1(), v0 = scan.getV0(), v1 = scan.getV1();
            for (BlockPos pos : poses) {
                if (!bb.intersects(new AABB(pos))) {
                    continue;
                }
                poseStack.pushPose();
                for (Direction face : Direction.VALUES) {
                    ModelUtil.createFace(
                                    face,
                                    new Vector3f(pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F),
                                    new Vector3f(0.5F, 0.5F, 0.5F),
                                    new ModelUtil.UvFaceData(u0, v0, u1, v1)
                            )
                            .lighti((byte) 15, (byte) 15)
                            .colouri(
                                    255,
                                    255,
                                    255,
                                    ClientArchitectTables.SCANNED_BLOCKS.get(pos)
                                            * 50
                                            / ClientArchitectTables.START_SCANNED_BLOCK_VALUE
                            )
                            .render(poseStack.last(), buffer);
                }
                poseStack.popPose();
            }
        }
    }

    private final int COLOUR = (15 << 20) | (15 << 4);
    private VertexConsumer buffer = null;
    private PoseStack.Pose pose = null;

    private VertexConsumer vertex(double x, double y, double z, float u, float v) {
        buffer
                .vertex(pose.pose(), (float) (x), (float) (y), (float) (z))
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(COLOUR)
                .normal(pose.normal(), 1, 1, 1)
        ;
        return buffer;
    }
}
