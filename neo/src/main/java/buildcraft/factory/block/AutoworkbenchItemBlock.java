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
import buildcraft.factory.blockentity.AutoworkbenchItemBlockEntity;

/**
 * M4.16 item auto workbench block (the legacy {@code BlockAutoWorkbenchItems}): the static placeholder model stays;
 * the block entity ticks the auto-craft loop server-side (see {@link AutoworkbenchItemBlockEntity}). The GUI (the
 * legacy {@code ContainerAutoCraftItems}) has not migrated &mdash; items reach the grid through the item capability
 * (hoppers and pipes).
 */
public class AutoworkbenchItemBlock extends Block implements EntityBlock {

    public static final MapCodec<AutoworkbenchItemBlock> CODEC = simpleCodec(AutoworkbenchItemBlock::new);

    public AutoworkbenchItemBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AutoworkbenchItemBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
        BlockEntityType<T> type) {
        if (level instanceof ServerLevel serverLevel) {
            return this.createTicker(type,
                (innerLevel, pos, innerState, entity) -> AutoworkbenchItemBlockEntity.serverTick(serverLevel, pos,
                    innerState, entity));
        }
        return null;
    }

    /** The plain-{@link Block} counterpart of {@code BaseEntityBlock#createTicker}: the ticker only matches when the
     * queried type is exactly this machine's block entity type (the {@code EngineBlock} pattern). */
    @SuppressWarnings("unchecked")
    private <T extends BlockEntity> BlockEntityTicker<T> createTicker(BlockEntityType<T> given,
        BlockEntityTicker<? super AutoworkbenchItemBlockEntity> ticker) {
        return given == BcFactoryBlockEntities.AUTOWORKBENCH_ITEM.value() ? (BlockEntityTicker<T>) ticker : null;
    }
}
