/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.transport;

import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central block registration for buildcrafttransport (task M2.4a skeleton). Empty for now: the 2 baseline block
 * ids register here as placeholders in M2.4c, following the {@code buildcraft.core.BcBlocks} pattern.
 */
public final class BcTransportBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BuildCraftTransport.MOD_ID);

    private BcTransportBlocks() {
    }
}
