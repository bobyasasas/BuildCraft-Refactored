/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.net.cache;

import buildcraft.lib.net.PayloadCodecTestHelper;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** M2.5 codec round-trip test for {@link MessageObjectCacheRequest}. */
public class MessageObjectCacheRequestTest {

    @Test
    public void roundTripsCacheIdAndIds() {
        int[] ids = { 0, 1, -1, Integer.MAX_VALUE, 123456 };
        MessageObjectCacheRequest decoded = PayloadCodecTestHelper.roundTrip(
                MessageObjectCacheRequest.STREAM_CODEC, new MessageObjectCacheRequest(3, ids));
        assertEquals(3, decoded.cacheId);
        assertArrayEquals(ids, decoded.ids);
    }

    @Test
    public void roundTripsEmptyRequest() {
        MessageObjectCacheRequest decoded = PayloadCodecTestHelper.roundTrip(
                MessageObjectCacheRequest.STREAM_CODEC, new MessageObjectCacheRequest(0, new int[0]));
        assertEquals(0, decoded.cacheId);
        assertEquals(0, decoded.ids.length);
    }
}
