/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.guide.parts.recipe;

import buildcraft.lib.client.guide.GuiGuide;
import buildcraft.lib.client.guide.parts.GuidePartFactory;
import buildcraft.lib.misc.ArrayUtil;
import buildcraft.lib.recipe.ChangingItemStack;
import buildcraft.lib.recipe.ChangingObject;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

public class GuideAssemblyFactory implements GuidePartFactory {
    private final ChangingItemStack[] input;
    private final ChangingItemStack output;
    private final ChangingObject<Long> mjCost;
    private final int hash;

    public GuideAssemblyFactory(ChangingItemStack[] input, ChangingItemStack output, ChangingObject<Long> mjCost) {
        this.input = input;
        this.output = output;
        this.mjCost = mjCost;
        this.hash = computeHash();
    }

    public GuideAssemblyFactory(ItemStack[] input, ItemStack output, long mjCost) {
        this.input = ArrayUtil.map(input, ChangingItemStack::new, ChangingItemStack[]::new);
        this.output = new ChangingItemStack(output);
        this.mjCost = new ChangingObject<>(new Long[] { mjCost });
        this.hash = computeHash();
    }

    private int computeHash() {
        return Arrays.deepHashCode(new Object[] { input, output, mjCost });
    }

    // BCLog.logger.warn("[lib.guide.crafting] Found a matching recipe, but of an unknown " + recipe.getClass() + " for

    // // TODO: Implement IRecipeViewable usage

    // // YAAAAY REFLECTION :(

    // @Nonnull
    // // It will be sorted out below
    // // Technically a safe cast as the first one WAS an Item Stack and we never add to the list
    // // The lower the ID of an item, the closer it is to minecraft. Hmmm.


    @Override
    public GuideAssembly createNew(GuiGuide gui) {
        return new GuideAssembly(gui, input, output, mjCost);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null) return false;
        if (obj.getClass() != getClass()) return false;
        GuideAssemblyFactory other = (GuideAssemblyFactory) obj;
        // Shortcut out of this full itemstack comparison as its really expensive
        if (hash != other.hash) return false;
        if (input.length != other.input.length) return false;
        return Arrays.equals(input, other.input) && output.equals(other.output);
    }
}
