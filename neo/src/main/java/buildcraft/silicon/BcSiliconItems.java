/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.silicon;

import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central item registration for buildcraftsilicon (task M2.4a skeleton). Empty for now: the 43 baseline item ids
 * register here as placeholders in M2.4b, following the {@code buildcraft.core.BcItems} pattern.
 */
public final class BcSiliconItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BuildCraftSilicon.MOD_ID);

    private BcSiliconItems() {
    }
}
