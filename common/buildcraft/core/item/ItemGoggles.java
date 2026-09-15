/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.item;

import buildcraft.lib.item.IItemBuildCraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Consumer;

//public class ItemGoggles extends ArmorItem implements IItemBuildCraft, ISpecialArmor
public class ItemGoggles extends ArmorItem implements IItemBuildCraft {
    private final String idBC;

    public ItemGoggles(String idBC, Item.Properties properties) {
        super(ArmorMaterials.CHAIN, Type.HELMET, properties);
        this.idBC = idBC;
        init();
    }

    @Override
    public String getIdBC() {
        return idBC;
    }

    private String unlocalizedName;

    @Override
    public void setUnlocalizedName(String unlocalizedName) {
        this.unlocalizedName = unlocalizedName;
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return this.unlocalizedName;
    }

//    @Override

    @Override
    public int getDamage(ItemStack stack) {
        return 0;
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<T> onBroken) {
        // Invulnerable goggles
        return 0;
    }

    public ResourceLocation getRegistryName() {
        return ForgeRegistries.ITEMS.getKey(this);
    }
}
