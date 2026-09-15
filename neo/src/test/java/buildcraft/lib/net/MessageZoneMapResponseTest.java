/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.net;

import buildcraft.robotics.zone.MessageZoneMapRequest;
import buildcraft.robotics.zone.MessageZoneMapResponse;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import net.minecraft.resources.Identifier;
import org.junit.Test;

/** M2.5 codec round-trip test for {@link MessageZoneMapResponse}. */
public class MessageZoneMapResponseTest {

    @Test
    public void roundTripsKeyAndChunkImage() {
        MessageZoneMapRequest.ChunkKey key = new MessageZoneMapRequest.ChunkKey(
                5, -5, Identifier.withDefaultNamespace("the_nether"), 1);
        MessageZoneMapResponse.ChunkData data = MessageZoneMapResponse.ChunkData.absent();
        data.posY()[0] = 64; // cell (0,0)
        data.colour()[0] = 0xFF00FF00;
        data.posY()[255] = 1; // cell (15,15)
        data.colour()[255] = 123456;

        MessageZoneMapResponse decoded = PayloadCodecTestHelper.roundTrip(
                MessageZoneMapResponse.STREAM_CODEC, new MessageZoneMapResponse(key, data));
        assertEquals(key.chunkX(), decoded.key.chunkX());
        assertEquals(key.chunkZ(), decoded.key.chunkZ());
        assertEquals(key.dimensionId(), decoded.key.dimensionId());
        assertEquals(key.level(), decoded.key.level());
        assertArrayEquals(data.posY(), decoded.data.posY());
        assertArrayEquals(data.colour(), decoded.data.colour());
    }

    @Test
    public void roundTripsFullyAbsentChunk() {
        MessageZoneMapRequest.ChunkKey key = new MessageZoneMapRequest.ChunkKey(
                0, 0, Identifier.withDefaultNamespace("overworld"), 0);
        MessageZoneMapResponse decoded = PayloadCodecTestHelper.roundTrip(
                MessageZoneMapResponse.STREAM_CODEC,
                new MessageZoneMapResponse(key, MessageZoneMapResponse.ChunkData.absent()));
        for (int i = 0; i < 256; i++) {
            assertEquals(MessageZoneMapResponse.ChunkData.ABSENT, decoded.data.posY()[i]);
        }
    }
}
