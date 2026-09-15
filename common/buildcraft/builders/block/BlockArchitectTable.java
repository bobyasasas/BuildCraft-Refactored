/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.builders.block;

import buildcraft.api.properties.BuildCraftProperties;
import buildcraft.builders.tile.TileArchitectTable;
import buildcraft.lib.block.BlockBCTile_Neptune;
import buildcraft.lib.block.IBlockWithFacing;
import buildcraft.lib.block.IBlockWithTickableTE;
import buildcraft.lib.misc.MessageUtil;
import buildcraft.lib.tile.TileBC_Neptune;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

//public class BlockArchitectTable extends BlockBCTile_Neptune implements IBlockWithFacing
public class BlockArchitectTable extends BlockBCTile_Neptune<TileArchitectTable> implements IBlockWithFacing, IBlockWithTickableTE<TileArchitectTable> {
    public static final Property<Boolean> PROP_VALID = BuildCraftProperties.VALID;


    public BlockArchitectTable(String idBC, BlockBehaviour.Properties properties) {
        super(idBC, properties);
        registerDefaultState(
                defaultBlockState()
                        .setValue(PROP_VALID, Boolean.TRUE)
        );
    }

    @Override
    protected void addProperties(List<Property<?>> properties) {
        super.addProperties(properties);
        properties.add(PROP_VALID);
    }

//    @Override

//    @Override

    @Override
    public TileBC_Neptune newBlockEntity(BlockPos pos, BlockState state) {
        return new TileArchitectTable(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!world.isClientSide) {
            if (world.getBlockEntity(pos) instanceof TileArchitectTable tile) {
                MessageUtil.serverOpenTileGui(player, tile);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean canBeRotated(LevelAccessor world, BlockPos pos, BlockState state) {
        return false;
    }
}
