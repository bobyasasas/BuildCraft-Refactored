/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.robotics.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import buildcraft.lib.charge.BcChargeableItem;
import buildcraft.robotics.entity.EntityRobot;

/**
 * M4.17 robot item behaviour slice (legacy counterpart:
 * {@code buildcraft.robotics.item.ItemRobot}, charging half): the 17 robot items
 * ({@code robot_base} + the 16 board robots) become chargeable containers &mdash; the legacy
 * {@code IMjContainerItem} implementers the charging table was built around.
 *
 * <p><b>Energy storage</b> &mdash; legacy: a single {@code stored} long in the item root NBT
 * ({@code MjBattery.NBT_STORED}, written by {@code ItemRobot#createRobotStack} through
 * {@code NBTUtilBC.getItemData}). Slice: the same single long under the same {@link #NBT_STORED} key inside the
 * vanilla {@code minecraft:custom_data} component &mdash; the modern form of the legacy item root compound, so no new
 * data component type is registered.
 *
 * <p><b>Capacity</b> &mdash; the robot battery, 500,000 &micro;MJ on the slice scale (legacy
 * {@code EntityRobotBase.MAX_POWER} = 5,000 MJ &times;10<sup>&minus;4</sup>, the same value the migrated
 * {@link EntityRobot} battery uses). Faithful to the legacy {@code getMaxPowerStored}, the empty-board robot
 * ({@code robot_base}) reports capacity 0 and accepts no charge; every board robot charges through the shared
 * {@link BcChargeableItem#clampReceive} clamp. Stack sizes are the legacy ones too ({@code setMaxStackSize(1)} for
 * board robots, 16 for the empty-board robot), so a stack's single shared component always describes exactly one
 * robot's charge. The remaining legacy {@code ItemRobot} halves (robot spawning, boards, the charge durability bar)
 * stay with the robotics behaviour migration.
 */
public class BcRobotItem extends Item implements BcChargeableItem {

    /** The energy key inside {@code minecraft:custom_data} (legacy {@code MjBattery.NBT_STORED}). */
    public static final String NBT_STORED = "stored";

    /** True for the empty-board robot ({@code robot_base}), which the legacy API made unchargeable. */
    private final boolean emptyBoard;

    public BcRobotItem(Properties properties, boolean emptyBoard) {
        super(properties);
        this.emptyBoard = emptyBoard;
    }

    /** The stack's stored energy, &micro;MJ; a missing component/keys reads as 0 (the legacy default). */
    public static long getEnergy(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return 0;
        }
        CompoundTag tag = data.copyTag();
        return tag.getLongOr(NBT_STORED, 0);
    }

    /** Writes the stack's stored energy into {@code minecraft:custom_data} (legacy root-NBT put). */
    private static void setEnergy(ItemStack stack, long energy) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putLong(NBT_STORED, energy));
    }

    // ---------------------------------------------------------------------
    // BcChargeableItem (the legacy IMjContainerItem clamp semantics verbatim)
    // ---------------------------------------------------------------------

    @Override
    public long getPowerStored(ItemStack stack) {
        return getEnergy(stack);
    }

    @Override
    public long getMaxPowerStored(ItemStack stack) {
        return this.emptyBoard ? 0 : EntityRobot.BATTERY_CAPACITY;
    }

    @Override
    public long receivePower(ItemStack stack, long maxReceive, boolean simulate) {
        if (this.emptyBoard) {
            return 0;
        }
        long accepted = BcChargeableItem.clampReceive(getEnergy(stack), EntityRobot.BATTERY_CAPACITY, maxReceive);
        if (accepted > 0 && !simulate) {
            setEnergy(stack, getEnergy(stack) + accepted);
        }
        return accepted;
    }
}
