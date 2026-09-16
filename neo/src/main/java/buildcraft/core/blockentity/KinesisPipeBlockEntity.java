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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import buildcraft.core.BcBlockEntities;
import buildcraft.core.gate.BcGateLogic;
import buildcraft.lib.datacomponent.gate.BcGateConfig;
import buildcraft.lib.datacomponent.gate.GateVariantData;

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
 *
 * <p><b>M2.11 gate slot:</b> the slice is also the host of the minimal gate ({@link BcGateLogic}, the port of legacy
 * {@code buildcraft.silicon.gate.GateLogic}). Legacy gates are pipe pluggables &mdash; one per pipe face &mdash; on a
 * real {@code TilePipeHolder}; that system has not migrated, so the slice carries a single gate on a single face
 * ({@link #attachGate(Direction, GateVariantData)}, called by the gate item, tests and the evidence rig). The gate is
 * evaluated every server tick after the diffusion phases; a state change pushes the usual update-tag sync and the red
 * wire broadcast / latched redstone output are observable through {@link #getGate()}.
 *
 * <p><b>Client sync (update tag, vanilla {@code BeaconBlockEntity} pattern, first used by
 * {@link StoneEngineBlockEntity} in M2.7b):</b> {@link #getUpdatePacket()} returns
 * {@code ClientboundBlockEntityDataPacket.create(this)} and {@link #getUpdateTag} returns {@link #saveCustomOnly},
 * i.e. the update tag carries exactly the {@link #saveAdditional} keys (energy and gate state alike), and the client
 * applies it through {@code loadWithComponents} &rarr; {@link #loadAdditional}.
 */
public class KinesisPipeBlockEntity extends BlockEntity {

    /** Internal energy buffer size in &micro;MJ (slice value, see class javadoc). */
    public static final long CAPACITY = 10_000;
    /** Maximum energy moved per neighbour connection and tick, in &micro;MJ (slice value, see class javadoc). */
    public static final long RATE = 1_000;

    /** Internal energy buffer, &micro;MJ (see class javadoc). */
    private long energyStored;

    /** The attached gate, or null when this pipe carries none (M2.11 slice: at most one gate on one face). */
    @Nullable
    private BcGateLogic gate;
    /** The face the gate is attached to (legacy {@code PipePluggable#side}); only meaningful when {@link #gate} != null. */
    @Nullable
    private Direction gateSide;

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

    // ---------------------------------------------------------------------
    // M2.11 gate
    // ---------------------------------------------------------------------

    /** The attached gate, or null when this pipe carries none. */
    @Nullable
    public BcGateLogic getGate() {
        return this.gate;
    }

    /** The face the gate occupies (legacy {@code PipePluggable#side}); null when no gate is attached. */
    @Nullable
    public Direction getGateSide() {
        return this.gateSide;
    }

    /**
     * Attaches a fresh, unconfigured gate of the given variant to the given face (legacy:
     * {@code ItemPluggableGate#onPlace} creating a {@code PluggableGate} on the clicked pipe face). Re-attaching
     * replaces any previous gate &mdash; legacy has one pluggable slot per face and refuses occupied ones, but the
     * slice pipe carries at most one gate, so replacement is the closest total-occupation analogue.
     */
    public boolean attachGate(Direction side, GateVariantData variant) {
        if (this.level != null && this.level.isClientSide()) {
            return false;
        }
        this.gate = new BcGateLogic(variant);
        this.gateSide = side;
        this.pushGateToClients();
        return true;
    }

    /** The gate's redstone output emitted towards {@code face}, or 0 (consumed by {@code KinesisPipeBlock}). */
    public int getGateRedstoneOutput(Direction face) {
        if (this.gate != null && this.gateSide == face) {
            return this.gate.getRedstoneOutput();
        }
        return 0;
    }

    /** Marks the gate state dirty and pushes it to clients (Beacon-pattern update tag, see class javadoc). */
    private void pushGateToClients() {
        this.setChanged();
        if (this.level instanceof ServerLevel serverLevel) {
            BlockState state = this.getBlockState();
            serverLevel.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Per-tick diffusion logic, wired through {@code KinesisPipeBlock#getTicker} with the vanilla furnace static-tick
     * pattern ({@code StoneEngineBlockEntity#serverTick}). No neighbour-state caching, no graph traversal: every tick
     * rescans the six direct neighbours (slice simplification, the cached pipe network arrives with the M2.4/M2.9
     * transport module). The M2.11 gate evaluation runs after the energy phases.
     */
    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, KinesisPipeBlockEntity pipe) {
        pipe.pullFromNeighbours(level, pos);
        pipe.pushToNeighbours(level, pos);
        if (pipe.gate != null && pipe.gateSide != null) {
            int previousRedstoneOutput = pipe.gate.getRedstoneOutput();
            if (pipe.gate.tick(level, pos, pipe.gateSide)) {
                pipe.pushGateToClients();
            }
            if (previousRedstoneOutput != pipe.gate.getRedstoneOutput()) {
                // the pipe's weak-signal emission towards the gate face changed: wake the neighbours (vanilla lamp,
                // redstone wire etc. re-evaluate on neighbour updates, exactly like legacy BlockPipeHolder's
                // setBlock/updateNeighboursForOutputSignal paths)
                level.updateNeighborsAt(pos, state.getBlock(), null);
            }
        }
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
        // M2.11 gate: the configuration rides the M2.6 component codec (the exact legacy gate_data compound shape, so
        // /data merge block can author it in the evidence rig); the runtime state gets dedicated flat keys because
        // BcGateConfig deliberately matches the legacy copier payload, which strips the runtime state.
        if (this.gate != null && this.gateSide != null) {
            output.store("bc_gate", BcGateConfig.CODEC, this.gate.toConfig());
            output.putInt("bc_gate_side", this.gateSide.ordinal());
            output.putBoolean("bc_gate_on", this.gate.isOn());
            output.putInt("bc_gate_redstone", this.gate.getRedstoneOutput());
            int wireBits = 0;
            for (DyeColor colour : this.gate.getWireBroadcasts()) {
                wireBits |= 1 << colour.ordinal();
            }
            output.putInt("bc_gate_wires", wireBits);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energyStored = input.getLongOr("bc_energy_stored", 0L);
        this.gate = null;
        this.gateSide = null;
        // read(String, Codec): the gate config compound is the legacy gate_data shape (BcGateNbt)
        BcGateConfig config = input.read("bc_gate", BcGateConfig.CODEC).orElse(null);
        if (config != null) {
            int sideOrdinal = input.getIntOr("bc_gate_side", -1);
            Direction[] directions = Direction.values();
            if (sideOrdinal >= 0 && sideOrdinal < directions.length) {
                this.gate = new BcGateLogic(config);
                this.gateSide = directions[sideOrdinal];
                // runtime state (not part of the legacy copier payload): restore for the client renderer
                this.gate.restoreClientGlow(input.getBooleanOr("bc_gate_on", false));
                this.gate.latchRedstoneOutput(input.getIntOr("bc_gate_redstone", 0));
                int wireBits = input.getIntOr("bc_gate_wires", 0);
                for (DyeColor colour : DyeColor.values()) {
                    if ((wireBits & (1 << colour.ordinal())) != 0) {
                        this.gate.emitWire(colour);
                    }
                }
            }
        }
    }

    /**
     * Client sync, vanilla {@code BeaconBlockEntity} pattern (first used by {@link StoneEngineBlockEntity} in M2.7b):
     * the update tag is {@link #saveCustomOnly}, i.e. exactly the {@link #saveAdditional} keys (energy and gate state
     * included). The client applies the packet through {@code loadWithComponents} &rarr; {@link #loadAdditional}, so
     * no separate wire format is needed.
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
