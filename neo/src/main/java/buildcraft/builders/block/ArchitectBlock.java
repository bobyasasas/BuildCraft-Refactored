/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;
import buildcraft.builders.BcBuildersBlockEntities;
import buildcraft.builders.blockentity.ArchitectBlockEntity;
import buildcraft.builders.blueprint.BlueprintItems;

/**
 * The M4.17 architect table block ({@code buildcraftbuilders:architect}; legacy counterpart:
 * {@code buildcraft.builders.block.BlockArchitectTable}). Deliberately no block state properties (no facing): the
 * M2.4c baseline blockstate palette must stay byte-identical for the strict registry diff (the {@link QuarryBlock}
 * precedent), so the legacy {@code PROP_FACING}/{@code PROP_VALID} do not return &mdash; the box comes from the marker
 * pair instead of the facing side.
 *
 * <p>Right-clicking with a blank blueprint item loads it into the input slot (the {@link FillerBlock} pattern);
 * right-clicking with an empty hand logs the [M417] status line (the legacy GUI is not migrated).
 */
public class ArchitectBlock extends BaseEntityBlock {

    public static final MapCodec<ArchitectBlock> CODEC = simpleCodec(ArchitectBlock::new);

    public ArchitectBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArchitectBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
        BlockHitResult hitResult
    ) {
        if (level.getBlockEntity(pos) instanceof ArchitectBlockEntity architect
                && architect.insertBlank(stack.copy()).isEmpty()) {
            // fully accepted: consume the held item (server side only, the StoneEngineBlock pattern)
            if (!level.isClientSide()) {
                stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ArchitectBlockEntity architect) {
            architect.logStatus();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level instanceof ServerLevel serverLevel) {
            return createTickerHelper(
                type,
                BcBuildersBlockEntities.ARCHITECT.value(),
                (innerLevel, pos, innerState, entity) -> ArchitectBlockEntity.serverTick(serverLevel, pos, innerState, entity));
        }
        return null;
    }
}
