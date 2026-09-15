/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import buildcraft.core.blockentity.EnergyMeterBlockEntity;

/**
 * The M2.2c slice-only measurement block (buildcraftcore:energy_meter). This is <em>not</em> final content: a plain
 * {@link BaseEntityBlock} hosting the {@link EnergyMeterBlockEntity} accumulator so the
 * {@code kinesis_chain_transfers_power} game test can assert that energy really flows through the pipe chain. No ticker
 * and no behaviour beyond the block entity's infinite sink (see {@link EnergyMeterBlockEntity}); removed again once the
 * M2.4/M2.9 registry migration brings real consumers to test against.
 */
public class EnergyMeterBlock extends BaseEntityBlock {

    public static final MapCodec<EnergyMeterBlock> CODEC = simpleCodec(EnergyMeterBlock::new);

    public EnergyMeterBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyMeterBlockEntity(pos, state);
    }
}
