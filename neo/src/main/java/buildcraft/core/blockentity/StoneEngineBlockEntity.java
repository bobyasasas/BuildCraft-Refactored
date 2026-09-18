/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import buildcraft.core.BcBlockEntities;
import buildcraft.core.block.StoneEngineBlock;
import buildcraft.core.menu.StoneEngineMenu;

/**
 * Minimal coal-fired stone engine block entity for the M2.2b vertical slice of buildcraftcore. This is a deliberately
 * reduced stand-in for the legacy {@code TileEngineStone}: no heat, explosion or piston animation simulation (the real
 * engine registry migration is M2.4/M2.9, rendering is M2.2c/M2.7).
 *
 * <p>Slice model:
 * <ul>
 * <li>The engine holds a single fuel slot (the legacy {@code invFuel}, now a real {@link Container} so the M4.8 GUI can
 * take items in and out of it). Fuel items are inserted programmatically via {@link #insertFuel(ItemStack, FuelValues)}
 * (also reachable by right-clicking the block with a fuel item, see {@code StoneEngineBlock#useItemOn}) or through the
 * stone engine menu's fuel slot.</li>
 * <li>While burning, the engine produces {@link #POWER_PER_TICK} into {@link #energyStored} every server tick;
 * production is clamped at {@link #CAPACITY} (burning fuel continues but overflow is discarded).</li>
 * <li>When idle, a fuel item in the slot and the buffer not full, that item ignites (one item is consumed from the
 * slot).</li>
 * <li>Consumers pull energy via {@link #extractEnergy(long, boolean)} (M2.2c pipes call this directly; the NeoForge
 * capability integration is deferred to M2.4/M2.5).</li>
 * </ul>
 *
 * <p>Units: {@link #energyStored} and {@link #POWER_PER_TICK} are micro-MJ (&micro;MJ, 1 MJ = 1_000_000 &micro;MJ, the
 * legacy BuildCraft internal unit), with slice-scaled numbers: 100 &micro;MJ/tick output (legacy stone engine is
 * 1 MJ/tick &mdash; the balancing is redone with the real engine module in M2.4/M2.9).
 *
 * <p><b>GUI (M4.8):</b> this block entity is a {@link MenuProvider}: {@code StoneEngineBlock#useWithoutItem} calls
 * {@code player.openMenu(this, pos)}, which builds the server-side {@link StoneEngineMenu} through {@link #createMenu}
 * and pushes the position to the client. The GUI-visible state (burn remaining/total, stored energy) reaches the open
 * menu through {@link #guiData}, the vanilla {@code ContainerData} sync channel.
 *
 * <p><b>Client sync (M2.7b, for {@code StoneEngineBlockRenderer}):</b> the burn state is replicated through the
 * vanilla block entity update channel, exactly the way {@code BeaconBlockEntity} does it: {@link #getUpdatePacket()}
 * returns {@code ClientboundBlockEntityDataPacket.create(this)} (which packs {@link #getUpdateTag}), and
 * {@link #getUpdateTag} returns {@link #saveCustomOnly} &mdash; i.e. the update tag carries exactly the
 * {@link #saveAdditional} keys (the fuel slot included, through the vanilla {@code Items} list), and the client applies
 * it through {@code loadAdditional(ValueInput)}. {@link #serverTick} calls
 * {@code ServerLevel#sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS)} on ignition, on burn-out and every
 * {@link #SYNC_INTERVAL} ticks while burning (the {@code ConduitBlockEntity} pattern) so the client can drive the
 * piston animation from {@code burnRemain}/{@code burnTotal}.
 */
public class StoneEngineBlockEntity extends BlockEntity implements Container, MenuProvider {

    /** Constant power output while burning, in &micro;MJ per tick (slice value, see class javadoc). */
    public static final long POWER_PER_TICK = 100;
    /** Internal energy buffer size in &micro;MJ (slice value, see class javadoc). */
    public static final long CAPACITY = 100_000;
    /** While burning, the burn state is re-synced to clients every this many ticks (M2.7b render sync). */
    public static final int SYNC_INTERVAL = 40;
    /** The one fuel slot (the legacy {@code invFuel}; see the slice model in the class javadoc). */
    public static final int FUEL_SLOTS = 1;

