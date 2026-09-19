/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.silicon.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * M4.16 shared base of the laser-driven tables (legacy counterpart:
 * {@code buildcraft.silicon.tile.TileLaserTableBase}): the machines accumulate the laser's energy in an internal
 * buffer ({@link #power}) while a work target exists ({@link #currentTarget()}), drain it to zero when the target
 * disappears (the legacy {@code update} rule) and run one craft through {@link #craft} once the buffer covers the
 * target. The targeting/draining/crafting economics are the base's; the recipes and inventories are the subclasses'.
 *
 * <p>v1 trims (GUIs are the second batch, M4.16 runs the machines headless): no menu/provider plumbing, no GUI-tick
 * payload ({@code NET_GUI_TICK}/{@code NET_GUI_DATA}) &mdash; the {@link #SYNC_INTERVAL} Beacon-pattern update tag
 * carries the power level for a future renderer instead.
 */
public abstract class LaserTableBaseBlockEntity extends BlockEntity implements LaserTarget {

    /** Server-side sync cadence while the table is charging/crafting (the M4.7 machine pattern). */
    public static final int SYNC_INTERVAL = 20;

    /** Accumulated laser energy, &micro;MJ (legacy {@code TileLaserTableBase#power}; unbounded like the baseline). */
    protected long power;
    /** Ticks since the last client sync while active. */
    private int syncTimer;

    protected LaserTableBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        // the ids keep their legacy binding (assembly/integration/charging/advanced_crafting tables), the concrete
        // classes pass their own registration holder
        super(type, pos, state);
    }

    public long getPower() {
        return this.power;
    }

    /** The work target of this tick, in &micro;MJ; 0 means no work (the base then drains {@link #power}). */
    protected abstract long currentTarget();

    /** One craft, run when {@link #power} covers {@link #currentTarget()} (which must be positive). */
    protected abstract void craft(ServerLevel level);

    // ---------------------------------------------------------------------
    // LaserTarget (legacy TileLaserTableBase semantics)
    // ---------------------------------------------------------------------

    @Override
    public long getRequiredLaserPower() {
        return Math.max(0, this.currentTarget() - this.power);
    }

    @Override
    public long receiveLaserPower(long microJoules) {
        long received = Math.min(microJoules, this.getRequiredLaserPower());
        if (received > 0) {
            this.power += received;
        }
        return microJoules - received;
    }

    @Override
    public boolean isInvalidTarget() {
        return this.isRemoved();
    }

    // ---------------------------------------------------------------------
    // Server tick (the subclasses' static serverTick calls refresh() then this)
    // ---------------------------------------------------------------------

    /** The shared per-tick power economics: drain on no-target, craft on full, periodic sync while active. */
    protected final void tickPower(ServerLevel level) {
        long target = this.currentTarget();
        if (target <= 0) {
            if (this.power != 0) {
                this.power = 0;
                this.syncToClients();
            }
            return;
        }
        if (this.power >= target) {
            this.craft(level);
        }
        if (++this.syncTimer >= SYNC_INTERVAL) {
            this.syncToClients();
        }
    }

    // ---------------------------------------------------------------------
    // Persistence + client sync (Beacon pattern)
    // ---------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("bc_power", this.power);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.power = input.getLongOr("bc_power", 0L);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Marks the state dirty and pushes it to clients (Beacon-pattern update tag, see class javadoc). */
    protected final void syncToClients() {
        this.setChanged();
        this.syncTimer = 0;
        if (this.level instanceof ServerLevel serverLevel) {
            BlockState state = this.getBlockState();
            serverLevel.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }
}
