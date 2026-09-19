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
import buildcraft.silicon.blockentity.ProgrammingTableBlockEntity;

/**
 * {@code buildcraftsilicon:programming_table} (legacy {@code BlockLaserTable} variant): since M4.17 the real
 * programming behaviour ticks here (the {@link ProgrammingTableBlockEntity} recipe loop), replacing the M4.16 v2
 * placeholder no-op.
 */
public class ProgrammingTableBlock extends SiliconTableBlock<ProgrammingTableBlockEntity> {

    public static final MapCodec<ProgrammingTableBlock> CODEC = simpleCodec(ProgrammingTableBlock::new);

    public ProgrammingTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ProgrammingTableBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<ProgrammingTableBlockEntity> type() {
        return BcSiliconBlockEntities.PROGRAMMING_TABLE.value();
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state, ProgrammingTableBlockEntity table) {
        ProgrammingTableBlockEntity.serverTick(level, pos, state, table);
    }
}
