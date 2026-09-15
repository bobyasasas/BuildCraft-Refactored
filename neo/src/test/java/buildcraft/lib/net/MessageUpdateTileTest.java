/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.net;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import net.minecraft.core.BlockPos;
import org.junit.Test;

/** M2.5 codec round-trip test for {@link MessageUpdateTile}. */
public class MessageUpdateTileTest {

    @Test
    public void roundTripsPosAndPayloadBlob() {
        byte[] payload = { 5, 10, 15, -3 };
        MessageUpdateTile decoded = PayloadCodecTestHelper.roundTrip(
                MessageUpdateTile.STREAM_CODEC, new MessageUpdateTile(new BlockPos(123456, 60, -123456), payload));
        assertEquals(new BlockPos(123456, 60, -123456), decoded.pos);
        assertArrayEquals(payload, decoded.payload);
    }
}
