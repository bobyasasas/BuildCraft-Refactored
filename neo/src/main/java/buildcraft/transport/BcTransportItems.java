/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.transport;

import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central item registration for buildcrafttransport (task M2.4a skeleton). Empty for now: the 787 baseline item
 * ids register here as placeholders in M2.4c, following the {@code buildcraft.core.BcItems} pattern. The 785 pipe
 * item ids register with the pipe system in M2.4c.
 */
public final class BcTransportItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BuildCraftTransport.MOD_ID);

    private BcTransportItems() {
    }
}
