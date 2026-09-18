/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.client.render.laser;

import net.minecraft.world.phys.Vec3;

/**
 * One laser in the world: its type, end points, thickness scale and lighting behaviour (M4.5 port of legacy
 * {@code buildcraft.lib.client.render.laser.LaserData_BC8}).
 *
 * @param scale world units per local laser unit (the legacy lasers use ~1/16, so local units are sprite pixels)
 * @param doubleFace legacy {@code doubleFace}: when true every quad is also emitted with a reversed winding, so the
 *            tube survives backface culling from both sides
 * @param minBlockLight legacy {@code minBlockLight}: floor applied to the sampled block light
 */
public record BcLaserData(BcLaserType type, Vec3 start, Vec3 end, double scale, boolean doubleFace,
    int minBlockLight) {

    public BcLaserData(BcLaserType type, Vec3 start, Vec3 end, double scale) {
        this(type, start, end, scale, false, 0);
    }

    public BcLaserData(BcLaserType type, Vec3 start, Vec3 end, double scale, boolean doubleFace) {
        this(type, start, end, scale, doubleFace, 0);
    }
}
