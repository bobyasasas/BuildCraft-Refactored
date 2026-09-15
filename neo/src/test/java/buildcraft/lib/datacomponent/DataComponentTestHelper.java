/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.datacomponent;

import static org.junit.Assert.assertEquals;

import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Shared assertions for the M2.6 NBT &harr; component parity tests. */
public final class DataComponentTestHelper {

    private DataComponentTestHelper() {}

    /** Asserts the acceptance criterion "key sets and values equal, order ignored" between two legacy-shape NBT
     * compounds. */
    public static void assertNbtEquals(CompoundTag expected, CompoundTag actual) {
        assertEquals("key sets must match", expected.keySet(), actual.keySet());
        assertEquals("compound values must match", expected, actual);
    }

    /** Encodes and immediately decodes a component value through its own stream codec on a fresh buffer (same
     * pattern as the M2.5 {@code PayloadCodecTestHelper}, but for values that are not packet payloads). */
    public static <T> T bufRoundTrip(StreamCodec<FriendlyByteBuf, T> codec, T value) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            codec.encode(buf, value);
            T decoded = codec.decode(buf);
            assertEquals("Buffer must be fully consumed by the codec", 0, buf.readableBytes());
            return decoded;
        } finally {
            buf.release();
        }
    }
}
