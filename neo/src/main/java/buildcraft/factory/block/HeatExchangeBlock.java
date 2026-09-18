/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import buildcraft.factory.BcFactoryBlockEntities;
import buildcraft.factory.blockentity.HeatExchangeBlockEntity;

/**
 * M4.7 heat exchanger block (the legacy {@code BlockHeatExchange}): renders nothing itself ({@link
 * RenderShape#INVISIBLE}, the 26.1.2 BER-driven-block pattern used by the pipe holder) &mdash; the M4.7 renderer
 * draws the {@code heat_exchange_static.jsonbc} geometry plus the two tank windows. The block entity ticks the
 * heatable/coolable batches server-side.
 */
public class HeatExchangeBlock extends FactoryMachineBlock {

    public static final MapCodec<HeatExchangeBlock> CODEC = simpleCodec(HeatExchangeBlock::new);

    public HeatExchangeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HeatExchangeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
        BlockEntityType<T> type) {
        if (level instanceof ServerLevel serverLevel) {
            return this.createTicker(type,
                (innerLevel, pos, innerState, entity) -> HeatExchangeBlockEntity.serverTick(serverLevel, pos,
                    innerState, entity));
        }
        return null;
    }

    /** The plain-{@link Block} counterpart of {@code BaseEntityBlock#createTicker}: the ticker only matches when the
     * queried type is exactly this machine's block entity type (the {@code EngineBlock} pattern). */
    @SuppressWarnings("unchecked")
    private <T extends BlockEntity> BlockEntityTicker<T> createTicker(BlockEntityType<T> given,
        BlockEntityTicker<? super HeatExchangeBlockEntity> ticker) {
        return given == BcFactoryBlockEntities.HEAT_EXCHANGE.value() ? (BlockEntityTicker<T>) ticker : null;
    }
}
