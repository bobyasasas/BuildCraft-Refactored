/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central block registration for buildcraftbuilders (task M2.4c registry parity). Every block id the 1.20.1 registry
 * baseline attributes to {@code buildcraftbuilders} registers here as a plain placeholder {@link Block}; the real
 * behaviour classes (legacy {@code BCBuildersBlocks}) migrate in M2.5+.
 */
public final class BcBuildersBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BuildCraftBuilders.MOD_ID);

    /** Placeholder for {@code buildcraftbuilders:architect} (legacy {@code BlockArchitect}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> ARCHITECT = BLOCKS.registerSimpleBlock("architect",
            properties -> properties.strength(5F, 10F));

    /** Placeholder for {@code buildcraftbuilders:builder} (legacy {@code BlockBuilder}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> BUILDER = BLOCKS.registerSimpleBlock("builder",
            properties -> properties.strength(5F, 10F));

    /** Placeholder for {@code buildcraftbuilders:filler} (legacy {@code BlockFiller}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> FILLER = BLOCKS.registerSimpleBlock("filler",
            properties -> properties.strength(5F, 10F));

    /** Placeholder for {@code buildcraftbuilders:frame} (legacy {@code BlockFrame}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> FRAME = BLOCKS.registerSimpleBlock("frame",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftbuilders:library} (legacy {@code BlockLibrary}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> LIBRARY = BLOCKS.registerSimpleBlock("library",
            properties -> properties.strength(5F, 10F));

    /** Placeholder for {@code buildcraftbuilders:marker_construction} (legacy {@code BlockMarker}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> MARKER_CONSTRUCTION = BLOCKS.registerSimpleBlock("marker_construction",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftbuilders:quarry} (legacy {@code BlockQuarry}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> QUARRY = BLOCKS.registerSimpleBlock("quarry",
            properties -> properties.strength(5F, 10F));

    /** Placeholder for {@code buildcraftbuilders:replacer} (legacy {@code BlockReplacer}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> REPLACER = BLOCKS.registerSimpleBlock("replacer",
            properties -> properties.strength(5F, 10F));

    private BcBuildersBlocks() {
    }
}
