/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.compat.jei;

import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;

import buildcraft.lib.recipe.BcIngredientStack;

/**
 * Shared plumbing for the seven BuildCraft JEI categories: fixed blank background (deliberately NOT the machine GUI
 * texture — JEI layouts don't line up with the in-game GUIs, main-agent decision), machine-block icon, slot
 * backgrounds cut from {@code buildcraftcore:textures/gui/slot.png}, and the micro-joule / info text line drawn under
 * the slots. Category titles reuse existing legacy machine-name translation keys (the lang file is a frozen set, no
 * new keys are allowed).
 */
abstract class AbstractBcRecipeCategory<T> implements IRecipeCategory<T> {

    /** 32x32 texture; the visible slot frame is the 18x18 area at (7, 7). */
    private static final Identifier SLOT_TEXTURE = Identifier
        .fromNamespaceAndPath("buildcraftcore", "textures/gui/slot.png");

    /** JEI default-ish info text colour (dark grey, matches vanilla GUI label style). */
    private static final int INFO_COLOR = 0xFF404040;

    /** Slot pitch: 18px slot + 1px gap, the vanilla inventory grid rhythm. */
    static final int SLOT_PITCH = 19;

    private final IRecipeType<T> recipeType;
    private final Component title;
    private final IDrawable icon;
    private final IDrawable slotBackground;
    private final IDrawableStatic arrow;
    private final int width;
    private final int height;

    AbstractBcRecipeCategory(IGuiHelper guiHelper, IRecipeType<T> recipeType, String titleKey, ItemLike iconBlock,
            int width, int height) {
        this.recipeType = recipeType;
        this.title = Component.translatable(titleKey);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(iconBlock));
        this.slotBackground = guiHelper.drawableBuilder(SLOT_TEXTURE, 7, 7, 18, 18)
            .setTextureSize(32, 32)
            .build();
        this.arrow = guiHelper.getRecipeArrow();
        this.width = width;
        this.height = height;
    }

    @Override
    public IRecipeType<T> getRecipeType() {
        return this.recipeType;
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public abstract void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses);

    /** Adds an item slot with the BuildCraft slot frame behind it, at the given top-left position. */
    IRecipeSlotBuilder addSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y) {
        return builder.addSlot(role, x, y).setBackground(this.slotBackground, 0, 0);
    }

    /** Adds a fluid slot with the BuildCraft slot frame behind it (16x16 renderer inside the 18x18 frame). */
    IRecipeSlotBuilder addFluidSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y,
            long capacityMb) {
        return addSlot(builder, role, x, y).setFluidRenderer(capacityMb, true, 16, 16);
    }

    /** Adds the ingredient candidates of a {@link BcIngredientStack} with its count baked into every stack. */
    void addIngredientStack(IRecipeSlotBuilder slot, BcIngredientStack stack) {
        stack.ingredient().items()
            .map(holder -> new ItemStack(holder, stack.count()))
            .forEach(slot::add);
    }

    /** Draws one plain info line (energy cost, temperatures, ...) at the given position. */
    void drawInfoLine(GuiGraphicsExtractor guiGraphics, String line, int x, int y) {
        Font font = Minecraft.getInstance().font;
        guiGraphics.text(font, line, x, y, INFO_COLOR, false);
    }

    /** Draws JEI's standard recipe arrow at the given position (vertically centred on an 18px slot row). */
    void drawArrow(GuiGraphicsExtractor guiGraphics, int x, int y) {
        this.arrow.draw(guiGraphics, x, y + (18 - this.arrow.getHeight()) / 2);
    }

    /** Formats a micro-joule amount with US-locale thousands separators (the lang set is English). */
    static String formatMicroJoules(long microJoules) {
        return String.format(Locale.US, "%,d \u03bcJ", microJoules);
    }
}
