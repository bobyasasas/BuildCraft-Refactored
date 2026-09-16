/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import buildcraft.builders.BcBuildersBlockEntities;
import buildcraft.builders.blockentity.QuarryBlockEntity;

/**
 * The M2.12 quarry block (buildcraftbuilders:quarry), first real behaviour class under the id the M2.4c registry parity
 * registered as a placeholder (legacy counterpart: {@code buildcraft.builders.block.BlockQuarry}). Deliberately no
 * block state properties: the M2.4c baseline blockstate palette ({@code buildcraftbuilders:quarry} with the empty
 * property set) must stay byte-identical for the strict registry diff, so the legacy {@code CONNECTED_*} drill-hose
 * properties do NOT return here (they describe the frame/drill visuals that have not migrated).
 *
 * <p>The mining area is set programmatically on the {@link QuarryBlockEntity} (legacy: volume markers + frame blocks;
 * not migrated, see the BE javadoc) &mdash; there is no block interaction in the slice.
 */
public class QuarryBlock extends BaseEntityBlock {

    public static final MapCodec<QuarryBlock> CODEC = simpleCodec(QuarryBlock::new);

    public QuarryBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QuarryBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level instanceof ServerLevel serverLevel) {
            return createTickerHelper(
                type,
                BcBuildersBlockEntities.QUARRY.value(),
                (innerLevel, pos, innerState, entity) -> QuarryBlockEntity.serverTick(serverLevel, pos, innerState, entity));
        }
        return null;
    }
}
