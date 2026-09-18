/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.compat.jei;

import mezz.jei.api.recipe.types.IRecipeType;

import buildcraft.energy.recipe.BcCoolantRecipe;
import buildcraft.energy.recipe.BcFuelRecipe;
import buildcraft.factory.recipe.BcDistillationRecipe;
import buildcraft.factory.recipe.BcHeatExchangeRecipe;
import buildcraft.silicon.recipe.BcAssemblyRecipe;
import buildcraft.silicon.recipe.BcIntegrationRecipe;
import buildcraft.silicon.recipe.BcProgrammingRecipe;

/**
 * The seven JEI {@link IRecipeType}s for the BuildCraft machine recipe families (M2.10 payloads). Uids live under the
 * {@code buildcraftlib} namespace, one per vanilla {@code RecipeType} registered by BcSiliconRecipes / BcFactoryRecipes
 * / BcEnergyRecipes. The {@code facade_swap} singleton deliberately has no category (main-agent decision: it is a
 * parameter-less marker recipe, not something a player looks up).
 * <p>
 * JEI types carry the raw recipe class (not {@code RecipeHolder}) so the categories can render the public final fields
 * directly; JEI only ever sees instances pulled out of the vanilla {@code RecipeManager} by the plugin. Built via
 * {@link IRecipeType#create} — the old concrete {@code mezz.jei.api.recipe.RecipeType} factories are deprecated for
 * removal since JEI 20.
 */
public final class BcJeiRecipeTypes {

    public static final IRecipeType<BcAssemblyRecipe> ASSEMBLY = IRecipeType
        .create("buildcraftlib", "assembly", BcAssemblyRecipe.class);
    public static final IRecipeType<BcIntegrationRecipe> INTEGRATION = IRecipeType
        .create("buildcraftlib", "integration", BcIntegrationRecipe.class);
    public static final IRecipeType<BcProgrammingRecipe> PROGRAMMING = IRecipeType
        .create("buildcraftlib", "programming", BcProgrammingRecipe.class);
    public static final IRecipeType<BcDistillationRecipe> DISTILLATION = IRecipeType
        .create("buildcraftlib", "distillation", BcDistillationRecipe.class);
    public static final IRecipeType<BcHeatExchangeRecipe> HEAT_EXCHANGE = IRecipeType
        .create("buildcraftlib", "heat_exchange", BcHeatExchangeRecipe.class);
    public static final IRecipeType<BcFuelRecipe> FUEL = IRecipeType
        .create("buildcraftlib", "fuel", BcFuelRecipe.class);
    public static final IRecipeType<BcCoolantRecipe> COOLANT = IRecipeType
        .create("buildcraftlib", "coolant", BcCoolantRecipe.class);

    private BcJeiRecipeTypes() {
    }
}
