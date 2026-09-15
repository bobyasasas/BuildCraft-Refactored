/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.gui;

import buildcraft.lib.misc.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public class GuiStack implements ISimpleDrawable {
    private final ItemStack stack;

    public GuiStack(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public void drawAt(GuiGraphics guiGraphics, double x, double y) {
        RenderUtil.color(1, 1, 1);
        RenderUtil.enableGUIStandardItemLighting();
        guiGraphics.renderFakeItem(stack, (int) x, (int) y);
        RenderUtil.disableStandardItemLighting();
        RenderUtil.color(1, 1, 1);
    }
}
