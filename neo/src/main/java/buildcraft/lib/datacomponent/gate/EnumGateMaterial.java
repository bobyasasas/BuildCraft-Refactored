/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.datacomponent.gate;

/**
 * Gate material, ported for the M2.6 gate config schema from legacy
 * {@code buildcraft.silicon.gate.EnumGateMaterial} (1.20.1): same constant order (the ordinal is the persisted byte)
 * and the same slot-count / modifiable flags. The legacy {@code block} field stays out of this schema port - it is
 * rendering/content data for the gate item task, not serialization state.
 */
public enum EnumGateMaterial {
    CLAY_BRICK(1, false),
    IRON(2, true),
    NETHER_BRICK(4, true),
    GOLD(8, true);

    public static final EnumGateMaterial[] VALUES = values();

    public final int numSlots;
    public final boolean canBeModified;

    EnumGateMaterial(int numSlots, boolean canBeModified) {
        this.numSlots = numSlots;
        this.canBeModified = canBeModified;
    }

    public static EnumGateMaterial getByOrdinal(int ord) {
        if (ord < 0 || ord >= VALUES.length) {
            return EnumGateMaterial.CLAY_BRICK;
        }
        return VALUES[ord];
    }
}
