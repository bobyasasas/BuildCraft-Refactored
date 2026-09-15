/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.net;

import buildcraft.transport.wire.MessageWireSystemsPowered;
import static org.junit.Assert.assertEquals;

import java.util.Map;
import org.junit.Test;

/** M2.5 codec round-trip test for {@link MessageWireSystemsPowered}. */
public class MessageWireSystemsPoweredTest {

    @Test
    public void roundTripsPoweredMap() {
        MessageWireSystemsPowered decoded = PayloadCodecTestHelper.roundTrip(
                MessageWireSystemsPowered.STREAM_CODEC,
                new MessageWireSystemsPowered(Map.of(12345, true, -6789, false)));
        assertEquals(Map.of(12345, true, -6789, false), decoded.hashesPowered);
    }
}
