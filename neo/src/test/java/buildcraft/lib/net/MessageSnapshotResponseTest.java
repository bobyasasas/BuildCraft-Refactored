/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.net;

import buildcraft.builders.snapshot.MessageSnapshotResponse;
import static org.junit.Assert.assertEquals;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.junit.Test;

/** M2.5 codec round-trip test for {@link MessageSnapshotResponse}. */
public class MessageSnapshotResponseTest {

    @Test
    public void roundTripsSnapshotNbt() {
        CompoundTag inner = new CompoundTag();
        inner.putByte("data", (byte) 42);
        inner.putString("name", "结构片段");

        ListTag palette = new ListTag();
        palette.add(StringTag.valueOf("minecraft:stone"));
        palette.add(StringTag.valueOf("buildcraftcore:marker"));
        inner.put("palette", palette);

        CompoundTag nbt = new CompoundTag();
        nbt.put("snapshot", inner);
        nbt.putLong("created", 1726400000000L);
        nbt.put("size", IntTag.valueOf(1024));

        MessageSnapshotResponse decoded = PayloadCodecTestHelper.roundTrip(
                MessageSnapshotResponse.STREAM_CODEC, new MessageSnapshotResponse(nbt));
        assertEquals(nbt, decoded.snapshotNbt);
    }
}
