/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.item;

import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.block.BlockWaterGel;
import buildcraft.lib.item.ItemBC_Neptune;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class ItemWaterGel extends ItemBC_Neptune {
    protected static Random itemRand = new Random();

    public ItemWaterGel(String idBC, Item.Properties properties) {
        super(idBC, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Vec3 start = player.position().add(0, player.getEyeHeight(), 0);
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(7));
        BlockHitResult ray = world.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player));

        if (ray.getType() == HitResult.Type.MISS) {
            return new InteractionResultHolder<>(InteractionResult.FAIL, stack);
        }

        Block b = world.getBlockState(ray.getBlockPos()).getBlock();
        if (b != Blocks.WATER) {
            return new InteractionResultHolder<>(InteractionResult.FAIL, stack);
        }

        if (!player.isCreative()) {
            stack.shrink(1);
        }

        // Same as ItemSnowball
        world.playSound(null, player.getX(), player.getY(), player.getZ(),//
                SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL,//
                0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));

        if (!world.isClientSide) {
            world.setBlock(ray.getBlockPos(), BCFactoryBlocks.waterGel.get().defaultBlockState().setValue(BlockWaterGel.PROP_STAGE, BlockWaterGel.GelStage.SPREAD_0), Block.UPDATE_ALL);
            world.scheduleTick(ray.getBlockPos(), BCFactoryBlocks.waterGel.get(), 200);

            // TODO: Snowball stuff

        }

        return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
    }

}
