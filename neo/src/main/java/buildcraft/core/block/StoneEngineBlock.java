/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;
import buildcraft.core.BcBlockEntities;
import buildcraft.core.blockentity.StoneEngineBlockEntity;

/**
 * The M2.2b stone engine block (buildcraftcore:engine_stone), vertical slice edition. {@code FACING} (horizontal, set
 * away from the player on placement like the vanilla furnace) marks the energy output face consumed by the M2.2c pipes.
 *
 * <p>This is the pattern template for the M2.4 engine registry migration ({@code BlockEngineStone}); heat, explosion
 * and animation behaviour are not migrated here (see {@link StoneEngineBlockEntity}). The final energy module registry
 * placement is an M2.4/M2.9 decision.
 *
 * <p>Right-clicking with a fuel item inserts one item into the {@link StoneEngineBlockEntity} (burn length resolved
 * through the vanilla fuel table); right-clicking otherwise opens the M4.8 stone engine GUI
 * ({@link #useWithoutItem}, the vanilla furnace pattern: the block entity is its own {@code MenuProvider} and the menu
 * opens through {@code player.openMenu}). Fuel insertion stays available programmatically via
 * {@link StoneEngineBlockEntity#insertFuel(net.minecraft.world.item.ItemStack, net.minecraft.world.level.block.entity.FuelValues)},
 * which the game tests call directly.
 */
public class StoneEngineBlock extends BaseEntityBlock {

    public static final MapCodec<StoneEngineBlock> CODEC = simpleCodec(StoneEngineBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public StoneEngineBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StoneEngineBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
        BlockHitResult hitResult
    ) {
        if (level.getBlockEntity(pos) instanceof StoneEngineBlockEntity engine && engine.insertFuel(stack, level.fuelValues())) {
            if (!level.isClientSide()) {
                stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        // Not fuel (or the slot is full): fall through to the empty-hand behaviour, which opens the GUI.
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        // The vanilla furnace pattern: the block entity is its own MenuProvider; the NeoForge openMenu extension
        // writes the position into the menu's extra data so the client half can re-resolve the engine.
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StoneEngineBlockEntity engine) {
            player.openMenu(engine, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level instanceof ServerLevel serverLevel) {
            return createTickerHelper(
                type,
                BcBlockEntities.ENGINE_STONE.value(),
                (innerLevel, pos, innerState, entity) -> StoneEngineBlockEntity.serverTick(serverLevel, pos, innerState, entity));
        }
        return null;
    }
}
