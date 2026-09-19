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
import buildcraft.builders.blockentity.LibraryBlockEntity;

/**
 * The M4.17 electronic library block ({@code buildcraftbuilders:library}; legacy counterpart:
 * {@code buildcraft.builders.block.BlockElectronicLibrary}). Deliberately no block state properties: the M2.4c
 * baseline blockstate palette must stay byte-identical for the strict registry diff (the {@link QuarryBlock}
 * precedent).
 *
 * <p>Right-clicking prints the [M417] index of the stored blueprints (the legacy GUI list is not migrated); the shelf
 * itself runs through the item capability (a hopper, a pipe or the probe rig moves blueprints in and out).
 */
public class LibraryBlock extends BaseEntityBlock {

    public static final MapCodec<LibraryBlock> CODEC = simpleCodec(LibraryBlock::new);

    public LibraryBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LibraryBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof LibraryBlockEntity library) {
            library.logIndex("right-click");
        }
        return InteractionResult.SUCCESS;
    }
}
