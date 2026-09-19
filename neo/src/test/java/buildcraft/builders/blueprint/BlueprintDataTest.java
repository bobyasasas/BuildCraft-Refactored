/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders.blueprint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.Test;

/**
 * Unit coverage for {@link BlueprintData} &mdash; the M4.17 blueprint record every blueprint-system machine passes
 * around (the {@code MarkerLinesTest} precedent: pure logic, no game bootstrap). Covers the NBT round trip
 * ({@link BlueprintData#writeTo}/{@link BlueprintData#from}), the corrupt-payload tolerance, the palette encoding,
 * the bottom-up placement order of {@link BlueprintData#nonAirCells()} and the {@link BlueprintData#fromRaw}
 * validation the architect's incremental scan leans on.
 */
public class BlueprintDataTest {

    /** A 3x2x2 blueprint: stone floor (y0, 6 cells), a brick ring on top (4 of 6 cells, air at (0,1,0)/(1,1,0)). */
    private static BlueprintData sample() {
        return BlueprintData.scan(3, 2, 2, (x, y, z) -> switch (y) {
            case 0 -> "minecraft:stone_bricks";
            default -> (x == 2 || z == 1) ? "minecraft:bricks" : null;
        });
    }

    @Test
    public void scanBuildsPaletteAndCounts() {
        BlueprintData blueprint = sample();
        Map<String, Integer> counts = blueprint.blockCounts();
        assertEquals(2, counts.size());
        assertEquals(Integer.valueOf(6), counts.get("minecraft:stone_bricks"));
        assertEquals(Integer.valueOf(4), counts.get("minecraft:bricks"));
        assertEquals("3x2x2 blocks=10: minecraft:stone_bricks x6: minecraft:bricks x4", blueprint.summary());
    }

    @Test
    public void idAtEncodesAirAsNull() {
        BlueprintData blueprint = sample();
        assertEquals("minecraft:stone_bricks", blueprint.idAt(0, 0, 0));
        assertNull(blueprint.idAt(0, 1, 0)); // the deliberate hole in the top ring
        assertEquals("minecraft:bricks", blueprint.idAt(2, 1, 1));
    }

    @Test
    public void nonAirCellsOrderIsBottomUpYThenZThenX() {
        List<BlockPos> cells = sample().nonAirCells();
        assertEquals(10, cells.size());
        // the whole floor (y0) comes before any top cell (y1); inside a layer z ascends before x
        assertEquals(new BlockPos(0, 0, 0), cells.get(0));
        assertEquals(new BlockPos(1, 0, 0), cells.get(1));
        assertEquals(new BlockPos(2, 0, 0), cells.get(2));
        assertEquals(new BlockPos(0, 0, 1), cells.get(3));
        assertEquals(new BlockPos(2, 0, 1), cells.get(5));
        // y1 skips the two air cells (0,1,0)/(1,1,0): first top cell is (2,1,0)
        assertEquals(new BlockPos(2, 1, 0), cells.get(6));
        assertEquals(new BlockPos(0, 1, 1), cells.get(7));
        assertEquals(new BlockPos(2, 1, 1), cells.get(9));
    }

    @Test
    public void nbtRoundTripPreservesEverything() {
        BlueprintData blueprint = sample();
        CompoundTag tag = blueprint.writeTo(new CompoundTag());
        BlueprintData read = BlueprintData.from(tag);
        assertEquals(blueprint, read);
        assertEquals(blueprint.hashCode(), read.hashCode());
        assertEquals(blueprint.summary(), read.summary());
        assertEquals(3, read.sizeX);
        assertEquals(2, read.sizeY);
        assertEquals(2, read.sizeZ);
    }

    @Test
    public void fromToleratesCorruptPayloads() {
        assertNull(BlueprintData.from(null));
        assertNull(BlueprintData.from(new CompoundTag())); // no keys at all
        // size only, no data/palette
        CompoundTag sizesOnly = new CompoundTag();
        sizesOnly.putInt("size_x", 1);
        assertNull(BlueprintData.from(sizesOnly));
        // truncated data array: 2x1x1 promises 2 cells, the array carries 1
        CompoundData data = truncatedData();
        assertNull(BlueprintData.from(data.tag));
        // palette index past the palette end
        assertNull(BlueprintData.from(badPaletteIndex().tag));
        // zero/negative/out-of-bound sizes
        CompoundTag zeroSize = new CompoundTag();
        zeroSize.putInt("size_x", 0);
        zeroSize.putInt("size_y", 1);
        zeroSize.putInt("size_z", 1);
        zeroSize.putIntArray("data", new int[1]);
        zeroSize.put("palette", new net.minecraft.nbt.ListTag());
        assertNull(BlueprintData.from(zeroSize));
    }

