/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central block registration for buildcraftcore (task M2.2a). Every block this mod registers gets a constant
 * {@link DeferredBlock} field here, registered through the single {@link #BLOCKS} holder on the mod event bus (see
 * {@link BuildCraftCore#BuildCraftCore(net.neoforged.bus.api.IEventBus)}).
 *
 * <p>This class is the template for the M2.4 registry migration (legacy {@code RegistrationHelper} +
 * {@code RegistryObject} fields become fields of this class): keep fields {@code public static final}, name them after
 * the registry path, and only use the holders' {@link DeferredBlock#value()} after registry events have run.
 */
public final class BcBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BuildCraftCore.MOD_ID);

    /**
     * M2.2a placeholder block: a plain {@link Block} with no behaviour, existing purely as compile- and runtime proof
     * that the registration pipeline works. M2.2b/c will replace it with the real engine and pipe blocks.
     */
    public static final DeferredBlock<Block> MARKER = BLOCKS.registerSimpleBlock("marker",
            properties -> properties.strength(0.5F));

    private BcBlocks() {
    }
}
