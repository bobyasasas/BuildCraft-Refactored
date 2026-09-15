package buildcraft.lib.gui.recipe;

import buildcraft.lib.gui.slot.SlotPhantom;
import buildcraft.lib.tile.item.ItemHandlerManager;
import buildcraft.lib.tile.item.ItemHandlerManager.EnumAccess;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;

import java.util.function.Consumer;

/** A {@link RecipeBookComponent} that can always craft things, as it puts the required items into phantom slots (Either
 * {@link SlotPhantom} or {@link ItemHandlerManager} with an argument of {@link EnumAccess#PHANTOM}. */
//public class GuiRecipeBookPhantom extends GuiRecipeBook
public class GuiRecipeBookPhantom extends RecipeBookComponent {


    public final Consumer<CraftingRecipe> recipeSetter;

//    // Unfortunately we have to use reflection in order to replace the necessary fields :(
//                    else

    //        public GuiRecipeBookPhantom(Consumer<Recipe> recipeSetter) throws ReflectiveOperationException
    public GuiRecipeBookPhantom(Consumer<CraftingRecipe> recipeSetter) throws ReflectiveOperationException
//    public GuiRecipeBookPhantom(Consumer<CraftingRecipe> recipeSetter, int width, int height, Minecraft minecraft, boolean widthTooNarrow, RecipeBookMenu<?> container) throws ReflectiveOperationException
    {
        this.recipeSetter = recipeSetter;
        this.recipeBookPage = new RecipeBookPagePhantom(this);

//        // Filtering craftable is really strange with phantom inventories
    }

    // 1.20.1: no override
//    @Override

    public void initVisuals(boolean someBoolean, CraftingContainer invCrafting) {
        this.initVisuals();
        super.xOffset = super.widthTooNarrow ? 0 : 86;
        invCrafting.fillStackedContents(super.stackedContents);
    }

    @Override
    public void initVisuals() {
        super.initVisuals();
//        try
        StateSwitchingButton button = this.filterButton;
        button.setX(-100000);
        button.setY(-100000);
    }
}
