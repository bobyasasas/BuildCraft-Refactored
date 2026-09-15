/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.builders.item;

import buildcraft.api.core.BCLog;
import buildcraft.api.core.InvalidInputDataException;
import buildcraft.api.schematics.ISchematicBlock;
import buildcraft.api.schematics.SchematicBlockContext;
import buildcraft.builders.snapshot.SchematicBlockManager;
import buildcraft.lib.inventory.InventoryWrapper;
import buildcraft.lib.item.ItemBC_Neptune;
import buildcraft.lib.misc.NBTUtilBC;
import buildcraft.lib.misc.SoundUtil;
import buildcraft.lib.misc.StackUtil;
import buildcraft.lib.misc.WorldUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import java.util.List;

public class ItemSchematicSingle extends ItemBC_Neptune {
    public static final int DAMAGE_CLEAN = 0;
    public static final int DAMAGE_USED = 1;
    public static final String NBT_KEY = "schematic";

    public ItemSchematicSingle(String idBC, Item.Properties properties) {
        super(idBC, properties);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return stack.getDamageValue() == DAMAGE_CLEAN ? 16 : super.getMaxStackSize(stack);
    }

//    @Override

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = StackUtil.asNonNull(player.getItemInHand(hand));
        if (world.isClientSide) {
            return new InteractionResultHolder<>(InteractionResult.PASS, stack);
        }
        if (player.isShiftKeyDown()) {
            CompoundTag itemData = NBTUtilBC.getItemData(stack);
            itemData.remove(NBT_KEY);
            if (itemData.isEmpty()) {
                stack.setTag(null);
            }
            stack.setDamageValue(DAMAGE_CLEAN);
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
        }
        return new InteractionResultHolder<>(InteractionResult.PASS, stack);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Player player = ctx.getPlayer();
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        InteractionHand hand = ctx.getHand();
        Direction side = ctx.getClickedFace();
        Vec3 vec3Pos = ctx.getClickLocation();
        if (world.isClientSide) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            CompoundTag itemData = NBTUtilBC.getItemData(StackUtil.asNonNull(stack));
            itemData.remove(NBT_KEY);
            if (itemData.isEmpty()) {
                stack.setTag(null);
            }
            stack.setDamageValue(DAMAGE_CLEAN);
            return InteractionResult.SUCCESS;
        }
        int damage = stack.getDamageValue();
        if (damage != DAMAGE_USED) {
            BlockState state = world.getBlockState(pos);
            ISchematicBlock schematicBlock = SchematicBlockManager.getSchematicBlock(new SchematicBlockContext(
                    world,
                    pos,
                    pos,
                    state,
                    state.getBlock()
            ));
            if (schematicBlock.isAir()) {
                return InteractionResult.FAIL;
            }
            NBTUtilBC.getItemData(stack).put(NBT_KEY, SchematicBlockManager.writeToNBT(schematicBlock));
            stack.setDamageValue(DAMAGE_USED);
            return InteractionResult.SUCCESS;
        } else {
            BlockPos placePos = pos;
            boolean replaceable = world.getBlockState(pos).canBeReplaced();
            if (!replaceable) {
                placePos = placePos.relative(side);
            }
            if (!WorldUtil.mayPlace(world, world.getBlockState(pos).getBlock(), placePos, false, side, null)) {
                return InteractionResult.FAIL;
            }
            if (replaceable && !world.isEmptyBlock(placePos)) {
                world.setBlock(placePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            try {
                ISchematicBlock schematicBlock = getSchematic(stack);
                if (schematicBlock != null) {
                    if (!schematicBlock.isBuilt(world, placePos) && schematicBlock.canBuild(world, placePos)) {
                        List<FluidStack> requiredFluids = schematicBlock.computeRequiredFluids();
                        List<ItemStack> requiredItems = schematicBlock.computeRequiredItems();
                        if (requiredFluids.isEmpty()) {
                            InventoryWrapper itemTransactor = new InventoryWrapper(player.getInventory());
                            if (StackUtil.mergeSameItems(requiredItems).stream().noneMatch(s ->
                                    itemTransactor.extract(
                                            extracted -> StackUtil.canMerge(s, extracted),
                                            s.getCount(),
                                            s.getCount(),
                                            true
                                    ).isEmpty()
                            ))
                            {
                                if (schematicBlock.build(world, placePos)) {
                                    StackUtil.mergeSameItems(requiredItems).forEach(s ->
                                            itemTransactor.extract(
                                                    extracted -> StackUtil.canMerge(s, extracted),
                                                    s.getCount(),
                                                    s.getCount(),
                                                    false
                                            )
                                    );
                                    SoundUtil.playBlockPlace(world, placePos);
                                    player.swing(hand);
                                    return InteractionResult.SUCCESS;
                                }
                            } else {
//                                player.sendStatusMessage(
//                                        Component.literalString(
//                                                "Not enough items. Total needed: " +
//                                        ),
//                                        true
                                MutableComponent message = Component.translatable("chat.buildcraft.schematic_single.not_enough_item").append("\n");
                                List<MutableComponent> requiredItemNames = StackUtil.mergeSameItems(requiredItems).stream()
                                        .map(s -> Component.literal("    ").append(s.getDisplayName()).append(" x " + s.getCount())).toList();
                                for (int index = 0; index < requiredItemNames.size(); index++) {
                                    message.append(requiredItemNames.get(index));
                                    if (index != requiredItemNames.size() - 1) {
                                        message.append("\n");
                                    }
                                }
                                player.sendSystemMessage(message);
                            }
                        } else {
//                            player.sendStatusMessage(
//                                    Component.literalString("Schematic requires fluids"),
//                                    true
                            player.sendSystemMessage(
                                    Component.translatable("chat.buildcraft.schematic_single.require_fluid")
                            );
                        }
                    }
                }
            } catch (InvalidInputDataException e) {
//                player.sendStatusMessage(
//                        Component.literalString("Invalid schematic: " + e.getMessage()),
//                        true
                player.sendSystemMessage(
                        Component.translatable("chat.buildcraft.schematic_single.invalid").append(e.getMessage())
                );
                e.printStackTrace();
                BCLog.logger.warn("[builders.schematic_single] Invalid schematic ", e);
            }
            return InteractionResult.FAIL;
        }
    }

    public static ISchematicBlock getSchematic(@Nonnull ItemStack stack) throws InvalidInputDataException {
        if (stack.getItem() instanceof ItemSchematicSingle) {
            return SchematicBlockManager.readFromNBT(NBTUtilBC.getItemData(stack).getCompound(NBT_KEY));
        }
        return null;
    }

    public static ISchematicBlock getSchematicSafe(@Nonnull ItemStack stack) {
        if ((!stack.hasTag()) || !(stack.getTag().contains("name"))) {
            return null;
        }
        // BC 1.12.2
        try {
            return getSchematic(stack);
        } catch (InvalidInputDataException e) {
            BCLog.logger.warn("[builders.schematic_single] Invalid schematic " + e.getMessage());
            return null;
        }
    }
}
