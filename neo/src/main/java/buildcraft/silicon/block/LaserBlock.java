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
import buildcraft.silicon.BcSiliconBlockEntities;
import buildcraft.silicon.blockentity.LaserBlockEntity;

/**
 * M4.16 laser block (legacy {@code BlockLaser}): plain {@link Block} + {@link EntityBlock} (the M4.7 machine
 * pattern &mdash; keeps the static baseline model rendering, {@code BaseEntityBlock} would hide it) with the server
 * ticker driving the energy pushes.
 */
public class LaserBlock extends Block implements EntityBlock {

    public static final MapCodec<LaserBlock> CODEC = simpleCodec(LaserBlock::new);

    public LaserBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LaserBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
        BlockEntityType<T> type) {
        if (level instanceof ServerLevel serverLevel) {
            return this.createTicker(type,
                (innerLevel, pos, innerState, entity) -> LaserBlockEntity.serverTick(serverLevel, pos, innerState,
                    entity));
        }
        return null;
    }

    /** The plain-{@link Block} counterpart of {@code BaseEntityBlock#createTicker} (the {@code DistillerBlock}
     * pattern): the ticker only matches this machine's block entity type. */
    @SuppressWarnings("unchecked")
    private <T extends BlockEntity> BlockEntityTicker<T> createTicker(BlockEntityType<T> given,
        BlockEntityTicker<? super LaserBlockEntity> ticker) {
        return given == BcSiliconBlockEntities.LASER.value() ? (BlockEntityTicker<T>) ticker : null;
    }
}
