/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.datacomponent.robot;

import com.mojang.serialization.Codec;

import net.minecraft.nbt.CompoundTag;

/**
 * Legacy-NBT &harr; component conversion for {@link BcRobotParams}: the legacy robot item stored a single
 * {@code stored} long in its root NBT ({@code MjBattery.NBT_STORED}, see
 * {@code buildcraft.robotics.item.ItemRobot}), so the conversion is that single key.
 */
public final class BcRobotNbt {

    /** Component persistent codec: the value's NBT form is exactly the legacy item root compound. */
    public static final Codec<BcRobotParams> CODEC = CompoundTag.CODEC.xmap(BcRobotParams::readFromNbt,
        BcRobotParams::writeToNbt);

    private BcRobotNbt() {}
}
