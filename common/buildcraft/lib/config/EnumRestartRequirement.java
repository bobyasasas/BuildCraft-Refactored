/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.config;

@Deprecated
public enum EnumRestartRequirement {
    // NONE(false, false),
    // WORLD(true, false),
    NONE(false),
    WORLD(true),
    ;

    private final boolean restartWorld;

    EnumRestartRequirement(boolean restartWorld) {
        this.restartWorld = restartWorld;
    }


    public boolean hasBeenRestarted(EnumRestartRequirement requirement) {
        if (restartWorld && !requirement.restartWorld) return false;
        return true;
    }
}
