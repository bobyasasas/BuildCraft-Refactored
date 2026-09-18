/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import buildcraft.core.blockentity.MarkerVolumeBlockEntity;

/**
 * The volume marker block (M4.5 port of legacy {@code buildcraft.core.block.BlockMarkerVolume}, under the unchanged id
 * {@code buildcraftcore:marker_volume} that the M2.4a registry parity registered as a placeholder). Replaces the
 * placeholder plain {@link net.minecraft.world.level.block.Block}: the block entity supplies the marker connection
 * bookkeeping and the redstone-driven signal lines. The legacy 6-way {@code BLOCK_FACING_6} placement property and its
 * small pillar shapes are not ported (the frozen blockstate palette for this id has no facing variants, and the laser
 * visuals do not depend on the attachment face).
 *
 * <p>Right-click tries to connect (legacy {@code BlockMarkerVolume#use} &rarr; {@code onManualConnectionAttempt});
 * a redstone neighbour change toggles the signal lines (legacy {@code checkSignalState}).
 */
public class MarkerVolumeBlock extends BaseEntityBlock {

    public static final MapCodec<MarkerVolumeBlock> CODEC = simpleCodec(MarkerVolumeBlock::new);

    public MarkerVolumeBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MarkerVolumeBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
        BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MarkerVolumeBlockEntity marker) {
            marker.onManualConnectionAttempt();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
        Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MarkerVolumeBlockEntity marker) {
            // legacy checkSignalState: the signal lines mirror the redstone input
            marker.updateSignalState(level.hasNeighborSignal(pos));
        }
    }
}
