/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.silicon.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import buildcraft.silicon.BcSiliconBlockEntities;
import buildcraft.silicon.blockentity.IntegrationTableBlockEntity;

/** {@code buildcraftsilicon:integration_table} (legacy {@code BlockLaserTable} variant): the M4.16 real behaviour. */
public class IntegrationTableBlock extends SiliconTableBlock<IntegrationTableBlockEntity> {

    public static final MapCodec<IntegrationTableBlock> CODEC = simpleCodec(IntegrationTableBlock::new);

    public IntegrationTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IntegrationTableBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<IntegrationTableBlockEntity> type() {
        return BcSiliconBlockEntities.INTEGRATION_TABLE.value();
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state, IntegrationTableBlockEntity table) {
        IntegrationTableBlockEntity.serverTick(level, pos, state, table);
    }
}
