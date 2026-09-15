/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 * <p/>
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.silicon.container;

import buildcraft.lib.gui.ContainerBCTile;
import buildcraft.lib.gui.slot.SlotValidated;
import buildcraft.silicon.tile.TileChargingTable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

public class ContainerChargingTable extends ContainerBCTile<TileChargingTable> {

    private TileChargingTable table;

    public ContainerChargingTable(MenuType menuType, int id, Player player, TileChargingTable tile) {
        super(menuType, id, player, tile);
        this.table = tile;

        addFullPlayerInventory(50);

        addSlot(new SlotValidated(table.inv, 0, 80, 18));


    }

//    @Override

//    @Override

//    @Override
}
