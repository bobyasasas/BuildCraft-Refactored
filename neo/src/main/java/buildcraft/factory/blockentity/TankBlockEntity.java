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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidType;
import org.jspecify.annotations.Nullable;
import buildcraft.factory.BcFactoryBlockEntities;

/**
 * M4.7 tank block entity: one 16-bucket fluid tank (the legacy {@code TileTank} capacity, 16 &times; 1000&nbsp;mB),
 * reachable through the fluid capability from every side for both filling and draining (buckets included, through
 * {@code TankBlock}'s right-click interaction).
 *
 * <p>v1 slice note: the legacy tank balances fluids across a whole stack of tanks ({@code balanceTankFluids}); this
 * slice keeps every tank independent &mdash; the stacking behaviour is a later milestone's work (the block model
 * already renders stacked tanks correctly).
 *
 * <p>Client sync (the {@code BeaconBlockEntity} pattern shared by this milestone's machines): the update tag is
 * {@link #saveCustomOnly}, the handler's {@code onContentsChanged} pushes it with {@code sendBlockUpdated}, and the
 * client applies it through {@code loadWithComponents} &rarr; {@link #loadAdditional} &mdash; so the M4.7 renderer
 * sees the same contents the server has, with no separate wire format.
 */
public class TankBlockEntity extends BlockEntity {

    /** The legacy {@code TileTank} tank size: 16 buckets. */
    public static final int CAPACITY = 16 * FluidType.BUCKET_VOLUME;

    /** The one tank; single source of truth for the fluid contents (fill and drain allowed everywhere). */
    private final BcFactoryFluidHandler tank = new BcFactoryFluidHandler(this::pushToClients, CAPACITY);

    public TankBlockEntity(BlockPos pos, BlockState state) {
        super(BcFactoryBlockEntities.TANK.value(), pos, state);
    }

    public BcFactoryFluidHandler getTank() {
        return this.tank;
    }

    /** The capability view: every side of a tank is the same tank. */
    public BcFactoryFluidHandler getFluidHandler(@Nullable Direction side) {
        return this.tank;
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
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.tank.deserialize(input);
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
