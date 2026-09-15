/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central item registration for buildcraftbuilders (task M2.4c registry parity). Every item id the 1.20.1 registry
 * baseline attributes to {@code buildcraftbuilders} registers here: block items for exactly the eight ids the
 * baseline pairs with a block, plus four pure placeholder items. All placeholder, behaviour classes (legacy
 * {@code BCBuildersItems}) migrate in M2.5+.
 */
public final class BcBuildersItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BuildCraftBuilders.MOD_ID);

    /** Item form of the {@code buildcraftbuilders:architect} placeholder block (legacy {@code BlockItem} of {@code BlockArchitect}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> ARCHITECT = ITEMS.registerSimpleBlockItem(BcBuildersBlocks.ARCHITECT);

    /** Item form of the {@code buildcraftbuilders:builder} placeholder block (legacy {@code BlockItem} of {@code BlockBuilder}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> BUILDER = ITEMS.registerSimpleBlockItem(BcBuildersBlocks.BUILDER);

    /** Item form of the {@code buildcraftbuilders:filler} placeholder block (legacy {@code BlockItem} of {@code BlockFiller}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> FILLER = ITEMS.registerSimpleBlockItem(BcBuildersBlocks.FILLER);

    /** Placeholder for {@code buildcraftbuilders:filler_planner} (legacy {@code ItemRobotStation}-style blueprint item); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> FILLER_PLANNER = ITEMS.registerSimpleItem("filler_planner");

    /** Item form of the {@code buildcraftbuilders:frame} placeholder block (legacy {@code BlockItem} of {@code BlockFrame}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> FRAME = ITEMS.registerSimpleBlockItem(BcBuildersBlocks.FRAME);

    /** Item form of the {@code buildcraftbuilders:library} placeholder block (legacy {@code BlockItem} of {@code BlockLibrary}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> LIBRARY = ITEMS.registerSimpleBlockItem(BcBuildersBlocks.LIBRARY);

    /** Item form of the {@code buildcraftbuilders:marker_construction} placeholder block (legacy {@code BlockItem} of {@code BlockMarker}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> MARKER_CONSTRUCTION = ITEMS.registerSimpleBlockItem(BcBuildersBlocks.MARKER_CONSTRUCTION);

    /** Item form of the {@code buildcraftbuilders:quarry} placeholder block (legacy {@code BlockItem} of {@code BlockQuarry}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> QUARRY = ITEMS.registerSimpleBlockItem(BcBuildersBlocks.QUARRY);

    /** Item form of the {@code buildcraftbuilders:replacer} placeholder block (legacy {@code BlockItem} of {@code BlockReplacer}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> REPLACER = ITEMS.registerSimpleBlockItem(BcBuildersBlocks.REPLACER);

    /** Placeholder for {@code buildcraftbuilders:schematic_single} (legacy {@code ItemSchematicSingle}-style snapshot item); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> SCHEMATIC_SINGLE = ITEMS.registerSimpleItem("schematic_single");

    /** Placeholder for {@code buildcraftbuilders:snapshot_blueprint} (legacy {@code ItemSnapshot} blueprint variant); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> SNAPSHOT_BLUEPRINT = ITEMS.registerSimpleItem("snapshot_blueprint");

    /** Placeholder for {@code buildcraftbuilders:snapshot_template} (legacy {@code ItemSnapshot} template variant); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> SNAPSHOT_TEMPLATE = ITEMS.registerSimpleItem("snapshot_template");

    private BcBuildersItems() {
    }
}
