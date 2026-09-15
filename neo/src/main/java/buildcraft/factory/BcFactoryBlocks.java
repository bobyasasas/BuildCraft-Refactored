/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory;

import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central block registration for buildcraftfactory (task M2.4a skeleton). Empty for now: the 10 baseline block ids
 * register here as placeholders in M2.4b, following the {@code buildcraft.core.BcBlocks} pattern.
 */
public final class BcFactoryBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BuildCraftFactory.MOD_ID);

    private BcFactoryBlocks() {
    }
}
