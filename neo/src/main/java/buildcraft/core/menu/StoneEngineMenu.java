/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import buildcraft.core.BcMenus;
import buildcraft.core.blockentity.StoneEngineBlockEntity;
import buildcraft.lib.gui.menu.BcBlockEntityMenu;

/**
 * The stone engine menu (task M4.8, 1.20.1 counterpart {@code ContainerEngineStone_BC8}): one restricted fuel slot at
 * the baseline position (80, 41), the full player inventory (rows at y = 84, hotbar at y = 142 &mdash; the baseline
 * {@code addFullPlayerInventory(84)} layout, which vanilla's {@link #addStandardInventorySlots} reproduces) and the
 * three {@link ContainerData} slots the screen's burn flame and energy line read (burn remaining, burn total, stored
 * energy).
 *
 * <p>Server side is built by {@link StoneEngineBlockEntity#createMenu}; client side by
 * {@link #clientCreate(int, Inventory, RegistryFriendlyByteBuf)}, which re-resolves the block entity from the menu
 * position written by {@code player.openMenu(MenuProvider, BlockPos)}. If the client cannot (yet) resolve the block
 * entity, the fuel slot binds to a throw-away {@link SimpleContainer} so the screen still renders an empty slot instead
 * of crashing (the server half is authoritative and re-syncs as soon as the menu ticks).
 */
public class StoneEngineMenu extends BcBlockEntityMenu<StoneEngineBlockEntity> {

    /** {@link ContainerData} slot ids: the values the {@code StoneEngineScreen} indicators are derived from. */
    public static final int DATA_BURN_REMAIN = 0;
    public static final int DATA_BURN_TOTAL = 1;
    public static final int DATA_ENERGY_STORED = 2;
    /** Number of {@link ContainerData} slots this menu syncs from the server to the client half. */
    public static final int DATA_COUNT = 3;

    /** Slot indices: 0 is the fuel slot, then the vanilla-standard player inventory block (1..27 main, 28..36 hotbar). */
    public static final int FUEL_SLOT = 0;
    private static final int INVENTORY_START = 1;
    private static final int INVENTORY_END = 37;
    private static final int HOTBAR_START = 28;

    private final Container fuelContainer;
    /** Null on a defunct client menu whose block entity went away (see the class javadoc). */
    private final @Nullable StoneEngineBlockEntity engine;
    /** The synced engine state (server: live view onto the block entity; client: vanilla-synced copy). */
    private final ContainerData data;

    public StoneEngineMenu(int id, Inventory playerInventory, @Nullable StoneEngineBlockEntity engine, BlockPos pos,
            ContainerData data) {
        super(BcMenus.ENGINE_STONE.value(), id, engine, pos);
        this.engine = engine;
        this.data = data;
        this.fuelContainer = engine != null ? engine : new SimpleContainer(1);
        // The fuel slot sits where the 1.20.1 baseline put it: the texture's slot hole at (79..95, 40..56).
        this.addSlot(new FuelSlot(0, 80, 41));
        this.addStandardInventorySlots(playerInventory, 8, 84);
        this.addDataSlots(data);
    }

    /** Server-side constructor, called from {@link StoneEngineBlockEntity#createMenu}. */
    public StoneEngineMenu(int id, Inventory playerInventory, StoneEngineBlockEntity engine, ContainerData data) {
        this(id, playerInventory, engine, engine.getBlockPos(), data);
    }

    /** Client-side factory for {@link BcMenus#ENGINE_STONE}: re-resolves the engine from the menu position. */
    public static StoneEngineMenu clientCreate(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        StoneEngineBlockEntity engine = be instanceof StoneEngineBlockEntity stoneEngine ? stoneEngine : null;
        return new StoneEngineMenu(id, playerInventory, engine, pos, new SimpleContainerData(DATA_COUNT));
    }

    @Override
    public boolean stillValid(Player player) {
        return this.stillValidBlockEntity(player);
    }

    /** Remaining burn ticks of the current fuel item (0 while idle); drives the screen's flame height. */
    public int getBurnRemain() {
        return this.data.get(DATA_BURN_REMAIN);
    }

    /** Total burn ticks of the current fuel item (0 while idle); the flame's 100% reference. */
    public int getBurnTotal() {
        return this.data.get(DATA_BURN_TOTAL);
    }

    /** Stored energy in &micro;J; the screen's stored-energy line. */
    public long getEnergyStored() {
        return this.data.get(DATA_ENERGY_STORED);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            if (slotIndex == FUEL_SLOT) {
                if (!this.moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                StoneEngineBlockEntity target = this.engine;
                if (target != null && target.canPlaceItem(FUEL_SLOT, stack)) {
                    if (!this.moveItemStackTo(stack, FUEL_SLOT, INVENTORY_START, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotIndex < HOTBAR_START) {
                    if (!this.moveItemStackTo(stack, HOTBAR_START, INVENTORY_END, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(stack, INVENTORY_START, HOTBAR_START, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == clicked.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return clicked;
    }

    /** The fuel slot: accepts only items with a vanilla fuel value (the legacy {@code FurnaceFuelSlot} pattern). */
    private final class FuelSlot extends Slot {

        private FuelSlot(int index, int x, int y) {
            super(StoneEngineMenu.this.fuelContainer, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            StoneEngineBlockEntity target = StoneEngineMenu.this.engine;
            return target != null && target.canPlaceItem(FUEL_SLOT, stack);
        }
    }
}
