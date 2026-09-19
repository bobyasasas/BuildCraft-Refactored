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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import buildcraft.factory.BcFactoryBlockEntities;
import buildcraft.factory.blockentity.MiningWellBlockEntity;

/**
 * M4.16 mining well block (the legacy {@code BlockMiningWell}): the static placeholder model stays; the block entity
 * ticks the MJ-fed drill loop server-side (see {@link MiningWellBlockEntity}).
 */
public class MiningWellBlock extends Block implements EntityBlock {

    public static final MapCodec<MiningWellBlock> CODEC = simpleCodec(MiningWellBlock::new);

    public MiningWellBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MiningWellBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
        BlockEntityType<T> type) {
        if (level instanceof ServerLevel serverLevel) {
            return this.createTicker(type,
                (innerLevel, pos, innerState, entity) -> MiningWellBlockEntity.serverTick(serverLevel, pos, innerState,
                    entity));
        }
        return null;
    }

    /** The plain-{@link Block} counterpart of {@code BaseEntityBlock#createTicker}: the ticker only matches when the
     * queried type is exactly this machine's block entity type (the {@code EngineBlock} pattern). */
    @SuppressWarnings("unchecked")
    private <T extends BlockEntity> BlockEntityTicker<T> createTicker(BlockEntityType<T> given,
        BlockEntityTicker<? super MiningWellBlockEntity> ticker) {
        return given == BcFactoryBlockEntities.MINING_WELL.value() ? (BlockEntityTicker<T>) ticker : null;
    }
}