    /** Remaining burn ticks of the currently burning fuel item. */
    private int burnRemain;
    /** Total burn ticks of the currently burning fuel item. */
    private int burnTotal;
    /** Internal energy buffer, &micro;MJ (see class javadoc). */
    private long energyStored;
    /** The fuel slot contents (vanilla {@code Items} list persistence through {@link ContainerHelper}). */
    private final NonNullList<ItemStack> fuelItems = NonNullList.withSize(FUEL_SLOTS, ItemStack.EMPTY);

    /**
     * The GUI-visible state of this engine, synced to the open {@link StoneEngineMenu} through the vanilla
     * {@code ContainerData} channel (server side reads the live fields; the client half gets a zero-initialised
     * {@code SimpleContainerData} that vanilla keeps in sync).
     */
    private final ContainerData guiData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case StoneEngineMenu.DATA_BURN_REMAIN -> StoneEngineBlockEntity.this.burnRemain;
                case StoneEngineMenu.DATA_BURN_TOTAL -> StoneEngineBlockEntity.this.burnTotal;
                case StoneEngineMenu.DATA_ENERGY_STORED -> (int) StoneEngineBlockEntity.this.energyStored;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Server-authoritative: the client never writes back (vanilla AbstractFurnaceBlockEntity pattern).
        }

        @Override
        public int getCount() {
            return StoneEngineMenu.DATA_COUNT;
        }
    };

    public StoneEngineBlockEntity(BlockPos pos, BlockState state) {
        super(BcBlockEntities.ENGINE_STONE.value(), pos, state);
    }

    /**
     * Inserts one item of {@code stack} into the fuel slot (resolving its burn length through the vanilla fuel table).
     * Returns false if the stack is empty, has no fuel value, or the slot already holds a different item; the caller
     * owns stack mutation (shrink one on success).
     */
    public boolean insertFuel(ItemStack stack, FuelValues fuelValues) {
        if (stack.isEmpty() || fuelValues == null || !isFuel(stack, fuelValues)) {
            return false;
        }
        ItemStack current = this.fuelItems.get(0);
        if (current.isEmpty()) {
            this.fuelItems.set(0, stack.copyWithCount(1));
            this.setChanged();
            return true;
        }
        // Same fuel type: top the slot up (the slot is capped at the stack's own max size, 64 for coal).
        if (ItemStack.isSameItemSameComponents(current, stack) && current.getCount() < current.getMaxStackSize()) {
            current.grow(1);
            this.setChanged();
            return true;
        }
        return false;
    }

    /** True when the vanilla fuel table assigns {@code stack} a positive burn length (the fuel slot's predicate). */
    public boolean isFuel(ItemStack stack) {
        return isFuel(stack, this.level != null ? this.level.fuelValues() : null);
    }

    private static boolean isFuel(ItemStack stack, @Nullable FuelValues fuelValues) {
        return fuelValues != null && stack.getBurnTime(RecipeType.SMELTING, fuelValues) > 0;
    }

    /**
     * Energy output interface for the M2.2c pipe slice: pulls up to {@code max} &micro;MJ out of the buffer, deducting
     * it unless {@code simulate} is true. Direct method call on purpose (see class javadoc).
     */
    public long extractEnergy(long max, boolean simulate) {
        long extracted = Math.min(max, this.energyStored);
        if (extracted > 0 && !simulate) {
            this.energyStored -= extracted;
            setChanged();
        }
        return extracted;
    }

    public long getEnergyStored() {
        return this.energyStored;
    }

    public int getBurnRemain() {
        return this.burnRemain;
    }

    /** Total burn ticks of the current fuel item (0 when idle); used by the M2.7b renderer's burn progress. */
    public int getBurnTotal() {
        return this.burnTotal;
    }

    /** True while a fuel item is burning (the client-visible "engine is running" flag for the renderer). */
    public boolean isBurning() {
        return this.burnRemain > 0;
    }

    /** Facing of the block this engine sits in = the energy output face (slice contract for M2.2c pipes). */
    public Direction getOutputFacing() {
        return this.getBlockState().getValue(StoneEngineBlock.FACING);
    }

    /**
     * Per-tick production logic, wired through {@code StoneEngineBlock#getTicker}. Kept in a static method mirroring
     * the vanilla furnace pattern ({@code AbstractFurnaceBlockEntity.serverTick}). Burn state changes are pushed to
     * clients through {@code sendBlockUpdated} (see the client sync note in the class javadoc).
     */
    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, StoneEngineBlockEntity engine) {
        boolean changed = false;
        boolean syncToClients = false;
        if (engine.burnRemain > 0) {
            engine.burnRemain--;
            if (engine.energyStored < CAPACITY) {
                engine.energyStored = Math.min(CAPACITY, engine.energyStored + POWER_PER_TICK);
            }
            changed = true;
            // Re-sync periodically while burning so clients keep animating the piston even after a
            // missed/out-of-order update; the remainder is what the renderer's progress is derived from.
            syncToClients = engine.burnRemain % SYNC_INTERVAL == 0;
            if (engine.burnRemain == 0) {
                // burn-out: the client must see burning = false
                syncToClients = true;
            }
        } else if (engine.energyStored < CAPACITY && !engine.fuelItems.get(0).isEmpty()) {
            // ignite the fuel slot's next item (the slot's canPlaceItem guarantees it actually has a burn value)
            int burnTicks = engine.fuelItems.get(0).getBurnTime(RecipeType.SMELTING, level.fuelValues());
            if (burnTicks > 0) {
                engine.removeItem(0, 1);
                engine.burnTotal = burnTicks;
                engine.burnRemain = burnTicks;
                changed = true;
                // ignition: the client must see burning = true
                syncToClients = true;
            }
        }
        if (changed) {
            engine.setChanged();
        }
        if (syncToClients) {
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    // ---------------------------------------------------------------------
    // GUI wiring (M4.8): the block entity is its own MenuProvider
    // ---------------------------------------------------------------------

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new StoneEngineMenu(containerId, playerInventory, this, this.guiData);
    }

    @Override
    public Component getDisplayName() {
        // The block's overridden description id = the frozen legacy key ("tile.engineStone.name"), see BcLangKeys.
        return this.getBlockState().getBlock().getName();
    }

    // ---------------------------------------------------------------------
    // Container (the single fuel slot, M4.8)
    // ---------------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return FUEL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return this.fuelItems.get(0).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? this.fuelItems.get(0) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack result = ContainerHelper.removeItem(this.fuelItems, slot, count);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.fuelItems, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0) {
            this.fuelItems.set(0, stack);
            stack.limitSize(this.getMaxStackSize(stack));
            setChanged();
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        // The fuel slot accepts only items with a vanilla fuel value (the legacy FurnaceFuelSlot predicate).
        return slot == 0 && isFuel(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.fuelItems.clear();
    }

    // ---------------------------------------------------------------------
    // Persistence + client sync (Beacon pattern, see class javadoc)
    // ---------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("bc_burn_remain", this.burnRemain);
        output.putInt("bc_burn_total", this.burnTotal);
        output.putLong("bc_energy_stored", this.energyStored);
        ContainerHelper.saveAllItems(output, this.fuelItems, false);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.burnRemain = input.getIntOr("bc_burn_remain", 0);
        this.burnTotal = input.getIntOr("bc_burn_total", 0);
        this.energyStored = input.getLongOr("bc_energy_stored", 0L);
        ContainerHelper.loadAllItems(input, this.fuelItems);
    }

    /**
     * M2.7b client sync, vanilla {@code BeaconBlockEntity} pattern: the update tag is {@link #saveCustomOnly}, i.e.
     * exactly the {@link #saveAdditional} keys (burn state included). The client applies the packet through
     * {@code loadWithComponents} → {@link #loadAdditional}, so no separate wire format is needed.
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
