package buildcraft.lib.recipe.programming;

import buildcraft.api.recipes.IProgrammingRecipe;
import buildcraft.api.recipes.IngredientStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.List;

public class BoardProgrammingRecipe implements IProgrammingRecipe {

//        @Override

    private final ResourceLocation id;
    private final IngredientStack input;
    private final ItemStack output;
    private final long energyCost;

    public BoardProgrammingRecipe(ResourceLocation id, IngredientStack input, ItemStack output, long energyCost) {
        this.id = id;
        this.input = input;
        this.output = output;
        this.energyCost = energyCost;
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    private List<ItemStack> cachedSorted;
    private List<ItemStack> cachedOptions;

//    @Override

    @Override
    public long getEnergyCost() {
        return this.energyCost;
    }

    @Override
    public boolean canCraft(ItemStack input) {
        return this.input.ingredient.test(input);
    }

    @Override
    public ItemStack craft(ItemStack input) {
        return output.copy();
    }

    @Override
    public IngredientStack getInput() {
        return input;
    }

    @Override
    public ItemStack getOutput() {
        return output;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ProgrammingRecipeSerializer.INSTANCE;
    }
}
