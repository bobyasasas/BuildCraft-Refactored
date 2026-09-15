/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.robotics.gui;

import buildcraft.core.BCCoreItems;
import buildcraft.core.item.ItemPaintbrush_BC8;
import buildcraft.lib.gui.GuiBC8;
import buildcraft.lib.gui.GuiIcon;
import buildcraft.lib.gui.pos.GuiRectangle;
import buildcraft.lib.misc.RenderUtil;
import buildcraft.robotics.container.ContainerZonePlanner;
import buildcraft.robotics.zone.*;
import buildcraft.robotics.zone.ZonePlannerMapChunk.MapColourData;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GuiZonePlanner extends GuiBC8<ContainerZonePlanner> {
    private static final ResourceLocation TEXTURE_BASE = new ResourceLocation("buildcraftrobotics:textures/gui/zone_planner.png");
    private static final int SIZE_X = 256, SIZE_Y = 228;
    private static final GuiIcon ICON_GUI = new GuiIcon(TEXTURE_BASE, 0, 0, SIZE_X, SIZE_Y);
    private static final GuiIcon ICON_PROGRESS_INPUT = new GuiIcon(TEXTURE_BASE, 9, 228, 28, 9);
    private static final GuiIcon ICON_PROGRESS_OUTPUT = new GuiIcon(TEXTURE_BASE, 0, 228, 9, 28);
    private static final GuiRectangle RECT_PROGRESS_INPUT = new GuiRectangle(44, 128, 28, 9);
    private static final GuiRectangle RECT_PROGRESS_OUTPUT = new GuiRectangle(236, 45, 9, 28);
    private float startMouseX = 0;
    private float startMouseY = 0;
    private float startPositionX = 0;
    private float startPositionZ = 0;
    private float camY = 256;
    private float scaleSpeed = 0;
    private float positionX;
    private float positionZ;
    private boolean canDrag = false;
    private BlockPos lastSelected = null;
    private BlockPos selectionStartXZ = null;
    private ZonePlan bufferLayer = null;

    private static final RenderType CUBOID_LINES_RENDER_TYPE = RenderType.create(
            "buildcraft_zone_planner_gui_lines",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.LINE_STRIP,
            RenderType.TRANSIENT_BUFFER_SIZE,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(2.0f)))
                    .createCompositeState(false)
    );

    public GuiZonePlanner(ContainerZonePlanner container, Inventory inventory, Component component) {
        super(container, inventory, component);
        imageWidth = SIZE_X;
        imageHeight = SIZE_Y;
        BlockPos tilePos = container.tile.getBlockPos();
        positionX = tilePos.getX();
        positionZ = tilePos.getZ();
    }

    private ItemStack getCurrentStack() {
        return minecraft.player.containerMenu.getCarried();
    }

    private ItemStack getPaintbrush() {
        ItemStack currentStack = getCurrentStack();
        if (!currentStack.isEmpty() && currentStack.getItem() instanceof ItemPaintbrush_BC8) {
            return currentStack;
        }
        return null;
    }

    private ItemPaintbrush_BC8.Brush getPaintbrushBrush() {
        ItemStack paintbrush = getPaintbrush();
        if (paintbrush != null) {
            ItemPaintbrush_BC8.Brush brush = BCCoreItems.paintbrushClean.get().getBrushFromStack(paintbrush);
            if (brush.colour != null) {
                return brush;
            }
        }
        return null;
    }

