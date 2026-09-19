/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.robotics.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import buildcraft.robotics.BcRoboticsBlockEntities;
import buildcraft.robotics.blockentity.RequesterBlockEntity;

/**
 * M4.17 requester block (the legacy {@code BlockRequester}): the static model stays; the block entity runs the
 * request-list + neighbour-pull behaviour server-side (see {@link RequesterBlockEntity}). The legacy comparator analog
 * output is not carried (v1 trim, javadoc-tracked on the block entity), and there is no GUI to open on use &mdash; the
 * legacy {@code use} opened {@code ContainerRequester}, which is a v2 GUI-era feature.
 */
public class RequesterBlock extends Block implements EntityBlock {

    public static final MapCodec<RequesterBlock> CODEC = simpleCodec(RequesterBlock::new);

    public RequesterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RequesterBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
        BlockEntityType<T> type) {
        if (level instanceof ServerLevel serverLevel) {
            return this.createTicker(type,
                (innerLevel, pos, innerState, entity) -> RequesterBlockEntity.serverTick(serverLevel, pos, innerState,
                    entity));
        }
        return null;
    }

    /** The plain-{@link Block} counterpart of {@code BaseEntityBlock#createTicker} (the {@code ChuteBlock} pattern). */
    @SuppressWarnings("unchecked")
    private <T extends BlockEntity> BlockEntityTicker<T> createTicker(BlockEntityType<T> given,
        BlockEntityTicker<? super RequesterBlockEntity> ticker) {
        return given == BcRoboticsBlockEntities.REQUESTER.value() ? (BlockEntityTicker<T>) ticker : null;
    }
}
