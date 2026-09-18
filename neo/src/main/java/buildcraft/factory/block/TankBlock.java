/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import buildcraft.factory.BcFactoryBlockEntities;
import buildcraft.factory.blockentity.TankBlockEntity;

/**
 * M4.7 tank block (the legacy {@code BlockTank}): an almost-full glass box whose block entity holds the fluids.
 * Right-clicks reach the tank through the fluid capability ({@link FactoryMachineBlock}); the M4.7 renderer draws the
 * liquid column inside the glass. No occlusion so the neighbouring faces behind the glass stay visible.
 */
public class TankBlock extends FactoryMachineBlock {

    public static final MapCodec<TankBlock> CODEC = simpleCodec(TankBlock::new);

    /** The glass box of the block model (12&times;16&times;12 pixels, the baseline {@code tank.json} geometry). */
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

    public TankBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TankBlockEntity(pos, state);
    }
}
