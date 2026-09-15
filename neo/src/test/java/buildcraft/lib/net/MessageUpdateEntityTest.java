/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.net;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.UUID;
import org.junit.Test;

/** M2.5 codec round-trip test for {@link MessageUpdateEntity}. */
public class MessageUpdateEntityTest {

    @Test
    public void roundTripsUuidAndPayloadBlob() {
        UUID uuid = UUID.fromString("c9b30e12-58a6-4b3f-a2a0-4f0d3c9d1e77");
        byte[] payload = { 0, -128, 127, 42, 1 };
        MessageUpdateEntity decoded = PayloadCodecTestHelper.roundTrip(
                MessageUpdateEntity.STREAM_CODEC, new MessageUpdateEntity(uuid, payload));
        assertEquals(uuid, decoded.uuid);
        assertArrayEquals(payload, decoded.payload);
    }
}
