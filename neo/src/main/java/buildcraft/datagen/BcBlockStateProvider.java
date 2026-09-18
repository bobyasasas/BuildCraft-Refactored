/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.datagen;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * M4.2: regenerates the block asset pair of one mod — the blockstate ({@code assets/<ns>/blockstates/<id>.json}) and
 * the block models it references ({@code assets/<ns>/models/block/<path>.json}) — with the 1.20.1 baseline's
 * <em>static</em> model content (real BuildCraft textures), replacing the M3.2/M3.4 concrete-cube placeholders.
 *
 * <p>Scope ruling (M4.2, supersedes the M3.2 "one placeholder model per block" ruling, which the datagen gate's N3
 * note already declared void):
 * <ul>
 * <li><b>Migrated</b>: blocks whose baseline model is pure JSON (vanilla parent or parent-less elements). All of them
 * keep the single-variant {@code ""} blockstate the placeholder era established, because the 26.1.2 block classes are
 * property-less placeholders (no {@code facing}/{@code connected_*}/{@code stage}) — a variant key the block does not
 * have would silently resolve to the missing model at runtime. The variant therefore points at the baseline model of
 * the block's default state (e.g. {@code architect_off}, {@code water_gel/gel}, {@code flood_gate/false_×5}), and
 * multi-variant only sub-models that need a property to be selected are not emitted. Two documented deviations, both
 * rendering-correctness fixes for baseline quirks: {@code autoworkbench_item} uses parent {@code cube} instead of the
 * baseline's self-contradictory {@code cube_all}+per-face map (which renders the missing {@code #all} texture), and
 * the 30 energy fluid blocks point their particle at the shipped {@code buildcraftlib:block/fluid/heat_N_still} base
 * texture (the baseline referenced runtime-tinted {@code buildcraftenergy:block/fluid/*_heat_N_still} files that
 * never existed as PNGs).</li>
 * <li><b>Kept as-is (dynamic-render families, M4.3/M4.4/M4.5)</b>: the five engines + {@code mj_dynamo} + {@code tube}
 * + {@code heat_exchange} + {@code pipe_holder} (baseline {@code minecraft:builtin/entity}, geometry lives in a BER),
 * {@code laser}, {@code quarry}/{@code frame}, {@code marker_path}/{@code marker_volume}/{@code marker_construction}
 * (baseline static body exists but the ruling reserves them for their BER tasks), and the M2.2 slice ids
 * {@code marker}/{@code energy_meter}/{@code pipe_kinesis_wood}. These keep their existing placeholder models and
 * blockstates untouched.</li>
 * </ul>
 * Every registered block id must resolve to an explicit rule here or the run fails (fail-closed, same philosophy as
 * the item provider); the drift gate then re-proves the shipped assets byte for byte on every run.
 */
public final class BcBlockStateProvider extends BcDatagenProvider {

    /** Mod-scoped default {@code cube_all} texture for the blocks that stay on placeholder models (M4.3+ scope). */
    private static final Map<String, String> MOD_PLACEHOLDER_TEXTURES = Map.of(
            "buildcraftbuilders", "cyan_concrete",
            "buildcraftcore", "light_blue_concrete",
            "buildcraftfactory", "gray_concrete",
            "buildcraftrobotics", "magenta_concrete",
            "buildcraftsilicon", "light_gray_concrete",
            "buildcrafttransport", "brown_concrete");

    /** Block-scoped placeholder overrides (unchanged M3.4 exceptions for non-migrated blocks). */
    private static final Map<String, String> PLACEHOLDER_EXCEPTIONS = Map.of(
            "buildcraftcore:energy_meter", "target_top",
            "buildcraftfactory:water_gel", "blue_concrete");

    /** Vanilla orientable textures for the stone engine (the only textured placeholder with a BER contract). */
    private static final String ENGINE_STONE_FRONT = "minecraft:block/furnace_front";
    private static final String ENGINE_STONE_SIDE = "minecraft:block/furnace_side";
    private static final String ENGINE_STONE_TOP = "minecraft:block/furnace_top";

    /** Blocks with a hand-shaped model instead of the mod default (both unchanged from M3.4). */
    private static final String PIPE_KINESIS_WOOD = "buildcraftcore:pipe_kinesis_wood";
    private static final String ENGINE_STONE = "buildcraftcore:engine_stone";

    private final Iterable<DeferredHolder<Block, ? extends Block>> blocks;

    public BcBlockStateProvider(String modid, PackOutput output, Iterable<DeferredHolder<Block, ? extends Block>> blocks) {
        super(modid, output);
        this.blocks = blocks;
    }

    /** One model file to write: path under {@code models/block} plus its JSON. */
    private record ModelFile(String path, JsonObject json) {
    }

    /** The assets of one block: the model its {@code ""} variant points at, plus every model file to write. */
    private record BlockAsset(String stateModel, List<ModelFile> models) {
        static BlockAsset of(String ns, String path, JsonObject model) {
            return new BlockAsset(ns + ":block/" + path, List.of(new ModelFile(path, model)));
        }
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        if ("buildcraftcore".equals(modid)) {
            // flood_gate's baseline models parent this shared hand transform cube; emitted once with the core slice
            futures.add(saveAsset(cache, defaultCubeModel(), "default_cube", "models/block"));
        }
        for (DeferredHolder<Block, ? extends Block> holder : blocks) {
            String path = holder.getId().getPath();
            String fullId = modid + ":" + path;
            if (ENGINE_STONE.equals(fullId)) {
                // unchanged M3.4 special: facing-variant blockstate + vanilla orientable model (BER base)
                futures.add(saveAsset(cache, facingVariants(path), path, "blockstates"));
                futures.add(saveAsset(cache, orientableModel(), path, "models/block"));
                continue;
            }
            if (PIPE_KINESIS_WOOD.equals(fullId)) {
                // unchanged M3.4 special: particle-only model, the pipe BER draws the geometry
                futures.add(saveAsset(cache, singleVariant(modid + ":block/" + path), path, "blockstates"));
                futures.add(saveAsset(cache, particleOnlyModel(), path, "models/block"));
                continue;
            }
            BlockAsset asset = assetFor(path, fullId);
            futures.add(saveAsset(cache, singleVariant(asset.stateModel()), path, "blockstates"));
            for (ModelFile model : asset.models()) {
                futures.add(saveAsset(cache, model.json(), model.path(), "models/block"));
            }
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    /** Resolves one block id to its baseline-static (or deliberately kept placeholder) assets. Fail-closed. */
    private BlockAsset assetFor(String path, String fullId) {
        return switch (modid) {
            case "buildcraftcore" -> coreAsset(path);
            case "buildcraftbuilders" -> buildersAsset(path);
            case "buildcraftenergy" -> energyAsset(path);
            case "buildcraftfactory" -> factoryAsset(path);
            case "buildcraftrobotics" -> roboticsAsset(path);
            case "buildcraftsilicon" -> siliconAsset(path);
            case "buildcrafttransport" -> transportAsset(path);
            default -> throw new IllegalStateException(
                    "BcBlockStateProvider: unexpected mod id '" + modid + "' for block '" + fullId + "'.");
        };
    }

    // -------------------------------------------------------------------------------------------- buildcraftcore

    private BlockAsset coreAsset(String path) {
        return switch (path) {
            case "decorated_blueprint" -> BlockAsset.of(modid, "decorated/blueprint",
                    BcModelJson.cubeAll("buildcraftcore:block/blueprint/blue"));
            case "decorated_destroy" -> BlockAsset.of(modid, "decorated/destroy",
                    BcModelJson.cubeAll("buildcraftcore:block/misc/texture_red_dark"));
            case "decorated_laser_back" -> BlockAsset.of(modid, "decorated/laser_back",
                    BcModelJson.cubeAll("buildcraftsilicon:block/laser/bottom"));
            case "decorated_leather" -> BlockAsset.of(modid, "decorated/leather",
                    BcModelJson.cubeAll("buildcraftcore:block/misc/leather"));
            case "decorated_paper" -> BlockAsset.of(modid, "decorated/paper",
                    BcModelJson.cubeAll("buildcraftcore:block/misc/paper"));
            case "decorated_template" -> BlockAsset.of(modid, "decorated/template",
                    BcModelJson.cubeAll("buildcraftcore:block/blueprint/black"));
            case "power_tester" -> BlockAsset.of(modid, path,
                    BcModelJson.cubeAll("buildcraftcore:block/power_tester"));
            // baseline blockstate points straight at vanilla bedrock; no model file of our own
            case "spring_oil", "spring_water" -> new BlockAsset("minecraft:block/bedrock", List.of());
            default -> placeholderAsset(path);
        };
    }

    // ------------------------------------------------------------------------------------------ buildcraftbuilders

    private BlockAsset buildersAsset(String path) {
        return switch (path) {
            case "architect" -> BlockAsset.of(modid, "architect_off", BcModelJson.parented(
                    "minecraft:block/orientable", BcModelJson.tex(
                            "down", "buildcraftbuilders:block/architect/bottom",
                            "east", "buildcraftbuilders:block/architect/left",
                            "north", "buildcraftbuilders:block/architect/front_off",
                            "particle", "buildcraftbuilders:block/architect/back",
                            "south", "buildcraftbuilders:block/architect/back",
                            "up", "buildcraftbuilders:block/architect/top",
                            "west", "buildcraftbuilders:block/architect/right")));
            case "builder" -> BlockAsset.of(modid, "builder/main", BcModelJson.parented(
                    "minecraft:block/cube", BcModelJson.tex(
                            "down", "buildcraftbuilders:block/builder/bottom",
                            "east", "buildcraftbuilders:block/builder/side",
                            "north", "buildcraftbuilders:block/builder/front",
                            "particle", "buildcraftbuilders:block/builder/side",
                            "south", "buildcraftbuilders:block/builder/back",
                            "up", "buildcraftbuilders:block/builder/top",
                            "west", "buildcraftbuilders:block/builder/side")));
            case "filler" -> BlockAsset.of(modid, "filler/main", BcModelJson.parented(
                    "minecraft:block/cube", BcModelJson.tex(
                            "down", "buildcraftbuilders:block/filler/bottom",
                            "east", "buildcraftbuilders:block/filler/side",
                            "north", "buildcraftbuilders:block/filler/front",
                            "particle", "buildcraftbuilders:block/filler/side",
                            "south", "buildcraftbuilders:block/filler/side",
                            "up", "buildcraftbuilders:block/filler/top",
                            "west", "buildcraftbuilders:block/filler/side")));
            case "library" -> BlockAsset.of(modid, path, BcModelJson.withDisplay(BcModelJson.parented(
                    "minecraft:block/cube", BcModelJson.tex(
                            "down", "buildcraftbuilders:block/library/bottom",
                            "east", "buildcraftbuilders:block/library/left",
                            "north", "buildcraftbuilders:block/library/front",
                            "particle", "buildcraftbuilders:block/library/back",
                            "south", "buildcraftbuilders:block/library/back",
                            "up", "buildcraftbuilders:block/library/top",
                            "west", "buildcraftbuilders:block/library/right")),
                    Map.of("firstperson_righthand",
                            new BcModelJson.Transform(new int[]{0, 135, 0}, new double[]{0.4, 0.4, 0.4}, null))));
            case "replacer" -> BlockAsset.of(modid, path, BcModelJson.parented(
                    "minecraft:block/cube", BcModelJson.tex(
                            "down", "buildcraftbuilders:block/replacer/bottom",
                            "east", "buildcraftbuilders:block/replacer/side",
                            "north", "buildcraftbuilders:block/replacer/front",
                            "particle", "buildcraftbuilders:block/replacer/side",
                            "south", "buildcraftbuilders:block/replacer/side",
                            "up", "buildcraftbuilders:block/replacer/top",
                            "west", "buildcraftbuilders:block/replacer/side")));
            // quarry/frame/marker_construction: dynamic-render families, M4.3+ scope, keep the placeholder
            default -> placeholderAsset(path);
        };
    }

    // ------------------------------------------------------------------------------------------- buildcraftenergy

    private BlockAsset energyAsset(String path) {
        if (path.startsWith("fluid_block_")) {
            // baseline: particle-only model referencing buildcraftenergy:block/fluid/<name>_heat_N_still, a
            // runtime-tinted texture that never shipped as a PNG; M4.2 ruling maps it to the shipped
            // buildcraftlib base heat texture of the same heat level
            int heat = heatLevel(path);
            return BlockAsset.of(modid, path, BcModelJson.parented(null, BcModelJson.tex(
                    "particle", "buildcraftlib:block/fluid/heat_" + heat + "_still")));
        }
        // mj_dynamo: builtin/entity in the baseline (BER), keep the placeholder
        return placeholderAsset(path);
    }

    /** The {@code _heat_N} suffix of a fluid block id (N in 0..2); fails closed on anything else. */
    private static int heatLevel(String path) {
        int idx = path.lastIndexOf("_heat_");
        if (idx < 0 || idx + 6 >= path.length()) {
            throw new IllegalStateException("BcBlockStateProvider: fluid block id without _heat_N suffix: " + path);
        }
        int value = Integer.parseInt(path.substring(idx + 6));
        if (value < 0 || value > 2) {
            throw new IllegalStateException("BcBlockStateProvider: unexpected heat level " + value + " in " + path);
        }
        return value;
    }

    // ------------------------------------------------------------------------------------------ buildcraftfactory

    private BlockAsset factoryAsset(String path) {
        return switch (path) {
            case "autoworkbench_item" -> BlockAsset.of(modid, path, BcModelJson.parented(
                    "minecraft:block/cube", BcModelJson.tex(
                            "down", "buildcraftfactory:block/auto_workbench_item/top",
                            "east", "buildcraftfactory:block/auto_workbench_item/side",
                            "north", "buildcraftfactory:block/auto_workbench_item/side",
                            "particle", "buildcraftfactory:block/auto_workbench_item/side",
                            "south", "buildcraftfactory:block/auto_workbench_item/side",
                            "up", "buildcraftfactory:block/auto_workbench_item/top",
                            "west", "buildcraftfactory:block/auto_workbench_item/side")));
            case "chute" -> BlockAsset.of(modid, path, chuteModel());
            case "distiller" -> BlockAsset.of(modid, path, distillerModel());
            case "flood_gate" -> new BlockAsset(modid + ":block/flood_gate/false_false_false_false_false", List.of(
                    new ModelFile("flood_gate/false_false_false_false_false", floodGateModel("closed")),
                    // the all-connected variant the baseline item model shows
                    new ModelFile("flood_gate/true_true_true_true_true", floodGateModel("open"))));
            case "mining_well" -> BlockAsset.of(modid, path, BcModelJson.parented(
                    "minecraft:block/cube", BcModelJson.tex(
                            "down", "buildcraftfactory:block/mining_well/bottom",
                            "east", "buildcraftfactory:block/mining_well/side",
                            "north", "buildcraftfactory:block/mining_well/front",
                            "particle", "buildcraftfactory:block/mining_well/side",
                            "south", "buildcraftfactory:block/mining_well/back",
                            "up", "buildcraftfactory:block/mining_well/top",
                            "west", "buildcraftfactory:block/mining_well/side")));
            case "pump" -> BlockAsset.of(modid, path, BcModelJson.parented(
                    "minecraft:block/cube", BcModelJson.tex(
                            "down", "buildcraftfactory:block/pump/bottom",
                            "east", "buildcraftfactory:block/pump/side",
                            "north", "buildcraftfactory:block/pump/side",
                            "particle", "buildcraftfactory:block/pump/side",
                            "south", "buildcraftfactory:block/pump/side",
                            "up", "buildcraftfactory:block/pump/top",
                            "west", "buildcraftfactory:block/pump/side")));
            case "tank" -> BlockAsset.of(modid, path, tankModel());
            case "water_gel" -> BlockAsset.of(modid, "water_gel/gel",
                    BcModelJson.cubeAll("buildcraftfactory:block/gel/gel"));
            // heat_exchange/tube: builtin/entity in the baseline (BER), keep the placeholder
            default -> placeholderAsset(path);
        };
    }

    /** The baseline tank glass: one 12×16×12 box, side texture without cullfaces so stacked tanks look right. */
    private JsonObject tankModel() {
        return BcModelJson.elements(BcModelJson.tex(
                "down", "buildcraftfactory:block/tank/end",
                "particle", "buildcraftfactory:block/tank/side",
                "side", "buildcraftfactory:block/tank/side",
                "up", "buildcraftfactory:block/tank/end"),
                BcModelJson.Element.of(2, 0, 2, 14, 16, 14)
                        .face("down", "#down", "down")
                        .face("east", "#side")
                        .face("north", "#side")
                        .face("south", "#side")
                        .face("up", "#up", "up")
                        .face("west", "#side"));
    }

    /** The baseline chute funnel: top box plus six tapering rings. */
    private JsonObject chuteModel() {
        return BcModelJson.elements(BcModelJson.tex(
                "bottom", "buildcraftfactory:block/chute/bottom",
                "particle", "buildcraftfactory:block/chute/top",
                "side", "buildcraftfactory:block/chute/side",
                "side2", "buildcraftfactory:block/chute/side2",
                "top", "buildcraftfactory:block/chute/top",
                "top_bottom", "buildcraftfactory:block/chute/top_bottom",
                "top_side", "buildcraftfactory:block/chute/top_side"),
                BcModelJson.Element.of(0, 9, 0, 16, 16, 16)
                        .face("down", "#top_bottom")
                        .face("east", "#top_side")
                        .face("north", "#top_side")
                        .face("south", "#top_side")
                        .face("up", "#top")
                        .face("west", "#top_side"),
                ring(1, 8, 1, 15, 9, 15),
                ring(2, 7, 2, 14, 8, 14),
                ring(3, 6, 3, 13, 7, 13),
                ring(4, 5, 4, 12, 6, 12),
                ring(5, 4, 5, 11, 5, 11),
                ring(6, 3, 6, 10, 4, 10));
    }

    /** One tapered chute ring: horizontal caps on {@code #side2}, ring walls on {@code #side} with the baseline UV
     * rectangle {@code [fx, 8-fy, tx, 9-fy]}. */
    private BcModelJson.Element ring(int fx, int fy, int fz, int tx, int ty, int tz) {
        double u0 = fx;
        double v0 = 8 - fy;
        double u1 = tx;
        double v1 = 9 - fy;
        return BcModelJson.Element.of(fx, fy, fz, tx, ty, tz)
                .face("down", "#side2")
                .face("east", "#side", null, null, u0, v0, u1, v1)
                .face("north", "#side", null, null, u0, v0, u1, v1)
                .face("south", "#side", null, null, u0, v0, u1, v1)
                .face("up", "#side2")
                .face("west", "#side", null, null, u0, v0, u1, v1);
    }

    /** The baseline distiller body: two crossed tanks (A/B sprites) with the baseline UV layout. */
    private JsonObject distillerModel() {
        return BcModelJson.elements(BcModelJson.tex(
                "particle", "buildcraftfactory:block/distiller/tank_sprite_a",
                "sprite_a", "buildcraftfactory:block/distiller/tank_sprite_a",
                "sprite_b", "buildcraftfactory:block/distiller/tank_sprite_b"),
                BcModelJson.Element.of(0, 0, 4, 8, 16, 12)
                        .face("down", "#sprite_a", null, null, 8.0, 0.0, 16.0, 8.0)
                        .face("east", "#sprite_a", null, null, 0.0, 0.0, 8.0, 16.0)
                        .face("north", "#sprite_a", null, null, 0.0, 0.0, 8.0, 16.0)
                        .face("south", "#sprite_a")
                        .face("up", "#sprite_a", null, null, 8.0, 0.0, 16.0, 8.0)
                        .face("west", "#sprite_a", null, null, 0.0, 0.0, 8.0, 16.0),
                BcModelJson.Element.of(8, 0, 0, 16, 8, 16)
                        .face("down", "#sprite_b", null, 90, 0.0, 0.0, 16.0, 8.0)
                        .face("east", "#sprite_b")
                        .face("north", "#sprite_a", null, null, 8.0, 8.0, 16.0, 16.0)
                        .face("south", "#sprite_a")
                        .face("up", "#sprite_b", null, 90, 0.0, 0.0, 16.0, 8.0)
                        .face("west", "#sprite_b"),
                BcModelJson.Element.of(8, 8, 0, 16, 16, 16)
                        .face("down", "#sprite_b", null, 90, 0.0, 0.0, 16.0, 8.0)
                        .face("east", "#sprite_b", null, null, 0.0, 8.0, 16.0, 16.0)
                        .face("north", "#sprite_a", null, null, 8.0, 8.0, 16.0, 16.0)
                        .face("south", "#sprite_a", null, null, 8.0, 8.0, 16.0, 16.0)
                        .face("up", "#sprite_b", null, 90, 0.0, 0.0, 16.0, 8.0)
                        .face("west", "#sprite_b", null, null, 0.0, 8.0, 16.0, 16.0));
    }

    /** One flood_gate box on the shared {@code buildcraftcore:block/default_cube} parent ({@code closed}/{@code open}). */
    private JsonObject floodGateModel(String side) {
        return BcModelJson.parented("buildcraftcore:block/default_cube", BcModelJson.tex(
                "down", "buildcraftfactory:block/flood_gate/" + side,
                "east", "buildcraftfactory:block/flood_gate/" + side,
                "north", "buildcraftfactory:block/flood_gate/" + side,
                "particle", "buildcraftfactory:block/flood_gate/top",
                "south", "buildcraftfactory:block/flood_gate/" + side,
                "up", "buildcraftfactory:block/flood_gate/top",
                "west", "buildcraftfactory:block/flood_gate/" + side));
    }

    /** The shared hand-transform cube the baseline flood_gate models parent. */
    private JsonObject defaultCubeModel() {
        return BcModelJson.withDisplay(BcModelJson.parented("minecraft:block/cube", Map.of()),
                Map.of("thirdperson_righthand",
                        new BcModelJson.Transform(new int[]{10, -45, 170},
                                new double[]{0.375, 0.375, 0.375}, new double[]{0, 1.5, -2.75})));
    }

    // ----------------------------------------------------------------------------------------- buildcraftrobotics

    private BlockAsset roboticsAsset(String path) {
        return switch (path) {
            case "requester" -> BlockAsset.of(modid, path, BcModelJson.parented(
                    "minecraft:block/cube", BcModelJson.tex(
                            "down", "buildcraftrobotics:block/requester/bottom",
                            "east", "buildcraftrobotics:block/requester/side",
                            "north", "buildcraftrobotics:block/requester/front",
                            "particle", "buildcraftrobotics:block/requester/side",
                            "south", "buildcraftrobotics:block/requester/back",
                            "up", "buildcraftrobotics:block/requester/top",
                            "west", "buildcraftrobotics:block/requester/side")));
            case "zone_planner" -> BlockAsset.of(modid, path, BcModelJson.parented(
                    "minecraft:block/cube", BcModelJson.tex(
                            "down", "buildcraftrobotics:block/zone_planner/default",
                            "east", "buildcraftrobotics:block/zone_planner/left",
                            "north", "buildcraftrobotics:block/zone_planner/front",
                            "particle", "buildcraftrobotics:block/zone_planner/default",
                            "south", "buildcraftrobotics:block/zone_planner/back",
                            "up", "buildcraftrobotics:block/zone_planner/top",
                            "west", "buildcraftrobotics:block/zone_planner/right")));
            default -> placeholderAsset(path);
        };
    }

    // ------------------------------------------------------------------------------------------ buildcraftsilicon

    private BlockAsset siliconAsset(String path) {
        return switch (path) {
            case "advanced_crafting_table" -> tableAsset("advanced_crafting", advancedCraftingElements());
            case "assembly_table" -> tableAsset("assembly", assemblyElements());
            case "charging_table" -> tableAsset("charging", chargingElements());
            case "integration_table" -> tableAsset("integration", integrationElements());
            case "programming_table" -> tableAsset("programming", programmingElements());
            // laser: static base exists in the baseline but the ruling reserves it for the laser BER task (M4.4)
            default -> placeholderAsset(path);
        };
    }

    /**
     * One silicon table model: {@code minecraft:block/block} parent, the table's element boxes and its texture map.
     * The texture map varies per table exactly like the baseline: {@code center} only on integration/programming
     * (the glowing core), {@code middle} on every table except assembly, {@code glass} only on programming (the top
     * pane). Keys the baseline ships even though no face references them (e.g. charging's {@code middle}) are kept
     * for parity.
     */
    private BlockAsset tableAsset(String name, BcModelJson.Element[] elements) {
        Map<String, String> textures = new LinkedHashMap<>();
        textures.put("bottom", "buildcraftsilicon:block/table/" + name + "/bottom");
        if ("integration".equals(name) || "programming".equals(name)) {
            textures.put("center", "buildcraftsilicon:block/table/" + name + "/center");
        }
        if ("programming".equals(name)) {
            textures.put("glass", "minecraft:block/white_stained_glass");
        }
        if (!"assembly".equals(name)) {
            textures.put("middle", "buildcraftsilicon:block/table/" + name + "/middle");
        }
        textures.put("particle", "buildcraftsilicon:block/table/" + name + "/top");
        textures.put("side", "buildcraftsilicon:block/table/" + name + "/side");
        textures.put("top", "buildcraftsilicon:block/table/" + name + "/top");
        return BlockAsset.of(modid, "table/" + name,
                BcModelJson.elements("minecraft:block/block", textures, elements));
    }

    private BcModelJson.Element[] assemblyElements() {
        return new BcModelJson.Element[]{
                BcModelJson.Element.of(0, 0, 0, 16, 1, 16).face("down", "#bottom").face("east", "#side")
                        .face("north", "#side").face("south", "#side").face("up", "#bottom").face("west", "#side"),
                BcModelJson.Element.of(1, 1, 1, 15, 3, 15).face("down", "#bottom").face("east", "#side")
                        .face("north", "#side").face("south", "#side").face("up", "#bottom").face("west", "#side"),
                BcModelJson.Element.of(0, 3, 0, 16, 9, 16).face("down", "#bottom").face("east", "#side")
                        .face("north", "#side").face("south", "#side").face("up", "#top").face("west", "#side")};
    }

    private BcModelJson.Element[] advancedCraftingElements() {
        return new BcModelJson.Element[]{
                corner(0, 0, 0, 4, 3, 4), corner(12, 0, 0, 16, 3, 4),
                corner(0, 0, 12, 4, 3, 16), corner(12, 0, 12, 16, 3, 16),
                BcModelJson.Element.of(0, 3, 0, 16, 9, 16).face("down", "#middle").face("east", "#side")
                        .face("north", "#side").face("south", "#side").face("up", "#top").face("west", "#side")};
    }

    private BcModelJson.Element[] chargingElements() {
        return new BcModelJson.Element[]{
                corner(0, 0, 0, 4, 6, 4), corner(12, 0, 0, 16, 6, 4),
                corner(0, 0, 12, 4, 6, 16), corner(12, 0, 12, 16, 6, 16),
                corner(4, 0, 4, 12, 6, 12),
                BcModelJson.Element.of(0, 6, 0, 16, 9, 16).face("down", "#bottom").face("east", "#side")
                        .face("north", "#side").face("south", "#side").face("up", "#top").face("west", "#side")};
    }

    private BcModelJson.Element[] integrationElements() {
        return new BcModelJson.Element[]{
                BcModelJson.Element.of(0, 0, 0, 16, 1, 16).face("down", "#bottom").face("east", "#side")
                        .face("north", "#side").face("south", "#side").face("up", "#middle").face("west", "#side"),
                corner(1, 1, 1, 5, 3, 5), corner(11, 1, 1, 15, 3, 5),
                corner(1, 1, 11, 5, 3, 15), corner(11, 1, 11, 15, 3, 15),
                rim(0, 3, 0, 16, 9, 5), rim(0, 3, 0, 5, 9, 16),
                rim(0, 3, 11, 16, 9, 16), rim(11, 3, 0, 16, 9, 16),
                BcModelJson.Element.of(5, 3, 5, 11, 7, 11).face("down", "#middle").face("east", "#side")
                        .face("north", "#side").face("south", "#side").face("up", "#top").face("west", "#side"),
                BcModelJson.Element.of(5, 7, 5, 11, 8, 11).face("down", "#center").face("east", "#center")
                        .face("north", "#center").face("south", "#center").face("up", "#center").face("west", "#center")};
    }

    private BcModelJson.Element[] programmingElements() {
        return new BcModelJson.Element[]{
                corner(0, 0, 0, 4, 3, 4), corner(12, 0, 0, 16, 3, 4),
                corner(0, 0, 12, 4, 3, 16), corner(12, 0, 12, 16, 3, 16),
                BcModelJson.Element.of(0, 3, 0, 16, 3, 16).face("down", "#middle").face("east", "#side")
                        .face("north", "#side").face("south", "#side").face("up", "#middle").face("west", "#side"),
                rim(0, 3, 0, 16, 9, 4), rim(0, 3, 0, 4, 9, 16),
                rim(0, 3, 12, 16, 9, 16), rim(12, 3, 0, 16, 9, 16),
                BcModelJson.Element.of(5, 4, 5, 11, 6, 11).face("down", "#center").face("east", "#center")
                        .face("north", "#center").face("south", "#center").face("up", "#center").face("west", "#center"),
                BcModelJson.Element.of(4, 9, 4, 12, 9, 12).face("up", "#glass")};
    }

    /** A table leg/foot box: {@code #bottom} caps, {@code #side} walls. */
    private BcModelJson.Element corner(int fx, int fy, int fz, int tx, int ty, int tz) {
        return BcModelJson.Element.of(fx, fy, fz, tx, ty, tz)
                .face("down", "#bottom").face("east", "#side").face("north", "#side")
                .face("south", "#side").face("up", "#bottom").face("west", "#side");
    }

    /** A table wall/rim box: {@code #middle} inner caps, {@code #top} up face, {@code #side} walls. */
    private BcModelJson.Element rim(int fx, int fy, int fz, int tx, int ty, int tz) {
        return BcModelJson.Element.of(fx, fy, fz, tx, ty, tz)
                .face("down", "#middle").face("east", "#side").face("north", "#side")
                .face("south", "#side").face("up", "#top").face("west", "#side");
    }

    // ----------------------------------------------------------------------------------------- buildcrafttransport

    private BlockAsset transportAsset(String path) {
        return switch (path) {
            case "filtered_buffer" -> BlockAsset.of(modid, path, BcModelJson.parented(
                    "minecraft:block/cube_all", BcModelJson.tex(
                            "all", "buildcrafttransport:block/filtered_buffer/default",
                            "particle", "buildcrafttransport:block/filtered_buffer/default")));
            // pipe_holder: builtin/entity in the baseline (pipe BER), keep the placeholder
            default -> placeholderAsset(path);
        };
    }

    // -------------------------------------------------------------------------------------------------- placeholders

    /** The kept M3.4 placeholder: {@code cube_all} with the mod's (or an explicit) vanilla concrete texture. */
    private BlockAsset placeholderAsset(String path) {
        String fullId = modid + ":" + path;
        String exception = PLACEHOLDER_EXCEPTIONS.get(fullId);
        if (exception != null) {
            return BlockAsset.of(modid, path, BcModelJson.cubeAll("minecraft:block/" + exception));
        }
        if ("buildcraftenergy".equals(modid)) {
            if (path.startsWith("fluid_block_fuel_")) {
                return BlockAsset.of(modid, path, BcModelJson.cubeAll("minecraft:block/yellow_concrete"));
            }
            if (path.startsWith("fluid_block_oil_")) {
                return BlockAsset.of(modid, path, BcModelJson.cubeAll("minecraft:block/black_concrete"));
            }
            return BlockAsset.of(modid, path, BcModelJson.cubeAll("minecraft:block/orange_concrete"));
        }
        String modDefault = MOD_PLACEHOLDER_TEXTURES.get(modid);
        if (modDefault == null) {
            throw new IllegalStateException(
                    "BcBlockStateProvider: no placeholder texture for mod '" + modid + "' (block '" + fullId
                            + "'). Add an explicit rule — the drift gate requires datagen to reproduce the shipped assets.");
        }
        return BlockAsset.of(modid, path, BcModelJson.cubeAll("minecraft:block/" + modDefault));
    }

    // -------------------------------------------------------------------------------------------------- blockstates

    /** {@code {"variants": {"": {"model": <ref>}}}} — the property-less placeholder-era blockstate, kept for M4.2. */
    private JsonObject singleVariant(String modelRef) {
        JsonObject state = new JsonObject();
        JsonObject variants = new JsonObject();
        JsonObject variant = new JsonObject();
        variant.addProperty("model", modelRef);
        variants.add("", variant);
        state.add("variants", variants);
        return state;
    }

    /**
     * The stone engine's 4 facing variants (its 26.1.2 block class is the one block with a real property). Key order
     * in the output is handled by the stock comparator (alphabetical), matching the shipped file.
     */
    private JsonObject facingVariants(String path) {
        JsonObject state = new JsonObject();
        JsonObject variants = new JsonObject();
        variants.add("facing=east", rotatedVariant(modid + ":block/" + path, 90));
        variants.add("facing=north", rotatedVariant(modid + ":block/" + path, 0));
        variants.add("facing=south", rotatedVariant(modid + ":block/" + path, 180));
        variants.add("facing=west", rotatedVariant(modid + ":block/" + path, 270));
        state.add("variants", variants);
        return state;
    }

    private JsonObject rotatedVariant(String modelRef, int yRotation) {
        JsonObject variant = new JsonObject();
        variant.addProperty("model", modelRef);
        if (yRotation != 0) {
            variant.addProperty("y", yRotation);
        }
        return variant;
    }

    // ----------------------------------------------------------------------------------------------------- specials

    /** The stone engine model: vanilla orientable parent with furnace textures (StoneEngineBlockRenderer's base). */
    private JsonObject orientableModel() {
        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:block/orientable");
        JsonObject textures = new JsonObject();
        textures.addProperty("front", ENGINE_STONE_FRONT);
        textures.addProperty("side", ENGINE_STONE_SIDE);
        textures.addProperty("top", ENGINE_STONE_TOP);
        model.add("textures", textures);
        return model;
    }

    /** The kinesis pipe model: parent-less, particle-only (the pipe BER draws the geometry). */
    private JsonObject particleOnlyModel() {
        JsonObject model = new JsonObject();
        JsonObject textures = new JsonObject();
        textures.addProperty("particle", "minecraft:block/oak_planks");
        model.add("textures", textures);
        return model;
    }
}
