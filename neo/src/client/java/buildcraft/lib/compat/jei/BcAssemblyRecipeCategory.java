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
import buildcraft.silicon.recipe.BcAssemblyRecipe;

/**
 * JEI category for {@code buildcraftsilicon:assembly} recipes. Layout: up to two input stacks (the 85 datagen
 * recipes use at most two {@code requiredStacks}), arrow, output stack, and the micro-joule cost on the info line.
 * The {@code FACADE} sub-type has no output and no inputs — it renders as just the energy line.
 */
public final class BcAssemblyRecipeCategory extends AbstractBcRecipeCategory<BcAssemblyRecipe> {

    private static final int INPUT_X = 0;
    private static final int ARROW_X = 44;
    private static final int OUTPUT_X = 70;
    private static final int INFO_Y = 24;

    public BcAssemblyRecipeCategory(IGuiHelper guiHelper) {
        // Title: legacy "Assembly Table" machine name; icon: the assembly table block itself.
        super(guiHelper, BcJeiRecipeTypes.ASSEMBLY, "tile.assemblyTableBlock.name", BcSiliconBlocks.ASSEMBLY_TABLE,
            OUTPUT_X + 18, INFO_Y + 10);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BcAssemblyRecipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < recipe.requiredStacks.size(); i++) {
            IRecipeSlotBuilder slot = addSlot(builder, RecipeIngredientRole.INPUT, INPUT_X + i * SLOT_PITCH, 0);
            addIngredientStack(slot, recipe.requiredStacks.get(i));
        }
        recipe.output.ifPresent(output -> {
            IRecipeSlotBuilder slot = addSlot(builder, RecipeIngredientRole.OUTPUT, OUTPUT_X, 0);
            slot.add(output);
        });
    }

    @Override
    public void draw(BcAssemblyRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics,
            double mouseX, double mouseY) {
        drawArrow(guiGraphics, ARROW_X, 0);
        drawInfoLine(guiGraphics, formatMicroJoules(recipe.requiredMicroJoules), INPUT_X, INFO_Y);
    }
}
