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
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * M3.4: regenerates the block asset pair of one mod — the blockstate ({@code assets/<ns>/blockstates/<id>.json}) and
 * the block model it references ({@code assets/<ns>/models/block/<id>.json}).
 *
 * <p>Regeneration contract ("no drift" vs the shipped assets, verified by the runData diff gate):
 * <ul>
 * <li>Every block is a single-variant blockstate (variant key {@code ""}) pointing at {@code <ns>:block/<id>} — except
 * the stone engine, whose 4 horizontal {@code facing} variants carry y-rotations (east 90, south 180, west 270).</li>
 * <li>Every block model is a {@code cube_all} with the mod's placeholder concrete — except the three audited specials:
 * the stone engine (vanilla orientable with furnace textures), the kinesis pipe (particle-only model) and the energy
 * meter ({@code target_top}). The kinesis pipe's separate {@code pipe_kinesis_wood_inventory} element model is the one
 * hand-written file of the asset set and is deliberately NOT generated (M3.4 whitelist entry #1).</li>
 * </ul>
 * The per-mod concrete colours and the exception tables were audited against the shipped files (79 models across the
 * 8 mods) before being written down here; unknown blocks throw so new blocks force an explicit decision.
 */
public final class BcBlockStateProvider extends BcDatagenProvider {

    /** Mod-scoped default {@code cube_all} texture stem (after {@code minecraft:block/}). */
    private static final Map<String, String> MOD_DEFAULT_TEXTURES = Map.of(
            "buildcraftbuilders", "cyan_concrete",
            "buildcraftcore", "light_blue_concrete",
            "buildcraftfactory", "gray_concrete",
            "buildcraftrobotics", "magenta_concrete",
            "buildcraftsilicon", "light_gray_concrete",
            "buildcrafttransport", "brown_concrete");

    /** Block-scoped {@code cube_all} texture overrides (energy is prefix-driven, so it lives in a rule below). */
    private static final Map<String, String> TEXTURE_EXCEPTIONS = Map.of(
            "buildcraftcore:energy_meter", "target_top",
            "buildcraftfactory:water_gel", "blue_concrete");

    /** Vanilla orientable textures for the stone engine (the only non-cube_all textured model datagen emits). */
    private static final String ENGINE_STONE_FRONT = "minecraft:block/furnace_front";
    private static final String ENGINE_STONE_SIDE = "minecraft:block/furnace_side";
    private static final String ENGINE_STONE_TOP = "minecraft:block/furnace_top";

    /** Blocks that get a hand-shaped model instead of the mod default (or energy prefix rule). */
    private static final String PIPE_KINESIS_WOOD = "buildcraftcore:pipe_kinesis_wood";
    private static final String ENGINE_STONE = "buildcraftcore:engine_stone";

    private final Iterable<DeferredHolder<Block, ? extends Block>> blocks;

    public BcBlockStateProvider(String modid, PackOutput output, Iterable<DeferredHolder<Block, ? extends Block>> blocks) {
        super(modid, output);
        this.blocks = blocks;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (DeferredHolder<Block, ? extends Block> holder : blocks) {
            String path = holder.getId().getPath();
            String fullId = modid + ":" + path;
            if (ENGINE_STONE.equals(fullId)) {
                futures.add(saveAsset(cache, facingVariants(path), path, "blockstates"));
                futures.add(saveAsset(cache, orientableModel(path), path, "models/block"));
            } else if (PIPE_KINESIS_WOOD.equals(fullId)) {
                futures.add(saveAsset(cache, singleVariant(path), path, "blockstates"));
                futures.add(saveAsset(cache, particleOnlyModel(path), path, "models/block"));
            } else {
                futures.add(saveAsset(cache, singleVariant(path), path, "blockstates"));
                futures.add(saveAsset(cache, cubeAllModel(path, fullId), path, "models/block"));
            }
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    /** {@code {"variants": {"": {"model": "<ns>:block/<id>"}}}} — the shipped single-variant blockstate. */
    private JsonObject singleVariant(String path) {
        JsonObject state = new JsonObject();
        JsonObject variants = new JsonObject();
        JsonObject variant = new JsonObject();
        variant.addProperty("model", modid + ":block/" + path);
        variants.add("", variant);
        state.add("variants", variants);
        return state;
    }

    /**
     * The stone engine's 4 facing variants. Key order in the output is handled by the stock comparator (alphabetical:
     * east, north, south, west), matching the shipped file.
     */
    private JsonObject facingVariants(String path) {
        JsonObject state = new JsonObject();
        JsonObject variants = new JsonObject();
        variants.add("facing=east", rotatedVariant(path, 90));
        variants.add("facing=north", rotatedVariant(path, 0));
        variants.add("facing=south", rotatedVariant(path, 180));
        variants.add("facing=west", rotatedVariant(path, 270));
        state.add("variants", variants);
        return state;
    }

    private JsonObject rotatedVariant(String path, int yRotation) {
        JsonObject variant = new JsonObject();
        variant.addProperty("model", modid + ":block/" + path);
        if (yRotation != 0) {
            variant.addProperty("y", yRotation);
        }
        return variant;
    }

    /** {@code cube_all} with the mod default concrete, the energy prefix rule or an explicit exception. */
    private JsonObject cubeAllModel(String path, String fullId) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:block/cube_all");
        JsonObject textures = new JsonObject();
        textures.addProperty("all", "minecraft:block/" + allTexture(fullId, path));
        model.add("textures", textures);
        return model;
    }

    private String allTexture(String fullId, String path) {
        String exception = TEXTURE_EXCEPTIONS.get(fullId);
        if (exception != null) {
            return exception;
        }
        if ("buildcraftenergy".equals(modid)) {
            // placeholder fluid blocks: fuel family yellow, oil family black; the dynamo is the only other block
            if (path.startsWith("fluid_block_fuel_")) {
                return "yellow_concrete";
            }
            if (path.startsWith("fluid_block_oil_")) {
                return "black_concrete";
            }
            return "orange_concrete";
        }
        String modDefault = MOD_DEFAULT_TEXTURES.get(modid);
        if (modDefault == null) {
            throw new IllegalStateException(
                    "BcBlockStateProvider: no default cube_all texture for mod '" + modid + "'. Add an explicit entry — "
                            + "the M3.4 drift gate requires datagen to reproduce the shipped assets.");
        }
        return modDefault;
    }

    /** The stone engine model: vanilla orientable parent with furnace textures. */
    private JsonObject orientableModel(String path) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:block/orientable");
        JsonObject textures = new JsonObject();
        textures.addProperty("front", ENGINE_STONE_FRONT);
        textures.addProperty("side", ENGINE_STONE_SIDE);
        textures.addProperty("top", ENGINE_STONE_TOP);
        model.add("textures", textures);
        return model;
    }

    /** The kinesis pipe model: parent-less, particle-only (the pipe renderer draws the geometry). */
    private JsonObject particleOnlyModel(String path) {
        JsonObject model = new JsonObject();
        JsonObject textures = new JsonObject();
        textures.addProperty("particle", "minecraft:block/oak_planks");
        model.add("textures", textures);
        return model;
    }
}
