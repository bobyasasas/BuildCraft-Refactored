/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.render.laser;

import buildcraft.lib.misc.RenderUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class LaserCompiledList {
    private static final RenderType LASER_RENDER_TYPE_FORMAT_ALL = RenderType.create(
            "buildcraft_laser_all",
            LaserRenderer_BC8.FORMAT_ALL, VertexFormat.Mode.QUADS,
            RenderType.TRANSIENT_BUFFER_SIZE,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setShaderState(RenderStateShard.RENDERTYPE_CUTOUT_SHADER)
                    .setTextureState(RenderStateShard.BLOCK_SHEET)
                    .createCompositeState(true)
    );

    public abstract void render(PoseStack.Pose modelViewMatrix);

    public abstract void delete();

    public static class Builder implements ILaserRenderer, AutoCloseable {
        public final RenderUtil.AutoTessellator tess;

        public Builder() {
            tess = RenderUtil.getThreadLocalUnusedTessellator();
            BufferBuilder bufferBuilder = tess.tessellator.getBuilder();
            bufferBuilder.begin(VertexFormat.Mode.QUADS, LaserRenderer_BC8.FORMAT_ALL);
        }

        @Override
        public void vertex(
                double x, double y, double z,
                double u, double v,
                int lmap,
                int overlay,
                float nx,
                float ny,
                float nz,
                float diffuse
        ) {
            BufferBuilder bufferBuilder = tess.tessellator.getBuilder();
            bufferBuilder.vertex(x, y, z);
            bufferBuilder.color(diffuse, diffuse, diffuse, 1.0f);
            bufferBuilder.uv((float) u, (float) v);
            bufferBuilder.uv2(lmap);
            bufferBuilder.endVertex();
        }

        public LaserCompiledList build() {
            BufferBuilder bufferBuilder = tess.tessellator.getBuilder();
            VertexBuffer vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            BufferBuilder.RenderedBuffer bufferbuilder$renderedbuffer = bufferBuilder.end();
            vertexBuffer.bind();
            vertexBuffer.upload(bufferbuilder$renderedbuffer);
            return new Vbo(vertexBuffer);
        }

        @Override
        public void close() {
            tess.close();
        }
    }

//        @Override
//        @Override

    private static class Vbo extends LaserCompiledList {
        private final VertexBuffer vertexBuffer;

        private Vbo(VertexBuffer vertexBuffer) {
            this.vertexBuffer = vertexBuffer;
        }

        @Override
        public void render(PoseStack.Pose modelViewMatrix) {
            LASER_RENDER_TYPE_FORMAT_ALL.setupRenderState();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            vertexBuffer.bind();
            vertexBuffer.drawWithShader(modelViewMatrix.pose(), RenderSystem.getProjectionMatrix(), GameRenderer.getPositionColorTexLightmapShader());
            LASER_RENDER_TYPE_FORMAT_ALL.clearRenderState();
        }

        @Override
        public void delete() {
            vertexBuffer.close();
        }
    }
}
