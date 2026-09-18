/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.gui.screen;

import com.mojang.logging.LogUtils;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;

/**
 * Minimal 26.1.2 GUI framework base (task M4.8) for the migrated machine screens: the 26.1.2 replacement for the
 * legacy 1.20.1 direct-drawing stack's {@code GuiBC8}/{@code GuiScreenBuildCraft} pair, covering only the small
 * capability surface the first machine screens share:
 * <ul>
 * <li>one full-size background texture, blitted at ({@link #leftPos}, {@link #topPos}) from
 * {@link #extractBackground} (the 26.1.2 hook that replaced the legacy {@code drawGuiContainerBackgroundLayer});</li>
 * <li>small texture-region blits for the animated indicators (the legacy {@code GuiIcon.drawAt});</li>
 * <li>the two standard label rows, both resolved from the frozen legacy lang keys &mdash; the container title comes
 * from the menu ({@code tile.<machine>.name} via the block's overridden description id) and the player inventory row
 * is the frozen {@code gui.inventory} key, exactly like the baseline screens drew it.</li>
 * </ul>
 *
 * <p>Screen subclasses draw their dynamic content by overriding {@link #extractBackground} (texture-space indicators,
 * before the slots) and {@link #extractLabels} (text, already translated into the panel's local space). This base is
 * deliberately not a port of the legacy widget/ledger/json machinery.
 */
public abstract class BcContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * The legacy label grey {@code 0x404040}, carried with an explicit opaque alpha byte: the 26.1.2 extract API
     * ({@code GuiGraphicsExtractor.text}) silently drops any colour whose alpha byte is 0, and the bare legacy
     * literal is exactly that.
     */
    protected static final int LABEL_COLOR = 0xFF_40_40_40;

    /** The full-size background texture (a 256&times;256 atlas whose top-left corner holds the panel). */
    private final Identifier backgroundTexture;
    /** Texture-space width of the panel (the legacy {@code SIZE_X}); smaller than the file itself. */
    private final int textureWidth;
    /** Texture-space height of the panel (the legacy {@code SIZE_Y}); smaller than the file itself. */
    private final int textureHeight;

    protected BcContainerScreen(T menu, Inventory playerInventory, Component title, int textureWidth, int textureHeight,
            Identifier backgroundTexture) {
        super(menu, playerInventory, title, textureWidth, textureHeight);
        this.backgroundTexture = backgroundTexture;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    @Override
    protected void init() {
        super.init();
        // M4.8 smoke-test evidence line: one log row per opened BuildCraft container screen (menu id + screen).
        Identifier menuId = BuiltInRegistries.MENU.getKey(this.menu.getType());
        LOGGER.info("BuildCraft GUI opened: menu '{}' screen '{}'", menuId, this.getClass().getSimpleName());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, this.backgroundTexture, this.leftPos, this.topPos, 0.0f, 0.0f,
                this.imageWidth, this.imageHeight, this.textureWidth, this.textureHeight, 256, 256);
    }

    /**
     * Blits a region of the background texture onto the panel, both in panel-local coordinates (add {@link #leftPos}
     * and {@link #topPos} first). The legacy {@code GuiIcon(LOCATION, u, v, w, h).drawAt(pos)} replacement.
     */
    protected final void blitRegion(GuiGraphicsExtractor graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, this.backgroundTexture, this.leftPos + x, this.topPos + y, u, v,
                width, height, width, height, 256, 256);
    }

    /** The two standard label rows: the (already centered by {@code init()}) container title and {@code gui.inventory}. */
    @Override
    public void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, LABEL_COLOR, false);
        graphics.text(this.font, Component.translatable("gui.inventory"), this.inventoryLabelX, this.inventoryLabelY,
                LABEL_COLOR, false);
    }

    /** {@code 12345} &rarr; {@code "12,345"} (grouping separator fixed by {@link Locale#ROOT}, like Jade lines). */
    protected static String formatNumber(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
