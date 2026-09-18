/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

/**
 * M4.7 shared behaviour of the machine blocks whose fluids are filled/drained by hand: right-clicking with a fluid
 * container (a bucket, usually) delegates to the 26.1.2 transfer-API interaction helper against the fluid capability,
 * which first tries to fill the held item from the machine and then to drain it into the machine. The block entity's
 * per-side capability view decides which tank (if any) the interaction reaches, so the blocks need no fluid logic of
 * their own (the legacy {@code TileBC_Neptune} bucket-interaction path).
 */
public abstract class FactoryMachineBlock extends Block implements EntityBlock {

    protected FactoryMachineBlock(Properties properties) {
        super(properties);
    }

    @Override
    public abstract MapCodec<? extends Block> codec();

    @Override
    protected InteractionResult useItemOn(
        ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
        BlockHitResult hitResult
    ) {
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hitResult.getDirection(), null)) {
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }
}
