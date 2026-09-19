/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.robotics.blockentity;

import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;
import buildcraft.robotics.zone.BoxZone;

/**
 * M4.17 pure zone decisions of the zone planner (the {@code FloodGateLogic} discipline: plain class, FML-less unit
 * testable). The v1 planner's zone is the M2.13 {@link BoxZone} stand-in for the legacy
 * {@code ZonePlan}/16-layer chunk bitmap &mdash; a single axis-aligned block box defined around the planner &mdash; so
 * the decisions are the box's geometry: the single-radius definition form, the null-safe containment (an undefined zone
 * contains nothing) and the cell volume for the {@code [M417]} bounds log.
 */
public final class ZonePlannerLogic {

    /** v1 sanity cap for the programmatic single-radius definition (the legacy GUI bitmaps were unlimited). */
    public static final int MAX_RADIUS = 16;

    private ZonePlannerLogic() {
    }

    /**
     * The single-radius zone form (the task's minimal definition): the box centred on {@code center}, spanning
     * {@code +-radius} on every axis, with the radius clamped to {@code [0, MAX_RADIUS]}. Never returns {@code null}
     * &mdash; radius 0 is the planner's own block cell.
     */
    public static BoxZone around(BlockPos center, int radius) {
        int clamped = Math.max(0, Math.min(MAX_RADIUS, radius));
        return new BoxZone(center.offset(-clamped, -clamped, -clamped), center.offset(clamped, clamped, clamped));
    }

    /** Null-safe {@link BoxZone#contains(BlockPos)}: an undefined zone contains nothing. */
    public static boolean contains(@Nullable BoxZone zone, BlockPos pos) {
        return zone != null && zone.contains(pos);
    }

    /** The zone's cell volume (the {@code [M417]} bounds log number), 0 for an undefined zone. */
    public static int volume(@Nullable BoxZone zone) {
        if (zone == null) {
            return 0;
        }
        BlockPos min = zone.min();
        BlockPos max = zone.max();
        return (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
    }
}
