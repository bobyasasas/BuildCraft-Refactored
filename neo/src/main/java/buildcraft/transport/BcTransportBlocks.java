/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.transport;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import buildcraft.lib.BcLangKeys;
import buildcraft.transport.block.PipeHolderBlock;

/**
 * Central block registration for buildcrafttransport (task M2.4c registry parity). Every block id the 1.20.1 registry
 * baseline attributes to {@code buildcrafttransport} registers here; since M4.6 the shared pipe block carries the real
 * {@link PipeHolderBlock} behaviour, the rest are plain placeholder {@link Block}s whose behaviour classes migrate in
 * M2.5+. Note the baseline deliberately has no item for {@code pipe_holder}, so none is invented in
 * {@link BcTransportItems}.
 */
public final class BcTransportBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BuildCraftTransport.MOD_ID);

    /** Placeholder for {@code buildcrafttransport:filtered_buffer} (legacy {@code BlockFilteredBuffer}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> FILTERED_BUFFER = BcLangKeys.simpleBlock(BLOCKS, "filtered_buffer",
            properties -> properties.strength(0.5F));

    /**
     * The shared pipe block (M4.6): one block, one block entity, every pipe family &mdash; the family/colour/plugs/
     * connections live in {@code PipeHolderBlockEntity} and the visual is a BER (legacy {@code BlockPipeHolder}).
     * Same properties as the M2.4c placeholder (strength 0.25/3, no occlusion), so the datagen output is unchanged.
     */
    public static final DeferredBlock<PipeHolderBlock> PIPE_HOLDER = BcLangKeys.block(BLOCKS, "pipe_holder",
            PipeHolderBlock::new,
            () -> BlockBehaviour.Properties.of().strength(0.25F, 3.0F).noOcclusion());

    private BcTransportBlocks() {
    }
}
