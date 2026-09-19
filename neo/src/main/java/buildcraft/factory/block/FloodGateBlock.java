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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import buildcraft.factory.BcFactoryBlockEntities;
import buildcraft.factory.blockentity.FloodGateBlockEntity;

/**
 * M4.16 flood gate block (the legacy {@code BlockFloodGate}): right-clicking with a fluid container refills/drains the
 * internal tank through the shared {@link FactoryMachineBlock} interaction; the block entity ticks the source-block
 * placement loop server-side (see {@link FloodGateBlockEntity}). The legacy connected-sides visual states are not
 * carried &mdash; the static placeholder model stays.
 */
public class FloodGateBlock extends FactoryMachineBlock {

    public static final MapCodec<FloodGateBlock> CODEC = simpleCodec(FloodGateBlock::new);

    public FloodGateBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FloodGateBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
        BlockEntityType<T> type) {
        if (level instanceof ServerLevel serverLevel) {
            return this.createTicker(type,
                (innerLevel, pos, innerState, entity) -> FloodGateBlockEntity.serverTick(serverLevel, pos, innerState,
                    entity));
        }
        return null;
    }

    /** The plain-{@link Block} counterpart of {@code BaseEntityBlock#createTicker}: the ticker only matches when the
     * queried type is exactly this machine's block entity type (the {@code EngineBlock} pattern). */
    @SuppressWarnings("unchecked")
    private <T extends BlockEntity> BlockEntityTicker<T> createTicker(BlockEntityType<T> given,
        BlockEntityTicker<? super FloodGateBlockEntity> ticker) {
        return given == BcFactoryBlockEntities.FLOOD_GATE.value() ? (BlockEntityTicker<T>) ticker : null;
    }
}
