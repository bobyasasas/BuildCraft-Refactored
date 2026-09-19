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
import buildcraft.silicon.blockentity.AdvancedCraftingTableBlockEntity;

/** {@code buildcraftsilicon:advanced_crafting_table} (legacy {@code BlockLaserTable} variant): the M4.16 behaviour. */
public class AdvancedCraftingTableBlock extends SiliconTableBlock<AdvancedCraftingTableBlockEntity> {

    public static final MapCodec<AdvancedCraftingTableBlock> CODEC = simpleCodec(AdvancedCraftingTableBlock::new);

    public AdvancedCraftingTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AdvancedCraftingTableBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<AdvancedCraftingTableBlockEntity> type() {
        return BcSiliconBlockEntities.ADVANCED_CRAFTING_TABLE.value();
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state, AdvancedCraftingTableBlockEntity table) {
        AdvancedCraftingTableBlockEntity.serverTick(level, pos, state, table);
    }
}
