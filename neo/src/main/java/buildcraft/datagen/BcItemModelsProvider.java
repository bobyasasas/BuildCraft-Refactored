/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.datagen;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * M3.4: regenerates the item asset pair of one mod — the 1.21.4+ item model definition ({@code assets/<ns>/items/<id>.json})
 * and, for every non-block item, the flat layer0 placeholder model it points at ({@code assets/<ns>/models/item/<id>.json}).
 *
 * <p>Regeneration contract ("no drift" vs the shipped assets, verified by the runData diff gate):
 * <ul>
 * <li>Block items point their definition at {@code <ns>:block/<id>} — except {@code buildcraftcore:pipe_kinesis_wood}
 * which points at {@code buildcraftcore:block/pipe_kinesis_wood_inventory} (the legacy inventory-view model). Block
 * items get NO {@code models/item/<id>.json}: the shipped asset set has none for them either.</li>
 * <li>Every other item gets a definition pointing at {@code <ns>:item/<id>} plus a model
 * {@code {"parent": "minecraft:item/generated", "textures": {"layer0": "minecraft:item/<placeholder>"}}}. The
 * placeholder texture comes from {@link #placeholderTexture(String)}: the explicit exception tables first, then the
 * semantic rules (pipes by colour, robots, boards, chipsets, buckets, plugs...), and anything unmapped throws — a new
 * registered item MUST be given an entry here, silently emitting a different texture than shipped would defeat the
 * drift gate.</li>
 * </ul>
 * The rules and the exception tables were audited against the shipped files (932 models across the 8 mods) before
 * being written down here; the diff gate re-proves the mapping on every run.
 */
public final class BcItemModelsProvider extends BcDatagenProvider {

    /** Vanilla texture stem (after {@code minecraft:item/}) per exception; consulted before every rule. */
    private static final Map<String, String> TEXTURE_EXCEPTIONS = Map.ofEntries(
            Map.entry("waterproof", "water_bucket"),
            Map.entry("wire", "redstone"),
            Map.entry("robot_station", "ender_eye"),
            Map.entry("robot_googles", "spyglass"),
            Map.entry("redstone_crystal", "amethyst_shard"),
            Map.entry("gate_copier", "paper"),
            Map.entry("oil_placer", "stick"),
            Map.entry("snapshot_template", "book"),
            Map.entry("snapshot_blueprint", "writable_book"),
            Map.entry("schematic_single", "map"),
            Map.entry("filler_planner", "paper"),
            Map.entry("plastic_sheet", "paper"),
            Map.entry("gel", "slime_ball"),
            Map.entry("water_gel_spawn", "slime_ball"),
            Map.entry("glob_oil", "slime_ball"));

    /** Items whose shipped placeholder is the vanilla stick texture (tools, plugs, gears, ...). */
    private static final Map<String, String> STICK_TEXTURES = Map.ofEntries(
            Map.entry("gear_diamond", "stick"),
            Map.entry("gear_gold", "stick"),
            Map.entry("gear_iron", "stick"),
            Map.entry("gear_stone", "stick"),
            Map.entry("gear_wood", "stick"),
            Map.entry("fragile_fluid_shard", "stick"),
            Map.entry("goggles", "stick"),
            Map.entry("list", "stick"),
            Map.entry("map_location", "stick"),
            Map.entry("marker_connector", "stick"),
            Map.entry("paintbrush_clean", "stick"),
            Map.entry("volume_box", "stick"),
            Map.entry("wrench", "stick"),
            Map.entry("debugger", "stick"),
            Map.entry("guide", "stick"),
            Map.entry("guide_note", "stick"),
            Map.entry("plug_facade", "stick"),
            Map.entry("plug_light_sensor", "stick"),
            Map.entry("plug_pulsar", "stick"),
            Map.entry("plug_timer", "stick"),
            Map.entry("plug_lens", "stick"),
            Map.entry("plug_blocker", "stick"),
            Map.entry("plug_power_adaptor", "stick"));

    /** The 16 dye colours; two-token colours first so {@code _light_blue} is not read as {@code _blue}. */
    private static final List<String> DYE_COLORS = List.of(
            "light_blue", "light_gray", "black", "blue", "brown", "cyan", "gray", "green",
            "lime", "magenta", "orange", "pink", "purple", "red", "white", "yellow");

    /** The one block item whose definition targets a dedicated inventory model instead of {@code <ns>:block/<id>}. */
    private static final String PIPE_KINESIS_INVENTORY_TARGET = "buildcraftcore:block/pipe_kinesis_wood_inventory";

    private final Iterable<DeferredHolder<Item, ? extends Item>> items;

    public BcItemModelsProvider(String modid, PackOutput output, Iterable<DeferredHolder<Item, ? extends Item>> items) {
        super(modid, output);
        this.items = items;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (DeferredHolder<Item, ? extends Item> holder : items) {
            String path = holder.getId().getPath();
            Item item = holder.get();
            if (item instanceof BlockItem) {
                // Definition only: the definition points at the block model, exactly like the shipped asset set.
                String target = "pipe_kinesis_wood".equals(path) && "buildcraftcore".equals(modid)
                        ? PIPE_KINESIS_INVENTORY_TARGET
                        : modid + ":block/" + path;
                futures.add(saveAsset(cache, definition(target), path, "items"));
            } else {
                String texture = placeholderTexture(path);
                String model = modid + ":item/" + path;
                futures.add(saveAsset(cache, definition(model), path, "items"));
                futures.add(saveAsset(cache, layer0Model(texture), path, "models/item"));
            }
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    /** The 1.21.4+ item model definition: {@code {"model": {"type": "minecraft:model", "model": <ref>}}}. */
    private JsonObject definition(String modelRef) {
        JsonObject def = new JsonObject();
        JsonObject modelObj = new JsonObject();
        modelObj.addProperty("type", "minecraft:model");
        modelObj.addProperty("model", modelRef);
        def.add("model", modelObj);
        return def;
    }

    /** The flat generated-layer placeholder model every non-block item ships with. */
    private JsonObject layer0Model(String textureStem) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:item/generated");
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", "minecraft:item/" + textureStem);
        model.add("textures", textures);
        return model;
    }

    /**
     * The shipped placeholder texture for one non-block item id path. Exception tables first, then the rules mirroring
     * the M2.x placeholder assignment; anything unmatched throws so new items force an explicit decision (fail-closed
     * drift protection).
     */
    private String placeholderTexture(String path) {
        String mapped = TEXTURE_EXCEPTIONS.get(path);
        if (mapped != null) {
            return mapped;
        }
        mapped = STICK_TEXTURES.get(path);
        if (mapped != null) {
            return mapped;
        }
        if (path.startsWith("pipe_")) {
            if (path.endsWith("_colorless")) {
                return "iron_nugget";
            }
            String dye = dyeColorSuffix(path);
            if (dye != null) {
                return dye + "_dye";
            }
        } else {
            String dye = dyeColorSuffix(path);
            if (dye != null && !path.startsWith("board_robot_")) {
                // coloured non-pipe items (the 16 colour paintbrushes) ship the stick placeholder
                return "stick";
            }
        }
        if (path.startsWith("robot_") && !path.startsWith("robot_station") && !path.startsWith("robot_googles")) {
            return "iron_ingot";
        }
        if (path.startsWith("board_robot_")) {
            return "repeater";
        }
        if (path.startsWith("chipset_")) {
            return "redstone";
        }
        if (path.startsWith("plug_gate")) {
            return "stick";
        }
        if (path.endsWith("_bucket")) {
            return "bucket";
        }
        throw new IllegalStateException(
                "BcItemModelsProvider: no placeholder texture rule for item '" + modid + ":" + path
                        + "'. Add an explicit entry — the M3.4 drift gate requires datagen to reproduce the shipped assets.");
    }

    /** The dye colour this id ends with, or null. Two-token colours win over their one-token prefixes. */
    private static String dyeColorSuffix(String path) {
        for (String color : DYE_COLORS) {
            if (path.endsWith("_" + color)) {
                return color;
            }
        }
        return null;
    }
}