//    @Override

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0) {
            scaleSpeed -= delta * 4.0F;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
//    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        canDrag = false;
        if (getPaintbrush() != null) {
            if (lastSelected != null) {
                selectionStartXZ = lastSelected;
            }
        } else if (getCurrentStack().isEmpty()) {
            startPositionX = positionX;
            startPositionZ = positionZ;
            startMouseX = (float) mouseX;
            startMouseY = (float) mouseY;
            canDrag = true;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int clickedMouseButton, double startX, double startY) {
        super.mouseDragged(mouseX, mouseY, clickedMouseButton, startX, startY);
        if (!canDrag) {
            if (lastSelected != null && getPaintbrushBrush() != null) {
                bufferLayer = new ZonePlan(container.tile.layers[getPaintbrushBrush().colour.getId()]);
                if (selectionStartXZ != null && getPaintbrushBrush() != null && lastSelected != null) {
                    for (int x = Math.min(selectionStartXZ.getX(), lastSelected.getX());
                         x < Math.max(selectionStartXZ.getX(), lastSelected.getX());
                         x++) {
                        for (int z = Math.min(selectionStartXZ.getZ(), lastSelected.getZ());
                             z < Math.max(selectionStartXZ.getZ(), lastSelected.getZ());
                             z++) {
                            if (clickedMouseButton == 0) {
                                bufferLayer.set(
                                        x - container.tile.getBlockPos().getX(),
                                        z - container.tile.getBlockPos().getZ(),
                                        true
                                );
                            } else if (clickedMouseButton == 1) {
                                bufferLayer.set(
                                        x - container.tile.getBlockPos().getX(),
                                        z - container.tile.getBlockPos().getZ(),
                                        false
                                );
                            }
                        }
                    }
                }
            }
            return true;
        }
        float deltaX = (float) (mouseX - startMouseX);
        float deltaY = (float) (mouseY - startMouseY);
        float s = 0.3F;
        positionX = startPositionX - deltaX * s;
        positionZ = startPositionZ - deltaY * s;

        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        selectionStartXZ = null;
        if (getPaintbrushBrush() != null && bufferLayer != null) {
            container.tile.layers[getPaintbrushBrush().colour.getId()] = bufferLayer;
            container.tile.sendLayerToServer(getPaintbrushBrush().colour.getId());
        }
        bufferLayer = null;

        return true;
    }

    @Override
    protected void drawBackgroundLayer(float partialTicks, GuiGraphics guiGraphics) {
        ICON_GUI.drawAt(mainGui.rootElement, guiGraphics);

        drawProgress(
                RECT_PROGRESS_INPUT,
                ICON_PROGRESS_INPUT,
                guiGraphics,
                container.tile.deltaProgressInput.getDynamic(partialTicks),
                1
        );
        drawProgress(
                RECT_PROGRESS_OUTPUT,
                ICON_PROGRESS_OUTPUT,
                guiGraphics,
                1,
                container.tile.deltaProgressOutput.getDynamic(partialTicks)
        );
    }

    private BlockPos rayTrace(double screenX, double screenY, PoseStack modelViewStack, Matrix4f projectionMatrix) {
        BlockPos found = null;

        int[] viewport = { GlStateManager.Viewport.x(), GlStateManager.Viewport.y(), GlStateManager.Viewport.width(), GlStateManager.Viewport.height() };

        Vector3f positionNear = new Vector3f();
        Vector3f positionFar = new Vector3f();

        Matrix4f mul = new Matrix4f(projectionMatrix).mul(modelViewStack.last().pose()); // mul: A.mul(B)=B*A
        mul.unproject((float) screenX, (float) screenY, 0f, viewport, positionNear);
        mul.unproject((float) screenX, (float) screenY, 1f, viewport, positionFar);

        Vector3d rayStart = new Vector3d(positionNear.get(0), positionNear.get(1), positionNear.get(2));
        Vector3d rayPosition = new Vector3d(rayStart);
        Vector3d rayDirection = new Vector3d(positionFar.get(0), positionFar.get(1), positionFar.get(2));
        rayDirection.sub(rayStart);
        rayDirection.normalize();

        for (int i = 0; i < 10000; i++) {
            int chunkX = (int) Math.round(rayPosition.get(0)) >> 4;
            int chunkZ = (int) Math.round(rayPosition.get(2)) >> 4;
            ZonePlannerMapChunk zonePlannerMapChunk = ZonePlannerMapDataClient.INSTANCE.getChunk(
                    minecraft.level,
                    new ZonePlannerMapChunkKey(
                            new ChunkPos(chunkX, chunkZ),
//                            minecraft.level.provider.getDimension(),
                            minecraft.level.dimension(),
                            container.tile.getLevelBC()
                    )
            );
            if (zonePlannerMapChunk != null) {
                BlockPos pos = new BlockPos(
//                        Math.round(rayPosition.getX()) - (chunkX << 4),
                        (int) Math.round(rayPosition.get(0)) - (chunkX << 4),
//                        Math.round(rayPosition.getY()),
                        (int) Math.round(rayPosition.get(1)),
                        (int) Math.round(rayPosition.get(2)) - (chunkZ << 4)
                );
                MapColourData data = zonePlannerMapChunk.getData(pos.getX(), pos.getZ());
                if (data != null && data.posY >= pos.getY()) {
                    found = new BlockPos(pos.getX() + (chunkX << 4), data.posY, pos.getZ() + (chunkZ << 4));
                    break;
                }
            } else {
                break;
            }
            rayPosition.add(rayDirection);
        }
        return found;
    }

    @SuppressWarnings("PointlessBitwiseExpression")
    @Override
    protected void drawForegroundLayer(GuiGraphics guiGraphics) {
        PoseStack poseStack = guiGraphics.pose();
        camY += scaleSpeed;
        scaleSpeed *= 0.7F;
        int posX = (int) positionX;
        int posZ = (int) positionZ;
        ResourceKey<Level> dimension = minecraft.level.dimension();
        {
            ChunkPos chunkPos = new ChunkPos(posX >> 4, posZ >> 4);
            ZonePlannerMapChunk zonePlannerMapChunk = ZonePlannerMapDataClient.INSTANCE.getChunk(
                    // mc.world,
                    minecraft.level,
                    new ZonePlannerMapChunkKey(
                            chunkPos,
                            dimension,
                            container.tile.getLevelBC()
                    )
            );
            BlockPos pos = null;
            if (zonePlannerMapChunk != null) {
                MapColourData data = zonePlannerMapChunk.getData(posX, posZ);
                if (data != null) {
                    pos = new BlockPos(posX, data.posY, posZ);
                }
            }
            if (pos != null && pos.getY() + 10 > camY) {
                camY = Math.max(camY, pos.getY() + 10);
            }
        }
        int x = leftPos;
        int y = topPos;
        if (lastSelected != null) {
            String text = "X: " + lastSelected.getX() + " Y: " + lastSelected.getY() + " Z: " + lastSelected.getZ();
            guiGraphics.drawString(font, text, x + 130, y + 130, 0x404040, false);
        }
        int offsetX = 8;
        int offsetY = 9;
        int sizeX = 213;
        int sizeY = 100;
        RenderSystem.backupProjectionMatrix();
        Matrix4f projectionMatrix = new Matrix4f().identity();
        double scaleFactor = minecraft.getWindow().getGuiScale();
        double realHeight = minecraft.getWindow().getHeight();
        int viewportX = (int) ((x + offsetX) * scaleFactor);
        int viewportY = (int) (minecraft.getWindow().getHeight() - (sizeY + y + offsetY) * scaleFactor);
        int viewportWidth = (int) (sizeX * scaleFactor);
        int viewportHeight = (int) (sizeY * scaleFactor);
//        GL11.glScissor(
//                viewportX,
//                viewportY,
//                viewportWidth,
//                viewportHeight
        RenderSystem.enableScissor(
                viewportX,
                viewportY,
                viewportWidth,
                viewportHeight
        );
        RenderSystem.clear(GlConst.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.disableScissor();
//        GlStateManager.viewport(
//                viewportX,
//                viewportY,
//                viewportWidth,
//                viewportHeight
        RenderSystem.viewport(
                viewportX,
                viewportY,
                viewportWidth,
                viewportHeight
        );
        projectionMatrix.perspective(70.0F, (float) sizeX / sizeY, 1F, 10000.0F);
        RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorting.ORTHOGRAPHIC_Z);
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.setIdentity();
        Lighting.setupFor3DItems();
//        GlStateManager.rotate(90, 1, 0, 0); // look down
        modelViewStack.rotateAround(Axis.XP.rotationDegrees(90), 1, 0, 0);
        modelViewStack.pushPose();
        modelViewStack.translate(-positionX, -camY, -positionZ);
        RenderUtil.disableBlend();
        RenderUtil.disableAlpha();
        int minScreenX = (int) ((x + offsetX) * scaleFactor);
        int minScreenY = (int) (realHeight - (y + offsetY) * scaleFactor);
        int maxScreenX = (int) ((x + offsetX + sizeX) * scaleFactor);
        int maxScreenY = (int) (realHeight - (y + offsetY + sizeY) * scaleFactor);
        int minChunkX = (posX >> 4) - 8;
        int minChunkZ = (posZ >> 4) - 8;
        int maxChunkX = (posX >> 4) + 8;
        int maxChunkZ = (posZ >> 4) + 8;
        // noinspection SuspiciousNameCombination
        List<ChunkPos> chunkPosBounds = Stream.of(
                        Pair.of(minScreenX, minScreenY),
                        Pair.of(minScreenX, maxScreenY),
                        Pair.of(maxScreenX, minScreenY),
                        Pair.of(maxScreenX, maxScreenY)
                )
                .map(p -> rayTrace(p.getLeft(), p.getRight(), modelViewStack, projectionMatrix))
                .filter(Objects::nonNull)
                .map(ChunkPos::new)
                .collect(Collectors.toList());
        for (ChunkPos chunkPos : chunkPosBounds) {
            if (chunkPos.x < minChunkX) {
                minChunkX = chunkPos.x;
            }
            if (chunkPos.z < minChunkZ) {
                minChunkZ = chunkPos.z;
            }
            if (chunkPos.x > maxChunkX) {
                maxChunkX = chunkPos.x;
            }
            if (chunkPos.z > maxChunkZ) {
                maxChunkZ = chunkPos.z;
            }
        }
        minChunkX--;
        minChunkZ--;
        maxChunkX++;
        maxChunkZ++;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                ZonePlannerMapRenderer.INSTANCE.getChunkGlList(
                                new ZonePlannerMapChunkKey(
                                        new ChunkPos(chunkX, chunkZ),
                                        dimension,
                                        container.tile.getLevelBC()
                                )
                        )
                        .ifPresent(vertexBuffer -> this.renderVertexBuffer(vertexBuffer, modelViewStack, projectionMatrix));
            }
        }

        BlockPos found = null;
        int foundMapColorABGR = 0;
        // 1.12.2 Mouse (0,0) is left down corner of screen
        // 1.16+ MouseHandler (0,0) is left up corner of screen
        MouseHandler mouse = Minecraft.getInstance().mouseHandler;
