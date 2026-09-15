/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.transport.gui;

import buildcraft.lib.gui.GuiBC8;
import buildcraft.lib.gui.GuiIcon;
import buildcraft.lib.misc.RenderUtil;
import buildcraft.lib.misc.SpriteUtil;
import buildcraft.transport.BCTransportSprites;
import buildcraft.transport.container.ContainerFilteredBuffer_BC8;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class GuiFilteredBuffer extends GuiBC8<ContainerFilteredBuffer_BC8> {
    private static final ResourceLocation TEXTURE_BASE = new ResourceLocation("buildcrafttransport:textures/gui/filtered_buffer.png");
    private static final int SIZE_X = 176, SIZE_Y = 169;
    private static final GuiIcon ICON_GUI = new GuiIcon(TEXTURE_BASE, 0, 0, SIZE_X, SIZE_Y);

    public GuiFilteredBuffer(ContainerFilteredBuffer_BC8 container, Inventory inventory, Component component) {
        super(container, inventory, component);
        imageWidth = SIZE_X;
        imageHeight = SIZE_Y;
    }

    @Override
    protected void drawBackgroundLayer(float partialTicks, GuiGraphics guiGraphics) {
        ICON_GUI.drawAt(mainGui.rootElement, guiGraphics);
        RenderUtil.enableGUIStandardItemLighting();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = container.tile.invFilter.getStackInSlot(i);
            double currentX = mainGui.rootElement.getX() + 8 + i * 18;
            double currentY = mainGui.rootElement.getY() + 61;
            if (!stack.isEmpty()) {
                guiGraphics.renderItem(this.minecraft.player, stack, (int) currentX, (int) currentY, 0);
            } else {
                SpriteUtil.bindTexture(TextureAtlas.LOCATION_BLOCKS);
                this.drawTexturedModalRect(guiGraphics, (int) currentX, (int) currentY, BCTransportSprites.NOTHING_FILTERED_BUFFER_SLOT.getSprite(), 16, 16, 0);
            }
        }
        RenderUtil.disableStandardItemLighting();
        RenderUtil.disableDepth();
        RenderUtil.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderUtil.color(1, 1, 1, 0.7f);
        ICON_GUI.drawAt(mainGui.rootElement, guiGraphics);

        RenderUtil.color(1, 1, 1, 1);
        RenderUtil.disableBlend();
        RenderUtil.enableDepth();
    }

    @Override
    protected void drawForegroundLayer(GuiGraphics guiGraphics) {
        int x = leftPos;
        int y = topPos;
        String title = I18n.get("tile.filteredBufferBlock.name");
        int xPos = (imageWidth - font.width(title)) / 2;
        guiGraphics.drawString(font, title, x + xPos, y + 10, 0x404040, false);
    }
}
