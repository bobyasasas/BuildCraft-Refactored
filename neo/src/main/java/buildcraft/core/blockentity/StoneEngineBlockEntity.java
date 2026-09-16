/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import buildcraft.core.BcBlockEntities;
import buildcraft.core.block.StoneEngineBlock;

/**
 * Minimal coal-fired stone engine block entity for the M2.2b vertical slice of buildcraftcore. This is a deliberately
 * reduced stand-in for the legacy {@code TileEngineStone}: no heat, explosion or piston animation simulation (the real
 * engine registry migration is M2.4/M2.9, rendering is M2.2c/M2.7).
 *
 * <p>Slice model:
 * <ul>
 * <li>Fuel items are inserted programmatically via {@link #insertFuel(ItemStack, FuelValues)} (also reachable by
 * right-clicking the block with a fuel item, see {@code StoneEngineBlock#useItemOn}) and queued as a simple count
 * ({@link #pendingFuel}) instead of a real inventory.</li>
 * <li>While burning, the engine produces {@link #POWER_PER_TICK} into {@link #energyStored} every server tick;
 * production is clamped at {@link #CAPACITY} (burning fuel continues but overflow is discarded).</li>
 * <li>When idle and a queued fuel item exists and the buffer is not full, one fuel item ignites.</li>
 * <li>Consumers pull energy via {@link #extractEnergy(long, boolean)} (M2.2c pipes call this directly; the NeoForge
 * capability integration is deferred to M2.4/M2.5).</li>
 * </ul>
 *
 * <p>Units: {@link #energyStored} and {@link #POWER_PER_TICK} are micro-MJ (&micro;MJ, 1 MJ = 1_000_000 &micro;MJ, the
 * legacy BuildCraft internal unit), with slice-scaled numbers: 100 &micro;MJ/tick output (legacy stone engine is
 * 1 MJ/tick &mdash; the balancing is redone with the real engine module in M2.4/M2.9).
 *
 * <p>Only the energy state is replicated here; the block's {@code FACING} state (the energy output face) is block
 * state, not block entity data.
 *
 * <p><b>Client sync (M2.7b, for {@code StoneEngineBlockRenderer}):</b> the burn state is replicated through the
 * vanilla block entity update channel, exactly the way {@code BeaconBlockEntity} does it: {@link #getUpdatePacket()}
 * returns {@code ClientboundBlockEntityDataPacket.create(this)} (which packs {@link #getUpdateTag}), and
 * {@link #getUpdateTag} returns {@link #saveCustomOnly} &mdash; i.e. the update tag carries exactly the
 * {@link #saveAdditional} keys, and the client applies it through {@code loadAdditional(ValueInput)} (vanilla's
 * {@code ClientPacketListener#handleBlockEntityData} calls {@code loadWithComponents}). {@link #serverTick} calls
 * {@code ServerLevel#sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS)} on ignition, on burn-out and every
 * {@link #SYNC_INTERVAL} ticks while burning (the {@code ConduitBlockEntity} pattern) so the client can drive the
 * piston animation from {@code burnRemain}/{@code burnTotal}.
 */
public class StoneEngineBlockEntity extends BlockEntity {

    /** Constant power output while burning, in &micro;MJ per tick (slice value, see class javadoc). */
    public static final long POWER_PER_TICK = 100;
    /** Internal energy buffer size in &micro;MJ (slice value, see class javadoc). */
    public static final long CAPACITY = 100_000;
    /** While burning, the burn state is re-synced to clients every this many ticks (M2.7b render sync). */
    public static final int SYNC_INTERVAL = 40;

    /** Remaining burn ticks of the currently burning fuel item. */
    private int burnRemain;
    /** Total burn ticks of the currently burning fuel item. */
    private int burnTotal;
    /** Internal energy buffer, &micro;MJ (see class javadoc). */
    private long energyStored;
    /** Number of queued fuel items waiting to be ignited. */
    private int pendingFuel;
    /**
     * Burn ticks of the queued fuel items. Slice simplification: the queue stores one burn length for all pending
     * items, so mixing different fuel types makes the last inserted type win (a real fuel inventory arrives with the
     * M2.4 engine migration).
     */
    private int pendingBurnTicks;

    public StoneEngineBlockEntity(BlockPos pos, BlockState state) {
        super(BcBlockEntities.ENGINE_STONE.value(), pos, state);
    }

    /**
     * Queues one fuel item for burning, resolving its burn length through the vanilla fuel table. Returns false if the
     * stack is empty or has no fuel value; the caller owns stack mutation (shrink on success).
     */
    public boolean insertFuel(ItemStack stack, FuelValues fuelValues) {
        if (stack.isEmpty() || fuelValues == null) {
            return false;
        }
        // RecipeType.SMELTING mirrors the vanilla furnace burn-time query; coal resolves to 1600 ticks.
        int burnTicks = stack.getBurnTime(RecipeType.SMELTING, fuelValues);
        if (burnTicks <= 0) {
            return false;
        }
        this.pendingFuel++;
        this.pendingBurnTicks = burnTicks;
        setChanged();
        return true;
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
        } else if (engine.pendingFuel > 0 && engine.energyStored < CAPACITY) {
            // ignite one queued fuel item
            engine.pendingFuel--;
            engine.burnTotal = engine.pendingBurnTicks;
            engine.burnRemain = engine.pendingBurnTicks;
            changed = true;
            // ignition: the client must see burning = true
            syncToClients = true;
        }
        if (changed) {
            engine.setChanged();
        }
        if (syncToClients) {
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("bc_burn_remain", this.burnRemain);
        output.putInt("bc_burn_total", this.burnTotal);
        output.putLong("bc_energy_stored", this.energyStored);
        output.putInt("bc_pending_fuel", this.pendingFuel);
        output.putInt("bc_pending_burn_ticks", this.pendingBurnTicks);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.burnRemain = input.getIntOr("bc_burn_remain", 0);
        this.burnTotal = input.getIntOr("bc_burn_total", 0);
        this.energyStored = input.getLongOr("bc_energy_stored", 0L);
        this.pendingFuel = input.getIntOr("bc_pending_fuel", 0);
        this.pendingBurnTicks = input.getIntOr("bc_pending_burn_ticks", 0);
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
