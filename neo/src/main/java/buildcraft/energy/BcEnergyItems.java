/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.energy;

import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central item registration for buildcraftenergy (task M2.4a skeleton). Empty for now: the 33 baseline item ids
 * register here as placeholders in M2.4b, following the {@code buildcraft.core.BcItems} pattern. The fluids (60
 * baseline ids) register with their own centre in M2.4b.
 */
public final class BcEnergyItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BuildCraftEnergy.MOD_ID);

    private BcEnergyItems() {
    }
}
