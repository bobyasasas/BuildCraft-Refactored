/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.blockentity;

/**
 * M4.4 port of the legacy {@code buildcraft.api.enums.EnumPowerStage} (the BuildCraft API submodule is not on the neo
 * classpath, and only the engines need it): the five heat/power stages an engine's piston trunk texture cycles through
 * plus the creative engine's {@link #BLACK}. Serialised names are the legacy lowercase ones.
 */
public enum EnumPowerStage {
    BLUE("blue"),
    GREEN("green"),
    YELLOW("yellow"),
    RED("red"),
    OVERHEAT("overheat"),
    /** Creative-engine only: always "running", never changes. */
    BLACK("black");

    public final String serializedName;

    EnumPowerStage(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    /** The legacy {@code TileEngineBase_BC8#computePowerStage} thresholds, applied to a 0..1 power/heat level. */
    public static EnumPowerStage fromLevel(double level) {
        if (level < 0.25) {
            return BLUE;
        } else if (level < 0.5) {
            return GREEN;
        } else if (level < 0.75) {
            return YELLOW;
        } else if (level < 0.85) {
            return RED;
        } else {
            return OVERHEAT;
        }
    }

    /** The legacy {@code TileEngineBase_BC8#getPistonSpeed} table (progress gained per tick per stage). */
    public double getPistonSpeed() {
        return switch (this) {
            case BLUE -> 0.02;
            case GREEN -> 0.04;
            case YELLOW -> 0.08;
            case RED -> 0.12;
            // OVERHEAT and BLACK don't animate through this table (creative overrides it)
            default -> 0;
        };
    }
}
