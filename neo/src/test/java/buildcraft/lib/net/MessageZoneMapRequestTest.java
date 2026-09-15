/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.net;

import buildcraft.robotics.zone.MessageZoneMapRequest;
import static org.junit.Assert.assertEquals;

import net.minecraft.resources.Identifier;
import org.junit.Test;

/** M2.5 codec round-trip test for {@link MessageZoneMapRequest}. */
public class MessageZoneMapRequestTest {

    @Test
    public void roundTripsChunkKey() {
        MessageZoneMapRequest.ChunkKey key = new MessageZoneMapRequest.ChunkKey(
                -17, 23,
                Identifier.fromNamespaceAndPath("minecraft", "overworld"),
                3);
        MessageZoneMapRequest decoded = PayloadCodecTestHelper.roundTrip(
                MessageZoneMapRequest.STREAM_CODEC, new MessageZoneMapRequest(key));
        assertEquals(key.chunkX(), decoded.key.chunkX());
        assertEquals(key.chunkZ(), decoded.key.chunkZ());
        assertEquals(key.dimensionId(), decoded.key.dimensionId());
        assertEquals(key.level(), decoded.key.level());
    }
}
