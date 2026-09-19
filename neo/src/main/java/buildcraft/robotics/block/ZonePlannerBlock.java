/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.robotics.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import buildcraft.robotics.blockentity.ZonePlannerBlockEntity;

/**
 * M4.17 zone planner block (the legacy {@code BlockZonePlanner}): the static model stays; the block entity carries the
 * v1 zone definition + persistence (see {@link ZonePlannerBlockEntity}). No ticker (the legacy
 * {@code IBlockWithTickableTE} ticking existed only for the map-location progress bars, a v1 trim), and no GUI to open
 * on use (the legacy {@code use} opened {@code ContainerZonePlanner}, a v2 GUI-era feature).
 */
public class ZonePlannerBlock extends Block implements EntityBlock {

    public static final MapCodec<ZonePlannerBlock> CODEC = simpleCodec(ZonePlannerBlock::new);

    public ZonePlannerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ZonePlannerBlockEntity(pos, state);
    }
}
