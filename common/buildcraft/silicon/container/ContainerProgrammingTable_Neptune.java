/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.container;

import buildcraft.lib.gui.ContainerBCTile;
import buildcraft.lib.gui.slot.SlotBase;
import buildcraft.lib.gui.slot.SlotDisplay;
import buildcraft.lib.gui.slot.SlotOutput;
import buildcraft.silicon.tile.TileProgrammingTable_Neptune;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class ContainerProgrammingTable_Neptune extends ContainerBCTile<TileProgrammingTable_Neptune> {

    public ContainerProgrammingTable_Neptune(MenuType menuType, int id, Player player, TileProgrammingTable_Neptune tile) {
        super(menuType, id, player, tile);


        addFullPlayerInventory(123);

        addSlot(new SlotBase(tile.input, 0, 8, 36));
        addSlot(new SlotOutput(tile.output, 0, 8, 90));



        for (int j = 0; j < TileProgrammingTable_Neptune.HEIGHT; ++j) {
            for (int i = 0; i < TileProgrammingTable_Neptune.WIDTH; ++i) {
                addSlot(new SlotDisplay(this::getDisplay, (j * TileProgrammingTable_Neptune.WIDTH) + i, 43 + 18 * i, 36 + 18 * j));
            }
        }
    }

    // @Override

    // @Override

    // @Override

    private ItemStack getDisplay(int index) {
        return (index < tile.optionRecipes.size() && tile.optionRecipes.get(index) != null)
                ? tile.optionRecipes.get(index).getOutput()
                : ItemStack.EMPTY;
    }
}
