/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders.blueprint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.jspecify.annotations.Nullable;

/**
 * The M4.17 blueprint data record (C route): the pure-logic block snapshot one architect scan produces and one builder
 * replays. Legacy counterpart: the {@code buildcraft.builders.snapshot.Snapshot}/{@code Blueprint} family (frozen
 * 1.20.1 tree) &mdash; there the scanned data lived in the world's {@code GlobalSavedDataSnapshots} and the blueprint
 * item only carried a hash {@code Header}; the M4.17 v1 instead stores the whole snapshot inside the blueprint item
 * itself (see {@link BlueprintItems}, the {@code minecraft:custom_data} component &mdash; zero new registry ids), so
 * this class is a plain value object with no FML/world dependencies (unit testable, the {@code MarkerLines} precedent;
 * {@code CompoundTag}/{@code BlockPos} initialize without the game bootstrap).
 *
 * <p><b>Format (v1, full listing; the legacy {@code palette + int[]} shape kept, the legacy run-length/pack refinements
 * cut &mdash; volumes are small by design):</b> a size {@code (sizeX, sizeY, sizeZ)}, a palette of non-air block id
 * strings ({@code "namespace:path"}), and one palette index per cell (index {@code 0 = air}, {@code n} =
 * {@code palette[n-1]}). Cells are addressed by {@link #cellIndex}, ordered y (bottom) first, then z, then x &mdash;
 * the same bottom-up order the builder places in (legacy {@code BoxIterator} randomised per session; v1 keeps one
 * deterministic order).
 */
public final class BlueprintData {

    /** The compound key the data rides under inside an item stack's {@code minecraft:custom_data} component. */
    public static final String NBT_KEY = "bc_blueprint";

    /** Palette entry guard: index 0 in {@link #data} always means air (never stored in {@link #palette}). */
    private static final int AIR = 0;

    /** The v1 sanity bound on one blueprint's cell count (a 48^3 box; the legacy architect had no bound but the
     * snapshot data lived in world storage, not one item component). */
    public static final int MAX_CELLS = 48 * 48 * 48;

    /** Volume size in blocks (all {@code >= 1}). */
    public final int sizeX;
    public final int sizeY;
    public final int sizeZ;

    /** The non-air block ids used in this blueprint, in first-scan order. */
    private final List<String> palette;
    /** Per-cell palette index + 1 ({@code 0 = air}); length {@code sizeX * sizeY * sizeZ}, see {@link #cellIndex}. */
    private final int[] data;

