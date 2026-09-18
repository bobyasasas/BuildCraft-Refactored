/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.compat.jei;

import java.util.Locale;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;

import buildcraft.core.BcBlocks;
import buildcraft.energy.recipe.BcFuelRecipe;

/**
 * JEI category for {@code buildcraftenergy:fuel} recipes (combustion engine fuels). Layout: the fuel fluid, arrow,
 * the residue fluid slot (only dirty fuels have one), and the power-per-cycle + burn time on the info line. The icon
 * and catalyst is the combustion engine block ({@code buildcraftcore:engine_iron}) — the machine that consumes these
 * recipes; BuildCraftEnergy itself registers no machine blocks beyond fluid placeholders.
 */
public final class BcFuelRecipeCategory extends AbstractBcRecipeCategory<BcFuelRecipe> {

    private static final int INPUT_X = 0;
    private static final int ARROW_X = 44;
    private static final int RESIDUE_X = 70;
    private static final int INFO_Y = 24;

    public BcFuelRecipeCategory(IGuiHelper guiHelper) {
        // Title: legacy "Combustion Engine" machine name; icon: the combustion engine block itself.
        super(guiHelper, BcJeiRecipeTypes.FUEL, "tile.engineIron.name", BcBlocks.ENGINE_IRON, RESIDUE_X + 18,
            INFO_Y + 10);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BcFuelRecipe recipe, IFocusGroup focuses) {
        IRecipeSlotBuilder fluid = addFluidSlot(builder, RecipeIngredientRole.INPUT, INPUT_X, 0, recipe.fluid.amount());
        fluid.add(recipe.fluid.fluid(), recipe.fluid.amount());

        recipe.residue.ifPresent(residue -> {
            IRecipeSlotBuilder slot = addFluidSlot(builder, RecipeIngredientRole.OUTPUT, RESIDUE_X, 0,
                residue.amount());
            slot.setSlotName("residue");
            slot.add(residue.fluid(), residue.amount());
        });
    }

    @Override
    public void draw(BcFuelRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics,
            double mouseX, double mouseY) {
        drawArrow(guiGraphics, ARROW_X, 0);
        String line = String.format(Locale.US, "%,d \u03bcJ/cycle \u00d7 %,d ticks", recipe.powerPerCycle,
            recipe.totalBurningTime);
        drawInfoLine(guiGraphics, line, INPUT_X, INFO_Y);
    }
}
