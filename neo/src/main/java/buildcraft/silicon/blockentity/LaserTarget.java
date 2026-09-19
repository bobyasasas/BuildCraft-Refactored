/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.silicon.blockentity;

/**
 * A machine the silicon laser can feed, mirroring the legacy {@code buildcraft.api.mj.ILaserTarget} semantics
 * (read-only API reference): the laser scans for targets, asks each how much it still needs and pushes that much of
 * its battery into {@link #receiveLaserPower}, which returns the part it did NOT accept (legacy {@code receivePower}
 * excess convention, the {@code MjReceiver} shape of this module).
 */
public interface LaserTarget {

    /** The &micro;MJ this target still needs before its work can run ({@code getTarget() - power} in the legacy
     * table base); 0 means "nothing to do &mdash; do not aim here". */
    long getRequiredLaserPower();

    /**
     * Accepts up to the still-required amount of &micro;J ({@code simulate} is not part of the legacy contract: the
     * laser always really pushes) and returns the excess &mdash; the portion of {@code microJoules} that was NOT
     * accepted, so the laser can refund it into its battery.
     */
    long receiveLaserPower(long microJoules);

    /** Legacy {@code ILaserTarget#isInvalidTarget}: true once the target block entity is going away. */
    boolean isInvalidTarget();
}
