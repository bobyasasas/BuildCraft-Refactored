/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.gui;

import buildcraft.lib.gui.GuiBC8;
import buildcraft.lib.gui.GuiIcon;
import buildcraft.lib.gui.pos.GuiRectangle;
import buildcraft.lib.gui.pos.IGuiArea;
import buildcraft.lib.gui.pos.IGuiPosition;
import buildcraft.lib.gui.pos.PositionAbsolute;
import buildcraft.lib.misc.LocaleUtil;
import buildcraft.lib.misc.RenderUtil;
import buildcraft.silicon.container.ContainerProgrammingTable_Neptune;
import buildcraft.silicon.tile.TileProgrammingTable_Neptune;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GuiProgrammingTable_Neptune extends GuiBC8<ContainerProgrammingTable_Neptune> {

    private static final ResourceLocation TEXTURE_BASE = new ResourceLocation("buildcraftsilicon:textures/gui/programming_table.png");
    private static final int SIZE_X = 176, SIZE_Y = 207;
    private static final GuiIcon ICON_GUI = new GuiIcon(TEXTURE_BASE, 0, 0, SIZE_X, SIZE_Y);
    private static final GuiIcon ICON_PROGRESS = new GuiIcon(TEXTURE_BASE, SIZE_X, 18, 4, 70);
    private static final GuiRectangle RECT_PROGRESS = new GuiRectangle(164, 36, 4, 70);
    private static final GuiIcon ICON_SAVED_ENOUGH_ACTIVE = new GuiIcon(TEXTURE_BASE, 196, 1, 16, 16);

//        @Override
//            // Draw background
//            // Draw icon
//        @Override

    private final TileProgrammingTable_Neptune table;

//        @Override

    public GuiProgrammingTable_Neptune(ContainerProgrammingTable_Neptune container, Inventory inventory, Component component) {
        super(container, inventory, component);
        this.table = container.tile;
        imageWidth = SIZE_X;
        imageHeight = SIZE_Y;



        mainGui.shownElements.add(new LedgerTablePower(mainGui, container.tile, true));
    }


    @Override
    protected void drawForegroundLayer(GuiGraphics guiGraphics) {
        String title = LocaleUtil.localize("tile.programmingTableBlock.name");
        guiGraphics.drawString(font, title, leftPos + (float) (imageWidth - font.width(title)) / 2, topPos + 15, 0x404040, false);
        guiGraphics.drawString(font, LocaleUtil.localize("gui.inventory"), leftPos + 8, topPos + imageHeight - 97, 0x404040, false);
    }

    @Override
    protected void drawBackgroundLayer(float partialTicks, GuiGraphics guiGraphics) {
        RenderUtil.color(1.0F, 1.0F, 1.0F, 1.0F);
        ICON_GUI.drawAt(mainGui.rootElement, guiGraphics);


        for (int i = 0; i < table.optionRecipes.size(); i++) {
            if (table.optionRecipes.get(i) != null) {
                if (table.optionId == i) {
                    ICON_SAVED_ENOUGH_ACTIVE.drawAt(getArea(i), guiGraphics);
                    break;
                }
            }
        }

        long target = container.tile.getTarget();
        if (target != 0) {
            double v = (double) container.tile.power / target;
            ICON_PROGRESS.drawCutInside(
                    new GuiRectangle(
                            RECT_PROGRESS.x,
                            (int) (RECT_PROGRESS.y + RECT_PROGRESS.height * Math.max(1 - v, 0)),
                            RECT_PROGRESS.width,
                            (int) Math.ceil(RECT_PROGRESS.height * Math.min(v, 1))
                    ).offset(mainGui.rootElement),
                    guiGraphics
            );
        }

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);


        if (mouseButton == 0) {
            for (int i = 0; i < table.optionRecipes.size(); i++) {
                if (getArea(i).contains(mouseX, mouseY)) {
                    if (table.optionId == i) {
                        table.rpcSelectOption(-1);
                    } else {
                        table.rpcSelectOption(i);
                    }
                }
            }
        }
        return true;
    }

    private IGuiArea getArea(int index) {
        return index < TileProgrammingTable_Neptune.WIDTH * TileProgrammingTable_Neptune.HEIGHT
                ? new GuiRectangle(16, 16).offset(mainGui.rootElement).offset(getPos(index))
                : GuiRectangle.ZERO;
    }

    private IGuiPosition getPos(int index) {
        int posX = index % TileProgrammingTable_Neptune.WIDTH;
        int posY = index / TileProgrammingTable_Neptune.WIDTH;
        return new PositionAbsolute(43 + posX * 18, 36 + posY * 18);
    }

//    @Override
}
