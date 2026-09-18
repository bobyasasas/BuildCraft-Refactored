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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidType;
import org.jspecify.annotations.Nullable;
import buildcraft.factory.BcFactoryBlockEntities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * M4.7 pump block entity (v1, drain mode (a) of the task slice): every {@link #PUMP_INTERVAL} ticks it picks up the
 * fluid source block directly below itself and puts one bucket's worth into its 16-bucket tank (the legacy
 * {@code TilePump} capacity), removing the source block in the world. Players drain the tank through buckets
 * ({@code PumpBlock}'s right-click interaction) or the fluid capability (drain-only from every side).
 *
 * <p>Deliberate v1 trims: the legacy pump digs a quarry of pipes down to fluids below ground and drains a whole
 * connected fluid body per operation ({@code TilePump#update()}); this slice only reaches the block underneath, and
 * the pipe-side draining is out of reach while the transport module's fluid-pipe pull logic is frozen (M4.6 slice).
 */
public class PumpBlockEntity extends BlockEntity {

    /** The legacy {@code TilePump} tank size: 16 buckets. */
    public static final int CAPACITY = 16 * FluidType.BUCKET_VOLUME;
    /** Ticks between pump operations (the v1 pacing; the legacy pump drains per tick once at full speed). */
    public static final int PUMP_INTERVAL = 20;

    /** The internal tank: external access is drain-only (the pump itself is what fills it). */
    private final BcFactoryFluidHandler tank = new BcFactoryFluidHandler(this::pushToClients, CAPACITY) {
        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return 0;
        }
    };

    /** Ticks until the next pump operation (0 = pump now). */
    private int cooldown;

    public PumpBlockEntity(BlockPos pos, BlockState state) {
        super(BcFactoryBlockEntities.PUMP.value(), pos, state);
    }

    public BcFactoryFluidHandler getTank() {
        return this.tank;
    }

    /** The capability view: every side drains the tank. */
    public BcFactoryFluidHandler getFluidHandler(@Nullable Direction side) {
        return this.tank;
    }

    /** The per-tick pump driver (wired through {@code PumpBlock#getTicker}). */
    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, PumpBlockEntity pump) {
        if (pump.cooldown > 0) {
            pump.cooldown--;
            return;
        }
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (!belowState.getFluidState().isSource()) {
            return;
        }
        Fluid fluid = belowState.getFluidState().getType();
        FluidStack current = pump.tank.getFluidStack();
        if (!current.isEmpty() && current.getFluid() != fluid) {
            return;
        }
        if (!BcMachineFluids.hasBucketRoom(pump.tank.getAmountAsLong(0), CAPACITY)) {
            return;
        }
        // one source block leaves the world, one bucket enters the tank (v1: whole-block pickup, see class javadoc)
        level.removeBlock(below, false);
        pump.tank.set(0, FluidResource.of(fluid), (int) (pump.tank.getAmountAsLong(0) + FluidType.BUCKET_VOLUME));
        pump.cooldown = PUMP_INTERVAL;
    }

    /** Marks the state dirty and pushes it to clients (Beacon-pattern update tag, see class javadoc). */
    private void pushToClients() {
        this.setChanged();
        if (this.level instanceof ServerLevel serverLevel) {
            BlockState state = this.getBlockState();
            serverLevel.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    // ---------------------------------------------------------------------
    // Persistence + client sync (Beacon pattern)
    // ---------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.tank.serialize(output);
        output.putInt("bc_cooldown", this.cooldown);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.tank.deserialize(input);
        this.cooldown = input.getIntOr("bc_cooldown", 0);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
