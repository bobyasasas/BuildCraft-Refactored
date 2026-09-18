/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders;

import buildcraft.lib.BcLangKeys;
import buildcraft.builders.block.FillerBlock;
import buildcraft.builders.block.QuarryBlock;
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
    public static final DeferredBlock<Block> ARCHITECT = BcLangKeys.simpleBlock(BLOCKS, "architect",
            properties -> properties.strength(5F, 10F));

    /** Placeholder for {@code buildcraftbuilders:builder} (legacy {@code BlockBuilder}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> BUILDER = BcLangKeys.simpleBlock(BLOCKS, "builder",
            properties -> properties.strength(5F, 10F));

    /**
     * {@code buildcraftbuilders:filler} (legacy {@code BlockFiller}); M2.12 replaced the plain placeholder block with
     * the real ticking {@link FillerBlock} under the same id and property set (zero new registry ids, the M2.4c
     * baseline blockstate palette stays byte-identical).
     */
    public static final DeferredBlock<FillerBlock> FILLER = BcLangKeys.block(BLOCKS, "filler",
            properties -> new FillerBlock(properties.strength(5F, 10F)));

    /** Placeholder for {@code buildcraftbuilders:frame} (legacy {@code BlockFrame}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> FRAME = BcLangKeys.simpleBlock(BLOCKS, "frame",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftbuilders:library} (legacy {@code BlockLibrary}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> LIBRARY = BcLangKeys.simpleBlock(BLOCKS, "library",
            properties -> properties.strength(5F, 10F));

    /** Placeholder for {@code buildcraftbuilders:marker_construction} (legacy {@code BlockMarker}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> MARKER_CONSTRUCTION = BcLangKeys.simpleBlock(BLOCKS, "marker_construction",
            properties -> properties.strength(0.5F));

    /**
     * {@code buildcraftbuilders:quarry} (legacy {@code BlockQuarry}); M2.12 replaced the plain placeholder block with
     * the real ticking {@link QuarryBlock} under the same id and property set (zero new registry ids, the M2.4c
     * baseline blockstate palette stays byte-identical).
     */
    public static final DeferredBlock<QuarryBlock> QUARRY = BcLangKeys.block(BLOCKS, "quarry",
            properties -> new QuarryBlock(properties.strength(5F, 10F)));

    /** Placeholder for {@code buildcraftbuilders:replacer} (legacy {@code BlockReplacer}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> REPLACER = BcLangKeys.simpleBlock(BLOCKS, "replacer",
            properties -> properties.strength(5F, 10F));

    private BcBuildersBlocks() {
    }
}
