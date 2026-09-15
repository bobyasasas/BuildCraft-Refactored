/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.block;

import buildcraft.api.properties.BuildCraftProperties;
import buildcraft.lib.block.BlockBCBase_Neptune;
import buildcraft.lib.misc.RotationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BlockFrame extends BlockBCBase_Neptune {
    public static final Map<Direction, Property<Boolean>> CONNECTED_MAP = BuildCraftProperties.CONNECTED_MAP;

    public static final VoxelShape BASE_AABB = Shapes.box(4 / 16D, 4 / 16D, 4 / 16D, 12 / 16D, 12 / 16D, 12 / 16D);
    public static final VoxelShape CONNECTION_AABB = Shapes.box(4 / 16D, 0 / 16D, 4 / 16D, 12 / 16D, 4 / 16D, 12 / 16D);

    public BlockFrame(String idBC, BlockBehaviour.Properties properties) {
        super(idBC, properties);
        this.registerDefaultState(
                this.getStateDefinition().any()
                        .setValue(BuildCraftProperties.CONNECTED_UP, false)
                        .setValue(BuildCraftProperties.CONNECTED_DOWN, false)
                        .setValue(BuildCraftProperties.CONNECTED_EAST, false)
                        .setValue(BuildCraftProperties.CONNECTED_WEST, false)
                        .setValue(BuildCraftProperties.CONNECTED_NORTH, false)
                        .setValue(BuildCraftProperties.CONNECTED_SOUTH, false)
        );
    }

    @Override
    protected void addProperties(List<Property<?>> properties) {
        super.addProperties(properties);
        properties.addAll(CONNECTED_MAP.values());
    }

    @Override
    public BlockState updateShape(BlockState thisState, Direction direction, BlockState otherState, LevelAccessor world, BlockPos thisPos, BlockPos otherPos) {
        return getActualState(thisState, world, thisPos);
    }

    public BlockState getActualState(BlockState state, LevelAccessor world, BlockPos pos) {
        for (Direction side : CONNECTED_MAP.keySet()) {
            Block block = world.getBlockState(pos.relative(side)).getBlock();
            state = state.setValue(CONNECTED_MAP.get(side), block instanceof BlockFrame || block instanceof BlockQuarry);
        }
        return state;
    }

//    @Override

//    @Override

    @Override
    public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter world, BlockPos pos) {
        return false;
    }

//    @Override

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter world, BlockPos pos) {
        return true;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter world, BlockPos pos) {
        return 1.0F;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean skipRendering(BlockState thisState, BlockState otherState, Direction side) {
        BlockState actualState = thisState;
        Direction[] facings = CONNECTED_MAP.keySet().stream()
                .filter(facing -> actualState.getValue(CONNECTED_MAP.get(facing)))
                .toArray(Direction[]::new);
        if (facings.length == 1) {
            return side == facings[0];
        } else if (facings.length == 2 && facings[0] == facings[1].getOpposite()) {
            return side == facings[0] || side == facings[1];
        }
        return false;
    }

//    @Override
//        CONNECTED_MAP.forEach((side, property) ->

    @Override
    public VoxelShape getShape(BlockState actualState, BlockGetter world, BlockPos pos, CollisionContext context) {
        List<VoxelShape> shapes = new ArrayList<>();
        CONNECTED_MAP.forEach((side, property) ->
        {
            if (actualState.getValue(property)) {
                shapes.add(Shapes.create(RotationUtil.rotateAABB(CONNECTION_AABB.bounds(), side)));
            }
        });
        return Shapes.or(BASE_AABB, shapes.toArray(new VoxelShape[0]));
    }

//    @Override

    @Override
    public VoxelShape getCollisionShape(BlockState actualState, BlockGetter world, BlockPos pos, CollisionContext context) {
        VoxelShape shape = super.getCollisionShape(actualState, world, pos, context);
        List<VoxelShape> shapes = new ArrayList<>();
        CONNECTED_MAP.keySet().stream()
                .filter(side -> actualState.getValue(CONNECTED_MAP.get(side)))
                .map(side -> Shapes.create(RotationUtil.rotateAABB(CONNECTION_AABB.bounds(), side)))
                .forEach(box -> shapes.add(Shapes.create(box.bounds().move(pos))));
        return Shapes.or(shape, shapes.toArray(new VoxelShape[0]));
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, Builder builder) {
        return Collections.emptyList();
    }
}
