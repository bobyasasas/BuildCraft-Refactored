/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import buildcraft.core.BcBlockEntities;
import buildcraft.core.blockentity.KinesisPipeBlockEntity;

/**
 * The M2.2c wooden kinesis pipe slice block (buildcraftcore:pipe_kinesis_wood), vertical slice edition. Deliberately
 * named {@code KinesisPipeBlock} (not {@code PipeBlock}, which is the vanilla stem-attachment base class) and placed as
 * a plain {@link BaseEntityBlock} because the slice needs a ticking block entity, not connection properties: the visual
 * is a fixed thin-column placeholder model ({@code assets/buildcraftcore/models/block/pipe_kinesis_wood.json}), and the
 * auto-connecting pipe model + BER system is M2.7.
 *
 * <p>This is the pattern template for the M2.4/M2.9 transport module migration (legacy {@code BlockGenericPipe} +
 * {@code PipeTransportPower}); connection states, pipe contents and the network graph are not migrated here (see
 * {@link KinesisPipeBlockEntity} for the slice diffusion model).
 *
 * <p><b>M2.11 redstone (legacy {@code BlockPipeHolder} signal surface):</b> the pipe is a signal source while a gate
 * ({@code KinesisPipeBlockEntity#getGate}) has latched a redstone output: {@link #getSignal}/{@link #getDirectSignal}
 * report the gate's level towards the gate's face (the 26.1.2 {@code direction} parameter points <em>from</em> the
 * querying neighbour <em>to</em> this block &mdash; see {@code SignalGetter#getBestNeighborSignal} &mdash; so the
 * queried pipe face is {@code direction.getOpposite()}, exactly the mapping legacy uses), and
 * {@link #canConnectRedstone} mirrors legacy's pluggable-aware connect check. {@link #getShape} unions the gate's
 * legacy bounding box onto the pipe column so the attached gate is selectable/solid like a legacy
 * {@code isBlocking()} pluggable.
 */
public class KinesisPipeBlock extends BaseEntityBlock {

    public static final MapCodec<KinesisPipeBlock> CODEC = simpleCodec(KinesisPipeBlock::new);

    /** Placeholder selection/collision shape matching the thin-column slice model. Real pipe shapes are M2.7. */
    private static final VoxelShape SHAPE = Block.box(6.0, 0.0, 6.0, 10.0, 16.0, 10.0);

    /**
     * The gate's bounding box per attached face, lifted from legacy {@code PluggableGate.BOXES} (5..11 pixel
     * cross-section, 2..4 pixel band on the face).
     */
    private static final VoxelShape[] GATE_BOXES = {
        Block.box(5.0, 2.0, 5.0, 11.0, 4.0, 11.0), // DOWN
        Block.box(5.0, 12.0, 5.0, 11.0, 14.0, 11.0), // UP
        Block.box(5.0, 5.0, 2.0, 11.0, 11.0, 4.0), // NORTH
        Block.box(5.0, 5.0, 12.0, 11.0, 11.0, 14.0), // SOUTH
        Block.box(2.0, 5.0, 5.0, 4.0, 11.0, 11.0), // WEST
        Block.box(12.0, 5.0, 5.0, 14.0, 11.0, 11.0), // EAST
    };

    /** Pipe column &cup; gate box, computed lazily per face (the common no-gate path keeps returning {@link #SHAPE}). */
    private static final VoxelShape[] SHAPE_WITH_GATE = new VoxelShape[6];

    public KinesisPipeBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KinesisPipeBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof KinesisPipeBlockEntity pipe) {
            Direction gateSide = pipe.getGateSide();
            if (gateSide != null) {
                int ordinal = gateSide.ordinal();
                if (SHAPE_WITH_GATE[ordinal] == null) {
                    SHAPE_WITH_GATE[ordinal] = Shapes.or(SHAPE, GATE_BOXES[ordinal]);
                }
                return SHAPE_WITH_GATE[ordinal];
            }
        }
        return SHAPE;
    }

    // ---------------------------------------------------------------------
    // M2.11 gate redstone emission (legacy BlockPipeHolder signal surface)
    // ---------------------------------------------------------------------

    @Override
    protected boolean isSignalSource(BlockState state) {
        // legacy BlockPipeHolder#isSignalSource: always true; consumers read the gate's per-face output
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return this.getDirectSignal(state, level, pos, direction);
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        // direction points from the querying neighbour to this block, so the queried pipe face is its opposite
        // (legacy BlockPipeHolder#getDirectSignal: tile.getRedstoneOutput(side.getOpposite()))
        if (level.getBlockEntity(pos) instanceof KinesisPipeBlockEntity pipe) {
            return pipe.getGateRedstoneOutput(direction.getOpposite());
        }
        return 0;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        // legacy BlockPipeHolder#canConnectRedstone: the queried face carries a gate that connects to redstone
        if (direction == null) {
            return false;
        }
        return level.getBlockEntity(pos) instanceof KinesisPipeBlockEntity pipe
            && pipe.getGateSide() == direction.getOpposite();
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level instanceof ServerLevel serverLevel) {
            return createTickerHelper(
                type,
                BcBlockEntities.PIPE_KINESIS_WOOD.value(),
                (innerLevel, pos, innerState, entity) -> KinesisPipeBlockEntity.serverTick(serverLevel, pos, innerState, entity));
        }
        return null;
    }
}
