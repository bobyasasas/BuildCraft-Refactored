/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
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
 */
public class StoneEngineBlockEntity extends BlockEntity {

    /** Constant power output while burning, in &micro;MJ per tick (slice value, see class javadoc). */
    public static final long POWER_PER_TICK = 100;
    /** Internal energy buffer size in &micro;MJ (slice value, see class javadoc). */
    public static final long CAPACITY = 100_000;

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

    /** Facing of the block this engine sits in = the energy output face (slice contract for M2.2c pipes). */
    public Direction getOutputFacing() {
        return this.getBlockState().getValue(StoneEngineBlock.FACING);
    }

    /**
     * Per-tick production logic, wired through {@code StoneEngineBlock#getTicker}. Kept in a static method mirroring the
     * vanilla furnace pattern ({@code AbstractFurnaceBlockEntity.serverTick}).
     */
    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, StoneEngineBlockEntity engine) {
        boolean changed = false;
        if (engine.burnRemain > 0) {
            engine.burnRemain--;
            if (engine.energyStored < CAPACITY) {
                engine.energyStored = Math.min(CAPACITY, engine.energyStored + POWER_PER_TICK);
            }
            changed = true;
        } else if (engine.pendingFuel > 0 && engine.energyStored < CAPACITY) {
            // ignite one queued fuel item
            engine.pendingFuel--;
            engine.burnTotal = engine.pendingBurnTicks;
            engine.burnRemain = engine.pendingBurnTicks;
            changed = true;
        }
        if (changed) {
            engine.setChanged();
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
}
