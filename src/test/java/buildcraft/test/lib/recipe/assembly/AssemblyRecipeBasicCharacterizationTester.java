/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.test.lib.recipe.assembly;

import java.util.Collections;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

import buildcraft.api.recipes.IngredientStack;
import buildcraft.lib.recipe.assembly.AssemblyRecipeBasic;
import buildcraft.test.VanillaSetupBaseTester;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * Characterization tests (M0.5 baseline) for {@link AssemblyRecipeBasic}, the pure data/matching core of the assembly
 * table recipes, on the 1.20.1 baseline. Surprising expectations pin down CURRENT behaviour on purpose - they are
 * regression guards for the NeoForge migration, NOT statements about what the behaviour ought to be.
 *
 * Coverage boundary: {@link buildcraft.lib.recipe.assembly.AssemblyRecipeRegistry#getAll} needs a live Level +
 * RecipeManager, and the facade recipe list needs block/item registries, so only AssemblyRecipeBasic (plus the
 * IngredientStack value it consumes) is tested here.
 */
public class AssemblyRecipeBasicCharacterizationTester extends VanillaSetupBaseTester {

    private static IngredientStack ingredient(ItemLike item, int count) {
        return new IngredientStack(net.minecraft.world.item.crafting.Ingredient.of(item), count);
    }

    private static AssemblyRecipeBasic makeRecipe() {
        return new AssemblyRecipeBasic("buildcraft:test_recipe", 100,//
            ImmutableSet.of(ingredient(Items.APPLE, 1)), new ItemStack(Items.DIAMOND));
    }

    private static NonNullList<ItemStack> inputs(ItemStack... stacks) {
        NonNullList<ItemStack> list = NonNullList.create();
        Collections.addAll(list, stacks);
        return list;
    }

    @Test
    public void matchingInputsProduceTheOutput() {
        AssemblyRecipeBasic recipe = makeRecipe();
        Set<ItemStack> outputs = recipe.getOutputs(inputs(new ItemStack(Items.APPLE, 2)));
        Assert.assertEquals(1, outputs.size());
        ItemStack out = outputs.iterator().next();
        Assert.assertEquals(Items.DIAMOND, out.getItem());
        Assert.assertEquals(1, out.getCount());
    }

    @Test
    public void emptyInputsProduceNothing() {
        Assert.assertTrue(makeRecipe().getOutputs(inputs()).isEmpty());
    }

    @Test
    public void inputCountMustReachTheRequiredCount() {
        AssemblyRecipeBasic recipe = new AssemblyRecipeBasic("buildcraft:test_recipe", 100,//
            ImmutableSet.of(ingredient(Items.APPLE, 3)), new ItemStack(Items.DIAMOND));
        // 2 < 3: no output
        Assert.assertTrue(recipe.getOutputs(inputs(new ItemStack(Items.APPLE, 2))).isEmpty());
        // exactly 3: output
        Assert.assertFalse(recipe.getOutputs(inputs(new ItemStack(Items.APPLE, 3))).isEmpty());
    }

    @Test
    public void emptyStacksNeverMatchEvenWithTheRightItem_quirk() {
        // Characterization baseline, not a correctness claim: a stack with count 0 isEmpty() and is skipped, even
        // though the item type would match
        Assert.assertTrue(makeRecipe().getOutputs(inputs(new ItemStack(Items.APPLE, 0))).isEmpty());
    }

    @Test
    public void everyRequiredIngredientMustBePresent() {
        AssemblyRecipeBasic recipe = new AssemblyRecipeBasic("buildcraft:test_recipe", 100,//
            ImmutableSet.of(ingredient(Items.APPLE, 1), ingredient(Items.IRON_NUGGET, 1)),//
            new ItemStack(Items.DIAMOND));
        Assert.assertFalse(
            recipe.getOutputs(inputs(new ItemStack(Items.APPLE), new ItemStack(Items.IRON_NUGGET))).isEmpty());
        Assert.assertTrue(recipe.getOutputs(inputs(new ItemStack(Items.APPLE))).isEmpty());
        Assert.assertTrue(recipe.getOutputs(inputs(new ItemStack(Items.IRON_NUGGET))).isEmpty());
    }

    @Test
    public void extraUnrelatedInputsAreIgnored() {
        Assert.assertFalse(makeRecipe()
            .getOutputs(inputs(new ItemStack(Items.APPLE), new ItemStack(Items.STONE, 64))).isEmpty());
    }

    @Test
    public void metadataAccessorsAreConstant() {
        AssemblyRecipeBasic recipe = makeRecipe();
        // previews are always the output, even with no inputs
        Assert.assertEquals(1, recipe.getOutputPreviews().size());
        Assert.assertEquals(Items.DIAMOND, recipe.getOutputPreviews().iterator().next().getItem());
        Assert.assertEquals(recipe.getRequiredIngredientStacks(), recipe.getInputsFor(new ItemStack(Items.DIAMOND)));
        Assert.assertEquals(100, recipe.getRequiredMicroJoulesFor(new ItemStack(Items.DIAMOND)));
        Assert.assertEquals(100, recipe.getRequiredMicroJoules());
        Assert.assertEquals(1, recipe.getOutput().size());
    }

    @Test
    public void equalityIsByIdOnly() {
        AssemblyRecipeBasic recipe = makeRecipe();
        AssemblyRecipeBasic sameIdDifferentData = new AssemblyRecipeBasic("buildcraft:test_recipe", 999,//
            ImmutableSet.of(ingredient(Items.STONE, 5)), new ItemStack(Items.STICK));
        AssemblyRecipeBasic differentId = new AssemblyRecipeBasic("buildcraft:other_recipe", 100,//
            ImmutableSet.of(ingredient(Items.APPLE, 1)), new ItemStack(Items.DIAMOND));
        Assert.assertEquals(recipe, sameIdDifferentData);
        Assert.assertEquals(recipe.hashCode(), sameIdDifferentData.hashCode());
        Assert.assertNotEquals(recipe, differentId);
        Assert.assertEquals(new ResourceLocation("buildcraft", "test_recipe"), recipe.getId());
    }

    @Test
    public void resultAndAssembleBothReturnTheOutputStack() {
        AssemblyRecipeBasic recipe = makeRecipe();
        ItemStack byResult = recipe.getResultItem(null);
        ItemStack byAssemble = recipe.assemble(null, null);
        Assert.assertEquals(Items.DIAMOND, byResult.getItem());
        Assert.assertEquals(Items.DIAMOND, byAssemble.getItem());
        Assert.assertEquals(1, byResult.getCount());
        Assert.assertEquals(1, byAssemble.getCount());
    }

    @Test
    public void ingredientStackKeepsItsCount() {
        IngredientStack stack = ingredient(Items.APPLE, 3);
        Assert.assertEquals(3, stack.count);
        Assert.assertTrue(stack.ingredient.test(new ItemStack(Items.APPLE)));
        Assert.assertFalse(stack.ingredient.test(new ItemStack(Items.STONE)));
    }

    @Test
    public void stringConstructorMapsToTheNamespacedId() {
        AssemblyRecipeBasic recipe = makeRecipe();
        Assert.assertEquals(new ResourceLocation("buildcraft", "test_recipe"), recipe.getId());
    }
}
