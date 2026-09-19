/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders.marker;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.jspecify.annotations.Nullable;

/**
 * The box one construction-marker pair defines (M4.17 C route): the corner-normalised bounding box of the two marker
 * cells, both cells included. Pure position math, split out of {@code ConstructionMarkerBlockEntity} so it stays unit
 * testable (the {@code MarkerLines} precedent). Legacy counterpart: {@code buildcraft.core.marker.VolumeConnection}
 * (frozen 1.20.1 tree) &mdash; {@code createBox} extends the box to encompass every marker position of the connection;
 * a v1 pair keeps exactly that rule for two members (the legacy third/fourth-marker extension of the box is not
 * migrated &mdash; a pair already spans a full 3D box here, see the javadoc on the marker block entity).
 */
public record MarkerPair(BlockPos min, BlockPos max) {

    /** The corner-normalised box spanning both marker cells. */
    public static MarkerPair of(BlockPos a, BlockPos b) {
        return new MarkerPair(
            new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ())),
            new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ())));
    }

    /** Both marker cells lie inside the box (a live pair's invariant, re-checked after chunk reloads). */
    public boolean spans(BlockPos a, BlockPos b) {
        return this.contains(a) && this.contains(b);
    }

    /** True for every cell the box covers, markers included. */
    public boolean contains(BlockPos pos) {
        return pos.getX() >= this.min.getX() && pos.getX() <= this.max.getX()
            && pos.getY() >= this.min.getY() && pos.getY() <= this.max.getY()
            && pos.getZ() >= this.min.getZ() && pos.getZ() <= this.max.getZ();
    }

    /** The covered cell count (markers included). */
    public long volume() {
        return (long) (this.max.getX() - this.min.getX() + 1)
            * (this.max.getY() - this.min.getY() + 1)
            * (this.max.getZ() - this.min.getZ() + 1);
    }

    /** One-line size summary ({@code "5x2x5"}) for the evidence logs. */
    public String sizeSummary() {
        return (this.max.getX() - this.min.getX() + 1) + "x" + (this.max.getY() - this.min.getY() + 1)
            + "x" + (this.max.getZ() - this.min.getZ() + 1);
    }

    /** Persists the box corners (the marker block entity's {@code bc_box_*} payload shape). */
    public CompoundTag writeTo(CompoundTag tag) {
        tag.putInt("min_x", this.min.getX());
        tag.putInt("min_y", this.min.getY());
        tag.putInt("min_z", this.min.getZ());
        tag.putInt("max_x", this.max.getX());
        tag.putInt("max_y", this.max.getY());
        tag.putInt("max_z", this.max.getZ());
        return tag;
    }

    /** Reads a box written by {@link #writeTo}; null on absent/corrupt corners. */
    public static @Nullable MarkerPair from(@Nullable CompoundTag tag) {
        if (tag == null || !tag.contains("min_x") || !tag.contains("max_z")) {
            return null;
        }
        return of(
            new BlockPos(tag.getIntOr("min_x", 0), tag.getIntOr("min_y", 0), tag.getIntOr("min_z", 0)),
            new BlockPos(tag.getIntOr("max_x", 0), tag.getIntOr("max_y", 0), tag.getIntOr("max_z", 0)));
    }
}
