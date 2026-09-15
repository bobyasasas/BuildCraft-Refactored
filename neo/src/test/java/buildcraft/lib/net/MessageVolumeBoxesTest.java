/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.net;

import buildcraft.core.marker.volume.MessageVolumeBoxes;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;

/** M2.5 codec round-trip test for {@link MessageVolumeBoxes}. */
public class MessageVolumeBoxesTest {

    @Test
    public void roundTripsSerializedBoxBlobs() {
        byte[] box1 = { 1, 2, 3, 4 };
        byte[] box2 = new byte[600];
        for (int i = 0; i < box2.length; i++) {
            box2[i] = (byte) (i & 0xFF); // exercises the varint length prefix (600 > 127)
        }
        MessageVolumeBoxes decoded = PayloadCodecTestHelper.roundTrip(
                MessageVolumeBoxes.STREAM_CODEC, new MessageVolumeBoxes(List.of(box1, box2)));
        assertEquals(2, decoded.buffers.size());
        assertArrayEquals(box1, decoded.buffers.get(0));
        assertArrayEquals(box2, decoded.buffers.get(1));
    }
}
