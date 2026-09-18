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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import buildcraft.core.blockentity.MarkerPathBlockEntity;

/**
 * The path marker block (M4.5 port of legacy {@code buildcraft.core.block.BlockMarkerPath}, under the unchanged id
 * {@code buildcraftcore:marker_path} that the M2.4a registry parity registered as a placeholder). Connections form
 * automatically on placement (see {@code MarkerBlockEntity#onLoad}); right-click additionally runs the manual
 * connection attempt &mdash; the legacy right-click action ({@code PathConnection#reverseDirection}) has no observable
 * effect on the ported visuals, which only depend on the chain's member positions, so this slot reuses the volume
 * marker's manual connect instead.
 */
public class MarkerPathBlock extends BaseEntityBlock {

    public static final MapCodec<MarkerPathBlock> CODEC = simpleCodec(MarkerPathBlock::new);

    public MarkerPathBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MarkerPathBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
        BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MarkerPathBlockEntity marker) {
            marker.onManualConnectionAttempt();
        }
        return InteractionResult.SUCCESS;
    }
}
