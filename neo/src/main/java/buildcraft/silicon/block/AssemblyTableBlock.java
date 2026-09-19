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
import buildcraft.silicon.blockentity.AssemblyTableBlockEntity;

/** {@code buildcraftsilicon:assembly_table} (legacy {@code BlockLaserTable} variant): the M4.16 real behaviour. */
public class AssemblyTableBlock extends SiliconTableBlock<AssemblyTableBlockEntity> {

    public static final MapCodec<AssemblyTableBlock> CODEC = simpleCodec(AssemblyTableBlock::new);

    public AssemblyTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AssemblyTableBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<AssemblyTableBlockEntity> type() {
        return BcSiliconBlockEntities.ASSEMBLY_TABLE.value();
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state, AssemblyTableBlockEntity table) {
        AssemblyTableBlockEntity.serverTick(level, pos, state, table);
    }
}