    /** Small helper: a 2x1x1 tag whose data array lost a cell. */
    private CompoundData truncatedData() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("size_x", 2);
        tag.putInt("size_y", 1);
        tag.putInt("size_z", 1);
        tag.putIntArray("data", new int[] { 1 });
        net.minecraft.nbt.ListTag palette = new net.minecraft.nbt.ListTag();
        palette.add(net.minecraft.nbt.StringTag.valueOf("minecraft:stone"));
        tag.put("palette", palette);
        return new CompoundData(tag);
    }

    /** A 1x1x1 tag whose single cell points at palette slot 2 with only one entry. */
    private CompoundData badPaletteIndex() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("size_x", 1);
        tag.putInt("size_y", 1);
        tag.putInt("size_z", 1);
        tag.putIntArray("data", new int[] { 2 });
        net.minecraft.nbt.ListTag palette = new net.minecraft.nbt.ListTag();
        palette.add(net.minecraft.nbt.StringTag.valueOf("minecraft:stone"));
        tag.put("palette", palette);
        return new CompoundData(tag);
    }

    private record CompoundData(CompoundTag tag) {
    }

    @Test
    public void fromRawValidatesTheScanContract() {
        // the architect's incremental fill: an already-scanned array wrapping into a blueprint
        BlueprintData blueprint = BlueprintData.fromRaw(2, 1, 1, List.of("minecraft:stone"), new int[] { 1, 0 });
        assertEquals("minecraft:stone", blueprint.idAt(0, 0, 0));
        assertNull(blueprint.idAt(1, 0, 0));
        assertTrue(blueprint.nonAirCells().size() == 1);
        // data length must match the volume exactly
        try {
            BlueprintData.fromRaw(3, 1, 1, List.of(), new int[] { 0, 0 });
            throw new AssertionError("expected IllegalArgumentException for a short data array");
        } catch (IllegalArgumentException expected) {
            // the scan wrapper refuses to build a lying blueprint
        }
        // palette indices point at most one past the palette end (0 = air, n = palette[n-1])
        try {
            BlueprintData.fromRaw(1, 1, 1, List.of("minecraft:stone"), new int[] { 2 });
            throw new AssertionError("expected IllegalArgumentException for an out-of-range palette index");
        } catch (IllegalArgumentException expected) {
            // same guard on the programmatic path
        }
    }

    @Test
    public void fromRawRejectsTheSameSizesFromDoes() {
        try {
            BlueprintData.fromRaw(0, 1, 1, List.of(), new int[0]);
            throw new AssertionError("expected IllegalArgumentException for a zero size");
        } catch (IllegalArgumentException expected) {
            // consistent with the from() size guard
        }
    }

    @Test
    public void scanDeduplicatesPaletteEntries() {
        BlueprintData blueprint = BlueprintData.scan(2, 1, 1,
            (x, y, z) -> "minecraft:dirt");
        assertEquals(1, blueprint.blockCounts().size());
        assertEquals(Integer.valueOf(2), blueprint.blockCounts().get("minecraft:dirt"));
        // an all-air scan is a valid empty blueprint (0 blocks)
        BlueprintData empty = BlueprintData.scan(2, 2, 2, (x, y, z) -> null);
        assertEquals("2x2x2 blocks=0", empty.summary());
        assertTrue(empty.nonAirCells().isEmpty());
    }

    @Test
    public void equalityDistinguishesPayloads() {
        BlueprintData a = sample();
        BlueprintData b = sample();
        assertEquals(a, b);
        BlueprintData other = BlueprintData.scan(3, 2, 2, (x, y, z) -> "minecraft:dirt");
        assertNotEquals(a, other);
        assertNotEquals(a, null);
    }
}
