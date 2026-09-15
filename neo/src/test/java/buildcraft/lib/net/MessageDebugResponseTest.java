/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.net;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;

/** M2.5 codec round-trip test for {@link MessageDebugResponse}. */
public class MessageDebugResponseTest {

    @Test
    public void roundTripsBothTextColumns() {
        MessageDebugResponse message = new MessageDebugResponse(
                List.of("left 1", "中文列", "left 3"),
                List.of("right 1"));
        MessageDebugResponse decoded = PayloadCodecTestHelper.roundTrip(MessageDebugResponse.STREAM_CODEC, message);
        assertEquals(message.left, decoded.left);
        assertEquals(message.right, decoded.right);
    }

    @Test
    public void roundTripsEmptyColumns() {
        MessageDebugResponse decoded = PayloadCodecTestHelper.roundTrip(
                MessageDebugResponse.STREAM_CODEC, new MessageDebugResponse());
        assertEquals(0, decoded.left.size());
        assertEquals(0, decoded.right.size());
    }
}
