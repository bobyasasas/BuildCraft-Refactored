/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.core.item;

import buildcraft.api.blocks.CustomPaintHelper;
import buildcraft.core.BCCoreItems;
import buildcraft.lib.item.ItemBC_Neptune;
import buildcraft.lib.misc.ColourUtil;
import buildcraft.lib.misc.ParticleUtil;
import buildcraft.lib.misc.SoundUtil;
import buildcraft.lib.misc.StackUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemPaintbrush_BC8 extends ItemBC_Neptune {
    private static final String DAMAGE = "damage";
    private static final int MAX_USES = 64;

    private final DyeColor colour;

    public ItemPaintbrush_BC8(String idBC, Item.Properties properties, DyeColor colour) {
        super(idBC, properties);
        this.colour = colour;
    }

//    @Override

//    @Override

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        InteractionHand hand = context.getHand();
        Direction facing = context.getHorizontalDirection();
        Vec3 hitPos = context.getClickLocation();
        Brush brush = new Brush(stack);
        if (brush.useOnBlock(world, pos, world.getBlockState(pos), hitPos, facing, player)) {
            ItemStack newStack = brush.save(stack);
            if (!newStack.isEmpty()) {
                player.setItemInHand(hand, newStack);
            }
            // We just changed the damage NBT value
            player.getInventory().setChanged();
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    public Brush getBrushFromStack(ItemStack stack) {
        return new Brush(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (this.colour != null) {
            String colourStr = ColourUtil.getTextFullTooltipSpecial(this.colour) + " ";
            return Component.literal(colourStr).append(Component.translatable(this.unlocalizedName));
        } else {
            return Component.translatable(this.unlocalizedName);
        }
    }

//    @Override

    @Override
    public int getDamage(ItemStack stack) {
        Brush brush = new Brush(stack);
        return MAX_USES - brush.usesLeft;
    }

    @Override
    public void setDamage(ItemStack stack, int damage) {
        // Explicitly disallow this- some core use cases mistake this for metadata and fail
    }

    @Override
    public boolean isDamaged(ItemStack stack) {
        return this.colour != null && stack.getDamageValue() > 0;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return isDamaged(stack);
    }

//    @Override

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        if (this.colour != null) {
            Brush brush = new Brush(stack);
            tooltip.add(Component.literal(brush.usesLeft + " / " + MAX_USES));
        }
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return MAX_USES;
    }

//    @Override

    /** Delegate class for handling */
    public class Brush {
        public DyeColor colour;
        public int usesLeft;

        public Brush(DyeColor colour) {
            this.colour = colour;
            usesLeft = MAX_USES;
        }

        public Brush(ItemStack stack) {
            DyeColor meta = ((ItemPaintbrush_BC8) stack.getItem()).colour;
            if (meta != null) {
                colour = meta;
                CompoundTag nbt = stack.getTag();
                if (nbt == null) {
                    usesLeft = MAX_USES;
                } else {
                    usesLeft = MAX_USES - nbt.getByte(DAMAGE);
                }
            } else {
                usesLeft = 0;
            }
        }

        @Nonnull
        public ItemStack save() {
            return save(StackUtil.EMPTY);
        }

        @Nonnull
        public ItemStack save(@Nonnull ItemStack existing) {
            ItemStack stack = existing;
            if (existing.isEmpty() || ColourUtil.getStackColourFromTag(stack) != getMeta()) {
                stack = new ItemStack(ItemPaintbrush_BC8.this, 1);
            }
            if (usesLeft != MAX_USES && colour != null) {
                CompoundTag nbt = stack.getTag();
                if (nbt == null) {
                    nbt = new CompoundTag();
                    stack.setTag(nbt);
                }
                nbt.putByte(DAMAGE, (byte) (MAX_USES - usesLeft));
            } else if (usesLeft == 0) {
                stack = new ItemStack(BCCoreItems.colourBrushMap.get(null).get());
            }
            return stack == existing ? StackUtil.EMPTY : stack;
        }

        public DyeColor getMeta() {
            return (usesLeft <= 0 || colour == null) ? null : colour;
        }

        public boolean useOnBlock(Level world, BlockPos pos, BlockState state, Vec3 hitPos, Direction side, Player player) {
            if (colour != null && usesLeft <= 0) {
                return false;
            }

            InteractionResult result = CustomPaintHelper.INSTANCE.attemptPaintBlock(world, pos, state, hitPos, side, colour);

            if (result == InteractionResult.SUCCESS) {
                ParticleUtil.showChangeColour(world, hitPos, colour);
                SoundUtil.playChangeColour(world, pos, colour);

                if (!player.isCreative()) {
                    usesLeft--;
                }

                if (usesLeft <= 0) {
                    colour = null;
                    usesLeft = 0;
                }
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return "[" + usesLeft + " of " + (colour == null ? "nothing" : colour.getName()) + "]";
        }
    }
}
