/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.transport.pipe;

import java.util.List;
import net.minecraft.world.item.DyeColor;

/**
 * M4.6 data table of the 46 legacy pipe families (task M2.4c registry parity prefixes, visual slice of legacy
 * {@code buildcraft.transport.BCTransportPipes}). Each family carries only what the world renderer and the minimal
 * transport slice need: the flow kind, whether it is a wooden pipe (wooden pipes refuse to connect to the same
 * behaviour — legacy {@code PipeBehaviourWood#canConnect} / {@code PipeBehaviourWoodPower#canConnect}) and the world
 * texture resolution.
 *
 * <p><b>Texture resolution (mirrors the baseline {@code PipeDefinition} texture tables, index 0 = the placed look):</b>
 * legacy built every definition's texture array as {@code prefix + suffixes[]} in registration order, and the default
 * behaviour texture index is 0 ({@code PipeBehaviour#getTextureIndex}). So {@code items_wood} (suffixes
 * {@code _clear, _filled}) shows {@code items_wood_clear} in the world, {@code power_iron} (limiter suffixes
 * {@code _m0.._m128}) shows {@code power_iron_m0}, and plain families show their bare stem. The colour-indexed
 * families ({@code lapis}/{@code daizuli}) pick the suffix from the pipe's dye colour (the colourless variants use
 * the dedicated {@code _base}/{@code _filled} sprites, matching the M4.3 item model resolution). Every stem named
 * here is checked to exist under {@code assets/buildcrafttransport/textures/pipes/} by
 * {@code BcPipeFamiliesTest} (fail-closed).
 */
public final class BcPipeFamilies {

    /** The flow kind of a pipe family (legacy {@code PipeFlowType} slice: rf pipes render like power pipes here). */
    public enum FlowKind {
        ITEMS,
        FLUIDS,
        POWER,
        STRUCTURE
    }

    /** How the family's six world face textures are picked. */
    public enum TextureKind {
        /** One texture for every face: {@code <stem>} (index 0 of the definition's texture array). */
        PLAIN,
        /** Wooden pipe variant: index 0 ({@code _clear}) everywhere. */
        CLEAR_FILLED,
        /** The limiter pipes: index 0 of {@code _m0.._m128}. */
        LIMITER,
        /** One texture per {@link DyeColor} (plus a colourless fallback stem). */
        COLOURED
    }

    /** One pipe family (legacy {@code PipeDefinition} visual slice). */
    public static final class Family {
        /** The registry id stem, e.g. {@code items_wood} ({@code pipe_items_wood_*} items, legacy identifier). */
        public final String stem;
        public final FlowKind flow;
        public final TextureKind textures;
        /** True for the wooden behaviours, which refuse direct wood-to-wood connections (legacy canConnect). */
        public final boolean wooden;
        /** The texture stem used for the colourless variant of a {@code COLOURED} family. */
        public final String colorlessTexture;

        Family(String stem, FlowKind flow, TextureKind textures, boolean wooden, String colorlessTexture) {
            this.stem = stem;
            this.flow = flow;
            this.textures = textures;
            this.wooden = wooden;
            this.colorlessTexture = colorlessTexture;
        }

        /** The family's registry prefix ({@code pipe_ + stem}), i.e. the item id stem of all 17 variants. */
        public String idPrefix() {
            return "pipe_" + this.stem;
        }
    }

