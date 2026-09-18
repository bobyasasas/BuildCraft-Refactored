/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders.menu;

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
import buildcraft.builders.BcBuildersMenus;
import buildcraft.builders.blockentity.FillerBlockEntity;
import buildcraft.builders.blockentity.FillerBlockEntity.Pattern;
import buildcraft.lib.gui.menu.BcBlockEntityMenu;

/**
 * The filler menu (task M4.8, 1.20.1 counterpart {@code ContainerFiller}, minimal capability surface): the 27 resource
 * slots of the legacy {@code invResources} at the baseline layout ({@code filler.json}: slot cells from (7, 84), i.e.
 * item positions (8, 85), 9 per row), the full player inventory (row block at y = 153, hotbar at y = 211) and the
 * three {@link ContainerData} slots the screen reads (stored energy, selected pattern ordinal, finished flag).
 *
 * <p>The legacy statement parameter slots, invert/excavate buttons and progress ledger are not part of this surface:
 * the slice filler has exactly one pattern and no gate-driven lock (see {@link FillerBlockEntity}); they migrate with
 * the full builders module.
 *
 * <p>Server side is built by {@link FillerBlockEntity#createMenu}; client side by
 * {@link #clientCreate(int, Inventory, RegistryFriendlyByteBuf)}, which re-resolves the block entity from the menu
 * position written by {@code player.openMenu(MenuProvider, BlockPos)}. If the client cannot (yet) resolve the block
 * entity, the resource slots bind to a throw-away {@link SimpleContainer} so the screen still renders instead of
 * crashing (the server half is authoritative and re-syncs as soon as the menu ticks).
 */
public class FillerMenu extends BcBlockEntityMenu<FillerBlockEntity> {

    /** {@link ContainerData} slot ids: the values the {@code FillerScreen} reads. */
    public static final int DATA_ENERGY_STORED = 0;
    /** -1 when no pattern is selected, otherwise the {@link FillerBlockEntity.Pattern} ordinal. */
    public static final int DATA_PATTERN = 1;
    public static final int DATA_FINISHED = 2;
    /** Number of {@link ContainerData} slots this menu syncs from the server to the client half. */
    public static final int DATA_COUNT = 3;

    /** Slot indices: 0..26 are the resource slots, then the player inventory block (27..53 main, 54..62 hotbar). */
    public static final int RESOURCES_START = 0;
    public static final int RESOURCES_END = 27;
    public static final int INVENTORY_START = 27;
    public static final int INVENTORY_END = 63;
    private static final int HOTBAR_START = 54;

    private final Container resourcesContainer;
    /** Null on a defunct client menu whose block entity went away (see the class javadoc). */
    private final @Nullable FillerBlockEntity filler;
    /** The synced filler state (server: live view onto the block entity; client: vanilla-synced copy). */
    private final ContainerData data;

    public FillerMenu(int id, Inventory playerInventory, @Nullable FillerBlockEntity filler, BlockPos pos,
            ContainerData data) {
        super(BcBuildersMenus.FILLER.value(), id, filler, pos);
        this.filler = filler;
        this.data = data;
        this.resourcesContainer = filler != null ? filler : new SimpleContainer(RESOURCES_END);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new ResourceSlot(row * 9 + col, 8 + col * 18, 85 + row * 18));
            }
        }
        this.addStandardInventorySlots(playerInventory, 8, 153);
        this.addDataSlots(data);
    }

    /** Server-side constructor, called from {@link FillerBlockEntity#createMenu}. */
    public FillerMenu(int id, Inventory playerInventory, FillerBlockEntity filler, ContainerData data) {
        this(id, playerInventory, filler, filler.getBlockPos(), data);
    }

    /** Client-side factory for {@link BcBuildersMenus#FILLER}: re-resolves the filler from the menu position. */
    public static FillerMenu clientCreate(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        FillerBlockEntity filler = be instanceof FillerBlockEntity realFiller ? realFiller : null;
        return new FillerMenu(id, playerInventory, filler, pos, new SimpleContainerData(DATA_COUNT));
    }

    @Override
    public boolean stillValid(Player player) {
        return this.stillValidBlockEntity(player);
    }

    /** Stored energy in &micro;J; the screen's stored-energy line. */
    public long getEnergyStored() {
        return this.data.get(DATA_ENERGY_STORED);
    }

    /** The selected pattern from the synced state, or null while none is selected. */
    public @Nullable Pattern getPattern() {
        int ordinal = this.data.get(DATA_PATTERN);
        return ordinal < 0 ? null : Pattern.values()[ordinal];
    }

    /** True once the work box matches the pattern; the screen may use it for a done indicator. */
    public boolean isFinished() {
        return this.data.get(DATA_FINISHED) != 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            if (slotIndex < RESOURCES_END) {
                if (!this.moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, RESOURCES_START, RESOURCES_END, false)) {
                return ItemStack.EMPTY;
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

    /** A resource slot: accepts only placeable block items (the legacy {@code invResources} slot predicate). */
    private final class ResourceSlot extends Slot {

        /** The resource-inventory slot this GUI slot stands for (equal to the menu slot index here). */
        private final int containerSlot;

        private ResourceSlot(int containerSlot, int x, int y) {
            super(FillerMenu.this.resourcesContainer, containerSlot, x, y);
            this.containerSlot = containerSlot;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            FillerBlockEntity target = FillerMenu.this.filler;
            return target != null && target.canPlaceItem(this.containerSlot, stack);
        }
    }
}
