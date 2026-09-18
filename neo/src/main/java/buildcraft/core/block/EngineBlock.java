/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.block;

import java.util.function.Supplier;
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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import buildcraft.core.blockentity.EngineBlockEntity;

/**
 * M4.4 engine-family block (wood / iron / rf / creative, plus the mj_dynamo in buildcraftenergy): the block half of
 * the {@link EngineBlockEntity} slice, following the {@link StoneEngineBlock} pattern —
 * {@code FACING} (horizontal, set away from the player on placement) marks the energy output face and drives the BER's
 * model rotation, right-clicking with a fuel item inserts it into the {@link EngineBlockEntity} (creative engines
 * accept nothing) and the block entity ticks on both sides.
 *
 * <p>In-world the static block model renders nothing (a particle-only model, like the baseline's
 * {@code minecraft:builtin/entity} + particle contract): every visible quad comes from the engine BER.
 */
public class EngineBlock extends Block implements EntityBlock {

    public static final MapCodec<EngineBlock> CODEC = simpleCodec(EngineBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** The block entity type of this engine instance; resolved lazily (blocks register before BE types). */
    private final Supplier<BlockEntityType<? extends EngineBlockEntity>> type;
    private final boolean creative;

    /** The codec-facing constructor (creative off, BE type resolved by subclasses — unused here, see {@link #CODEC}). */
    public EngineBlock(BlockBehaviour.Properties properties) {
        this(properties, () -> {
            throw new IllegalStateException("EngineBlock requires a block entity type supplier");
        }, false);
    }

    public EngineBlock(BlockBehaviour.Properties properties,
        Supplier<BlockEntityType<? extends EngineBlockEntity>> type, boolean creative) {
        super(properties);
        this.type = type;
        this.creative = creative;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<? extends Block> codec() {
        return CODEC;
    }

    /** Whether this engine instance is the creative one (passed through to the block entity). */
    public boolean isCreativeEngine() {
        return this.creative;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EngineBlockEntity(this.type.get(), pos, state, this.creative);
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
        if (!this.creative && level.getBlockEntity(pos) instanceof EngineBlockEntity engine
            && engine.insertFuel(stack, level.fuelValues())) {
            if (!level.isClientSide()) {
                stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
        BlockEntityType<T> type) {
        if (level instanceof ServerLevel serverLevel) {
            return this.createTicker(type,
                (innerLevel, pos, innerState, entity) -> EngineBlockEntity.serverTick(serverLevel, pos, innerState, entity));
        }
        return this.createTicker(type, EngineBlockEntity::clientTick);
    }

    /** The plain-{@link Block} counterpart of {@link BaseEntityBlock#createTickerHelper}: the ticker only matches when
     * the queried type is exactly this engine's block entity type. */
    @SuppressWarnings("unchecked")
    private <T extends BlockEntity> BlockEntityTicker<T> createTicker(BlockEntityType<T> given,
        BlockEntityTicker<? super EngineBlockEntity> ticker) {
        return given == this.type.get() ? (BlockEntityTicker<T>) ticker : null;
    }
}
