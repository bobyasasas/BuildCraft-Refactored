/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.transport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central item registration for buildcrafttransport (task M2.4c registry parity). The 1.20.1 registry baseline
 * attributes 787 item ids to {@code buildcrafttransport}, of which 782 are the coloured pipe variants that legacy
 * generates programmatically: {@code PipeRegistry#createItemForPipe} (via {@code ItemPipeHolder#createAndTag})
 * registers every pipe definition once as {@code <definition>_colorless} and once per {@link DyeColor} as
 * {@code <definition>_<color>}, i.e. 17 variants per family across 46 families
 * ({@code pipe_structure_cobblestone}, 16 {@code pipe_items_*}, 11 {@code pipe_fluids_*}, 9 {@code pipe_power_*},
 * 9 {@code pipe_rf_*}).
 *
 * <p>Following the M2.4c decision, those ids are <em>not</em> given one field each: the family prefixes live in
 * {@link #PIPE_FAMILIES}, the expansion loop below derives the 782 full ids exactly like legacy did, and every
 * registration lands in {@link #PIPE_ITEMS} keyed by registry path. The parity gametest asserts on the registry
 * contents rather than on fields. When the real pipe behaviour migrates (M2.5+) the placeholder supplier is replaced
 * family-by-family without touching the id list.
 *
 * <p>The remaining 5 items are ordinary explicit fields. Note the baseline deliberately has no item for the
 * {@code pipe_holder} block, so none is invented here.
 */
public final class BcTransportItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BuildCraftTransport.MOD_ID);

    /**
     * The 46 legacy pipe family prefixes, in legacy registration order (structure; item pipes by material; fluid
     * pipes; power pipes; rf pipes). Every family expands to {@code <family>_colorless} plus one entry per
     * {@link DyeColor#values()} (17 variants each, 782 ids total) &mdash; the id set is asserted against the 1.20.1
     * registry baseline by the {@code buildcraftcore:registry_parity} gametest.
     */
    private static final List<String> PIPE_FAMILIES = List.of(
            // structure pipes (1 family): legacy BCTransportPipes.structure
            "pipe_structure_cobblestone",
            // item pipes (16 families): legacy BCTransportPipes.*Item
            "pipe_items_wood",
            "pipe_items_cobblestone",
            "pipe_items_stone",
            "pipe_items_quartz",
            "pipe_items_iron",
            "pipe_items_gold",
            "pipe_items_clay",
            "pipe_items_sandstone",
            "pipe_items_void",
            "pipe_items_obsidian",
            "pipe_items_diamond",
            "pipe_items_diamond_wood",
            "pipe_items_lapis",
            "pipe_items_daizuli",
            "pipe_items_emzuli",
            "pipe_items_stripes",
            // fluid pipes (11 families): legacy BCTransportPipes.*Fluid
            "pipe_fluids_wood",
            "pipe_fluids_cobblestone",
            "pipe_fluids_stone",
            "pipe_fluids_quartz",
            "pipe_fluids_gold",
            "pipe_fluids_iron",
            "pipe_fluids_clay",
            "pipe_fluids_sandstone",
            "pipe_fluids_void",
            "pipe_fluids_diamond",
            "pipe_fluids_diamond_wood",
            // power pipes (9 families): legacy BCTransportPipes.*Power
            "pipe_power_wood",
            "pipe_power_cobblestone",
            "pipe_power_stone",
            "pipe_power_quartz",
            "pipe_power_iron",
            "pipe_power_gold",
            "pipe_power_sandstone",
            "pipe_power_diamond",
            "pipe_power_diamond_wood",
            // rf pipes (9 families): legacy BCTransportPipes.*Rf
            "pipe_rf_wood",
            "pipe_rf_cobblestone",
            "pipe_rf_stone",
            "pipe_rf_quartz",
            "pipe_rf_iron",
            "pipe_rf_gold",
            "pipe_rf_sandstone",
            "pipe_rf_diamond",
            "pipe_rf_diamond_wood");

    /**
     * Every registered pipe item id (registry path, without namespace) to its deferred item, keyed exactly like the
     * baseline. Populated by the static expansion of {@link #PIPE_FAMILIES}; 782 entries.
     */
    public static final Map<String, DeferredItem<Item>> PIPE_ITEMS = new HashMap<>();

    /** Placeholder for {@code buildcrafttransport:filtered_buffer} (item form of {@link BcTransportBlocks#FILTERED_BUFFER}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> FILTERED_BUFFER = ITEMS.registerSimpleBlockItem(BcTransportBlocks.FILTERED_BUFFER);

    /** Placeholder for {@code buildcrafttransport:plug_blocker} (legacy {@code ItemPluggableSimple}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_BLOCKER = ITEMS.registerSimpleItem("plug_blocker");

    /** Placeholder for {@code buildcrafttransport:plug_power_adaptor} (legacy {@code ItemPluggableSimple}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_POWER_ADAPTOR = ITEMS.registerSimpleItem("plug_power_adaptor");

    /** Placeholder for {@code buildcrafttransport:waterproof} (legacy {@code ItemBC_Neptune}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> WATERPROOF = ITEMS.registerSimpleItem("waterproof");

    /** Placeholder for {@code buildcrafttransport:wire} (legacy {@code ItemWire}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> WIRE = ITEMS.registerSimpleItem("wire");

    static {
        // Expand every pipe family into its 17 colour variants (colorless + the 16 dye colours) and register them all
        // as plain placeholder items, exactly like legacy PipeRegistry#createItemForPipe did. Placeholder only; the
        // real ItemPipeHolder behaviour migrates in M2.5+.
        List<String> ids = new ArrayList<>(PIPE_FAMILIES.size() * 17);
        for (String family : PIPE_FAMILIES) {
            ids.add(family + "_colorless");
            for (DyeColor color : DyeColor.values()) {
                ids.add(family + "_" + color.getName());
            }
        }
        for (String id : ids) {
            PIPE_ITEMS.put(id, ITEMS.registerSimpleItem(id));
        }
    }

    private BcTransportItems() {
    }
}
