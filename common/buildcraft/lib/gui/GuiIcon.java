/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.gui;

import buildcraft.api.core.render.ISprite;
import buildcraft.lib.client.sprite.SpriteRaw;
import buildcraft.lib.gui.pos.GuiRectangle;
import buildcraft.lib.gui.pos.IGuiArea;
import buildcraft.lib.gui.pos.IGuiPosition;
import buildcraft.lib.misc.RenderUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class GuiIcon implements ISimpleDrawable {
    public final ISprite sprite;
    public final int textureSize;
    public final int width, height;

    public GuiIcon(ISprite sprite, int textureSize) {
        this.sprite = sprite;
        this.textureSize = textureSize;
        this.width = (int) (Math.abs(sprite.getInterpU(1) - sprite.getInterpU(0)) * textureSize);
        this.height = (int) (Math.abs(sprite.getInterpV(1) - sprite.getInterpV(0)) * textureSize);
    }

    public GuiIcon(ResourceLocation texture, double u, double v, double width, double height, int texSize) {
        this(new SpriteRaw(texture, u, v, width, height, texSize), texSize);
    }

    public GuiIcon(ResourceLocation texture, double u, double v, double width, double height) {
        this(texture, u, v, width, height, 256);
    }

    public GuiIcon offset(double u, double v) {
        SpriteRaw raw = (SpriteRaw) sprite;
        double uMin = raw.uMin + u / textureSize;
        double vMin = raw.vMin + v / textureSize;
        return new GuiIcon(new SpriteRaw(raw.location, uMin, vMin, raw.width, raw.height), textureSize);
    }

    public boolean containsGuiPos(double x, double y, IGuiPosition pos) {
        return new GuiRectangle(x, y, width, height).contains(pos);
    }

    public DynamicTexture createDynamicTexture(int scale) {
        return new DynamicTexture(width * scale, height * scale, /*pUseCalloc*/ true);
    }

    @Override
    public void drawAt(GuiGraphics guiGraphics, double x, double y) {
        this.drawScaledInside(guiGraphics, x, y, this.width, this.height);
    }

    public void drawScaledInside(IGuiArea element, GuiGraphics guiGraphics) {
        drawScaledInside(guiGraphics, element.getX(), element.getY(), element.getWidth(), element.getHeight());
    }

    public void drawScaledInside(GuiGraphics guiGraphics, double x, double y, double drawnWidth, double drawnHeight) {
        draw(sprite, guiGraphics, x, y, x + drawnWidth, y + drawnHeight);
    }

    public void drawCustomQuad(GuiGraphics guiGraphics, double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
        PoseStack poseStack = guiGraphics.pose();

        sprite.bindTexture();

        double uMin = sprite.getInterpU(0);
        double uMax = sprite.getInterpU(1);

        double vMin = sprite.getInterpV(0);
        double vMax = sprite.getInterpV(1);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);



        Matrix4f pose = poseStack.last().pose();
        bufferbuilder.vertex(pose, (float) x1, (float) y1, 0).uv((float) uMin, (float) vMax).endVertex();
        bufferbuilder.vertex(pose, (float) x2, (float) y2, 0).uv((float) uMax, (float) vMax).endVertex();
        bufferbuilder.vertex(pose, (float) x3, (float) y3, 0).uv((float) uMax, (float) vMin).endVertex();
        bufferbuilder.vertex(pose, (float) x4, (float) y4, 0).uv((float) uMin, (float) vMin).endVertex();

        tessellator.end();
    }

//    private static double[] calcQ(double x1, double y1, double x2, double y2, double x3, double y3, double x4,
//        // Method contents taken from http://www.bitlush.com/posts/arbitrary-quadrilaterals-in-opengl-es-2-0
//        // (or github https://github.com/bitlush/android-arbitrary-quadrilaterals-in-opengl-es-2-0 if the site is down)
//        // this code is by Keith Wood
//        // in case (for some reason) some of the input was wrong then we will fail back to default rendering


    public void drawCutInside(IGuiArea element, GuiGraphics guiGraphics) {
        drawCutInside(guiGraphics, element.getX(), element.getY(), element.getWidth(), element.getHeight());
    }

    public void drawCutInside(GuiGraphics guiGraphics, double x, double y, double displayWidth, double displayHeight) {
        PoseStack poseStack = guiGraphics.pose();

        sprite.bindTexture();

        displayWidth = Math.min(this.width, displayWidth);
        displayHeight = Math.min(this.height, displayHeight);

        double xMin = x;
        double yMin = y;

        double xMax = x + displayWidth;
        double yMax = y + displayHeight;

        double uMin = sprite.getInterpU(0);
        double vMin = sprite.getInterpV(0);

        double uMax = sprite.getInterpU(displayWidth / width);
        double vMax = sprite.getInterpV(displayHeight / height);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderUtil.enableBlend();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder vb = tess.getBuilder();
        vb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        PoseStack.Pose pose = poseStack.last();
        vertex(pose, vb, xMin, yMax, uMin, vMax);
        vertex(pose, vb, xMax, yMax, uMax, vMax);
        vertex(pose, vb, xMax, yMin, uMax, vMin);
        vertex(pose, vb, xMin, yMin, uMin, vMin);

        tess.end();
    }

    public static void drawAt(ISprite sprite, GuiGraphics guiGraphics, double x, double y, double size) {
        drawAt(sprite, guiGraphics, x, y, size, size);
    }

    public static void drawAt(ISprite sprite, GuiGraphics guiGraphics, double x, double y, double width, double height) {
        draw(sprite, guiGraphics, x, y, x + width, y + height);
    }

    public static void draw(ISprite sprite, GuiGraphics guiGraphics, double xMin, double yMin, double xMax, double yMax) {
        PoseStack poseStack = guiGraphics.pose();

        sprite.bindTexture();

        double uMin = sprite.getInterpU(0);
        double vMin = sprite.getInterpV(0);

        double uMax = sprite.getInterpU(1);
        double vMax = sprite.getInterpV(1);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderUtil.enableBlend();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder vb = tess.getBuilder();
        vb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        PoseStack.Pose pose = poseStack.last();
        vertex(pose, vb, xMin, yMax, uMin, vMax);
        vertex(pose, vb, xMax, yMax, uMax, vMax);
        vertex(pose, vb, xMax, yMin, uMax, vMin);
        vertex(pose, vb, xMin, yMin, uMin, vMin);

        tess.end();
    }

    private static void vertex(PoseStack.Pose pose, BufferBuilder vb, double x, double y, double u, double v) {
        vb.vertex(pose.pose(), (float) x, (float) y, 0);
        vb.uv((float) u, (float) v);
        vb.endVertex();
    }
}
