/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.datacomponent.gate;

/**
 * Gate modifier, ported for the M2.6 gate config schema from legacy
 * {@code buildcraft.silicon.gate.EnumGateModifier} (1.20.1): same constant order (the ordinal is the persisted byte)
 * and the same parameter counts / slot divisor.
 */
public enum EnumGateModifier {
    NO_MODIFIER(0, 0, 1),
    LAPIS(1, 0, 1),
    QUARTZ(1, 1, 2),
    DIAMOND(3, 3, 2);

    public static final EnumGateModifier[] VALUES = values();

    public final int triggerParams, actionParams;
    public final int slotDivisor;

    EnumGateModifier(int triggerParams, int actionParams, int slotDivisor) {
        this.triggerParams = triggerParams;
        this.actionParams = actionParams;
        this.slotDivisor = slotDivisor;
    }

    public static EnumGateModifier getByOrdinal(int ord) {
        if (ord < 0 || ord >= VALUES.length) {
            return EnumGateModifier.NO_MODIFIER;
        }
        return VALUES[ord];
    }
}
