package buildcraft.lib.recipe.programming;

import buildcraft.api.core.BCLog;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.recipes.IProgrammingRecipe;
import buildcraft.api.recipes.IProgrammingRecipeManager;
import com.google.common.collect.Lists;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.*;

public enum ProgrammingRecipeManager implements IProgrammingRecipeManager {
    INSTANCE;
    private final HashMap<ResourceLocation, IProgrammingRecipe> recipes = new HashMap<ResourceLocation, IProgrammingRecipe>();

    @Override
    public void addRecipe(IProgrammingRecipe recipe) {
        if (recipe == null || recipe.getId() == null) {
            return;
        }

        if (!recipes.containsKey(recipe.getId())) {
            recipes.put(recipe.getId(), recipe);
        } else {
            BCLog.logger.warn("Programming Table Recipe '" + recipe.getId() + "' seems to be duplicated! This is a bug!");
        }
    }

    @Override
    public void removeRecipe(ResourceLocation id) {
        recipes.remove(id);
    }

    @Override
    public void removeRecipe(IProgrammingRecipe recipe) {
        if (recipe == null || recipe.getId() == null) {
            return;
        }

        recipes.remove(recipe.getId());
    }

    @Override
    public Collection<IProgrammingRecipe> getRecipes(Level world) {
        Collection<IProgrammingRecipe> ret = Lists.newArrayList();
        ret.addAll(recipes.values());
        world.getRecipeManager().byType(IProgrammingRecipe.TYPE).values().stream().filter(c -> c instanceof IProgrammingRecipe).forEach(c -> ret.add((IProgrammingRecipe) c));
        return ret;
    }

    @Override
    public IProgrammingRecipe getRecipe(Level world, ResourceLocation id) {
        for (IProgrammingRecipe recipe : getRecipes(world)) {
            if (Objects.equals(recipe.getId(), id)) {
                return recipe;
            }
        }
        return null;
    }

    public List<IProgrammingRecipe> getOptions(List<IProgrammingRecipe> recipes, int width, int height) {
        List<IProgrammingRecipe> options = new ArrayList<IProgrammingRecipe>(width * height);
        for (int i = 0; i < recipes.size(); i++) {
            options.add(i, recipes.get(i));
        }
        Collections.sort(options, new Sorter());
        return options;
    }

    public static class Sorter implements Comparator<IProgrammingRecipe> {
        @Override
        public int compare(IProgrammingRecipe o1, IProgrammingRecipe o2) {
            long iL = (o1.getEnergyCost() - o2.getEnergyCost()) * 200L;
            int i = (int) (iL / MjAPI.MJ);
            return i != 0 ? i : o1.getId().compareTo(o2.getId());
        }
    }
}
