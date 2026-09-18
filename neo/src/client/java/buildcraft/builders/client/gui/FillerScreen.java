/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import buildcraft.builders.blockentity.FillerBlockEntity;
import buildcraft.builders.menu.FillerMenu;
import buildcraft.lib.gui.screen.BcContainerScreen;

/**
 * The filler screen (task M4.8, 1.20.1 counterpart {@code GuiFiller}, minimal capability surface): the baseline
 * {@code buildcraftbuilders:textures/gui/filler.png} panel (176&times;241) with the centered
 * {@code tile.fillerBlock.name} title, the pattern box at (12, 32, 32, 32) drawn with the selected pattern's name (the
 * legacy key derivation {@code "fillerpattern." + <unique tag path>}, resolved to the frozen {@code fillerpattern.*}
 * keys; the {@code fillerpattern.none} key stands in while no pattern is selected), the stored-energy line in the band
 * above the slot grid (label left / value right &mdash; the frozen {@code gui.stored} key, formatted like the Jade
 * providers), the frozen {@code gui.filling.resources} label at the baseline "inv_title" position (7, 74) and the
 * player inventory block below.
 *
 * <p>The legacy statement parameter slots, invert/excavate buttons and progress ledger are not part of this surface
 * (the slice filler has exactly one pattern and no gate lock, see {@link FillerBlockEntity}); the texture therefore
 * shows its empty param/button holes around the pattern box, and they migrate with the full builders module.
 */
public class FillerScreen extends BcContainerScreen<FillerMenu> {

    /** The baseline texture: panel at (0, 0, 176, 241); the button/lock sprites right of it are unused here. */
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("buildcraftbuilders",
            "textures/gui/filler.png");
    private static final int SIZE_X = 176;
    private static final int SIZE_Y = 241;
    /** Panel-local area of the pattern box (the baseline {@code pattern_drawable} element). */
    private static final int PATTERN_X = 12;
    private static final int PATTERN_Y = 32;
    private static final int PATTERN_W = 32;
    private static final int PATTERN_H = 32;

    public FillerScreen(FillerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, SIZE_X, SIZE_Y, TEXTURE);
    }

    @Override
    protected void init() {
        super.init();
        // The baseline json centered the title at (88, 10) and put both label rows at x = 7.
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY = 10;
        this.inventoryLabelX = 7;
        this.inventoryLabelY = 141;
    }

    @Override
    public void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        // The pattern name, centred in the pattern box (the legacy sprite needs the statement framework, so the
        // slice draws the localized pattern name instead).
        Component pattern = patternName();
        graphics.text(this.font, pattern, PATTERN_X + (PATTERN_W - this.font.width(pattern)) / 2,
                PATTERN_Y + (PATTERN_H - 9) / 2 + 1, LABEL_COLOR, false);

        // The stored-energy line, label left / value right in the band between the pattern box and the resource
        // slots (the StoneEngineScreen row pattern; it must not sit on the resources label row below).
        long stored = this.menu.getEnergyStored();
        String value = formatNumber(stored) + " / " + formatNumber(FillerBlockEntity.CAPACITY) + " μJ";
        graphics.text(this.font, Component.translatable("gui.stored"), 7, 64, LABEL_COLOR, false);
        graphics.text(this.font, value, this.imageWidth - 7 - this.font.width(value), 64, LABEL_COLOR, false);

        // The resources label above the slot grid, at the baseline filler.json "inv_title" position (7, 74).
        graphics.text(this.font, Component.translatable("gui.filling.resources"), 7, 74, LABEL_COLOR, false);
    }

    /** The localized pattern name (legacy {@code "fillerpattern." + tag} derivation, frozen keys). */
    private Component patternName() {
        FillerBlockEntity.Pattern pattern = this.menu.getPattern();
        if (pattern == null) {
            return Component.translatable("fillerpattern.none");
        }
        String tag = pattern.uniqueTag;
        String path = tag.substring(tag.indexOf(':') + 1);
        return Component.translatable("fillerpattern." + path);
    }
}
