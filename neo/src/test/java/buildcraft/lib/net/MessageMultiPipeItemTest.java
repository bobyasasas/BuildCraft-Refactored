/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.net;

import buildcraft.transport.net.MessageMultiPipeItem;
import static org.junit.Assert.assertEquals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import org.junit.Test;

/** M2.5 codec round-trip test for {@link MessageMultiPipeItem}. */
public class MessageMultiPipeItemTest {

    @Test
    public void roundTripsItemsPerPipe() {
        MessageMultiPipeItem message = new MessageMultiPipeItem();
        message.append(new BlockPos(50, 64, -50), (int) 1, (byte) 8, true, Direction.UP, DyeColor.RED, (byte) 20);
        message.append(new BlockPos(50, 64, -50), (int) 65535, (byte) 1, false, Direction.DOWN, null, (byte) 120);
        message.append(new BlockPos(-300, 32, 300), (int) 42, (byte) 3, true, Direction.NORTH, DyeColor.YELLOW, (byte) 5);

        MessageMultiPipeItem decoded = PayloadCodecTestHelper.roundTrip(MessageMultiPipeItem.STREAM_CODEC, message);

        assertEquals(message.items.keySet(), decoded.items.keySet());
        MessageMultiPipeItem.TravellingItemData original = message.items.get(new BlockPos(50, 64, -50)).get(0);
        MessageMultiPipeItem.TravellingItemData copy = decoded.items.get(new BlockPos(50, 64, -50)).get(0);
        assertEquals(original.stackId, copy.stackId);
        assertEquals(original.stackCount, copy.stackCount);
        assertEquals(original.toCenter, copy.toCenter);
        assertEquals(original.side, copy.side);
        assertEquals(original.colour, copy.colour);
        assertEquals(original.timeToDest, copy.timeToDest);

        MessageMultiPipeItem.TravellingItemData originalNullColour = message.items.get(new BlockPos(50, 64, -50)).get(1);
        MessageMultiPipeItem.TravellingItemData copyNullColour = decoded.items.get(new BlockPos(50, 64, -50)).get(1);
        assertEquals(originalNullColour.colour, copyNullColour.colour); // null colour survives the nullable enum
        assertEquals(originalNullColour.stackId, copyNullColour.stackId);
        assertEquals(1, decoded.items.get(new BlockPos(-300, 32, 300)).size());
    }
}
