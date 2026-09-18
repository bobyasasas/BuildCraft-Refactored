/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * M3.4 + M4.2 + M4.3: regenerates the item asset pair of one mod — the 1.21.4+ item model definition
 * ({@code assets/<ns>/items/<id>.json}) and the model it points at.
 *
 * <p>Regeneration contract ("no drift" vs the shipped assets, verified by the runData diff gate):
 * <ul>
 * <li>Block items point their definition at the block model of the block's default state — {@code <ns>:block/<id>}
 * for the blocks whose model kept its flat path, and the baseline's real sub-path for the blocks whose static model
 * M4.2 moved into a subdirectory or another variant (e.g. {@code builders:block/builder/main},
 * {@code factory:block/flood_gate/true_×5}, {@code silicon:block/table/<name>}, {@code minecraft:block/bedrock} for
 * the springs) — mirroring what the 1.20.1 item models parented. Exception unchanged from M3.4:
 * {@code buildcraftcore:pipe_kinesis_wood} targets {@code block/pipe_kinesis_wood_inventory}. Block items get NO
 * {@code models/item/<id>.json} <em>except</em> the M4.3 overrides below.</li>
 * <li>M4.3 dynamic-item overrides — the items whose 1.20.1 baseline model was a {@code BakedModel} bake
 * ({@code ModelItemSimple} over the {@code .jsonbc} variable models) or a display-carrying wrapper get real-texture
 * item models, replacing the M2/M3.4 vanilla placeholders:
 * <ul>
 * <li>the five engines freeze the baseline's bake-time variable state (progress 0.2, power stage BLUE — BLACK for the
 * creative engine, whose {@code trunk_black} abstracts onto {@code trunk_overheat}) into an elements model parented to
 * {@code minecraft:block/block} (its display block is byte-equal to the baseline {@code ModelItemSimple.TRANSFORM_BLOCK})
 * with {@code gui_light} front, matching {@code ModelItemSimple#usesBlockLight()} {@code == false};</li>
     * <li>every {@code buildcrafttransport} pipe id gets the baseline {@code ModelPipeItem} item segment (the
     * {@code QUADS_SAME} cuboid) textured with its family's item texture; both the pipe segment and the plugs carry
     * front GUI light, matching the {@code ModelItemSimple} the baseline {@code PipeItemOverride} resolved to
     * ({@code usesBlockLight() == false});</li>
 * <li>{@code plug_blocker}/{@code plug_power_adaptor} get the baseline plug cuboids with the baseline
 * {@code ModelItemSimple.TRANSFORM_PLUG_AS_ITEM} display and front GUI light;</li>
 * <li>{@code builders:builder}, {@code factory:tank}, {@code factory:chute} and {@code factory:distiller} get a
 * {@code models/item/<id>.json} wrapper parenting the block model plus the baseline item display block (the M4.2
 * carry-over: the baseline item models carried the display that their parent-less block models lack);</li>
 * </ul>
 * these overrides emit a {@code models/item/<id>.json} and repoint the definition at it — the geometry/transform
 * tables below were transcribed from the baseline {@code buildcraft_resources} jsonbc models and generated item
 * models.</li>
 * <li>Transport additionally emits {@code assets/minecraft/atlases/blocks.json} (the M4.3 pipes atlas entry): the
 * pipe/plug item models reference {@code textures/pipes/} sprites, which no vanilla atlas directory rule covers, so
 * a {@code minecraft:directory} source stitches them into the shared blocks atlas — the same coverage the 1.20.1
 * baseline's generated atlas provided through one {@code minecraft:single} entry per sprite.</li>
 * <li>Every other item gets a definition pointing at {@code <ns>:item/<id>} plus a {@code models/item/<id>.json}.
 * M4.2 swapped in the 1.20.1 baseline's real-texture flat models for every item whose texture shipped (gears,
 * goggles, wrench, paintbrushes, chipsets, waterproof, the robot boards and robot bodies, ...) and keeps the M3.4
 * vanilla placeholder ({@code minecraft:item/<stem>}) for the rest — items whose baseline model is a variable or
 * override construct outside M4.2 scope (list, map_location, wire, gate_copier, pipes handled by M4.3, buckets,
 * snapshots, ...) or whose baseline texture did not ship. Anything unmapped throws — a new registered item MUST be
 * given an entry here; silently emitting a different texture than shipped would defeat the drift gate.</li>
 * <li>Robotics additionally emits the shared robot body geometry once at {@code models/item/robot/base.json} (the
 * baseline's top-level {@code robot.json}). A top-level file would register as the item id {@code robotics:robot},
 * which is not a registered item — the S2 gate whitelists that id as base-only — so the sub-path keeps the geometry
 * invisible to the id space while {@code robot_base.json}/{@code robot_<type>.json} parent it exactly like the
 * baseline did.</li>
 * </ul>
 */
public final class BcItemModelsProvider extends BcDatagenProvider {

    /** Vanilla texture stem (after {@code minecraft:item/}) per exception; consulted before every rule. */
    private static final Map<String, String> TEXTURE_EXCEPTIONS = Map.ofEntries(
            Map.entry("wire", "redstone"),
            Map.entry("robot_station", "ender_eye"),
            Map.entry("robot_googles", "spyglass"),
            Map.entry("gate_copier", "paper"),
            Map.entry("snapshot_template", "book"),
            Map.entry("snapshot_blueprint", "writable_book"),
            Map.entry("schematic_single", "map"),
            Map.entry("filler_planner", "paper"));

    /** Items whose shipped placeholder is the vanilla stick texture (plugs, box tools, ...). */
    private static final Map<String, String> STICK_TEXTURES = Map.ofEntries(
            Map.entry("fragile_fluid_shard", "stick"),
            Map.entry("list", "stick"),
            Map.entry("map_location", "stick"),
            Map.entry("volume_box", "stick"),
            Map.entry("plug_facade", "stick"),
            Map.entry("plug_light_sensor", "stick"),
            Map.entry("plug_pulsar", "stick"),
            Map.entry("plug_timer", "stick"),
            Map.entry("plug_lens", "stick"),
            Map.entry("plug_blocker", "stick"),
            Map.entry("plug_power_adaptor", "stick"));

    /** The board colour each robot board item shows (the baseline {@code robotics:item/board/<colour>} texture). */
    private static final Map<String, String> BOARD_ROBOT_COLORS = Map.ofEntries(
            Map.entry("board_robot_bomber", "red"),
            Map.entry("board_robot_builder", "yellow"),
            Map.entry("board_robot_butcher", "blue"),
            Map.entry("board_robot_carrier", "green"),
            Map.entry("board_robot_delivery", "green"),
            Map.entry("board_robot_empty", "clean"),
            Map.entry("board_robot_farmer", "blue"),
            Map.entry("board_robot_fluid_carrier", "green"),
            Map.entry("board_robot_harvester", "blue"),
            Map.entry("board_robot_knight", "red"),
            Map.entry("board_robot_leave_cutter", "blue"),
            Map.entry("board_robot_lumberjack", "blue"),
            Map.entry("board_robot_miner", "blue"),
            Map.entry("board_robot_picker", "green"),
            Map.entry("board_robot_planter", "blue"),
            Map.entry("board_robot_pump", "blue"),
            Map.entry("board_robot_shovelman", "blue"),
            Map.entry("board_robot_stripes", "yellow"));

    /**
     * M4.3: the five engines, by full item id, to the namespace holding their {@code block/engine/<type>} back/side
     * textures (the baseline {@code models/tile/engine_*.jsonbc} texture tables: wood/creative live in core, the
     * combustion engines in energy).
     */
    private static final Map<String, String> ENGINE_TEXTURE_NAMESPACES = Map.ofEntries(
            Map.entry("buildcraftcore:engine_wood", "buildcraftcore"),
            Map.entry("buildcraftcore:engine_creative", "buildcraftcore"),
            Map.entry("buildcraftcore:engine_stone", "buildcraftenergy"),
            Map.entry("buildcraftcore:engine_iron", "buildcraftenergy"),
            Map.entry("buildcraftcore:engine_rf", "buildcraftenergy"));

    /**
     * M4.3: the pipe families whose item texture is not the plain {@code <flow>_<material>} stem — the baseline
     * {@code PipeDefinitionBuilder.itemTex(...)} index picks a suffix (wood/iron use the unfilled {@code _clear}
     * sprite, the diamond item pipes the {@code _itemstack} sprite, the limiter pipes the {@code _m128} sprite). The
     * coloured families (lapis/daizuli/emzuli) are handled separately in {@link #pipeItemTexture}. Every emitted
     * reference is checked to exist under {@code assets/buildcrafttransport/textures/pipes/} (fail-closed).
     */
    private static final Map<String, String> PIPE_TEXTURE_OVERRIDES = Map.ofEntries(
            Map.entry("items_wood", "items_wood_clear"),
            Map.entry("items_iron", "items_iron_clear"),
            Map.entry("items_diamond", "items_diamond_itemstack"),
            Map.entry("items_diamond_wood", "items_diamond_wood_clear"),
            Map.entry("fluids_wood", "fluids_wood_clear"),
            Map.entry("fluids_iron", "fluids_iron_clear"),
            Map.entry("fluids_diamond_wood", "fluids_diamond_wood_clear"),
            Map.entry("power_wood", "power_wood_clear"),
            Map.entry("power_iron", "power_iron_m128"),
            Map.entry("power_diamond", "power_diamond_m128"),
            Map.entry("power_diamond_wood", "power_diamond_wood_clear"),
            Map.entry("rf_wood", "rf_wood_clear"),
            Map.entry("rf_iron", "rf_iron_m128"),
            Map.entry("rf_diamond", "rf_diamond_m128"),
            Map.entry("rf_diamond_wood", "rf_diamond_wood_clear"));

    /** The pipe texture stems for the two colour-indexed item pipe families ({@code _colorless} included). */
    private static final String LAPIS_COLORLESS_TEXTURE = "items_lapis_base";
    private static final String DAIZULI_COLORLESS_TEXTURE = "items_daizuli_filled";
    private static final String EMZULI_TEXTURE = "items_emzuli_clear";

    /** The one block item whose definition targets a dedicated inventory model instead of a block model path. */
    private static final String PIPE_KINESIS_INVENTORY_TARGET = "buildcraftcore:block/pipe_kinesis_wood_inventory";

    /**
     * M4.3: the baseline's block-style item display block, shared verbatim by the {@code builder}/{@code tank}/
     * {@code chute} item models (and byte-equal to the {@code minecraft:block/block} display the {@code distiller}
     * item model inherited); taken from {@code buildcraft_resources_generated} item models.
     */
    private static final Map<String, BcModelJson.Transform> BLOCK_ITEM_DISPLAY = Map.ofEntries(
            Map.entry("firstperson_lefthand", new BcModelJson.Transform(
                    new int[]{0, 225, 0}, new double[]{0.4, 0.4, 0.4}, null)),
            Map.entry("firstperson_righthand", new BcModelJson.Transform(
                    new int[]{0, 45, 0}, new double[]{0.4, 0.4, 0.4}, null)),
            Map.entry("fixed", new BcModelJson.Transform(
                    null, new double[]{0.5, 0.5, 0.5}, null)),
            Map.entry("ground", new BcModelJson.Transform(
                    null, new double[]{0.25, 0.25, 0.25}, new double[]{0, 3, 0})),
            Map.entry("gui", new BcModelJson.Transform(
                    new int[]{30, 225, 0}, new double[]{0.625, 0.625, 0.625}, null)),
            Map.entry("thirdperson_righthand", new BcModelJson.Transform(
                    new int[]{75, 45, 0}, new double[]{0.375, 0.375, 0.375}, new double[]{0, 2.5, 0})));

    /**
     * M4.3: the baseline {@code ModelItemSimple.TRANSFORM_PLUG_AS_ITEM} (the static-init values of the 1.20.1
     * {@code ModelItemSimple}), display of the two plug item models; the {@code head} entry is the all-defaults
     * transform and is omitted.
     */
    private static final Map<String, BcModelJson.Transform> PLUG_DISPLAY = Map.ofEntries(
            Map.entry("firstperson_lefthand", new BcModelJson.Transform(
                    new int[]{0, 225, 0}, new double[]{0.4, 0.4, 0.4}, new double[]{0, 0, -4})),
            Map.entry("firstperson_righthand", new BcModelJson.Transform(
                    new int[]{0, 45, 0}, new double[]{0.4, 0.4, 0.4}, new double[]{0, 0, -4})),
            Map.entry("fixed", new BcModelJson.Transform(
                    null, new double[]{0.85, 0.85, 0.85}, null)),
            Map.entry("ground", new BcModelJson.Transform(
                    null, new double[]{0.5, 0.5, 0.5}, new double[]{0, 3, 0})),
            Map.entry("gui", new BcModelJson.Transform(
                    new int[]{0, 90, 0}, null, null)),
            Map.entry("thirdperson_lefthand", new BcModelJson.Transform(
                    new int[]{75, 45, 0}, new double[]{0.375, 0.375, 0.375}, new double[]{0, 2.5, 0})),
            Map.entry("thirdperson_righthand", new BcModelJson.Transform(
                    new int[]{75, 225, 0}, new double[]{0.375, 0.375, 0.375}, new double[]{0, 2.5, 0})));

    /** The 16 dye colours; two-token colours first so {@code _light_blue} is not read as {@code _blue}. */
    private static final List<String> DYE_COLORS = List.of(
            "light_blue", "light_gray", "black", "blue", "brown", "cyan", "gray", "green",
            "lime", "magenta", "orange", "pink", "purple", "red", "white", "yellow");

    private final Iterable<DeferredHolder<Item, ? extends Item>> items;

    public BcItemModelsProvider(String modid, PackOutput output, Iterable<DeferredHolder<Item, ? extends Item>> items) {
        super(modid, output);
        this.items = items;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        if ("buildcrafttransport".equals(modid)) {
            // M4.3: the pipe item models reference sprites under textures/pipes/, which no vanilla atlas directory
            // source covers — stitch them into the shared minecraft blocks atlas like the 1.20.1 baseline's
            // generated assets/minecraft/atlases/blocks.json did (single source per sprite there, one directory
            // rule here: same coverage, no per-texture maintenance). Cross-namespace on purpose, hence no saveAsset.
            JsonObject atlas = new JsonObject();
            JsonArray sources = new JsonArray();
            JsonObject directory = new JsonObject();
            directory.addProperty("type", "minecraft:directory");
            directory.addProperty("source", "pipes");
            directory.addProperty("prefix", "pipes/");
            sources.add(directory);
            atlas.add("sources", sources);
            futures.add(BcDatagenJson.save(cache, atlas, paths.asset("minecraft", "atlases", "blocks"),
                    DataProvider.KEY_COMPARATOR));
        }
        for (DeferredHolder<Item, ? extends Item> holder : items) {
            String path = holder.getId().getPath();
            Item item = holder.get();
            if (item instanceof BlockItem) {
                String modelPath = blockItemModelOverride(path);
                if (modelPath == null) {
                    futures.add(saveAsset(cache, definition(blockItemTarget(path)), path, "items"));
                } else {
                    // M4.3: real-geometry or display-wrapper item model, definition repointed at it
                    futures.add(saveAsset(cache, definition(modid + ":" + modelPath), path, "items"));
                    futures.add(saveAsset(cache, blockItemModel(path), path, "models/item"));
                }
            } else {
                futures.add(saveAsset(cache, definition(modid + ":item/" + path), path, "items"));
                futures.add(saveAsset(cache, itemModel(path), path, "models/item"));
            }
        }
        if ("buildcraftrobotics".equals(modid)) {
            // the shared robot body (baseline robot.json) — sub-path so it is not misread as the item id robotics:robot
            futures.add(saveAsset(cache, robotBaseModel(), "robot/base", "models/item"));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    // ---------------------------------------------------------------------------------------------- item definitions

    /** The block model a block item's definition points at: the default-state model of the M4.2 block assets. */
    private String blockItemTarget(String path) {
        return switch (modid + ":" + path) {
            case "buildcraftcore:decorated_blueprint" -> "buildcraftcore:block/decorated/blueprint";
            case "buildcraftcore:decorated_destroy" -> "buildcraftcore:block/decorated/destroy";
            case "buildcraftcore:decorated_laser_back" -> "buildcraftcore:block/decorated/laser_back";
            case "buildcraftcore:decorated_leather" -> "buildcraftcore:block/decorated/leather";
            case "buildcraftcore:decorated_paper" -> "buildcraftcore:block/decorated/paper";
            case "buildcraftcore:decorated_template" -> "buildcraftcore:block/decorated/template";
            case "buildcraftcore:spring_oil", "buildcraftcore:spring_water" -> "minecraft:block/bedrock";
            case "buildcraftcore:pipe_kinesis_wood" -> PIPE_KINESIS_INVENTORY_TARGET;
            case "buildcraftbuilders:architect" -> "buildcraftbuilders:block/architect_off";
            case "buildcraftbuilders:builder" -> "buildcraftbuilders:block/builder/main";
            case "buildcraftbuilders:filler" -> "buildcraftbuilders:block/filler/main";
            case "buildcraftfactory:flood_gate" -> "buildcraftfactory:block/flood_gate/true_true_true_true_true";
            case "buildcraftsilicon:advanced_crafting_table" -> "buildcraftsilicon:block/table/advanced_crafting";
            case "buildcraftsilicon:assembly_table" -> "buildcraftsilicon:block/table/assembly";
            case "buildcraftsilicon:charging_table" -> "buildcraftsilicon:block/table/charging";
            case "buildcraftsilicon:integration_table" -> "buildcraftsilicon:block/table/integration";
            case "buildcraftsilicon:programming_table" -> "buildcraftsilicon:block/table/programming";
            default -> modid + ":block/" + path;
        };
    }

    /**
     * The {@code models/item} path (without the {@code item/} prefix) a block item overrides its definition with, or
     * null when the definition points straight at the block model (the default M3.4/M4.2 shape). M4.3: the five
     * engines (frozen jsonbc geometry) and the four baseline display-wrapper items (the M4.2 carry-over).
     */
    private String blockItemModelOverride(String path) {
        if (ENGINE_TEXTURE_NAMESPACES.containsKey(modid + ":" + path)) {
            return "item/" + path;
        }
        return switch (modid + ":" + path) {
            case "buildcraftbuilders:builder", "buildcraftfactory:chute", "buildcraftfactory:distiller",
                    "buildcraftfactory:tank" -> "item/" + path;
            default -> null;
        };
    }

    /** The item model of a block item with a {@link #blockItemModelOverride} — engines get the frozen jsonbc
     * geometry, the four wrapper items parent the block model plus the baseline display block. */
    private JsonObject blockItemModel(String path) {
        if (ENGINE_TEXTURE_NAMESPACES.containsKey(modid + ":" + path)) {
            return engineItemModel(path);
        }
        return BcModelJson.withDisplay(BcModelJson.parented(blockItemTarget(path), null), BLOCK_ITEM_DISPLAY);
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

    // ---------------------------------------------------------------------------------------------------- item models

    /** The model of one non-block item: the baseline's real-texture model where M4.2/M4.3 migrated it, else the
     * M3.4 vanilla placeholder. Fail-closed for unknown ids. */
    private JsonObject itemModel(String path) {
        if (isRobotBody(path)) {
            // every robot body parents the shared geometry with its own skin
            return BcModelJson.parented("buildcraftrobotics:item/robot/base",
                    BcModelJson.tex("all", "buildcraftrobotics:item/robot/" + path));
        }
        String pipeTexture = pipeItemTexture(path);
        if (pipeTexture != null) {
            return pipeItemModel(pipeTexture);
        }
        String plugTexture = plugItemTexture(path);
        if (plugTexture != null) {
            return plugItemModel(path, plugTexture);
        }
        String layer0 = realItemTexture(path);
        String parent = "minecraft:item/generated";
        if (layer0 == null) {
            layer0 = "minecraft:item/" + placeholderTexture(path);
        } else if ("buildcraftcore".equals(modid)
                && (path.equals("wrench") || path.equals("marker_connector") || path.startsWith("paintbrush_"))) {
            parent = "minecraft:item/handheld";
        }
        return BcModelJson.parented(parent, BcModelJson.tex("layer0", layer0));
    }

    // ------------------------------------------------------------------------------------------------- M4.3 overrides

    /**
     * The pipe texture stem (after {@code buildcrafttransport:pipes/}) for one pipe item id, or null for non-pipe
     * ids. Mirrors the baseline {@code PipeDefinition} item sprite resolution: coloured families carry one texture
     * per colour ({@code lapis}/{@code daizuli}, with the dedicated {@code _base}/{@code _filled} sprite for the
     * colourless variant), the {@code emzuli} family is {@code _clear} throughout, and every plain family uses its
     * {@link #PIPE_TEXTURE_OVERRIDES} stem or the bare {@code <flow>_<material>}. Unknown families fail-closed
     * through the texture-existence check in {@link #pipeItemModel}.
     */
    private String pipeItemTexture(String path) {
        if (!"buildcrafttransport".equals(modid) || !path.startsWith("pipe_")) {
            return null;
        }
        String familyAndVariant = path.substring("pipe_".length());
        String family = familyAndVariant;
        String colour = null;
        for (String dye : DYE_COLORS) {
            if (familyAndVariant.endsWith("_" + dye)) {
                colour = dye;
                family = familyAndVariant.substring(0, familyAndVariant.length() - dye.length() - 1);
                break;
            }
        }
        if (colour == null && familyAndVariant.endsWith("_colorless")) {
            colour = "colorless";
            family = familyAndVariant.substring(0, familyAndVariant.length() - "_colorless".length());
        }
        if ("items_lapis".equals(family)) {
            return "colorless".equals(colour) ? LAPIS_COLORLESS_TEXTURE : "items_lapis_" + colour;
        }
        if ("items_daizuli".equals(family)) {
            return "colorless".equals(colour) ? DAIZULI_COLORLESS_TEXTURE : "items_daizuli_" + colour;
        }
        if ("items_emzuli".equals(family)) {
            return EMZULI_TEXTURE;
        }
        return PIPE_TEXTURE_OVERRIDES.getOrDefault(family, family);
    }

    /** The baseline {@code ModelPipeItem} item segment: the {@code QUADS_SAME} cuboid (4/16 wide, full height,
     * side UVs 4,0→12,16, cap UVs 4,4→12,12) under the {@code minecraft:block/block} display (= the baseline
     * {@code TRANSFORM_BLOCK}) with the family's real pipe texture and front GUI light (the resolved baseline
     * {@code ModelItemSimple} reports {@code usesBlockLight() == false}). */
    private JsonObject pipeItemModel(String textureStem) {
        validateTexture("buildcrafttransport", "pipes/" + textureStem);
        String texture = "buildcrafttransport:pipes/" + textureStem;
        JsonObject model = BcModelJson.elements("minecraft:block/block", BcModelJson.tex("particle", texture, "pipe", texture),
                BcModelJson.Element.of(4, 0, 4, 12, 16, 12)
                        .face("down", "#pipe", null, null, 4, 4, 12, 12)
                        .face("up", "#pipe", null, null, 4, 4, 12, 12)
                        .face("north", "#pipe", null, null, 4, 0, 12, 16)
                        .face("south", "#pipe", null, null, 4, 0, 12, 16)
                        .face("west", "#pipe", null, null, 4, 0, 12, 16)
                        .face("east", "#pipe", null, null, 4, 0, 12, 16));
        model.addProperty("gui_light", "front");
        return model;
    }

    /** The pipe plug texture stem, or null for other items ({@code pipes/plug}, {@code pipes/power_adapter}). */
    private String plugItemTexture(String path) {
        if (!"buildcrafttransport".equals(modid)) {
            return null;
        }
        return switch (path) {
            case "plug_blocker" -> "pipes/plug";
            case "plug_power_adaptor" -> "pipes/power_adapter";
            default -> null;
        };
    }

    /** The baseline plug item model ({@code models/plugs/<plug>.jsonbc}): the shaft cuboid (plus the adapter's
     * sleeve), the {@code TRANSFORM_PLUG_AS_ITEM} display and front GUI light ({@code usesBlockLight() == false}). */
    private JsonObject plugItemModel(String path, String textureStem) {
        validateTexture("buildcrafttransport", textureStem);
        String texture = "buildcrafttransport:" + textureStem;
        JsonObject model = BcModelJson.elements("minecraft:block/block", BcModelJson.tex("all", texture, "particle", texture),
                BcModelJson.Element.of(2, 4, 4, 4.01, 12, 12)
                        .face("down", "#all", null, null, 2, 4, 4, 12)
                        .face("up", "#all", null, null, 2, 4, 4, 12)
                        .face("north", "#all", null, null, 2, 4, 4, 12)
                        .face("south", "#all", null, null, 2, 4, 4, 12)
                        .face("west", "#all", null, null, 4, 4, 12, 12)
                        .face("east", "#all", null, null, 4, 4, 12, 12));
        if ("plug_power_adaptor".equals(path)) {
            model.get("elements").getAsJsonArray().add(BcModelJson.Element.of(0, 3, 3, 2, 13, 13)
                    .face("down", "#all", null, null, 0, 3, 2, 13)
                    .face("up", "#all", null, null, 0, 3, 2, 13)
                    .face("north", "#all", null, null, 0, 3, 2, 13)
                    .face("south", "#all", null, null, 0, 3, 2, 13)
                    .face("west", "#all", null, null, 3, 3, 13, 13)
                    .face("east", "#all", null, null, 3, 3, 13, 13)
                    .toJson());
        }
        return BcModelJson.withDisplay(model, PLUG_DISPLAY);
    }

    /**
     * The M4.3 engine item model: the baseline {@code engine_base.jsonbc} geometry frozen at the bake-time variable
     * state ({@code progress = 0.2} → {@code progress_size = 3.198}, stage BLUE; the creative engine baked BLACK,
     * whose {@code trunk_black} abstracts onto {@code trunk_overheat}), parented to {@code minecraft:block/block}
     * (= the baseline {@code TRANSFORM_BLOCK}) with front GUI light ({@code ModelItemSimple#usesBlockLight()} false).
     */
    private JsonObject engineItemModel(String path) {
        String namespace = ENGINE_TEXTURE_NAMESPACES.get(modid + ":" + path);
        if (namespace == null) {
            throw new IllegalStateException("BcItemModelsProvider: engine without texture namespace " + modid + ":" + path);
        }
        String type = path.substring("engine_".length());
        String trunkStem = "creative".equals(type) ? "block/engine/trunk_overheat" : "block/engine/trunk_blue";
        validateTexture(namespace, "block/engine/" + type + "/back");
        validateTexture(namespace, "block/engine/" + type + "/side");
        validateTexture("buildcraftlib", trunkStem);
        validateTexture("buildcraftlib", "block/engine/chamber_base");
        JsonObject model = BcModelJson.elements("minecraft:block/block", BcModelJson.tex(
                "back", namespace + ":block/engine/" + type + "/back",
                "chamber", "buildcraftlib:block/engine/chamber_base",
                "particle", namespace + ":block/engine/" + type + "/back",
                "side", namespace + ":block/engine/" + type + "/side",
                "trunk", "buildcraftlib:" + trunkStem),
                // base: the fixed foot block, back texture on the caps, side texture around the rim
                BcModelJson.Element.of(0, 0, 0, 16, 4, 16)
                        .face("down", "#back", null, null, 0, 0, 16, 16)
                        .face("up", "#back", null, null, 0, 0, 16, 16)
                        .face("north", "#side", null, null, 0, 0, 16, 4)
                        .face("south", "#side", null, null, 0, 0, 16, 4)
                        .face("west", "#side", null, null, 0, 0, 16, 4)
                        .face("east", "#side", null, null, 0, 0, 16, 4),
                // chamber: the expanding piston pocket, revealed portion of the texture (v runs 3.198→0)
                BcModelJson.Element.of(3, 4, 3, 13, 7.198, 13)
                        .face("north", "#chamber", null, null, 3, 3.198, 13, 0)
                        .face("south", "#chamber", null, null, 3, 3.198, 13, 0)
                        .face("west", "#chamber", null, null, 3, 3.198, 13, 0)
                        .face("east", "#chamber", null, null, 3, 3.198, 13, 0),
                // base_moving: the travelling middle block at progress 0.2 (y 4+3.198 → 8+3.198)
                BcModelJson.Element.of(0, 7.198, 0, 16, 11.198, 16)
                        .face("down", "#back", null, null, 0, 0, 16, 16)
                        .face("up", "#back", null, null, 0, 0, 16, 16)
                        .face("north", "#side", null, null, 0, 0, 16, 4)
                        .face("south", "#side", null, null, 0, 0, 16, 4)
                        .face("west", "#side", null, null, 0, 0, 16, 4)
                        .face("east", "#side", null, null, 0, 0, 16, 4),
                // trunk: the power stalk, stage-coloured (baseline item bake froze stage BLUE / creative BLACK)
                BcModelJson.Element.of(4, 4, 4, 12, 16, 12)
                        .face("down", "#trunk", null, null, 0, 0, 8, 8)
                        .face("up", "#trunk", null, null, 0, 0, 8, 8)
                        .face("north", "#trunk", null, null, 8, 0, 16, 12)
                        .face("south", "#trunk", null, null, 8, 0, 16, 12)
                        .face("west", "#trunk", null, null, 8, 0, 16, 12)
                        .face("east", "#trunk", null, null, 8, 0, 16, 12));
        model.addProperty("gui_light", "front");
        return model;
    }

    /** Fail-closed texture check: every emitted real-texture reference must exist in the shipped (source-set)
     * resources — a typo would otherwise render the checkerboard the drift gates cannot see. */
    private static void validateTexture(String namespace, String texturePath) {
        if (BcItemModelsProvider.class.getResource("/assets/" + namespace + "/textures/" + texturePath + ".png") == null) {
            throw new IllegalStateException(
                    "BcItemModelsProvider: texture reference assets/" + namespace + "/textures/" + texturePath
                            + ".png does not exist in the resources");
        }
    }

    /** Whether this robotics id is a robot body ({@code robot_base} or one of the 17 {@code robot_<type>} skins). */
    private boolean isRobotBody(String path) {
        return "buildcraftrobotics".equals(modid)
                && (path.equals("robot_base") || path.startsWith("robot_"))
                && !path.equals("robot_station")
                && !path.equals("robot_googles");
    }

    /**
     * The real 1.20.1 texture reference ({@code layer0}) for one migrated item, or null if the item keeps its
     * placeholder. Every reference below was checked to exist under {@code assets/<ns>/textures/}.
     */
    private String realItemTexture(String path) {
        return switch (modid) {
            case "buildcraftlib" -> switch (path) {
                case "debugger" -> "buildcraftlib:item/debugger";
                case "guide" -> "buildcraftlib:item/guide_book";
                case "guide_note" -> "buildcraftlib:item/guide_note";
                default -> null;
            };
            case "buildcraftcore" -> switch (path) {
                case "gear_diamond", "gear_gold", "gear_iron", "gear_stone", "gear_wood", "goggles", "wrench",
                        "marker_connector" -> "buildcraftcore:item/" + path;
                default -> path.startsWith("paintbrush_")
                        ? "buildcraftcore:item/paintbrush/" + path.substring("paintbrush_".length())
                        : null;
            };
            case "buildcraftenergy" -> switch (path) {
                case "glob_oil", "oil_placer" -> "buildcraftenergy:item/glob_oil";
                default -> null;
            };
            case "buildcraftfactory" -> switch (path) {
                case "gel" -> "buildcraftfactory:item/gel";
                case "plastic_sheet" -> "buildcraftfactory:item/plastic_sheet";
                case "water_gel_spawn" -> "buildcraftfactory:item/water_gel";
                default -> null;
            };
            case "buildcraftsilicon" -> switch (path) {
                case "chipset_diamond", "chipset_gold", "chipset_iron", "chipset_quartz" ->
                    "buildcraftsilicon:item/redstone_chipset/" + path.substring("chipset_".length());
                case "chipset_redstone" -> "buildcraftsilicon:item/redstone_chipset/red";
                case "redstone_crystal" -> "buildcraftsilicon:item/redstone_crystal";
                default -> null;
            };
            case "buildcrafttransport" -> "waterproof".equals(path) ? "buildcrafttransport:item/pipewaterproof" : null;
            case "buildcraftrobotics" -> BOARD_ROBOT_COLORS.containsKey(path)
                    ? "buildcraftrobotics:item/board/" + BOARD_ROBOT_COLORS.get(path)
                    : null;
            default -> null;
        };
    }

    /** The shared robot body: one textured box with hand/GUI transforms, no parent, no own texture. */
    private JsonObject robotBaseModel() {
        return BcModelJson.withDisplay(
                BcModelJson.elements((String) null, null,
                        BcModelJson.Element.of(4, 4, 4, 12, 12, 12)
                                .face("down", "#all", null, null, 4, 0, 16, 4)
                                .face("east", "#all", null, null, 12, 4, 16, 8)
                                .face("north", "#all", null, null, 0, 4, 4, 8)
                                .face("south", "#all", null, null, 4, 4, 8, 8)
                                .face("up", "#all", null, null, 8, 0, 12, 4)
                                .face("west", "#all", null, null, 8, 4, 12, 8)),
                Map.of(
                        "firstperson_lefthand", new BcModelJson.Transform(
                                new int[]{0, 225, 0}, new double[]{0.64, 0.64, 0.64}, null),
                        "firstperson_righthand", new BcModelJson.Transform(
                                new int[]{0, 45, 0}, new double[]{0.64, 0.64, 0.64}, null),
                        "fixed", new BcModelJson.Transform(
                                null, new double[]{0.8, 0.8, 0.8}, null),
                        "ground", new BcModelJson.Transform(
                                null, new double[]{0.4, 0.4, 0.4}, new double[]{0, 3, 0}),
                        "gui", new BcModelJson.Transform(
                                new int[]{30, 225, 0}, null, null),
                        "thirdperson_righthand", new BcModelJson.Transform(
                                new int[]{75, 45, 0}, new double[]{0.6, 0.6, 0.6}, new double[]{0, 2.5, 0})));
    }

    // ----------------------------------------------------------------------------------------------------- placeholders

    /**
     * The shipped placeholder texture stem (after {@code minecraft:item/}) for one non-block item id path. Exception
     * tables first, then the rules mirroring the M2.x placeholder assignment; anything unmatched throws so new items
     * force an explicit decision (fail-closed drift protection). The M2.x {@code pipe_*} rules are gone since M4.3:
     * pipe ids resolve to real pipe textures in {@link #pipeItemTexture} and never reach this method.
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
        String dye = dyeColorSuffix(path);
        if (dye != null && !path.startsWith("board_robot_")) {
            // coloured non-pipe items have no vanilla stand-in closer than the stick
            return "stick";
        }
        if (path.startsWith("chipset_")) {
            return "redstone";
        }
        if (path.startsWith("plug_")) {
            return "stick";
        }
        if (path.endsWith("_bucket")) {
            return "bucket";
        }
        throw new IllegalStateException(
                "BcItemModelsProvider: no placeholder texture rule for item '" + modid + ":" + path
                        + "'. Add an explicit entry — the drift gate requires datagen to reproduce the shipped assets.");
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
