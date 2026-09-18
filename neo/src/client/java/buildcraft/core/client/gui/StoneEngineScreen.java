/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import buildcraft.core.blockentity.StoneEngineBlockEntity;
import buildcraft.core.menu.StoneEngineMenu;
import buildcraft.lib.gui.screen.BcContainerScreen;

/**
 * The stone engine screen (task M4.8, 1.20.1 counterpart {@code GuiEngineStone_BC8}, minimal capability surface): the
 * baseline {@code buildcraftenergy:textures/gui/steam_engine_gui.png} panel (176&times;166) with the centered
 * {@code tile.engineStone.name} title (the block's frozen description id), the burn flame indicator at (81, 25) &mdash;
 * bottom-anchored and shrinking with the remaining burn of the current fuel item, exactly the baseline overlay &mdash;
 * the stored-energy line in the flat band above the player inventory (the frozen {@code gui.stored} key, formatted like
 * the Jade providers: grouped numbers and &micro;J), and the {@code gui.inventory} label at y = 70.
 *
 * <p>The legacy heat gauge has no slice counterpart (the slice engine carries no heat simulation, see
 * {@code StoneEngineBlockEntity}); it migrates with the real engine module.
 */
public class StoneEngineScreen extends BcContainerScreen<StoneEngineMenu> {

    /** The baseline texture: panel at (0, 0, 176, 166), flame strip at (176, 0, 14, 16). */
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("buildcraftenergy",
            "textures/gui/steam_engine_gui.png");
    private static final int SIZE_X = 176;
    private static final int SIZE_Y = 166;
    /** Panel-local area of the flame indicator (the baseline {@code flameRect}). */
    private static final int FLAME_X = 81;
    private static final int FLAME_Y = 25;
    private static final int FLAME_W = 14;
    private static final int FLAME_H = 14;

    public StoneEngineScreen(StoneEngineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, SIZE_X, SIZE_Y, TEXTURE);
    }

    @Override
    protected void init() {
        super.init();
        // The baseline centered the title on the panel and put the inventory row above the player grid (y = 70).
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 70;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        // Bottom-anchored flame from the texture strip at (176, 0..16): height follows the remaining fraction of the
        // current fuel item (the baseline drew ceil(amount * h) with amount = fuel left / 100).
        int burnRemain = this.menu.getBurnRemain();
        int burnTotal = this.menu.getBurnTotal();
        if (burnRemain > 0 && burnTotal > 0 && burnRemain <= burnTotal) {
            int flameHeight = Mth.ceil((float) burnRemain / burnTotal * FLAME_H);
            this.blitRegion(graphics, FLAME_X, FLAME_Y + FLAME_H - flameHeight, 176, FLAME_H - flameHeight,
                    FLAME_W, flameHeight + 2);
        }
    }

    @Override
    public void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        long stored = this.menu.getEnergyStored();
        String value = formatNumber(stored) + " / " + formatNumber(StoneEngineBlockEntity.CAPACITY) + " μJ";
        graphics.text(this.font, Component.translatable("gui.stored"), 8, 57, LABEL_COLOR, false);
        graphics.text(this.font, value, this.imageWidth - 8 - this.font.width(value), 57, LABEL_COLOR, false);
    }
}
