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
 * M3.4 + M4.2: regenerates the item asset pair of one mod — the 1.21.4+ item model definition
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
 * {@code models/item/<id>.json}, like the shipped asset set.</li>
 * <li>Every other item gets a definition pointing at {@code <ns>:item/<id>} plus a {@code models/item/<id>.json}.
 * M4.2 swapped in the 1.20.1 baseline's real-texture flat models for every item whose texture shipped (gears,
 * goggles, wrench, paintbrushes, chipsets, waterproof, the robot boards and robot bodies, ...) and keeps the M3.4
 * vanilla placeholder ({@code minecraft:item/<stem>}) for the rest — items whose baseline model is a variable or
 * override construct outside M4.2 scope (list, map_location, wire, gate_copier, plugs, pipes, buckets, snapshots,
 * ...) or whose baseline texture did not ship. Anything unmapped throws — a new registered item MUST be given an
 * entry here; silently emitting a different texture than shipped would defeat the drift gate.</li>
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

    /** The 16 dye colours; two-token colours first so {@code _light_blue} is not read as {@code _blue}. */
    private static final List<String> DYE_COLORS = List.of(
            "light_blue", "light_gray", "black", "blue", "brown", "cyan", "gray", "green",
            "lime", "magenta", "orange", "pink", "purple", "red", "white", "yellow");

    /** The one block item whose definition targets a dedicated inventory model instead of a block model path. */
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
                futures.add(saveAsset(cache, definition(blockItemTarget(path)), path, "items"));
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

    /** The model of one non-block item: the baseline's real-texture model where M4.2 migrated it, else the M3.4
     * vanilla placeholder. Fail-closed for unknown ids. */
    private JsonObject itemModel(String path) {
        if (isRobotBody(path)) {
            // every robot body parents the shared geometry with its own skin
            return BcModelJson.parented("buildcraftrobotics:item/robot/base",
                    BcModelJson.tex("all", "buildcraftrobotics:item/robot/" + path));
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
     * force an explicit decision (fail-closed drift protection).
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
                // coloured non-pipe items have no vanilla stand-in closer than the stick
                return "stick";
            }
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
