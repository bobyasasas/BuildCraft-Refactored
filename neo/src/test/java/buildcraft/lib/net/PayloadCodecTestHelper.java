/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.net;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Shared encode/decode round-trip helper for the M2.5 payload codec tests. */
public final class PayloadCodecTestHelper {

    private PayloadCodecTestHelper() {}

    /** Encodes and immediately decodes a payload through its own stream codec on a fresh buffer. */
    public static <T extends CustomPacketPayload> T roundTrip(StreamCodec<FriendlyByteBuf, T> codec, T payload) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            codec.encode(buf, payload);
            T decoded = codec.decode(buf);
            org.junit.Assert.assertEquals("Buffer must be fully consumed by the codec", 0, buf.readableBytes());
            return decoded;
        } finally {
            buf.release();
        }
    }
}
