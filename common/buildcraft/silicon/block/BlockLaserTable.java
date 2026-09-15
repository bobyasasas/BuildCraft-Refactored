/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.block;

import buildcraft.api.enums.EnumLaserTableType;
import buildcraft.api.mj.ILaserTargetBlock;
import buildcraft.api.tiles.IBCTileMenuProvider;
import buildcraft.lib.block.BlockBCTile_Neptune;
import buildcraft.lib.block.IBlockWithTickableTE;
import buildcraft.lib.misc.MessageUtil;
import buildcraft.lib.tile.TileBC_Neptune;
import buildcraft.silicon.tile.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockLaserTable extends BlockBCTile_Neptune<TileLaserTableBase> implements ILaserTargetBlock, IBlockWithTickableTE<TileLaserTableBase> {
    private final EnumLaserTableType type;

    public BlockLaserTable(String idBC, BlockBehaviour.Properties props, EnumLaserTableType type) {
        super(idBC, props);
        this.type = type;
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

//    @Override

//    @Override

    @Override
    public TileBC_Neptune newBlockEntity(BlockPos pos, BlockState state) {
        switch (type) {
            case ASSEMBLY_TABLE:
                return new TileAssemblyTable(pos, state);
            case ADVANCED_CRAFTING_TABLE:
                return new TileAdvancedCraftingTable(pos, state);
            case INTEGRATION_TABLE:
                return new TileIntegrationTable(pos, state);
            case CHARGING_TABLE:
                return new TileChargingTable(pos, state);
            case PROGRAMMING_TABLE:
                return new TileProgrammingTable_Neptune(pos, state);
        }
        return null;
    }

    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 9.0D, 16.0D);

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
//            case ASSEMBLY_TABLE:
//            case ADVANCED_CRAFTING_TABLE:
//            case INTEGRATION_TABLE:
//            case CHARGING_TABLE:
//            case PROGRAMMING_TABLE:
        if (!world.isClientSide) {
            if (world.getBlockEntity(pos) instanceof IBCTileMenuProvider tile) {
                MessageUtil.serverOpenTileGui(player, tile, pos);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.SUCCESS;
    }
}
