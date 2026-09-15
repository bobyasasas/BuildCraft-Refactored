/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.datacomponent.robot;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import buildcraft.lib.datacomponent.DataComponentTestHelper;
import net.minecraft.nbt.CompoundTag;

/**
 * M2.6 parity tests for {@link BcRobotParams}. Fixture compounds are the legacy robot item root NBT:
 * {@code buildcraft.robotics.item.ItemRobot#createRobotStack} writes {@code putLong(MjBattery.NBT_STORED, energy)}
 * with {@code buildcraft.api.mj.MjBattery#NBT_STORED} = {@code "stored"} and {@code MjBattery#serializeNBT} writes the
 * same single long. Values: 0 (uncharged robot, the {@code createRobotStack(robotNBT, 0)} creative/empty case), a
 * mid charge, and 2^40 microjoules (large-battery boundary).
 */
public class BcRobotNbtTest {

    private static CompoundTag fixture(long stored) {
        CompoundTag nbt = new CompoundTag();
        nbt.putLong("stored", stored);
        return nbt;
    }

    @Test
    public void unchargedRobotRoundTrips() {
        CompoundTag nbt = fixture(0);
        BcRobotParams params = BcRobotParams.readFromNbt(nbt);
        assertEquals(0, params.stored());
        DataComponentTestHelper.assertNbtEquals(nbt, params.writeToNbt());
    }

    @Test
    public void midChargedRobotRoundTrips() {
        CompoundTag nbt = fixture(1_000_000);
        BcRobotParams params = BcRobotParams.readFromNbt(nbt);
        assertEquals(1_000_000, params.stored());
        DataComponentTestHelper.assertNbtEquals(nbt, params.writeToNbt());
    }

    @Test
    public void largeBatteryRoundTrips() {
        CompoundTag nbt = fixture(1L << 40);
        BcRobotParams params = BcRobotParams.readFromNbt(nbt);
        assertEquals(1L << 40, params.stored());
        DataComponentTestHelper.assertNbtEquals(nbt, params.writeToNbt());
    }

    /** Legacy tolerance: a missing {@code stored} key read as 0, exactly like legacy {@code CompoundTag#getLong}.
     * One-way (a rewritten compound carries the explicit default key). */
    @Test
    public void missingKeyReadsZero() {
        assertEquals(0, BcRobotParams.readFromNbt(new CompoundTag()).stored());
    }

    /** Component codec round trip (buffer, fixed-width long like legacy {@code MjBattery#writeToBuffer}). */
    @Test
    public void robotStreamCodecRoundTrips() {
        for (BcRobotParams params : new BcRobotParams[] { BcRobotParams.EMPTY, new BcRobotParams(1_000_000),
            new BcRobotParams(1L << 40) }) {
            assertEquals(params, DataComponentTestHelper.bufRoundTrip(BcRobotParams.STREAM_CODEC, params));
        }
    }
}
