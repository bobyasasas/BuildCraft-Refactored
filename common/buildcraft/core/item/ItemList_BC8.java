/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.core.item;

import buildcraft.api.items.IList;
import buildcraft.core.BCCoreItems;
import buildcraft.core.BCCoreMenuTypes;
import buildcraft.core.list.ContainerList;
import buildcraft.lib.item.ItemBC_Neptune;
import buildcraft.lib.list.ListHandler;
import buildcraft.lib.misc.AdvancementUtil;
import buildcraft.lib.misc.MessageUtil;
import buildcraft.lib.misc.NBTUtilBC;
import buildcraft.lib.misc.StackUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

//public class ItemList_BC8 extends ItemBC_Neptune implements IList
public class ItemList_BC8 extends ItemBC_Neptune implements IList, MenuProvider {
    private static final ResourceLocation ADVANCEMENT = new ResourceLocation("buildcraftcore:list");

    public static final String NBT_KEY = "label";

    public ItemList_BC8(String idBC, Item.Properties properties) {
        super(idBC, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        AdvancementUtil.unlockAdvancement(player, ADVANCEMENT);
        MessageUtil.serverOpenItemGui(player, BCCoreItems.list.get());
        return new InteractionResultHolder<>(InteractionResult.SUCCESS, player.getItemInHand(hand));
    }

//    @Override

//    @Override

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag flag) {
        String name = getName_INamedItem(StackUtil.asNonNull(stack));
        if (StringUtil.isNullOrEmpty(name)) return;
        tooltip.add(Component.literal(ChatFormatting.ITALIC + name));
    }

    // IList

    @Override
    public String getName_INamedItem(@Nonnull ItemStack stack) {
        return NBTUtilBC.getItemData(stack).getString(NBT_KEY);
    }

    @Override
    public boolean setName(@Nonnull ItemStack stack, String name) {
        NBTUtilBC.getItemData(stack).putString(NBT_KEY, name);
        return true;
    }

    @Override
    public boolean matches(@Nonnull ItemStack stackList, @Nonnull ItemStack item) {
        return ListHandler.matches(stackList, item);
    }

    public static boolean isUsed(ItemStack stack) {
        return ListHandler.hasItems(StackUtil.asNonNull(stack));
    }

    // MenuProvider

    @Override
    public Component getDisplayName() {
        return Component.literal("list");
    }

    @Nullable
    @Override
    public ContainerList createMenu(int id, Inventory inv, Player player) {
        return new ContainerList(BCCoreMenuTypes.LIST, id, player);
    }
}
