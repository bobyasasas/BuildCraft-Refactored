/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;

/**
 * Common base of the BuildCraft machine recipes migrated in M2.10 (assembly, programming, integration, facade swap,
 * heat exchange, distillation, fuel, coolant). Their 1.20.1 counterparts were never vanilla crafting recipes: they are
 * data carriers the machines scan by {@link net.minecraft.world.item.crafting.RecipeType} (the legacy
 * {@code IAssemblyRecipe} etc. already answered {@code matches(...) = false} and {@code isSpecial() = true}). This
 * base provides that inert behaviour; the {@link Recipe} interface itself is declared by the concrete types (their
 * serializer/type lookups bind to each module's own registration holders):
 * <ul>
 * <li>{@code isSpecial() = true} keeps them out of the recipe book and the placement-ingredient collection;</li>
 * <li>{@code matches/assemble} stay inert because the consuming machines (assembly table, refinery, ...) port in the
 * later content milestones M2.11-M2.13 and will query the data via the typed getters instead.</li>
 * </ul>
 */
public abstract class BcMachineRecipe {

    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    public ItemStack assemble(RecipeInput input) {
        return ItemStack.EMPTY;
    }

    public boolean isSpecial() {
        return true;
    }

    public boolean showNotification() {
        return false;
    }

    public String group() {
        return "";
    }

    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }
}
