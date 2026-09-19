/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import org.jspecify.annotations.Nullable;
import buildcraft.factory.BcFactoryBlockEntities;

/**
 * M4.16 flood gate block entity: a minimal port of the legacy {@code TileFloodGate} under the unchanged id
 * {@code buildcraftfactory:flood_gate}. The gate holds the legacy 2-bucket internal tank (refilled by hand through
 * {@code FloodGateBlock}'s right-click bucket interaction or any fluid-capability neighbour) and, while it holds at
 * least one bucket, places one source block of that fluid every {@link #PLACE_INTERVAL} ticks at the legacy cadence
 * (16 ticks), draining exactly the placed bucket from the tank through the stock
 * {@link FluidUtil#tryPlaceFluid} placement path (the legacy {@code FakePlayer} + bucket-emptying half).
 *
 * <p><b>v1 trim (javadoc-tracked, the task's sanctioned simplification):</b> the baseline BFS over the open sides
 * ({@code buildQueue} with the 4096-cell queue, path tracking and the escalating rebuild delays) is reduced to the
 * straight column above the gate: the scan ({@link FloodGateLogic}) hops over cells that already hold the gate's fluid
 * and places the next source on top, so a submerged gate keeps raising its column until a foreign block (or the build
 * limit) stops it. The legacy six-side open/close switches are not carried (the column is always "open"). Gaseous
 * fluids use the same upward column (the legacy searched downward for them); with the 2-bucket tank the visible
 * behaviour &mdash; pour fluid in, a column of sources grows out of the gate &mdash; matches.
 *
 * <p><b>Client sync</b> is the milestone's {@code BeaconBlockEntity} pattern (the {@code TankBlockEntity} house
 * style): {@link #getUpdateTag} returns {@link #saveCustomOnly} and the tank handler pushes it with
 * {@code sendBlockUpdated}.
 */
public class FloodGateBlockEntity extends BlockEntity {

    /** The legacy {@code TileFloodGate} tank size: 2 buckets. */
    public static final int CAPACITY = 2 * FluidType.BUCKET_VOLUME;
    /** Ticks between placements (the legacy {@code tick % 16} cadence). */
    public static final int PLACE_INTERVAL = 16;
    /** How far the v1 straight-column scan may reach above the gate (see the class javadoc trim note). */
    public static final int COLUMN_LIMIT = 32;

    /** The one tank; single source of truth for the fluid contents (fill and drain allowed everywhere). */
    private final BcFactoryFluidHandler tank = new BcFactoryFluidHandler(this::pushToClients, CAPACITY);
    /** Ticks until the next placement attempt. */
    private int cooldown;
    /** Lifetime placed source count (evidence counter). */
    private long placedTotal;

    public FloodGateBlockEntity(BlockPos pos, BlockState state) {
        super(BcFactoryBlockEntities.FLOOD_GATE.value(), pos, state);
    }

    public BcFactoryFluidHandler getTank() {
        return this.tank;
    }

    /** The capability view: every side of the gate reaches the same tank (the legacy all-parts fluid registration). */
    public BcFactoryFluidHandler getFluidHandler(@Nullable Direction side) {
        return this.tank;
    }

    public long getPlacedTotal() {
        return this.placedTotal;
    }

    /** The per-tick placement driver (wired through {@code FloodGateBlock#getTicker}). */
    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, FloodGateBlockEntity gate) {
        if (gate.cooldown > 0) {
            gate.cooldown--;
        } else {
            gate.cooldown = PLACE_INTERVAL;
            gate.tryPlace(level, pos);
        }
    }

    /**
     * Places one source block of the tank fluid in the column above the gate (see the class javadoc): the scan picks
     * the cell, the stock {@code FluidUtil} placement drains exactly one bucket out of the tank and writes the source
     * state (or vaporises it, the nether half of the vanilla bucket path).
     */
    private void tryPlace(ServerLevel level, BlockPos pos) {
        FluidStack fluid = this.tank.getFluidStack();
        if (fluid.getAmount() < FluidType.BUCKET_VOLUME) {
            return;
        }
        Fluid tankFluid = fluid.getFluid();
        int offset = FloodGateLogic.findPlacementUp(COLUMN_LIMIT, o -> cellKind(level, pos.offset(0, o, 0), tankFluid));
        if (offset < 0) {
            return;
        }
        FluidStack placed = FluidUtil.tryPlaceFluid(this.tank, null, level, pos.offset(0, offset, 0), false, null);
        if (!placed.isEmpty()) {
            this.placedTotal++;
            this.pushToClients();
        }
    }

    /** Maps one column cell to the scan's world state (see {@link FloodGateLogic.Cell}). */
    private static FloodGateLogic.Cell cellKind(ServerLevel level, BlockPos pos, Fluid tankFluid) {
        if (level.isOutsideBuildHeight(pos)) {
            return FloodGateLogic.Cell.BLOCKED;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return FloodGateLogic.Cell.PLACEABLE;
        }
        FluidState fluidState = state.getFluidState();
        if (!fluidState.isEmpty()) {
            return fluidState.getType().isSame(tankFluid)
                    ? FloodGateLogic.Cell.SAME_FLUID
                    : FloodGateLogic.Cell.BLOCKED;
        }
        return state.canBeReplaced() ? FloodGateLogic.Cell.PLACEABLE : FloodGateLogic.Cell.BLOCKED;
    }

    // ---------------------------------------------------------------------
    // Persistence + client sync (Beacon pattern)
    // ---------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.tank.serialize(output.child("bc_tank"));
        output.putLong("bc_placed_total", this.placedTotal);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.tank.deserialize(input.childOrEmpty("bc_tank"));
        this.placedTotal = input.getLongOr("bc_placed_total", 0L);
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
    private void pushToClients() {
        this.setChanged();
        if (this.level instanceof ServerLevel serverLevel) {
            BlockState state = this.getBlockState();
            serverLevel.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }
}
