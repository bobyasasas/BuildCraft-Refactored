/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import buildcraft.core.BcBlockEntities;

/**
 * Slice-only measurement block entity for the M2.2c vertical slice of buildcraftcore
 * ({@code buildcraftcore:energy_meter}). This is <em>not</em> final content: it exists purely as the sink that lets the
 * {@code kinesis_chain_transfers_power} game test observe that energy really flows engine &rarr; kinesis pipes &rarr;
 * consumer, because the slice has no real machines to power yet (the M2.4/M2.9 registry migration brings the first real
 * consumers).
 *
 * <p>Behaviour: {@link #receiveEnergy(long, boolean)} accepts <em>everything</em> it is offered (infinite sink) and
 * accumulates it in {@link #totalReceived}. There is no consumption logic, no buffer limit and no ticker &mdash;
 * {@link #getTotalReceived()} is the only observable, read by the game test assertion.
 */
public class EnergyMeterBlockEntity extends BlockEntity {

    /** Lifetime sum of all accepted energy, &micro;MJ (micro-MJ, see {@link KinesisPipeBlockEntity} units note). */
    private long totalReceived;

    public EnergyMeterBlockEntity(BlockPos pos, BlockState state) {
        super(BcBlockEntities.ENERGY_METER.value(), pos, state);
    }

    /**
     * Energy sink interface of the slice: always accepts the full {@code amount} (simulate simply returns it). Direct
     * method call on purpose, mirroring {@link StoneEngineBlockEntity#extractEnergy(long, boolean)}.
     */
    public long receiveEnergy(long amount, boolean simulate) {
        if (amount <= 0) {
            return 0;
        }
        if (!simulate) {
            this.totalReceived += amount;
            setChanged();
        }
        return amount;
    }

    public long getTotalReceived() {
        return this.totalReceived;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("bc_total_received", this.totalReceived);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.totalReceived = input.getLongOr("bc_total_received", 0L);
    }
}
