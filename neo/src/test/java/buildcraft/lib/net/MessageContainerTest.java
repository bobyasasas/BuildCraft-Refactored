/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.net;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** M2.5 codec round-trip test for {@link MessageContainer}. */
public class MessageContainerTest {

    @Test
    public void roundTripsWindowMsgIdAndPayload() {
        byte[] payload = { 1, 2, 3, 4, 5, -1 };
        MessageContainer message = new MessageContainer(42, 0xCAFE, payload);
        MessageContainer decoded = PayloadCodecTestHelper.roundTrip(MessageContainer.STREAM_CODEC, message);
        assertEquals(42, decoded.windowId);
        assertEquals(0xCAFE, decoded.msgId); // unsigned short range survives
        assertArrayEquals(payload, decoded.payload);
    }
}
