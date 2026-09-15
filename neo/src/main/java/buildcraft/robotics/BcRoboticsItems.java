/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.robotics;

import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central item registration for buildcraftrobotics (task M2.4a skeleton). Empty for now: the 40 baseline item ids
 * register here as placeholders in M2.4c, following the {@code buildcraft.core.BcItems} pattern. The 17 baseline
 * entity ids register with their own centre in M2.4c.
 */
public final class BcRoboticsItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BuildCraftRobotics.MOD_ID);

    private BcRoboticsItems() {
    }
}
