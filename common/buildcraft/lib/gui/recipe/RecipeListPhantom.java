package buildcraft.lib.gui.recipe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.world.item.crafting.Recipe;

import java.lang.reflect.Field;
import java.util.Set;

//public class RecipeListPhantom extends RecipeList
public class RecipeListPhantom extends RecipeCollection {

    // public RecipeListPhantom(RecipeList from) throws ReflectiveOperationException
    public RecipeListPhantom(RecipeCollection from) throws ReflectiveOperationException {
        super(Minecraft.getInstance().level.registryAccess(), from.getRecipes());
        Class<?> clazzBitSet = Set.class;
        boolean first = true;
        for (Field fld : RecipeCollection.class.getDeclaredFields()) {
            if (fld.getType() == clazzBitSet) {
                fld.setAccessible(true);
                Object object = fld.get(from);
                if (first) {
                }
                fld.set(this, object);
                first = false;
            }
        }
    }

    @Override
    public boolean hasSingleResultItem() {
        // Only called by the draw function -- for some reason this will render a second
        // item beside the first if this returns true and getOrderedRecipes().size() > 1
        return false;
    }

    @Override
    public boolean isCraftable(Recipe recipe) {
        return true;
    }

    @Override
    public boolean hasCraftable() {
        return !getRecipes().isEmpty();
    }
}
