/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.list;

import buildcraft.api.lists.ListMatchHandler;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;

import javax.annotation.Nonnull;

public class ListMatchHandlerTools extends ListMatchHandler {
    /** Matches tool type (axe...), not tier (wooden/iron/...). */
    @Override
    public boolean matches(Type type, @Nonnull ItemStack stack, @Nonnull ItemStack target, boolean precise) {
        if (type == Type.TYPE) {
            if (stack.getItem() instanceof TieredItem item1 && target.getItem() instanceof TieredItem item2) {
                return item1.getClass() == item2.getClass();
            }
        }
        return false;
    }

    @Override
    public boolean isValidSource(Type type, @Nonnull ItemStack stack) {
        return stack.getItem() instanceof TieredItem;
    }
}
