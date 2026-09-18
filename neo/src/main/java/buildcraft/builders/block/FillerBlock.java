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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;
import buildcraft.builders.BcBuildersBlockEntities;
import buildcraft.builders.blockentity.FillerBlockEntity;

/**
 * The M2.12 filler block (buildcraftbuilders:filler), first real behaviour class under the id the M2.4c registry parity
 * registered as a placeholder (legacy counterpart: {@code buildcraft.builders.block.BlockFiller}). Deliberately no
 * block state properties: the M2.4c baseline blockstate palette ({@code buildcraftbuilders:filler} with the empty
 * property set) must stay byte-identical for the strict registry diff, so the legacy {@code PROP_FACING} does NOT
 * return here (the slice filler works on a programmatic work area instead of a facing-relative one; facing comes back
 * with the marker/volume box systems).
 *
 * <p>Right-clicking with a placeable block item loads it into the {@link FillerBlockEntity} resource inventory (the
 * {@code StoneEngineBlock#useItemOn} pattern); right-clicking otherwise opens the M4.8 filler GUI
 * ({@link #useWithoutItem}, the vanilla furnace pattern: the block entity is its own {@code MenuProvider} and the menu
 * opens through {@code player.openMenu}). The same insertion entry point is available programmatically via
 * {@link FillerBlockEntity#insertResource}, which the game tests call directly. Pattern/area selection stay
 * programmatic too (legacy: the pattern GUI and marker boxes &mdash; not migrated, see {@link FillerBlockEntity}).
 */
public class FillerBlock extends BaseEntityBlock {

    public static final MapCodec<FillerBlock> CODEC = simpleCodec(FillerBlock::new);

    public FillerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FillerBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
        BlockHitResult hitResult
    ) {
        if (stack.getItem() instanceof BlockItem
                && level.getBlockEntity(pos) instanceof FillerBlockEntity filler
                && filler.insertResource(stack.copy()).isEmpty()) {
            if (!level.isClientSide()) {
                stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        // Not a placeable block item (or every slot is full): fall through to the empty-hand GUI behaviour.
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        // The vanilla furnace pattern: the block entity is its own MenuProvider; the NeoForge openMenu extension
        // writes the position into the menu's extra data so the client half can re-resolve the filler.
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FillerBlockEntity filler) {
            player.openMenu(filler, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level instanceof ServerLevel serverLevel) {
            return createTickerHelper(
                type,
                BcBuildersBlockEntities.FILLER.value(),
                (innerLevel, pos, innerState, entity) -> FillerBlockEntity.serverTick(serverLevel, pos, innerState, entity));
        }
        return null;
    }
}
