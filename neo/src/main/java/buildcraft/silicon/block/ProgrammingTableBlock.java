/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.silicon.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import buildcraft.silicon.BcSiliconBlockEntities;
import buildcraft.silicon.blockentity.ProgrammingTableBlockEntity;

/**
 * {@code buildcraftsilicon:programming_table} (legacy {@code BlockLaserTable} variant): M4.16 keeps the real block
 * entity (v2 placeholder, see {@link ProgrammingTableBlockEntity}) under the static baseline model &mdash; no
 * ticker, no interaction behaviour beyond the vanilla defaults.
 */
public class ProgrammingTableBlock extends Block implements EntityBlock {

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
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
        BlockEntityType<T> type) {
        return null; // v2 placeholder: nothing ticks
    }
}
