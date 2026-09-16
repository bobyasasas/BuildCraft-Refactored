/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.robotics.zone;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Minimal zone concept for the M2.13 robot slice: one axis-aligned block box that a robot can treat as its
 * "作业区域" (work area). Deliberately reduced stand-in for the legacy zone system (frozen 1.20.1 tree):
 * <ul>
 * <li>legacy {@code buildcraft.api.core.IZone} — "defines some volume in the world": {@link #contains(BlockPos)}
 * mirrors {@code IZone#contains(Vec3)} (block-centre form) and {@link #getRandomBlockPos(RandomSource)} mirrors
 * {@code IZone#getRandomBlockPos(RandomSource)};</li>
 * <li>legacy {@code buildcraft.robotics.zone.ZonePlan}/{@code ZoneChunk} — a per-chunk bitmap edited by the zone
 * planner GUI ({@code TileZonePlanner} + 64&#179; volume) and attached to robots through gate statements
 * ({@code ActionRobotWorkInArea}); neither the planner block behaviour/GUI nor the gate statement plumbing has
 * migrated, so the slice zone is a plain box carried directly on the robot (programmatic setter + persistence).</li>
 * </ul>
 *
 * <p>The deterministic scan helper {@link #advanceScan(BlockPos)} orders cells x asc, z asc, y asc. Legacy picks
 * random cells inside the zone ({@code BlockScannerZoneRandom}); the fixed order keeps gametests deterministic
 * (same trade-off the M2.12 quarry slice made for its mining scan).
 */
public final class BoxZone {

    /** Inclusive minimum corner. */
    private final BlockPos min;
    /** Inclusive maximum corner. */
    private final BlockPos max;

    public BoxZone(BlockPos min, BlockPos max) {
        this.min = new BlockPos(Math.min(min.getX(), max.getX()), Math.min(min.getY(), max.getY()),
                Math.min(min.getZ(), max.getZ()));
        this.max = new BlockPos(Math.max(min.getX(), max.getX()), Math.max(min.getY(), max.getY()),
                Math.max(min.getZ(), max.getZ()));
    }

    public BlockPos min() {
        return this.min;
    }

    public BlockPos max() {
        return this.max;
    }

    /** {@code IZone#contains(Vec3)} in block form: the position's block cell lies inside the box. */
    public boolean contains(BlockPos pos) {
        return pos.getX() >= this.min.getX() && pos.getX() <= this.max.getX()
                && pos.getY() >= this.min.getY() && pos.getY() <= this.max.getY()
                && pos.getZ() >= this.min.getZ() && pos.getZ() <= this.max.getZ();
    }

    /** {@code IZone#contains(Vec3)}: the point lies inside the box (inclusive of the max face). */
    public boolean contains(Vec3 point) {
        return point.x >= this.min.getX() && point.x <= this.max.getX() + 1.0
                && point.y >= this.min.getY() && point.y <= this.max.getY() + 1.0
                && point.z >= this.min.getZ() && point.z <= this.max.getZ() + 1.0;
    }

    /** {@code IZone#getRandomBlockPos(RandomSource)}: a uniformly random block cell inside the box. */
    public BlockPos getRandomBlockPos(RandomSource rand) {
        int x = this.min.getX() + rand.nextInt(this.max.getX() - this.min.getX() + 1);
        int y = this.min.getY() + rand.nextInt(this.max.getY() - this.min.getY() + 1);
        int z = this.min.getZ() + rand.nextInt(this.max.getZ() - this.min.getZ() + 1);
        return new BlockPos(x, y, z);
    }

    /**
     * Deterministic scan helper: given the current cursor (or {@code null} to start at {@link #min}), returns the
     * next cell in x asc, z asc, y asc order, or {@code null} once the scan has passed {@link #max}. Never returns
     * the cursor itself, so a loop of {@code cursor = zone.advanceScan(cursor)} visits every cell exactly once.
     */
    @Nullable
    public BlockPos advanceScan(@Nullable BlockPos cursor) {
        if (cursor == null) {
            return this.min;
        }
        if (cursor.getX() < this.max.getX()) {
            return cursor.offset(1, 0, 0);
        }
        if (cursor.getZ() < this.max.getZ()) {
            return new BlockPos(this.min.getX(), cursor.getY(), cursor.getZ() + 1);
        }
        if (cursor.getY() < this.max.getY()) {
            return new BlockPos(this.min.getX(), cursor.getY() + 1, this.min.getZ());
        }
        return null;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BoxZone other)) {
            return false;
        }
        return this.min.equals(other.min) && this.max.equals(other.max);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.min, this.max);
    }

    @Override
    public String toString() {
        return "BoxZone[" + this.min.getX() + " " + this.min.getY() + " " + this.min.getZ() + " -> "
                + this.max.getX() + " " + this.max.getY() + " " + this.max.getZ() + "]";
    }
}
