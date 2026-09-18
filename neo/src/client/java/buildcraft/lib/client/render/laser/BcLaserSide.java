/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.client.render.laser;

/**
 * The four long sides of one laser (M4.5 port of legacy
 * {@code buildcraft.lib.client.render.laser.LaserData_BC8.LaserSide}). A laser runs along the local +X axis; these are
 * the four faces of its (square) tube. {@code LEFT} faces local +Z and {@code RIGHT} faces local -Z, matching the
 * legacy naming.
 */
public enum BcLaserSide {
    TOP,
    BOTTOM,
    /** +Z */
    LEFT,
    /** -Z */
    RIGHT;

    public static final BcLaserSide[] VALUES = values();
}