    private BlueprintData(int sizeX, int sizeY, int sizeZ, List<String> palette, int[] data) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.palette = List.copyOf(palette);
        this.data = data;
    }

    /**
     * The per-cell id provider of {@link #scan} (a three-argument view of the scan cursor; {@code IntFunction} only
     * carries one int, so the scan keeps its own lambda shape).
     */
    @FunctionalInterface
    public interface BlockIdAt {
        /** The block id at blueprint-local {@code (x, y, z)}, or null for air. */
        @Nullable String apply(int x, int y, int z);
    }

    /**
     * Builds a blueprint from a per-cell id provider (the architect's scan loop shape): {@code blockIdAt} receives
     * blueprint-local {@code (x, y, z)} and returns the block id string or null for air.
     */
    public static BlueprintData scan(int sizeX, int sizeY, int sizeZ, BlockIdAt blockIdAt) {
        int[] data = new int[checkedLength(sizeX, sizeY, sizeZ)];
        List<String> palette = new ArrayList<>();
        for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < sizeZ; z++) {
                for (int x = 0; x < sizeX; x++) {
                    String id = blockIdAt.apply(x, y, z);
                    if (id == null || id.isEmpty()) {
                        continue; // air stays 0
                    }
                    int paletteIndex = palette.indexOf(id);
                    if (paletteIndex < 0) {
                        paletteIndex = palette.size();
                        palette.add(id);
                    }
                    data[cellIndex(x, y, z, sizeX, sizeZ)] = paletteIndex + 1;
                }
            }
        }
        return new BlueprintData(sizeX, sizeY, sizeZ, palette, data);
    }

    /** The flat cell index for blueprint-local {@code (x, y, z)}: y (bottom) first, then z, then x. */
    public static int cellIndex(int x, int y, int z, int sizeX, int sizeZ) {
        return (y * sizeZ + z) * sizeX + x;
    }

    /**
     * Wraps already-scanned arrays (the architect's incremental fill): {@code data} must use the internal palette
     * index + 1 encoding and match the volume exactly, {@code palette} the non-air ids; validates both (the same
     * contract {@link #from} enforces on load).
     */
    public static BlueprintData fromRaw(int sizeX, int sizeY, int sizeZ, List<String> palette, int[] data) {
        if (data.length != checkedLength(sizeX, sizeY, sizeZ)) {
            throw new IllegalArgumentException("data length " + data.length + " != " + sizeX + "*" + sizeY + "*" + sizeZ);
        }
        for (int paletteIndex : data) {
            if (paletteIndex < 0 || paletteIndex > palette.size()) {
                throw new IllegalArgumentException("palette index out of range: " + paletteIndex);
            }
        }
        return new BlueprintData(sizeX, sizeY, sizeZ, palette, data);
    }

    private static int checkedLength(int sizeX, int sizeY, int sizeZ) {
        long length = (long) sizeX * sizeY * sizeZ;
        if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0 || length > MAX_CELLS) {
            throw new IllegalArgumentException("blueprint size out of range: " + sizeX + "x" + sizeY + "x" + sizeZ);
        }
        return (int) length;
    }

    // ----------------------------------------------------------------- queries

    /** The block id at blueprint-local {@code (x, y, z)}, or null for air. */
    public @Nullable String idAt(int x, int y, int z) {
        int paletteIndex = this.data[cellIndex(x, y, z, this.sizeX, this.sizeZ)];
        return paletteIndex == AIR ? null : this.palette.get(paletteIndex - 1);
    }

    /** The non-air cells in placement order (bottom-up y, then z, then x), as blueprint-local offsets. */
    public List<BlockPos> nonAirCells() {
        List<BlockPos> cells = new ArrayList<>();
        for (int y = 0; y < this.sizeY; y++) {
            for (int z = 0; z < this.sizeZ; z++) {
                for (int x = 0; x < this.sizeX; x++) {
                    if (this.data[cellIndex(x, y, z, this.sizeX, this.sizeZ)] != AIR) {
                        cells.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        return cells;
    }

    /** Blocks of each kind the blueprint needs (the builder's resource demand summary; first-scan order = key order). */
    public Map<String, Integer> blockCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (int paletteIndex : this.data) {
            if (paletteIndex != AIR) {
                counts.merge(this.palette.get(paletteIndex - 1), 1, Integer::sum);
            }
        }
        return counts;
    }

    /** The one-line human summary the library index and evidence logs print ({@code "5x2x5 blocks=17: ... x17"}). */
    public String summary() {
        int nonAir = 0;
        for (int paletteIndex : this.data) {
            if (paletteIndex != AIR) {
                nonAir++;
            }
        }
        StringBuilder text = new StringBuilder()
            .append(this.sizeX).append('x').append(this.sizeY).append('x').append(this.sizeZ)
            .append(" blocks=").append(nonAir);
        this.blockCounts().forEach((id, count) -> text.append(": ").append(id).append(" x").append(count));
        return text.toString();
    }

    public int cellCount() {
        return this.data.length;
    }

    // ------------------------------------------------------------- persistence

    /** Writes this blueprint into {@code tag} (the {@link #NBT_KEY} payload shape, mirrored by {@link #from}). */
    public CompoundTag writeTo(CompoundTag tag) {
        tag.putInt("size_x", this.sizeX);
        tag.putInt("size_y", this.sizeY);
        tag.putInt("size_z", this.sizeZ);
        ListTag paletteTag = new ListTag();
        for (String id : this.palette) {
            paletteTag.add(StringTag.valueOf(id));
        }
        tag.put("palette", paletteTag);
        tag.putIntArray("data", this.data);
        return tag;
    }

    /**
     * Reads a blueprint written by {@link #writeTo}; null when the tag is missing keys, out of range or truncated (the
     * machines treat a null read as "no blueprint" and idle, legacy {@code ItemSnapshot#getHeader} null semantics).
     */
    public static @Nullable BlueprintData from(@Nullable CompoundTag tag) {
        if (tag == null || !tag.contains("size_x") || !tag.contains("data") || !tag.contains("palette")) {
            return null;
        }
        int sizeX = tag.getIntOr("size_x", 0);
        int sizeY = tag.getIntOr("size_y", 0);
        int sizeZ = tag.getIntOr("size_z", 0);
        int[] data;
        try {
            data = new int[checkedLength(sizeX, sizeY, sizeZ)];
        } catch (IllegalArgumentException outOfRange) {
            return null;
        }
        ListTag paletteTag = tag.getListOrEmpty("palette");
        List<String> palette = new ArrayList<>(paletteTag.size());
        for (int i = 0; i < paletteTag.size(); i++) {
            if (!(paletteTag.get(i) instanceof StringTag idTag)) {
                return null;
            }
            palette.add(idTag.value());
        }
        int[] raw = tag.getIntArray("data").orElse(null);
        if (raw == null || raw.length != data.length) {
            return null;
        }
        System.arraycopy(raw, 0, data, 0, data.length);
        for (int paletteIndex : data) {
            if (paletteIndex < 0 || paletteIndex > palette.size()) {
                return null;
            }
        }
        return new BlueprintData(sizeX, sizeY, sizeZ, palette, data);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BlueprintData other)) {
            return false;
        }
        return this.sizeX == other.sizeX && this.sizeY == other.sizeY && this.sizeZ == other.sizeZ
            && this.palette.equals(other.palette) && Arrays.equals(this.data, other.data);
    }

    @Override
    public int hashCode() {
        int hash = this.sizeX * 31 + this.sizeY;
        hash = hash * 31 + this.sizeZ;
        hash = hash * 31 + this.palette.hashCode();
        return hash * 31 + Arrays.hashCode(this.data);
    }

    @Override
    public String toString() {
        return "BlueprintData[" + this.summary() + "]";
    }
}
