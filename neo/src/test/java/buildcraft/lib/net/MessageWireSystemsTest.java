/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.net;

import buildcraft.transport.wire.MessageWireSystems;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import org.junit.Test;

/** M2.5 codec round-trip test for {@link MessageWireSystems}. */
public class MessageWireSystemsTest {

    @Test
    public void roundTripsSystemsAndElements() {
        MessageWireSystems message = new MessageWireSystems(Map.of(
                1001,
                List.of(
                        new MessageWireSystems.WireElementData(0, new BlockPos(10, 64, -10), 3),
                        new MessageWireSystems.WireElementData(1, new BlockPos(11, 65, -11), 5)),
                -2002,
                List.of(new MessageWireSystems.WireElementData(0, new BlockPos(0, 1, 2), 15))));
        MessageWireSystems decoded = PayloadCodecTestHelper.roundTrip(MessageWireSystems.STREAM_CODEC, message);
        assertEquals(message.wireSystems, decoded.wireSystems);
    }
}
