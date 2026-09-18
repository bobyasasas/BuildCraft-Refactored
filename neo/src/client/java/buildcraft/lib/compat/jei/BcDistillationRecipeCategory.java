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
import buildcraft.factory.recipe.BcDistillationRecipe;
import buildcraft.lib.recipe.BcFluidAmount;

/**
 * JEI category for {@code buildcraftfactory:distillation} recipes. Layout: one fluid input, arrow, gas + liquid
 * fluid outputs side by side (matching the distiller's two output tanks), and the micro-joule cost on the info line.
 */
public final class BcDistillationRecipeCategory extends AbstractBcRecipeCategory<BcDistillationRecipe> {

    private static final int INPUT_X = 0;
    private static final int ARROW_X = 44;
    private static final int GAS_X = 70;
    private static final int LIQUID_X = GAS_X + SLOT_PITCH;
    private static final int INFO_Y = 24;

    public BcDistillationRecipeCategory(IGuiHelper guiHelper) {
        // Title: legacy "Distiller" machine name; icon: the distiller block itself.
        super(guiHelper, BcJeiRecipeTypes.DISTILLATION, "tile.distiller.name", BcFactoryBlocks.DISTILLER,
            LIQUID_X + 18, INFO_Y + 10);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BcDistillationRecipe recipe, IFocusGroup focuses) {
        IRecipeSlotBuilder input = addFluidSlot(builder, RecipeIngredientRole.INPUT, INPUT_X, 0, recipe.in.amount());
        input.add(recipe.in.fluid(), recipe.in.amount());

        IRecipeSlotBuilder gas = addFluidSlot(builder, RecipeIngredientRole.OUTPUT, GAS_X, 0, recipe.outGas.amount());
        gas.setSlotName("outGas");
        gas.add(recipe.outGas.fluid(), recipe.outGas.amount());

        IRecipeSlotBuilder liquid = addFluidSlot(builder, RecipeIngredientRole.OUTPUT, LIQUID_X, 0,
            recipe.outLiquid.amount());
        liquid.setSlotName("outLiquid");
        liquid.add(recipe.outLiquid.fluid(), recipe.outLiquid.amount());
    }

    @Override
    public void draw(BcDistillationRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics,
            double mouseX, double mouseY) {
        drawArrow(guiGraphics, ARROW_X, 0);
        drawInfoLine(guiGraphics, formatMicroJoules(recipe.powerRequired), INPUT_X, INFO_Y);
    }
}
