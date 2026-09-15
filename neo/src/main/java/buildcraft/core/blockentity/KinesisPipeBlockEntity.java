/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import buildcraft.core.BcBlockEntities;

/**
 * Minimal kinesis pipe block entity for the M2.2c vertical slice of buildcraftcore
 * ({@code buildcraftcore:pipe_kinesis_wood}, wooden kinesis pipe). Deliberately reduced stand-in for the legacy
 * {@code TileGenericPipe} + {@code PipeTransportPower}: no pipe network graph, no path finding, no pipe contents and no
 * wire/emitter logic &mdash; the real transport module migration is M2.4/M2.9 and the real pipe model/BER system is
 * M2.7.
 *
 * <p>Slice model (per-tick local diffusion, see class javadoc above for what is <em>not</em> simulated):
 * <ul>
 * <li>Every server tick the pipe first <em>pulls</em> energy into its small buffer ({@link #CAPACITY}) from its six
 * direct neighbours: a {@link StoneEngineBlockEntity} whose output facing points at this pipe, or an adjacent
 * {@code KinesisPipeBlockEntity} whose buffer is higher (pulled amount is half the difference, clamped to
 * {@link #RATE}). Pipes never push into other pipes &mdash; pipe-to-pipe equalisation is purely pull driven, so energy
 * can never loop back and forth on its own.</li>
 * <li>It then <em>pushes</em> energy out of its buffer into accepting neighbours. The only accepting block of this
 * slice is the {@link EnergyMeterBlockEntity} measurement block.</li>
 * <li>All transfers run through the plain public methods {@link #extractEnergy(long, boolean)} /
 * {@link #receiveEnergy(long, boolean)} (same shape as {@link StoneEngineBlockEntity#extractEnergy(long, boolean)}).
 * The NeoForge capability integration is deferred to M2.4/M2.5 on purpose.</li>
 * </ul>
 *
 * <p>Units: micro-MJ (&micro;MJ, 1 MJ = 1_000_000 &micro;MJ, the legacy BuildCraft internal unit), slice-scaled
 * numbers ({@link #RATE} &micro;MJ/tick per connection &mdash; the legacy wooden pipe moves 1 MJ/tick, rebalancing
 * happens with the real transport module in M2.4/M2.9).
 */
public class KinesisPipeBlockEntity extends BlockEntity {

    /** Internal energy buffer size in &micro;MJ (slice value, see class javadoc). */
    public static final long CAPACITY = 10_000;
    /** Maximum energy moved per neighbour connection and tick, in &micro;MJ (slice value, see class javadoc). */
    public static final long RATE = 1_000;

    /** Internal energy buffer, &micro;MJ (see class javadoc). */
    private long energyStored;

    public KinesisPipeBlockEntity(BlockPos pos, BlockState state) {
        super(BcBlockEntities.PIPE_KINESIS_WOOD.value(), pos, state);
    }

    public long getEnergyStored() {
        return this.energyStored;
    }

    /**
     * Energy input interface of the slice: accepts up to the free buffer space, returns the accepted part of
     * {@code amount}. Direct method call on purpose (see class javadoc).
     */
    public long receiveEnergy(long amount, boolean simulate) {
        long accepted = Math.min(amount, CAPACITY - this.energyStored);
        if (accepted <= 0) {
            return 0;
        }
        if (!simulate) {
            this.energyStored += accepted;
            setChanged();
        }
        return accepted;
    }

    /**
     * Energy output interface of the slice (same contract as
     * {@link StoneEngineBlockEntity#extractEnergy(long, boolean)}): pulls up to {@code max} &micro;MJ out of the
     * buffer, deducting it unless {@code simulate} is true.
     */
    public long extractEnergy(long max, boolean simulate) {
        long extracted = Math.min(max, this.energyStored);
        if (extracted > 0 && !simulate) {
            this.energyStored -= extracted;
            setChanged();
        }
        return extracted;
    }

    /**
     * Per-tick diffusion logic, wired through {@code KinesisPipeBlock#getTicker} with the vanilla furnace static-tick
     * pattern ({@code StoneEngineBlockEntity#serverTick}). No neighbour-state caching, no graph traversal: every tick
     * rescans the six direct neighbours (slice simplification, the cached pipe network arrives with the M2.4/M2.9
     * transport module).
     */
    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, KinesisPipeBlockEntity pipe) {
        pipe.pullFromNeighbours(level, pos);
        pipe.pushToNeighbours(level, pos);
    }

    /** Pull phase: engines pointing at this pipe, then higher adjacent pipes (half-difference equalisation). */
    private void pullFromNeighbours(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (this.energyStored >= CAPACITY) {
                return;
            }
            BlockEntity neighbour = level.getBlockEntity(pos.relative(direction));
            if (neighbour instanceof StoneEngineBlockEntity engine
                    && engine.getOutputFacing() == direction.getOpposite()) {
                long pulled = engine.extractEnergy(Math.min(RATE, CAPACITY - this.energyStored), false);
                if (pulled > 0) {
                    this.energyStored += pulled;
                    setChanged();
                }
            } else if (neighbour instanceof KinesisPipeBlockEntity other && other.getEnergyStored() > this.energyStored) {
                long halfDifference = (other.getEnergyStored() - this.energyStored) / 2;
                long wanted = Math.min(RATE, Math.min(halfDifference, CAPACITY - this.energyStored));
                long pulled = other.extractEnergy(wanted, false);
                if (pulled > 0) {
                    this.energyStored += pulled;
                    setChanged();
                }
            }
        }
    }

    /** Push phase: only blocks that actively accept energy. This slice accepts through {@link EnergyMeterBlockEntity}. */
    private void pushToNeighbours(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (this.energyStored <= 0) {
                return;
            }
            BlockEntity neighbour = level.getBlockEntity(pos.relative(direction));
            if (neighbour instanceof EnergyMeterBlockEntity meter) {
                long accepted = meter.receiveEnergy(Math.min(RATE, this.energyStored), false);
                if (accepted > 0) {
                    this.energyStored -= accepted;
                    setChanged();
                }
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("bc_energy_stored", this.energyStored);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energyStored = input.getLongOr("bc_energy_stored", 0L);
    }
}
