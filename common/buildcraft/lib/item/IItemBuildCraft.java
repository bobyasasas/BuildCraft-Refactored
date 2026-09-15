/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.item;

import buildcraft.lib.registry.CreativeTabManager;
import buildcraft.lib.registry.TagManager;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public interface IItemBuildCraft {
    String getIdBC();

    default void init() {
        this.setUnlocalizedName("item." + TagManager.getTag(this.getIdBC(), TagManager.EnumTagType.UNLOCALIZED_NAME) + ".name");
    }

    public abstract void setUnlocalizedName(String unlocalizedName);

//    /** Sets up all of the model information for this item. This is called multiple times, and you *must* make sure that
//     * you add all the same values each time. Use {@link #addVariant(TIntObjectHashMap, int, String)} to help get
//     * everything correct. */


//                BCLog.logger.info("[lib.registry][" + thisItem.getRegistryName() + "] Registering a variant " + variant

    default ResourceLocation getRegistryName() {
        return ((Item) this).builtInRegistryHolder().key().location();
    }

    default void tab(CreativeModeTab tab) {
        CreativeTabManager.addItem(tab, (Item) this);
    }

    public default void fillItemCategory(NonNullList<ItemStack> items) {
        items.add(new ItemStack((Item) this));
    }
}
