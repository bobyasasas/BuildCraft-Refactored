/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.transport.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import buildcraft.transport.BcTransportBlockEntities;
import buildcraft.transport.blockentity.FilteredBufferBlockEntity;

/**
 * The M4.16 filtered buffer block ({@code buildcrafttransport:filtered_buffer}), first real behaviour class under the
 * id the M2.4c registry parity registered as a placeholder (legacy counterpart:
 * {@code buildcraft.transport.block.BlockFilteredBuffer}). Deliberately no block state properties: the M2.4c baseline
 * blockstate palette ({@code buildcrafttransport:filtered_buffer} with the empty property set) must stay byte-identical
 * for the strict registry diff, so the placeholder's plain {@code Block} registration in {@code BcTransportBlocks} is
 * swapped for this class under the unchanged id.
 *
 * <p>Right-click has no behaviour (the legacy buffer opens its filter GUI, which has not migrated &mdash; see
 * {@link FilteredBufferBlockEntity} for the trimmed filter semantics). The item transfer runs entirely through the
 * {@code Capabilities.Item.BLOCK} capability registered in {@code BuildCraftTransport}, so pipes (M4.6 item slice) and
 * hoppers push into and pull out of the buffer from any face.
 */
public class FilteredBufferBlock extends BaseEntityBlock {

    public static final MapCodec<FilteredBufferBlock> CODEC = simpleCodec(FilteredBufferBlock::new);

    public FilteredBufferBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FilteredBufferBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level instanceof ServerLevel serverLevel) {
            return createTickerHelper(
                type,
                BcTransportBlockEntities.FILTERED_BUFFER.value(),
                (innerLevel, pos, innerState, entity) -> FilteredBufferBlockEntity.serverTick(serverLevel, pos,
                        innerState, entity));
        }
        return null;
    }
}
