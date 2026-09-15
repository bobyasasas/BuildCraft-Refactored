/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.recipe.refinery;

import buildcraft.api.recipes.IRefineryRecipeManager;
import buildcraft.lib.misc.StackUtil;
import com.google.common.collect.Lists;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public enum RefineryRecipeRegistry implements IRefineryRecipeManager {
    INSTANCE;

    public final IRefineryRegistry<IDistillationRecipe> distillationRegistry = new SingleRegistry(IDistillationRecipe.TYPE);
    public final IRefineryRegistry<IHeatableRecipe> heatableRegistry = new SingleRegistry(IHeatableRecipe.TYPE);
    public final IRefineryRegistry<ICoolableRecipe> coolableRegistry = new SingleRegistry(ICoolableRecipe.TYPE);

    @Override
    public IHeatableRecipe createHeatingRecipe(ResourceLocation id, FluidStack in, FluidStack out, int heatFrom, int heatTo) {
        return new HeatableRecipe(id, in, out, heatFrom, heatTo);
    }

    @Override
    public ICoolableRecipe createCoolableRecipe(ResourceLocation id, FluidStack in, FluidStack out, int heatFrom, int heatTo) {
        return new CoolableRecipe(id, in, out, heatFrom, heatTo);
    }

    @Override
    public IDistillationRecipe createDistillationRecipe(ResourceLocation id, FluidStack in, FluidStack outGas, FluidStack outLiquid, long powerRequired) {
        return new DistillationRecipe(id, powerRequired, in, outGas, outLiquid);
    }

    @Override
    public IRefineryRegistry<IHeatableRecipe> getHeatableRegistry() {
        return heatableRegistry;
    }

    @Override
    public IRefineryRegistry<ICoolableRecipe> getCoolableRegistry() {
        return coolableRegistry;
    }

    @Override
    public IRefineryRegistry<IDistillationRecipe> getDistillationRegistry() {
        return distillationRegistry;
    }

    private static class SingleRegistry<R extends IRefineryRecipe> implements IRefineryRegistry<R> {
        private final List<R> unregisteredRecipes = new LinkedList<>();
        private final RecipeType<R> type;

        private SingleRegistry(RecipeType<R> type) {
            this.type = type;
        }

        @Override
        public Stream<R> getRecipes(Level world, Predicate<R> filter) {
            Collection<R> ret = Lists.newArrayList();
            ret.addAll(unregisteredRecipes);
            world.getRecipeManager().byType(type).values().forEach(r -> ret.add((R) r));
            return ret.stream().filter(filter);
        }

        @Override
        public Collection<R> getAllRecipes(Level world) {
            Collection<R> ret = Lists.newArrayList();
            ret.addAll(unregisteredRecipes);
            world.getRecipeManager().byType(type).values().forEach(r -> ret.add((R) r));
            return ret;
        }

        @Override
        @Nullable
        public R getRecipeForInput(Level world, @Nullable FluidStack fluid) {
            if (fluid == null) {
                return null;
            }
            for (R recipe : getAllRecipes(world)) {
                if (recipe.in().isFluidEqual(fluid)) {
                    return recipe;
                }
            }
            return null;
        }

        @Override
        public Collection<R> removeUnregisteredRecipes(Predicate<R> toRemove) {
            List<R> removed = new ArrayList<>();
            Iterator<R> iter = unregisteredRecipes.iterator();
            while (iter.hasNext()) {
                R recipe = iter.next();
                if (toRemove.test(recipe)) {
                    iter.remove();
                    removed.add(recipe);
                }
            }
            return removed;
        }

        @Override
        public R addUnregisteredRecipe(R recipe) {
            if (recipe == null) throw new NullPointerException("recipe");
            ListIterator<R> iter = unregisteredRecipes.listIterator();
            while (iter.hasNext()) {
                R existing = iter.next();
                if (existing.in().isFluidEqual(recipe.in())) {
                    iter.set(recipe);
                    return recipe;
                }
            }
            unregisteredRecipes.add(recipe);
            return recipe;
        }
    }

    public static abstract class RefineryRecipe implements IRefineryRecipe {
        private final ResourceLocation id;
        private final FluidStack in;

        public RefineryRecipe(ResourceLocation id, FluidStack in) {
            this.id = id;
            this.in = in;
        }

        @Override
        public FluidStack in() {
            return in;
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public boolean matches(Container inv, Level world) {
            return false;
        }

        @Override
        public ItemStack assemble(Container inv, RegistryAccess registryAccess) {
            return StackUtil.EMPTY;
        }

        @Override
        public boolean canCraftInDimensions(int width, int height) {
            return true;
        }

        @Override
        public ItemStack getResultItem(RegistryAccess registryAccess) {
            return StackUtil.EMPTY;
        }

//    @Override


        @Override
        public boolean isSpecial() {
            return true;
        }
    }

    public static class DistillationRecipe extends RefineryRecipe implements IDistillationRecipe {
        private final FluidStack outGas, outLiquid;
        private final long powerRequired;

        public DistillationRecipe(ResourceLocation id, long powerRequired, FluidStack in, FluidStack outGas, FluidStack outLiquid) {
            super(id, in);
            this.powerRequired = powerRequired;
            this.outGas = outGas;
            this.outLiquid = outLiquid;
        }

        @Override
        public FluidStack outGas() {
            return outGas;
        }

        @Override
        public FluidStack outLiquid() {
            return outLiquid;
        }

        @Override
        public long powerRequired() {
            return powerRequired;
        }

        @Override
        public RecipeSerializer<IDistillationRecipe> getSerializer() {
            return DistillationRecipeSerializer.INSTANCE;
        }
    }

    public static abstract class HeatExchangeRecipe extends RefineryRecipe implements IHeatExchangerRecipe {
        private final FluidStack out;
        private final int heatFrom, heatTo;

        public HeatExchangeRecipe(ResourceLocation id, FluidStack in, FluidStack out, int heatFrom, int heatTo) {
            super(id, in);
            this.out = out;
            this.heatFrom = heatFrom;
            this.heatTo = heatTo;
        }

        @Override
        public FluidStack out() {
            return out;
        }

        @Override
        public int heatFrom() {
            return heatFrom;
        }

        @Override
        public int heatTo() {
            return heatTo;
        }
    }

    public static class HeatableRecipe extends HeatExchangeRecipe implements IHeatableRecipe {
        public HeatableRecipe(ResourceLocation id, FluidStack in, FluidStack out, int heatFrom, int heatTo) {
            super(id, in, out, heatFrom, heatTo);
        }

        @Override
        public RecipeSerializer<IHeatExchangerRecipe> getSerializer() {
            return HeatExchangeRecipeSerializer.HEATABLE;
        }
    }

    public static class CoolableRecipe extends HeatExchangeRecipe implements ICoolableRecipe {
        public CoolableRecipe(ResourceLocation id, FluidStack in, FluidStack out, int heatFrom, int heatTo) {
            super(id, in, out, heatFrom, heatTo);
        }

        @Override
        public RecipeSerializer<IHeatExchangerRecipe> getSerializer() {
            return HeatExchangeRecipeSerializer.COOLABLE;
        }
    }
}
