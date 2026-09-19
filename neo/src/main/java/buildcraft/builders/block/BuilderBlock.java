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
import buildcraft.builders.blockentity.BuilderBlockEntity;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The M4.17 builder block ({@code buildcraftbuilders:builder}; legacy counterpart:
 * {@code buildcraft.builders.block.BlockBuilder}). Deliberately no block state properties (no facing): the M2.4c
 * baseline blockstate palette must stay byte-identical for the strict registry diff (the {@link QuarryBlock}
 * precedent), so the legacy {@code PROP_FACING} does not return &mdash; the v1 build origin is a fixed east offset
 * (see {@code BuilderBlockEntity}).
 *
 * <p>Right-clicking with a written blueprint item loads it into the blueprint slot, with a placeable block item it
 * loads the resource inventory (the {@link FillerBlock} pattern); right-clicking with an empty hand logs the [M417]
 * status line (the legacy GUI is not migrated).
 */
public class BuilderBlock extends BaseEntityBlock {

    public static final MapCodec<BuilderBlock> CODEC = simpleCodec(BuilderBlock::new);

    public BuilderBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BuilderBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
        BlockHitResult hitResult
    ) {
        if (level.getBlockEntity(pos) instanceof BuilderBlockEntity builder) {
            ItemStack copy = stack.copy();
            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = 0;
                for (int slot = 0; slot < BuilderBlockEntity.INV_SLOTS && inserted < copy.getCount(); slot++) {
                    inserted += builder.getInv().insert(slot, ItemResource.of(copy), copy.getCount() - inserted,
                        transaction);
                }
                if (inserted > 0) {
                    transaction.commit();
                    if (!level.isClientSide()) {
                        stack.shrink(inserted);
                    }
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BuilderBlockEntity builder) {
            builder.logStatus();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level instanceof ServerLevel serverLevel) {
            return createTickerHelper(
                type,
                BcBuildersBlockEntities.BUILDER.value(),
                (innerLevel, pos, innerState, entity) -> BuilderBlockEntity.serverTick(serverLevel, pos, innerState, entity));
        }
        return null;
    }
}
