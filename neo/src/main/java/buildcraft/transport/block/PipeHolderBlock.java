/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.transport.block;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jspecify.annotations.Nullable;
import buildcraft.transport.BcTransportBlockEntities;
import buildcraft.transport.BcTransportItems;
import buildcraft.transport.blockentity.PipeHolderBlockEntity;
import buildcraft.transport.pipe.BcPipeFamilies.Family;
import buildcraft.transport.pipe.BcPipeFamilies.FlowKind;

/**
 * The M4.6 single pipe block ({@code buildcrafttransport:pipe_holder}, legacy {@code BlockGenericPipe} +
 * {@code BlockPipeHolder} visual slice). Every pipe family is this one block: the family/colour/plugs/connections live
 * in {@link PipeHolderBlockEntity} and the world visual is drawn entirely by the pipe BER, so the block renders as
 * {@link RenderShape#INVISIBLE} (the 26.1.2 BER-driven-block pattern) instead of using a baked model.
 *
 * <p>The selection/collision shape is the legacy pipe geometry: the 8&times;8&times;8 centre cube
 * ({@code PipeBaseModelGenStandard} bounds 0.25..0.75) plus one 8&times;8 arm per connected face plus the plug plates,
 * computed from the block entity's connection set and cached per state key.
 */
public class PipeHolderBlock extends BaseEntityBlock {

    public static final MapCodec<PipeHolderBlock> CODEC = simpleCodec(PipeHolderBlock::new);

    /** The pipe centre cube, {@code PipeBaseModelGenStandard} bounds 0.25..0.75. */
    private static final VoxelShape CENTER = Block.box(4.0, 4.0, 4.0, 12.0, 12.0, 12.0);

    /** One arm per face, 0.75..1.0 (or 0..0.25) along the axis, 4..12 across, matching the rendered arms. */
    private static final VoxelShape[] ARM_SHAPES = {
        Block.box(4.0, 0.0, 4.0, 12.0, 4.0, 12.0), // DOWN
        Block.box(4.0, 12.0, 4.0, 12.0, 16.0, 12.0), // UP
        Block.box(4.0, 4.0, 0.0, 12.0, 12.0, 4.0), // NORTH
        Block.box(4.0, 4.0, 12.0, 12.0, 12.0, 16.0), // SOUTH
        Block.box(0.0, 4.0, 4.0, 4.0, 12.0, 12.0), // WEST
        Block.box(12.0, 4.0, 4.0, 16.0, 12.0, 12.0), // EAST
    };

    /**
     * The pluggable plate per face, lifted from the baseline pluggable jsonbc geometry (blocker box
     * {@code 2,4,4 -> 4.01,12,12}, a 2-pixel plate on the pipe centre's west side).
     */
    private static final VoxelShape[] PLUG_BOXES = {
        Block.box(4.0, 0.0, 4.0, 12.0, 2.0, 12.0), // DOWN
        Block.box(4.0, 14.0, 4.0, 12.0, 16.0, 12.0), // UP
        Block.box(4.0, 4.0, 2.0, 12.0, 12.0, 4.0), // NORTH
        Block.box(4.0, 4.0, 12.0, 12.0, 12.0, 14.0), // SOUTH
        Block.box(2.0, 4.0, 4.0, 4.0, 12.0, 12.0), // WEST
        Block.box(12.0, 4.0, 4.0, 14.0, 12.0, 12.0), // EAST
    };

    /** Selection/collision shape cache, keyed by connections (bits 0-5) and plugs (bits 6-11). */
    private static final Map<Integer, VoxelShape> SHAPE_CACHE = new ConcurrentHashMap<>();

    public PipeHolderBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PipeHolderBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        // the pipe visual is 100% BER (arms, flow, plugs); no baked model exists for pipe_holder
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof PipeHolderBlockEntity pipe) {
            return shapeFor(pipe);
        }
        return CENTER;
    }

    /** The cached centre &cup; arms &cup; plugs union for the pipe's current connection/plug state. */
    private static VoxelShape shapeFor(PipeHolderBlockEntity pipe) {
        int key = 0;
        for (Direction face : Direction.values()) {
            int bit = 1 << face.ordinal();
            if (pipe.connections.contains(face)) {
                key |= bit;
            }
            if (pipe.getPlug(face) != PipeHolderBlockEntity.PLUG_NONE) {
                key |= bit << 6;
            }
        }
        return SHAPE_CACHE.computeIfAbsent(key, k -> {
            VoxelShape shape = CENTER;
            for (Direction face : Direction.values()) {
                int bit = 1 << face.ordinal();
                if ((k & bit) != 0) {
                    shape = Shapes.or(shape, ARM_SHAPES[face.ordinal()]);
                }
                if ((k & (bit << 6)) != 0) {
                    shape = Shapes.or(shape, PLUG_BOXES[face.ordinal()]);
                }
            }
            return shape;
        });
    }

    // ---------------------------------------------------------------------
    // fluid slice interaction (buckets, fluid pipe families only)
    // ---------------------------------------------------------------------

    @Override
    protected InteractionResult useItemOn(
        ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
        BlockHitResult hitResult
    ) {
        if (level.getBlockEntity(pos) instanceof PipeHolderBlockEntity pipe) {
            Family family = pipe.getFamily();
            if (family != null && family.flow == FlowKind.FLUIDS && stack.getItem() instanceof BucketItem bucket) {
                if (!level.isClientSide()) {
                    boolean filledBucketUsed = bucket.content != Fluids.EMPTY;
                    FluidStack content = filledBucketUsed
                        ? new FluidStack(bucket.content, PipeHolderBlockEntity.FLUID_CAPACITY)
                        : FluidStack.EMPTY;
                    if (pipe.interactWithBucket(content)) {
                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                            // the vanilla bucket-use swap: filled bucket in <-> empty bucket out
                            ItemStack other = filledBucketUsed
                                ? new ItemStack(Items.BUCKET)
                                : new ItemStack(bucket.content.getBucket());
                            if (!player.getInventory().add(other)) {
                                player.drop(other, false);
                            }
                        }
                    }
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    // ---------------------------------------------------------------------
    // drops (legacy: the pipe item plus every pluggable pop out on break)
    // ---------------------------------------------------------------------

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        if (movedByPiston) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof PipeHolderBlockEntity pipe) {
            Family family = pipe.getFamily();
            if (family != null) {
                String id = family.idPrefix()
                    + (pipe.getColour() == null ? "_colorless" : "_" + pipe.getColour().getName());
                DeferredItem<Item> item = BcTransportItems.PIPE_ITEMS.get(id);
                if (item != null) {
                    Block.popResource(level, pos, new ItemStack(item.value()));
                }
            }
            for (Direction face : Direction.values()) {
                byte plug = pipe.getPlug(face);
                if (plug == PipeHolderBlockEntity.PLUG_BLOCKER) {
                    Block.popResource(level, pos, new ItemStack(BcTransportItems.PLUG_BLOCKER.value()));
                } else if (plug == PipeHolderBlockEntity.PLUG_POWER_ADAPTOR) {
                    Block.popResource(level, pos, new ItemStack(BcTransportItems.PLUG_POWER_ADAPTOR.value()));
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // ticking
    // ---------------------------------------------------------------------

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level instanceof ServerLevel serverLevel) {
            return createTickerHelper(
                type,
                BcTransportBlockEntities.PIPE_HOLDER.value(),
                (innerLevel, pos, innerState, entity) -> PipeHolderBlockEntity.serverTick(serverLevel, pos, innerState, entity));
        }
        return null;
    }
}
