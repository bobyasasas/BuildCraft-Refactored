/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders.marker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.Test;

/**
 * Unit coverage for {@link MarkerPair} &mdash; the box one construction-marker pair defines and the architect scan,
 * builder-independent machines and the renderer all consume (M4.17; the {@code MarkerLinesTest} precedent: pure
 * position math, no game bootstrap). Covers the corner-normalised box definition ({@link MarkerPair#of}), the
 * containment queries, the volume math and the NBT round trip the marker block entity persists.
 */
public class MarkerPairTest {

    @Test
    public void ofNormalisesCornersEitherWay() {
        BlockPos a = new BlockPos(10, 64, 10);
        BlockPos b = new BlockPos(14, 65, 14);
        MarkerPair forward = MarkerPair.of(a, b);
        MarkerPair backward = MarkerPair.of(b, a);
        assertEquals(forward, backward);
        assertEquals(new BlockPos(10, 64, 10), forward.min());
        assertEquals(new BlockPos(14, 65, 14), forward.max());
    }

    @Test
    public void ofHandlesReversedAxes() {
        // the pair spanning (-3..1) on every axis: min corner must survive sign-flipped input
        MarkerPair pair = MarkerPair.of(new BlockPos(1, 65, 1), new BlockPos(-3, 63, -3));
        assertEquals(new BlockPos(-3, 63, -3), pair.min());
        assertEquals(new BlockPos(1, 65, 1), pair.max());
        assertEquals(75, pair.volume());
        assertEquals("5x3x5", pair.sizeSummary());
    }

    @Test
    public void singleMarkerPairIsOneCell() {
        BlockPos pos = new BlockPos(7, 64, 7);
        MarkerPair pair = MarkerPair.of(pos, pos);
        assertEquals(1, pair.volume());
        assertEquals("1x1x1", pair.sizeSummary());
        assertTrue(pair.contains(pos));
        assertFalse(pair.contains(pos.offset(1, 0, 0)));
    }

    @Test
    public void containsCoversEveryBoxCell() {
        MarkerPair pair = MarkerPair.of(new BlockPos(0, 64, 0), new BlockPos(4, 65, 4));
        for (int x = 0; x <= 4; x++) {
            for (int y = 64; y <= 65; y++) {
                for (int z = 0; z <= 4; z++) {
                    assertTrue("cell " + x + "," + y + "," + z, pair.contains(new BlockPos(x, y, z)));
                }
            }
        }
        // just outside every face
        assertFalse(pair.contains(new BlockPos(-1, 64, 0)));
        assertFalse(pair.contains(new BlockPos(0, 63, 0)));
        assertFalse(pair.contains(new BlockPos(0, 64, 5)));
        assertFalse(pair.contains(new BlockPos(5, 65, 4)));
    }

    @Test
    public void spansRequiresBothMarkersInside() {
        MarkerPair pair = MarkerPair.of(new BlockPos(0, 64, 0), new BlockPos(4, 64, 4));
        assertTrue(pair.spans(new BlockPos(0, 64, 0), new BlockPos(4, 64, 4)));
        assertTrue(pair.spans(new BlockPos(2, 64, 2), new BlockPos(3, 64, 3)));
        assertFalse(pair.spans(new BlockPos(0, 64, 0), new BlockPos(5, 64, 4)));
        assertFalse(pair.spans(new BlockPos(-1, 64, 0), new BlockPos(4, 64, 4)));
    }

    @Test
    public void volumeCountsEveryCoveredCell() {
        assertEquals(50, MarkerPair.of(new BlockPos(0, 64, 0), new BlockPos(4, 65, 4)).volume());
        assertEquals(25, MarkerPair.of(new BlockPos(0, 64, 0), new BlockPos(4, 64, 4)).volume());
        // axis-aligned lines stay legal pairs (the legacy same-axis connection)
        assertEquals(5, MarkerPair.of(new BlockPos(0, 64, 0), new BlockPos(4, 64, 0)).volume());
        assertEquals(3, MarkerPair.of(new BlockPos(0, 63, 0), new BlockPos(0, 65, 0)).volume());
    }

    @Test
    public void nbtRoundTrip() {
        MarkerPair pair = MarkerPair.of(new BlockPos(-2, 64, 3), new BlockPos(4, 65, 9));
        CompoundTag tag = pair.writeTo(new CompoundTag());
        MarkerPair read = MarkerPair.from(tag);
        assertEquals(pair, read);
        assertEquals(pair.volume(), read.volume());
        // corrupt/absent payloads read as null (the machines idle instead of crashing)
        assertNull(MarkerPair.from(null));
        assertNull(MarkerPair.from(new CompoundTag()));
    }
}
