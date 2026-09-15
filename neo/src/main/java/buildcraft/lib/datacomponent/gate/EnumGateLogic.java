/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.datacomponent.gate;

/**
 * Gate logic (AND / OR), ported for the M2.6 gate config schema from legacy
 * {@code buildcraft.silicon.gate.EnumGateLogic} (1.20.1): same constant order (the ordinal is the persisted byte) and
 * the same out-of-range fallback to {@code AND} used by {@code getByOrdinal}.
 */
public enum EnumGateLogic {
    AND,
    OR;

    public static final EnumGateLogic[] VALUES = values();

    public static EnumGateLogic getByOrdinal(int ord) {
        if (ord < 0 || ord >= VALUES.length) {
            return EnumGateLogic.AND;
        }
        return VALUES[ord];
    }
}
