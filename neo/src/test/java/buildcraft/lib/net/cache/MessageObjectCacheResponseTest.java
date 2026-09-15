/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.net.cache;

import buildcraft.lib.net.PayloadCodecTestHelper;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** M2.5 codec round-trip test for {@link MessageObjectCacheResponse}. */
public class MessageObjectCacheResponseTest {

    @Test
    public void roundTripsIdsAndValueBlobs() {
        int[] ids = { 7, 8, 9 };
        byte[][] values = {
                { 1, 2, 3 },
                {},
                { -1, -2, -3, -4, -5, -6, -7 }
        };
        MessageObjectCacheResponse decoded = PayloadCodecTestHelper.roundTrip(
                MessageObjectCacheResponse.STREAM_CODEC, new MessageObjectCacheResponse(5, ids, values));
        assertEquals(5, decoded.cacheId);
        assertArrayEquals(ids, decoded.ids);
        assertEquals(3, decoded.values.length);
        assertArrayEquals(values[0], decoded.values[0]);
        assertArrayEquals(values[1], decoded.values[1]);
        assertArrayEquals(values[2], decoded.values[2]);
    }
}
