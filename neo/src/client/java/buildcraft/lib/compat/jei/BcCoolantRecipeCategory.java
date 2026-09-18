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
import buildcraft.energy.recipe.BcCoolantRecipe;

/**
 * JEI category for {@code buildcraftenergy:coolant} recipes. Layout: the coolant fluid plus the optional solid
 * coolant ingredient (ice-type coolants carry one, fluid coolants don't), and the cooling strength on the info line —
 * {@code degreesCoolingPerMb} for fluid coolants, the per-item {@code multiplier} for solid ones. Title key note: no
 * dedicated coolant-machine name exists in the frozen lang set, so the nearest semantic key ("Coolant Tank", from the
 * tank help screen) is reused — flagged for main-agent review.
 */
public final class BcCoolantRecipeCategory extends AbstractBcRecipeCategory<BcCoolantRecipe> {

    private static final int FLUID_X = 0;
    private static final int SOLID_X = SLOT_PITCH;
    private static final int INFO_Y = 24;

    public BcCoolantRecipeCategory(IGuiHelper guiHelper) {
        // Title: "Coolant Tank" (nearest existing coolant key); icon/catalyst: the combustion engine that is cooled.
        super(guiHelper, BcJeiRecipeTypes.COOLANT, "buildcraft.help.tank.title.tankCoolant", BcBlocks.ENGINE_IRON,
            SOLID_X + 18, INFO_Y + 10);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BcCoolantRecipe recipe, IFocusGroup focuses) {
        IRecipeSlotBuilder fluid = addFluidSlot(builder, RecipeIngredientRole.INPUT, FLUID_X, 0, recipe.fluid.amount());
        fluid.add(recipe.fluid.fluid(), recipe.fluid.amount());

        recipe.solid.ifPresent(solid -> {
            IRecipeSlotBuilder slot = addSlot(builder, RecipeIngredientRole.INPUT, SOLID_X, 0);
            slot.setSlotName("solid");
            slot.add(solid);
        });
    }

    @Override
    public void draw(BcCoolantRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics,
            double mouseX, double mouseY) {
        String line = recipe.degreesCoolingPerMb
            .map(degrees -> formatDegreesPerMb(degrees))
            .orElseGet(() -> recipe.multiplier
                .map(multiplier -> String.format(Locale.US, "x%,.1f per item", multiplier))
                .orElse(recipe.coolantType));
        drawInfoLine(guiGraphics, line, FLUID_X, INFO_Y);
    }

    private static String formatDegreesPerMb(float degrees) {
        return String.format(Locale.US, "%,.2f \u00b0C/mb", degrees);
    }
}
