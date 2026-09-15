/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.datacomponent.robot;

import com.mojang.serialization.Codec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Robot parameters component value ({@code buildcraftrobotics:robot_params}): the stored-energy field the legacy robot
 * items keep in their root NBT. Legacy counterpart:
 * {@code buildcraft.robotics.item.ItemRobot#createRobotStack} writes it via
 * {@code NBTUtilBC.getItemData(robot).putLong(MjBattery.NBT_STORED, energy)} where
 * {@code buildcraft.api.mj.MjBattery} defines {@code NBT_STORED = "stored"} and serializes the same single long
 * ({@code MjBattery#serializeNBT}); the wire form {@code buffer.writeLong} of {@code MjBattery#writeToBuffer} maps to
 * the fixed-width long used by {@link #writeToBuf}.
 */
public record BcRobotParams(long stored) {

    public static final BcRobotParams EMPTY = new BcRobotParams(0);

    /** Persistent codec: encodes to exactly the legacy item root compound shape ({@code stored} long). */
    public static final Codec<BcRobotParams> CODEC = Codec.lazyInitialized(() -> BcRobotNbt.CODEC);

    public static final StreamCodec<FriendlyByteBuf, BcRobotParams> STREAM_CODEC = StreamCodec.ofMember(
        BcRobotParams::writeToBuf, BcRobotParams::readFromBuf);

    public CompoundTag writeToNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putLong("stored", stored);
        return nbt;
    }

    /** Reads the legacy item root: a missing {@code stored} key is the legacy default
     * ({@code CompoundTag#getLong} returned 0), i.e. an uncharged robot. */
    public static BcRobotParams readFromNbt(CompoundTag nbt) {
        return new BcRobotParams(nbt.getLongOr("stored", 0));
    }

    public void writeToBuf(FriendlyByteBuf buf) {
        buf.writeLong(stored);
    }

    public static BcRobotParams readFromBuf(FriendlyByteBuf buf) {
        return new BcRobotParams(buf.readLong());
    }
}
