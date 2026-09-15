/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.net;

import static org.junit.Assert.assertEquals;

import java.util.List;
import net.minecraft.core.BlockPos;
import org.junit.Test;

/** M2.5 codec round-trip test for {@link MessageMarker} (single and multi-position variants). */
public class MessageMarkerTest {

    @Test
    public void roundTripsSinglePosition() {
        MessageMarker message = new MessageMarker();
        message.add = true;
        message.connection = false;
        message.cacheId = 2;
        message.positions.add(new BlockPos(1, 2, 3));
        MessageMarker decoded = PayloadCodecTestHelper.roundTrip(MessageMarker.STREAM_CODEC, message);
        assertEquals(true, decoded.add);
        assertEquals(false, decoded.multiple); // derived exactly like legacy: count != 1
        assertEquals(false, decoded.connection);
        assertEquals(2, decoded.cacheId);
        assertEquals(List.of(new BlockPos(1, 2, 3)), decoded.positions);
    }

    @Test
    public void roundTripsMultiplePositions() {
        MessageMarker message = new MessageMarker();
        message.add = false;
        message.connection = true;
        message.cacheId = -1;
        message.positions.add(new BlockPos(1000, -5, 99999));
        message.positions.add(new BlockPos(0, 0, 0));
        message.positions.add(new BlockPos(7, 8, 9));
        MessageMarker decoded = PayloadCodecTestHelper.roundTrip(MessageMarker.STREAM_CODEC, message);
        assertEquals(false, decoded.add);
        assertEquals(true, decoded.multiple);
        assertEquals(true, decoded.connection);
        assertEquals(-1, decoded.cacheId);
        assertEquals(
                List.of(new BlockPos(1000, -5, 99999), new BlockPos(0, 0, 0), new BlockPos(7, 8, 9)),
                decoded.positions);
    }
}
