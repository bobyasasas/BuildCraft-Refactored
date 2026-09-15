/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.Test;

/** M2.5 codec round-trip test for {@link MessageDebugRequest} (both block and entity variants). */
public class MessageDebugRequestTest {

    @Test
    public void roundTripsBlockVariant() {
        MessageDebugRequest message = MessageDebugRequest.forBlock(new BlockPos(-12, 65, 400), Direction.WEST);
        MessageDebugRequest decoded = PayloadCodecTestHelper.roundTrip(MessageDebugRequest.STREAM_CODEC, message);
        assertEquals(false, decoded.isEntity);
        assertEquals(new BlockPos(-12, 65, 400), decoded.pos);
        assertEquals(Direction.WEST, decoded.side);
        assertNull(decoded.uuid);
    }

    @Test
    public void roundTripsEntityVariant() {
        UUID uuid = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
        MessageDebugRequest message = MessageDebugRequest.forEntity(uuid);
        MessageDebugRequest decoded = PayloadCodecTestHelper.roundTrip(MessageDebugRequest.STREAM_CODEC, message);
        assertEquals(true, decoded.isEntity);
        assertEquals(uuid, decoded.uuid);
        assertNull(decoded.pos);
        assertNull(decoded.side);
    }
}
