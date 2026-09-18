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

import buildcraft.silicon.BcSiliconBlocks;
import buildcraft.silicon.recipe.BcProgrammingRecipe;

/**
 * JEI category for {@code buildcraftsilicon:programming} recipes. Layout: one input board, arrow, the programmed
 * board output, and the laser energy cost on the info line. The 17 datagen recipes live under the buildcraftrobotics
 * namespace but share the silicon programming-table serializer/type.
 */
public final class BcProgrammingRecipeCategory extends AbstractBcRecipeCategory<BcProgrammingRecipe> {

    private static final int INPUT_X = 0;
    private static final int ARROW_X = 25;
    private static final int OUTPUT_X = 51;
    private static final int INFO_Y = 24;

    public BcProgrammingRecipeCategory(IGuiHelper guiHelper) {
        // Title: legacy "Programming Table" machine name; icon: the programming table block itself.
        super(guiHelper, BcJeiRecipeTypes.PROGRAMMING, "tile.programmingTableBlock.name",
            BcSiliconBlocks.PROGRAMMING_TABLE, OUTPUT_X + 18, INFO_Y + 10);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BcProgrammingRecipe recipe, IFocusGroup focuses) {
        IRecipeSlotBuilder input = addSlot(builder, RecipeIngredientRole.INPUT, INPUT_X, 0);
        addIngredientStack(input, recipe.input);

        IRecipeSlotBuilder output = addSlot(builder, RecipeIngredientRole.OUTPUT, OUTPUT_X, 0);
        output.add(recipe.output);
    }

    @Override
    public void draw(BcProgrammingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics,
            double mouseX, double mouseY) {
        drawArrow(guiGraphics, ARROW_X, 0);
        drawInfoLine(guiGraphics, formatMicroJoules(recipe.energyCost), INPUT_X, INFO_Y);
    }
}
