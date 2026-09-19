/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.silicon.block;

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
import org.jspecify.annotations.Nullable;
import buildcraft.silicon.blockentity.LaserTableBaseBlockEntity;

/**
 * Shared behaviour of the M4.16 laser-driven table blocks (legacy {@code BlockLaserTable}): plain {@link Block} +
 * {@link EntityBlock} (the static baseline model keeps rendering, {@code BaseEntityBlock} would hide it) whose
 * server ticker drives the concrete table's block entity.
 *
 * @param <T> the concrete block entity type this block creates and ticks
 */
public abstract class SiliconTableBlock<T extends LaserTableBaseBlockEntity> extends Block implements EntityBlock {

    protected SiliconTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public abstract MapCodec<? extends Block> codec();

    @Override
    public abstract BlockEntity newBlockEntity(BlockPos pos, BlockState state);

    /** The registered block entity type this block's ticker matches ({@code BcSiliconBlockEntities} holder). */
    protected abstract BlockEntityType<T> type();

    /** The concrete per-tick driver ({@code XBlockEntity#serverTick}). */
    protected abstract void tick(ServerLevel level, BlockPos pos, BlockState state, T table);

    @Override
    public <E extends BlockEntity> @Nullable BlockEntityTicker<E> getTicker(Level level, BlockState state,
        BlockEntityType<E> type) {
        if (level instanceof ServerLevel serverLevel) {
            return this.createTicker(type,
                (innerLevel, pos, innerState, entity) -> this.tick(serverLevel, pos, innerState, entity));
        }
        return null;
    }

    /** The plain-{@link Block} counterpart of {@code BaseEntityBlock#createTicker} (the {@code DistillerBlock}
     * pattern): the ticker only matches this table's block entity type. */
    @SuppressWarnings("unchecked")
    private <E extends BlockEntity> @Nullable BlockEntityTicker<E> createTicker(BlockEntityType<E> given,
        BlockEntityTicker<? super T> ticker) {
        return given == this.type() ? (BlockEntityTicker<E>) ticker : null;
    }
}
