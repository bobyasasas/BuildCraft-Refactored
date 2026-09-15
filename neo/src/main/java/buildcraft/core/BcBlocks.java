/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import buildcraft.core.block.StoneEngineBlock;

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
     * that the registration pipeline works. M2.2c will add the pipe blocks next to it.
     */
    public static final DeferredBlock<Block> MARKER = BLOCKS.registerSimpleBlock("marker",
            properties -> properties.strength(0.5F));

    /**
     * M2.2b stone engine slice block (see {@link StoneEngineBlock}). Placeholder placement for the M2.2 slice only:
     * the real engine registry migration (legacy {@code BlockRegistry} + engine module) is M2.4/M2.9.
     */
    public static final DeferredBlock<StoneEngineBlock> ENGINE_STONE = BLOCKS.registerBlock("engine_stone",
            StoneEngineBlock::new, () -> BlockBehaviour.Properties.of().strength(3.5F));

    private BcBlocks() {
    }
}
