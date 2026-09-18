/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.compat.jei;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;

import buildcraft.factory.BcFactoryBlocks;
import buildcraft.factory.recipe.BcHeatExchangeRecipe;

/**
 * JEI category for {@code buildcraftfactory:heat_exchange} recipes. One category covers both serializers
 * ({@code heat_exchange/heatable} + {@code heat_exchange/coolable}, 21 recipes each): the kind is spelled out on the
 * per-recipe info line ("HEATABLE 300 -> 573") because JEI titles are per-category, not per-recipe (main-agent
 * decision: single category, kind badge/title-suffix distinction).
 */
public final class BcHeatExchangeRecipeCategory extends AbstractBcRecipeCategory<BcHeatExchangeRecipe> {

    private static final int INPUT_X = 0;
    private static final int ARROW_X = 44;
    private static final int OUTPUT_X = 70;
    private static final int INFO_Y = 24;

    public BcHeatExchangeRecipeCategory(IGuiHelper guiHelper) {
        // Title: legacy "Heat Exchanger" machine name; icon: the heat exchanger block itself.
        super(guiHelper, BcJeiRecipeTypes.HEAT_EXCHANGE, "tile.heat_exchange.name", BcFactoryBlocks.HEAT_EXCHANGE,
            OUTPUT_X + 18, INFO_Y + 10);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BcHeatExchangeRecipe recipe, IFocusGroup focuses) {
        IRecipeSlotBuilder input = addFluidSlot(builder, RecipeIngredientRole.INPUT, INPUT_X, 0, recipe.in.amount());
        input.add(recipe.in.fluid(), recipe.in.amount());

        IRecipeSlotBuilder output = addFluidSlot(builder, RecipeIngredientRole.OUTPUT, OUTPUT_X, 0, recipe.out.amount());
        output.add(recipe.out.fluid(), recipe.out.amount());
    }

    @Override
    public void draw(BcHeatExchangeRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics,
            double mouseX, double mouseY) {
        drawArrow(guiGraphics, ARROW_X, 0);
        drawInfoLine(guiGraphics, recipe.kind + " " + recipe.heatFrom + " -> " + recipe.heatTo, INPUT_X, INFO_Y);
    }
}
