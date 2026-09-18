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
import buildcraft.silicon.recipe.BcIntegrationRecipe;

/**
 * JEI category for {@code buildcraftsilicon:integration} recipes. Layout: the centre stack, the surrounding
 * requirement stacks (the 17 datagen recipes use at most one), arrow, output, and the micro-joule cost on the info
 * line — matching the integration table's "one board plus additions" semantics.
 */
public final class BcIntegrationRecipeCategory extends AbstractBcRecipeCategory<BcIntegrationRecipe> {

    private static final int CENTER_X = 0;
    private static final int REQUIREMENT_X = SLOT_PITCH;
    private static final int ARROW_X = 44;
    private static final int OUTPUT_X = 70;
    private static final int INFO_Y = 24;

    public BcIntegrationRecipeCategory(IGuiHelper guiHelper) {
        // Title: legacy "Integration Table" machine name; icon: the integration table block itself.
        super(guiHelper, BcJeiRecipeTypes.INTEGRATION, "tile.integrationTableBlock.name",
            BcSiliconBlocks.INTEGRATION_TABLE, OUTPUT_X + 18, INFO_Y + 10);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BcIntegrationRecipe recipe, IFocusGroup focuses) {
        IRecipeSlotBuilder center = addSlot(builder, RecipeIngredientRole.INPUT, CENTER_X, 0);
        center.setSlotName("center");
        addIngredientStack(center, recipe.centerStack);

        for (int i = 0; i < recipe.requirements.size(); i++) {
            IRecipeSlotBuilder requirement = addSlot(builder, RecipeIngredientRole.INPUT,
                REQUIREMENT_X + i * SLOT_PITCH, 0);
            addIngredientStack(requirement, recipe.requirements.get(i));
        }

        IRecipeSlotBuilder output = addSlot(builder, RecipeIngredientRole.OUTPUT, OUTPUT_X, 0);
        output.add(recipe.output);
    }

    @Override
    public void draw(BcIntegrationRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics,
            double mouseX, double mouseY) {
        drawArrow(guiGraphics, ARROW_X, 0);
        drawInfoLine(guiGraphics, formatMicroJoules(recipe.requiredMicroJoules), CENTER_X, INFO_Y);
    }
}
