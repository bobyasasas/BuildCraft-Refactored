/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import buildcraft.builders.BcBuildersBlockEntities;
import buildcraft.builders.blockentity.ConstructionMarkerBlockEntity;

/**
 * The M4.17 construction marker block ({@code buildcraftbuilders:marker_construction}; legacy counterpart:
 * {@code buildcraft.builders.block.BlockMarkerConstruction}). Deliberately no block state properties: the M2.4c
 * baseline blockstate palette must stay byte-identical for the strict registry diff (the {@link QuarryBlock}
 * precedent).
 *
 * <p>Right-clicking (re-)tries the pairing and logs the [M417] status line; pairing also happens automatically on
 * load ({@code ConstructionMarkerBlockEntity#onLoad}). The box visuals ride the client renderer.
 */
public class ConstructionMarkerBlock extends BaseEntityBlock {

    public static final MapCodec<ConstructionMarkerBlock> CODEC = simpleCodec(ConstructionMarkerBlock::new);

    public ConstructionMarkerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConstructionMarkerBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ConstructionMarkerBlockEntity marker) {
            marker.onManualConnectionAttempt();
        }
        return InteractionResult.SUCCESS;
    }
}