//                Mouse.getY() <= minScreenY &&
//                Mouse.getX() <= maxScreenX &&
        if (mouse.xpos() >= minScreenX &&
                (realHeight - mouse.ypos()) <= minScreenY &&
                mouse.xpos() <= maxScreenX &&
                (realHeight - mouse.ypos()) >= maxScreenY) {
            found = rayTrace(mouse.xpos(), (realHeight - mouse.ypos()), modelViewStack, projectionMatrix);
        }
        if (found != null) {
            ZonePlannerMapChunk zonePlannerMapChunk = ZonePlannerMapDataClient.INSTANCE.getChunk(
                    minecraft.level,
                    new ZonePlannerMapChunkKey(
                            new ChunkPos(found),
//                            mc.world.provider.getDimension(),
                            minecraft.level.dimension(),
                            container.tile.getLevelBC()
                    )
            );
            if (zonePlannerMapChunk != null) {
                MapColourData data = zonePlannerMapChunk.getData(found.getX(), found.getZ());
                if (data != null) {
                    foundMapColorABGR = data.colour;
                }
            }
        }

        if (found != null) {
            RenderSystem.polygonMode(GlConst.GL_FRONT_AND_BACK, GlConst.GL_LINE);
            byte r = (byte) (((foundMapColorABGR >> 0) & 0xFF) * 0.7);
            byte g = (byte) (((foundMapColorABGR >> 8) & 0xFF) * 0.7);
            byte b = (byte) (((foundMapColorABGR >> 16) & 0xFF) * 0.7);
            byte a = 0x77;
            ZonePlannerMapRenderer.INSTANCE.setColor(r, g, b, a);
            VertexConsumer builder = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(CUBOID_LINES_RENDER_TYPE);
            ZonePlannerMapRenderer.INSTANCE.drawBlockCuboid(builder, found.getX(), found.getY(), found.getZ());
            RenderSystem.applyModelViewMatrix();
            Minecraft.getInstance().renderBuffers().bufferSource().endBatch(CUBOID_LINES_RENDER_TYPE);
            RenderSystem.polygonMode(GlConst.GL_FRONT_AND_BACK, GlConst.GL_FILL);
        }

        RenderUtil.enableBlend();

        // draw colored zones
        for (int i = 0; i < container.tile.layers.length; i++) {
            if (getPaintbrushBrush() != null && getPaintbrushBrush().colour.getId() != i) {
                continue;
            }
            ZonePlan layer = container.tile.layers[i];
            if (getPaintbrushBrush() != null && getPaintbrushBrush().colour.getId() == i && bufferLayer != null) {
                layer = bufferLayer;
            }
            if (!layer.getChunkPoses().isEmpty()) {
                Tesselator.getInstance().getBuilder().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                    for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                        for (int blockX = chunkPos.getMinBlockX(); blockX <= chunkPos.getMaxBlockX(); blockX++) {
                            for (int blockZ = chunkPos.getMinBlockZ(); blockZ <= chunkPos.getMaxBlockZ(); blockZ++) {
                                if (!layer.get(
                                        blockX - container.tile.getBlockPos().getX(),
                                        blockZ - container.tile.getBlockPos().getZ()
                                )) {
                                    continue;
                                }
                                int height;
                                ZonePlannerMapChunk zonePlannerMapChunk = ZonePlannerMapDataClient.INSTANCE.getChunk(
                                        minecraft.level,
                                        new ZonePlannerMapChunkKey(
                                                chunkPos,
                                                dimension,
                                                container.tile.getLevelBC()
                                        )
                                );
                                if (zonePlannerMapChunk != null) {
                                    MapColourData data = zonePlannerMapChunk.getData(blockX, blockZ);
                                    if (data != null) {
                                        height = data.posY;
                                    } else {
                                        continue;
                                    }
                                } else {
                                    continue;
                                }
                                float[] color = DyeColor.byId(i).getTextureDiffuseColors();
                                byte r = (byte) (color[0] * 255.0F);
                                byte g = (byte) (color[1] * 255.0F);
                                byte b = (byte) (color[2] * 255.0F);
                                byte a = 0x55;
                                ZonePlannerMapRenderer.INSTANCE.setColor(r, g, b, a);
                                ZonePlannerMapRenderer.INSTANCE.drawBlockCuboid(
//                                        Tessellator.getInstance().getBuffer(),
                                        Tesselator.getInstance().getBuilder(),
                                        blockX,
                                        height + 0.1,
                                        blockZ,
                                        height,
                                        0.6
                                );
                            }
                        }
                    }
                }
                this.uploadBufferBuilderAndRender(Tesselator.getInstance().getBuilder(), modelViewStack, projectionMatrix, GameRenderer.getPositionColorShader());
            }
        }
        RenderUtil.disableBlend();

        lastSelected = found;
        modelViewStack.popPose();
        RenderSystem.viewport(0, 0, minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());
        RenderSystem.restoreProjectionMatrix();
        modelViewStack.popPose();
        RenderUtil.disableBlend();

        RenderSystem.applyModelViewMatrix();
    }

    private void renderVertexBuffer(VertexBuffer vertexBuffer, PoseStack modelViewStack, Matrix4f projectionMatrix) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        FogRenderer.setupNoFog();
        vertexBuffer.bind();
        vertexBuffer.drawWithShader(modelViewStack.last().pose(), projectionMatrix, GameRenderer.getPositionColorShader());
        VertexBuffer.unbind();
    }

    private void uploadBufferBuilderAndRender(BufferBuilder builder, PoseStack modelViewStack, Matrix4f projectionMatrix, ShaderInstance shader) {
        BufferBuilder.RenderedBuffer bufferbuilder$renderedbuffer = builder.end();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        FogRenderer.setupNoFog();
        VertexBuffer vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        vertexBuffer.bind();
        vertexBuffer.upload(bufferbuilder$renderedbuffer);
        vertexBuffer.drawWithShader(modelViewStack.last().pose(), projectionMatrix, shader);
        VertexBuffer.unbind();
    }
}
