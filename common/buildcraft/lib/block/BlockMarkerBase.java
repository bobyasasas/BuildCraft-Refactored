/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.block;

import buildcraft.api.blocks.ICustomRotationHandler;
import buildcraft.api.properties.BuildCraftProperties;
import buildcraft.lib.tile.TileMarker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;
import java.util.EnumMap;
import java.util.Map;

public abstract class BlockMarkerBase extends BlockBCTile_Neptune<TileMarker> implements ICustomRotationHandler {
    private static final Map<Direction, VoxelShape> BOUNDING_BOXES = new EnumMap<>(Direction.class);

    private static final double halfWidth = 0.1;
    private static final double h = 0.65;
    // Little variables to make reading a *bit* more sane
    private static final double nw = 0.5 - halfWidth;
    private static final double pw = 0.5 + halfWidth;
    private static final double ih = 1 - h;
    private static final VoxelShape BOUNDING_BOX_DOWN = Shapes.box(nw, ih, nw, pw, 1, pw);
    private static final VoxelShape BOUNDING_BOX_UP = Shapes.box(nw, 0, nw, pw, h, pw);
    private static final VoxelShape BOUNDING_BOX_SOUTH = Shapes.box(nw, nw, 0, pw, pw, h);
    private static final VoxelShape BOUNDING_BOX_NORTH = Shapes.box(nw, nw, ih, pw, pw, 1);
    private static final VoxelShape BOUNDING_BOX_EAST = Shapes.box(0, nw, nw, h, pw, pw);
    private static final VoxelShape BOUNDING_BOX_WEST = Shapes.box(ih, nw, nw, 1, pw, pw);

    static {
        BOUNDING_BOXES.put(Direction.DOWN, BOUNDING_BOX_DOWN);
        BOUNDING_BOXES.put(Direction.UP, BOUNDING_BOX_UP);
        BOUNDING_BOXES.put(Direction.SOUTH, BOUNDING_BOX_SOUTH);
        BOUNDING_BOXES.put(Direction.NORTH, BOUNDING_BOX_NORTH);
        BOUNDING_BOXES.put(Direction.EAST, BOUNDING_BOX_EAST);
        BOUNDING_BOXES.put(Direction.WEST, BOUNDING_BOX_WEST);
    }

    public BlockMarkerBase(String idBC, BlockBehaviour.Properties props) {
        super(idBC, props);

        this.registerDefaultState(this.getStateDefinition().any()
                        .setValue(BuildCraftProperties.BLOCK_FACING_6, Direction.UP)
        );
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BuildCraftProperties.BLOCK_FACING_6);
    }

//    @Override

//    @Override

//    @Override

//    @Override

//    @Override

//    @Override

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter world, BlockPos pos) {
        return true;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter world, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter source, BlockPos pos, CollisionContext context) {
        return BOUNDING_BOXES.get(state.getValue(BuildCraftProperties.BLOCK_FACING_6));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        BlockState state = defaultBlockState();
        state = state.setValue(BuildCraftProperties.BLOCK_FACING_6, facing);
        return state;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        Direction facing = state.getValue(BuildCraftProperties.BLOCK_FACING_6);
        Direction sideOn = facing.getOpposite();
        BlockPos otherPos = pos.relative(sideOn);
        BlockState otherState = world.getBlockState(otherPos);
        return otherState.isFaceSturdy(world, otherPos, facing, SupportType.CENTER);
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block blockIn, BlockPos fromPos, boolean p_60514_) {
        if (state.getBlock() != this) {
            return;
        }
        if (!canSurvive(state, world, pos)) {
            world.destroyBlock(pos, true);
        }
    }

    @Override
    public InteractionResult attemptRotation(Level world, BlockPos pos, BlockState state, Direction sideWrenched) {
        if (state.getBlock() instanceof BlockMarkerBase) {// Just check to make sure we have the right block...
            Property<Direction> prop = BuildCraftProperties.BLOCK_FACING_6;
            return VanillaRotationHandlers.rotateEnumFacing(world, pos, state, prop, VanillaRotationHandlers.ROTATE_FACING);
        } else {
            return InteractionResult.PASS;
        }
    }
}
