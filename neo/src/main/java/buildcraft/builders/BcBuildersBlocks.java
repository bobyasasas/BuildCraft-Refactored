/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders;

import buildcraft.lib.BcLangKeys;
import buildcraft.builders.block.ArchitectBlock;
import buildcraft.builders.block.BuilderBlock;
import buildcraft.builders.block.ConstructionMarkerBlock;
import buildcraft.builders.block.FillerBlock;
import buildcraft.builders.block.LibraryBlock;
import buildcraft.builders.block.QuarryBlock;
import buildcraft.builders.block.ReplacerBlock;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central block registration for buildcraftbuilders (task M2.4c registry parity). Every block id the 1.20.1 registry
 * baseline attributes to {@code buildcraftbuilders} registers here; the placeholder plain {@link Block}s migrate to
 * their real behaviour classes as the content lands (M2.12: filler, quarry; M4.17: architect, builder, library,
 * marker_construction, replacer under the unchanged ids).
 */
public final class BcBuildersBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BuildCraftBuilders.MOD_ID);

    /**
     * {@code buildcraftbuilders:architect} (legacy {@code BlockArchitectTable}); M4.17 replaced the plain placeholder
     * block with the ticking {@link ArchitectBlock} under the same id and property set (zero new registry ids, the
     * M2.4c baseline blockstate palette stays byte-identical).
     */
    public static final DeferredBlock<ArchitectBlock> ARCHITECT = BcLangKeys.block(BLOCKS, "architect",
            properties -> new ArchitectBlock(properties.strength(5F, 10F)));

    /**
     * {@code buildcraftbuilders:builder} (legacy {@code BlockBuilder}); M4.17 replaced the plain placeholder block
     * with the ticking {@link BuilderBlock} under the same id and property set (zero new registry ids, the M2.4c
     * baseline blockstate palette stays byte-identical).
     */
    public static final DeferredBlock<BuilderBlock> BUILDER = BcLangKeys.block(BLOCKS, "builder",
            properties -> new BuilderBlock(properties.strength(5F, 10F)));

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

    /**
     * {@code buildcraftbuilders:library} (legacy {@code BlockElectronicLibrary}); M4.17 replaced the plain placeholder
     * block with the real {@link LibraryBlock} under the same id and property set (zero new registry ids, the M2.4c
     * baseline blockstate palette stays byte-identical).
     */
    public static final DeferredBlock<LibraryBlock> LIBRARY = BcLangKeys.block(BLOCKS, "library",
            properties -> new LibraryBlock(properties.strength(5F, 10F)));

    /**
     * {@code buildcraftbuilders:marker_construction} (legacy {@code BlockMarkerConstruction}); M4.17 replaced the
     * plain placeholder block with the real {@link ConstructionMarkerBlock} under the same id and property set (zero
     * new registry ids, the M2.4c baseline blockstate palette stays byte-identical).
     */
    public static final DeferredBlock<ConstructionMarkerBlock> MARKER_CONSTRUCTION = BcLangKeys.block(BLOCKS,
            "marker_construction", properties -> new ConstructionMarkerBlock(properties.strength(0.5F)));

    /**
     * {@code buildcraftbuilders:quarry} (legacy {@code BlockQuarry}); M2.12 replaced the plain placeholder block with
     * the real ticking {@link QuarryBlock} under the same id and property set (zero new registry ids, the M2.4c
     * baseline blockstate palette stays byte-identical).
     */
    public static final DeferredBlock<QuarryBlock> QUARRY = BcLangKeys.block(BLOCKS, "quarry",
            properties -> new QuarryBlock(properties.strength(5F, 10F)));

    /**
     * {@code buildcraftbuilders:replacer} (legacy {@code BlockReplacer}); M4.17 replaced the plain placeholder block
     * with the ticking {@link ReplacerBlock} under the same id and property set (zero new registry ids, the M2.4c
     * baseline blockstate palette stays byte-identical).
     */
    public static final DeferredBlock<ReplacerBlock> REPLACER = BcLangKeys.block(BLOCKS, "replacer",
            properties -> new ReplacerBlock(properties.strength(5F, 10F)));

    private BcBuildersBlocks() {
    }
}
