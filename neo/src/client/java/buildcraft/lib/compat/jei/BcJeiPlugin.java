/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import org.slf4j.Logger;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;

import buildcraft.core.BcBlocks;
import buildcraft.energy.recipe.BcEnergyRecipes;
import buildcraft.factory.BcFactoryBlocks;
import buildcraft.factory.recipe.BcFactoryRecipes;
import buildcraft.silicon.BcSiliconBlocks;
import buildcraft.silicon.recipe.BcSiliconRecipes;

/**
 * BuildCraft's JEI plugin (JEI 29.x for MC 26.1.2, compileOnly integration).
 * <p>
 * Discovery: JEI scans mod files for classes annotated with {@link JeiPlugin} via FML's {@code ModFileScanData}
 * (bytecode level, no class loading), then reflects and instantiates them itself when JEI is present. Nothing in
 * BuildCraft references this class, so with JEI absent it is never loaded — which is what keeps the compileOnly
 * integration safe at runtime. All JEI API types stay inside this package.
 * <p>
 * v2: the seven machine recipe categories (assembly / integration / programming / distillation / heat exchange /
 * fuel / coolant) over the M2.10 recipe payloads. Recipe data is pulled from the integrated server's
 * {@link RecipeManager} — JEI 29 registers recipes after the client has a {@code ClientLevel}, so the singleplayer
 * server is up by then. Dedicated-server sync (JEI's own recipe sync channel) is out of scope for v1: on a remote
 * server without JEI the categories simply stay empty.
 */
@JeiPlugin
public class BcJeiPlugin implements IModPlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath("buildcraftcore", "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
            new BcAssemblyRecipeCategory(guiHelper),
            new BcIntegrationRecipeCategory(guiHelper),
            new BcProgrammingRecipeCategory(guiHelper),
            new BcDistillationRecipeCategory(guiHelper),
            new BcHeatExchangeRecipeCategory(guiHelper),
            new BcFuelRecipeCategory(guiHelper),
            new BcCoolantRecipeCategory(guiHelper)
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            LOGGER.warn("[BuildCraft JEI] no integrated server — BuildCraft recipe categories stay empty "
                + "(remote servers need JEI's server-side recipe sync, out of scope for v1)");
            return;
        }
        RecipeManager recipeManager = server.getRecipeManager();
        addRecipes(registration, BcJeiRecipeTypes.ASSEMBLY, BcSiliconRecipes.ASSEMBLY_TYPE.get(), recipeManager);
        addRecipes(registration, BcJeiRecipeTypes.INTEGRATION, BcSiliconRecipes.INTEGRATION_TYPE.get(), recipeManager);
        addRecipes(registration, BcJeiRecipeTypes.PROGRAMMING, BcSiliconRecipes.PROGRAMMING_TYPE.get(), recipeManager);
        addRecipes(registration, BcJeiRecipeTypes.DISTILLATION, BcFactoryRecipes.DISTILLATION_TYPE.get(),
            recipeManager);
        addRecipes(registration, BcJeiRecipeTypes.HEAT_EXCHANGE, BcFactoryRecipes.HEAT_EXCHANGE_TYPE.get(),
            recipeManager);
        addRecipes(registration, BcJeiRecipeTypes.FUEL, BcEnergyRecipes.FUEL_TYPE.get(), recipeManager);
        addRecipes(registration, BcJeiRecipeTypes.COOLANT, BcEnergyRecipes.COOLANT_TYPE.get(), recipeManager);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(BcJeiRecipeTypes.ASSEMBLY, BcSiliconBlocks.ASSEMBLY_TABLE);
        registration.addCraftingStation(BcJeiRecipeTypes.INTEGRATION, BcSiliconBlocks.INTEGRATION_TABLE);
        registration.addCraftingStation(BcJeiRecipeTypes.PROGRAMMING, BcSiliconBlocks.PROGRAMMING_TABLE);
        registration.addCraftingStation(BcJeiRecipeTypes.DISTILLATION, BcFactoryBlocks.DISTILLER);
        registration.addCraftingStation(BcJeiRecipeTypes.HEAT_EXCHANGE, BcFactoryBlocks.HEAT_EXCHANGE);
        // No combustion engine block exists under buildcraftenergy yet — the legacy engine lives in core.
        registration.addCraftingStation(BcJeiRecipeTypes.FUEL, BcBlocks.ENGINE_IRON);
        registration.addCraftingStation(BcJeiRecipeTypes.COOLANT, BcBlocks.ENGINE_IRON);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        LOGGER.info("BuildCraft JEI compat registered");
        // Second opinion on the registration counts, straight from JEI's runtime recipe manager.
        for (IRecipeType<?> type : List.of(BcJeiRecipeTypes.ASSEMBLY, BcJeiRecipeTypes.INTEGRATION,
                BcJeiRecipeTypes.PROGRAMMING, BcJeiRecipeTypes.DISTILLATION, BcJeiRecipeTypes.HEAT_EXCHANGE,
                BcJeiRecipeTypes.FUEL, BcJeiRecipeTypes.COOLANT)) {
            long count = runtime.getRecipeManager().createRecipeLookup(type).get().count();
            LOGGER.info("[BuildCraft JEI] category {} visible recipes: {}", type.getUid(), count);
        }
    }

    /** Adds every vanilla-recipe-manager recipe of {@code vanillaType} to the JEI category {@code jeiType}. */
    private static <T extends Recipe<?>> void addRecipes(IRecipeRegistration registration, IRecipeType<T> jeiType,
            net.minecraft.world.item.crafting.RecipeType<?> vanillaType, RecipeManager recipeManager) {
        List<T> recipes = new ArrayList<>();
        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            Recipe<?> recipe = holder.value();
            if (recipe.getType() == vanillaType && jeiType.getRecipeClass().isInstance(recipe)) {
                recipes.add(jeiType.getRecipeClass().cast(recipe));
            }
        }
        registration.addRecipes(jeiType, recipes);
        LOGGER.info("[BuildCraft JEI] category {} registered {} recipes", jeiType.getUid(), recipes.size());
    }
}
