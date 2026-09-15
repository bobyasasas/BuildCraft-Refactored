/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.net;

import buildcraft.builders.snapshot.MessageSnapshotRequest;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Date;
import java.util.UUID;
import org.junit.Test;

/** M2.5 codec round-trip test for {@link MessageSnapshotRequest} (key with and without header). */
public class MessageSnapshotRequestTest {

    @Test
    public void roundTripsBareKey() {
        MessageSnapshotRequest.Key key = new MessageSnapshotRequest.Key(
                new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 }, null);
        MessageSnapshotRequest decoded = PayloadCodecTestHelper.roundTrip(
                MessageSnapshotRequest.STREAM_CODEC, new MessageSnapshotRequest(key));
        assertArrayEquals(key.hash(), decoded.key.hash());
        assertNull(decoded.key.header());
    }

    @Test
    public void roundTripsRecursiveHeader() {
        MessageSnapshotRequest.Key innerKey = new MessageSnapshotRequest.Key(
                new byte[] { 9, 8, 7, 6, 5, 4, 3, 2, 1 }, null);
        MessageSnapshotRequest.Header header = new MessageSnapshotRequest.Header(
                innerKey, UUID.fromString("81675c43-cc5c-4db7-a6aa-f6b39c63a125"), 1726400000000L, "我的蓝图 §a");
        MessageSnapshotRequest.Key key = new MessageSnapshotRequest.Key(new byte[] { 1, 2, 3 }, header);
        MessageSnapshotRequest decoded = PayloadCodecTestHelper.roundTrip(
                MessageSnapshotRequest.STREAM_CODEC, new MessageSnapshotRequest(key));
        assertArrayEquals(key.hash(), decoded.key.hash());
        assertEquals(header.owner(), decoded.key.header().owner());
        assertEquals(header.created(), decoded.key.header().created());
        assertEquals(header.name(), decoded.key.header().name());
        assertArrayEquals(innerKey.hash(), decoded.key.header().key().hash());
    }
}
