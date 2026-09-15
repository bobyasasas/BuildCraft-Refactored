/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.transport;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central block registration for buildcrafttransport (task M2.4c registry parity). Every block id the 1.20.1 registry
 * baseline attributes to {@code buildcrafttransport} registers here as a plain placeholder {@link Block}; the real
 * behaviour classes (legacy {@code BCTransportBlocks}) migrate in M2.5+. Note the baseline deliberately has no item for
 * {@code pipe_holder}, so none is invented in {@link BcTransportItems}.
 */
public final class BcTransportBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BuildCraftTransport.MOD_ID);

    /** Placeholder for {@code buildcrafttransport:filtered_buffer} (legacy {@code BlockFilteredBuffer}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> FILTERED_BUFFER = BLOCKS.registerSimpleBlock("filtered_buffer",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcrafttransport:pipe_holder} (legacy {@code BlockPipeHolder}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> PIPE_HOLDER = BLOCKS.registerSimpleBlock("pipe_holder",
            properties -> properties.strength(0.25F, 3.0F).noOcclusion());

    private BcTransportBlocks() {
    }
}