    /** All 46 families in legacy registration order ({@code BCTransportPipes.preInit}). */
    public static final List<Family> FAMILIES = List.of(
        new Family("structure_cobblestone", FlowKind.STRUCTURE, TextureKind.PLAIN, false, ""),
        // item pipes (16): legacy BCTransportPipes.*Item
        new Family("items_wood", FlowKind.ITEMS, TextureKind.CLEAR_FILLED, true, ""),
        new Family("items_cobblestone", FlowKind.ITEMS, TextureKind.PLAIN, false, ""),
        new Family("items_stone", FlowKind.ITEMS, TextureKind.PLAIN, false, ""),
        new Family("items_quartz", FlowKind.ITEMS, TextureKind.PLAIN, false, ""),
        new Family("items_iron", FlowKind.ITEMS, TextureKind.CLEAR_FILLED, false, ""),
        new Family("items_gold", FlowKind.ITEMS, TextureKind.PLAIN, false, ""),
        new Family("items_clay", FlowKind.ITEMS, TextureKind.PLAIN, false, ""),
        new Family("items_sandstone", FlowKind.ITEMS, TextureKind.PLAIN, false, ""),
        new Family("items_void", FlowKind.ITEMS, TextureKind.PLAIN, false, ""),
        new Family("items_obsidian", FlowKind.ITEMS, TextureKind.PLAIN, false, ""),
        new Family("items_diamond", FlowKind.ITEMS, TextureKind.PLAIN, false, ""),
        new Family("items_diamond_wood", FlowKind.ITEMS, TextureKind.CLEAR_FILLED, true, ""),
        new Family("items_lapis", FlowKind.ITEMS, TextureKind.COLOURED, false, "items_lapis_base"),
        new Family("items_daizuli", FlowKind.ITEMS, TextureKind.COLOURED, false, "items_daizuli_filled"),
        new Family("items_emzuli", FlowKind.ITEMS, TextureKind.CLEAR_FILLED, false, ""),
        new Family("items_stripes", FlowKind.ITEMS, TextureKind.PLAIN, false, ""),
        // fluid pipes (11): legacy BCTransportPipes.*Fluid
        new Family("fluids_wood", FlowKind.FLUIDS, TextureKind.CLEAR_FILLED, true, ""),
        new Family("fluids_cobblestone", FlowKind.FLUIDS, TextureKind.PLAIN, false, ""),
        new Family("fluids_stone", FlowKind.FLUIDS, TextureKind.PLAIN, false, ""),
        new Family("fluids_quartz", FlowKind.FLUIDS, TextureKind.PLAIN, false, ""),
        new Family("fluids_gold", FlowKind.FLUIDS, TextureKind.PLAIN, false, ""),
        new Family("fluids_iron", FlowKind.FLUIDS, TextureKind.CLEAR_FILLED, false, ""),
        new Family("fluids_clay", FlowKind.FLUIDS, TextureKind.PLAIN, false, ""),
        new Family("fluids_sandstone", FlowKind.FLUIDS, TextureKind.PLAIN, false, ""),
        new Family("fluids_void", FlowKind.FLUIDS, TextureKind.PLAIN, false, ""),
        new Family("fluids_diamond", FlowKind.FLUIDS, TextureKind.PLAIN, false, ""),
        new Family("fluids_diamond_wood", FlowKind.FLUIDS, TextureKind.CLEAR_FILLED, true, ""),
        // power pipes (9): legacy BCTransportPipes.*Power (limiter behaviour for iron/diamond)
        new Family("power_wood", FlowKind.POWER, TextureKind.CLEAR_FILLED, true, ""),
        new Family("power_cobblestone", FlowKind.POWER, TextureKind.PLAIN, false, ""),
        new Family("power_stone", FlowKind.POWER, TextureKind.PLAIN, false, ""),
        new Family("power_quartz", FlowKind.POWER, TextureKind.PLAIN, false, ""),
        new Family("power_iron", FlowKind.POWER, TextureKind.LIMITER, false, ""),
        new Family("power_gold", FlowKind.POWER, TextureKind.PLAIN, false, ""),
        new Family("power_sandstone", FlowKind.POWER, TextureKind.PLAIN, false, ""),
        new Family("power_diamond", FlowKind.POWER, TextureKind.LIMITER, false, ""),
        new Family("power_diamond_wood", FlowKind.POWER, TextureKind.CLEAR_FILLED, true, ""),
        // rf pipes (9): same visuals as power pipes; the rf flow itself has not migrated (renders like power)
        new Family("rf_wood", FlowKind.POWER, TextureKind.CLEAR_FILLED, true, ""),
        new Family("rf_cobblestone", FlowKind.POWER, TextureKind.PLAIN, false, ""),
        new Family("rf_stone", FlowKind.POWER, TextureKind.PLAIN, false, ""),
        new Family("rf_quartz", FlowKind.POWER, TextureKind.PLAIN, false, ""),
        new Family("rf_iron", FlowKind.POWER, TextureKind.LIMITER, false, ""),
        new Family("rf_gold", FlowKind.POWER, TextureKind.PLAIN, false, ""),
        new Family("rf_sandstone", FlowKind.POWER, TextureKind.PLAIN, false, ""),
        new Family("rf_diamond", FlowKind.POWER, TextureKind.LIMITER, false, ""),
        new Family("rf_diamond_wood", FlowKind.POWER, TextureKind.CLEAR_FILLED, true, ""));

    /** The two limiter texture tables ({@code _m0.._m128}, legacy suffix order). */
    private static final String[] LIMITER_SUFFIXES = { "_m0", "_m4", "_m8", "_m16", "_m32", "_m64", "_m128" };

    private BcPipeFamilies() {
    }

    /** Looks a family up by its registry stem ({@code items_wood} etc.), or null for unknown stems. */
    public static Family byStem(String stem) {
        for (Family family : FAMILIES) {
            if (family.stem.equals(stem)) {
                return family;
            }
        }
        return null;
    }

    /**
     * Splits a pipe item registry path ({@code pipe_items_daizuli_red}) into its family stem and colour ({@code null}
     * for the {@code _colorless} variant). Longest dye-name suffix match wins so {@code _light_blue} is not read as
     * {@code _blue} — the same trick the M4.3 item model provider used.
     */
    public static Family parse(String itemIdPath, DyeColor[] colourOut) {
        colourOut[0] = null;
        if (!itemIdPath.startsWith("pipe_")) {
            return null;
        }
        String rest = itemIdPath.substring("pipe_".length());
        if (rest.endsWith("_colorless")) {
            return byStem(rest.substring(0, rest.length() - "_colorless".length()));
        }
        DyeColor[] colors = DyeColor.values();
        // longest suffix match first, so "_light_blue" wins over "_blue"
        DyeColor best = null;
        int bestLen = 0;
        for (DyeColor color : colors) {
            String suffix = "_" + color.getName();
            if (rest.endsWith(suffix) && suffix.length() > bestLen) {
                best = color;
                bestLen = suffix.length();
            }
        }
        Family family = best == null ? byStem(rest) : byStem(rest.substring(0, rest.length() - bestLen));
        if (family != null && best != null) {
            colourOut[0] = best;
        }
        return family;
    }

    /**
     * The world texture stem ({@code buildcrafttransport:pipes/<stem>}) for the family's placed, unexcited look:
     * texture index 0 of the legacy definition's array (see class javadoc), or the colour-indexed sprite for
     * {@code COLOURED} families.
     */
    public static String textureStem(Family family, DyeColor colour) {
        return switch (family.textures) {
            case PLAIN -> family.stem;
            case CLEAR_FILLED -> family.stem + "_clear";
            case LIMITER -> family.stem + LIMITER_SUFFIXES[0];
            case COLOURED -> colour == null ? family.colorlessTexture : family.stem + "_" + colour.getName();
        };
    }
}
