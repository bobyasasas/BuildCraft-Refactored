/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.tile;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.mj.IMjContainerItem;
import buildcraft.api.tiles.IBCTileMenuProvider;
import buildcraft.api.tiles.IHasWork;
import buildcraft.lib.tile.item.ItemHandlerManager;
import buildcraft.lib.tile.item.ItemHandlerSimple;
import buildcraft.silicon.BCSiliconBlocks;
import buildcraft.silicon.BCSiliconMenuTypes;
import buildcraft.silicon.container.ContainerChargingTable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class TileChargingTable extends TileLaserTableBase implements IHasWork, IBCTileMenuProvider {
    public final ItemHandlerSimple inv = itemManager.addInvHandler(
            "inv",
            1,
            this::isItemValidForSlot,
            ItemHandlerManager.EnumAccess.BOTH,
            EnumPipePart.VALUES
    );

    public TileChargingTable(BlockPos pos, BlockState blockState) {
        super(BCSiliconBlocks.chargingTableTile.get(), pos, blockState);
    }

    @Override
    public long getTarget() {
        ItemStack stack = inv.getStackInSlot(0);
        if (!stack.isEmpty() && stack.getItem() instanceof IMjContainerItem) {
            IMjContainerItem containerItem = (IMjContainerItem) stack.getItem();
            return containerItem.getMaxPowerStored(stack) - containerItem.getPowerStored(stack);
        }

        return 0;
    }

    @Override
    public void update() {
        super.update();

        if (level.isClientSide) {
            return;
        }

        if (power > 0) {
            if (getTarget() > 0) {
                ItemStack stack = this.inv.getStackInSlot(0);
                IMjContainerItem containerItem = (IMjContainerItem) stack.getItem();
                power += (0 - containerItem.receivePower(stack, power, false));
                this.inv.setStackInSlot(0, stack);
            } else {
                power -= (Math.min(power, 10));
            }
        }
    }

    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && stack != null && !stack.isEmpty() && stack.getItem() instanceof IMjContainerItem;
    }

    // IHasWork

    @Override
    public boolean hasWork() {
        return getTarget() > 0;
    }

    // MenuProvider

    @Override
    public Component getDisplayName() {
        return this.getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerChargingTable(BCSiliconMenuTypes.CHARGING_TABLE, id, player, this);
    }
}
