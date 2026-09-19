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
import buildcraft.factory.blockentity.ChuteBlockEntity;

/**
 * M4.16 chute block (the legacy {@code BlockChute}): the static placeholder model stays; the block entity ticks the
 * above-to-below item pass-through server-side (see {@link ChuteBlockEntity}). The legacy six-side facing property is
 * not carried &mdash; the v1 chute always pulls from above and pushes below.
 */
public class ChuteBlock extends Block implements EntityBlock {

    public static final MapCodec<ChuteBlock> CODEC = simpleCodec(ChuteBlock::new);

    public ChuteBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChuteBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
        BlockEntityType<T> type) {
        if (level instanceof ServerLevel serverLevel) {
            return this.createTicker(type,
                (innerLevel, pos, innerState, entity) -> ChuteBlockEntity.serverTick(serverLevel, pos, innerState,
                    entity));
        }
        return null;
    }

    /** The plain-{@link Block} counterpart of {@code BaseEntityBlock#createTicker}: the ticker only matches when the
     * queried type is exactly this machine's block entity type (the {@code EngineBlock} pattern). */
    @SuppressWarnings("unchecked")
    private <T extends BlockEntity> BlockEntityTicker<T> createTicker(BlockEntityType<T> given,
        BlockEntityTicker<? super ChuteBlockEntity> ticker) {
        return given == BcFactoryBlockEntities.CHUTE.value() ? (BlockEntityTicker<T>) ticker : null;
    }
}
