/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.datagen;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;

/**
 * M4.4: copies the shipped tile {@code .jsonbc} models (the jsonbc variable models the {@code EngineBlockRenderer}
 * loads at runtime) into the datagen output, byte for byte — the S1 drift gate diffs the runData output against the
 * shipped {@code neo/src/main/resources/assets/} tree, and these files must exist on both sides with identical bytes.
 *
 * <p>The jsonbc sources of truth are the shipped resources themselves (the baseline
 * {@code buildcraft_resources/assets/<ns>/models/tile/*.jsonbc} files, ported verbatim); this provider re-emits them
 * from the classpath so the drift gate proves the shipped copies stay in place. The byte-faithful copy is deliberate:
 * unlike the other providers this one must not re-serialise (there is nothing to normalise, and any difference would
 * trip the S1 gate).
 *
 * <p>M4.5 close-out: this provider also emits the shared {@code assets/minecraft/atlases/blocks.json} — the sprite
 * sources of the block atlas whose sprites are consumed directly by BuildCraft's BER/tile-model machinery and are
 * therefore invisible to vanilla's model-driven atlas discovery. That data is exactly this provider's tile/laser
 * model semantics, so the atlas moved here from the transport item model provider (which had owned the pipes source
 * since M4.3). One file, one writer: the sources of all three namespaces are stitched by this single emission:
 * <ul>
 * <li>the {@code pipes} directory — the pipe/plug item models reference {@code textures/pipes/} sprites, which no
 * vanilla atlas directory rule covers (the M4.3 pipes entry, emitted here unchanged);</li>
 * <li>the {@code lasers} directory — {@code buildcraftcore:lasers/*}, the sprite set behind every
 * {@code buildcraft.lib.client.render.laser} baked laser quad;</li>
 * <li>{@code buildcraftbuilders:block/frame/default} and {@code buildcraftbuilders:block/quarry/drill} singles — the
 * frame-rail and drill-column sprites the laser rows of {@code BcLaserTypes} look up directly.</li>
 * </ul>
 * The emission is fail-closed: every explicitly named sprite (and one member of each directory) must exist in the
 * shipped resources, mirroring {@code BcItemModelsProvider#validateTexture}.
 */
public final class BcTileModelsProvider extends BcDatagenProvider {

    /** The shipped {@code models/tile/*.jsonbc} file stems per mod namespace (fail-closed on a missing resource). */
    private static final Map<String, List<String>> TILE_JSONBC = Map.of(
            "buildcraftlib", List.of("engine_base"),
            "buildcraftcore", List.of("engine_redstone", "engine_creative"),
            "buildcraftenergy", List.of("engine_stone", "engine_iron", "engine_rf", "mj_dynamo"));

    /**
     * The namespace whose provider instance emits the shared {@code minecraft:atlases/blocks.json} (exactly one of
     * the eight per-mod instances may write the cross-namespace file — {@code buildcraftcore}, where the
     * {@code lasers/} sprites live, anchors the laser-model semantics).
     */
    private static final String ATLAS_EMITTER = "buildcraftcore";

    public BcTileModelsProvider(String modid, net.minecraft.data.PackOutput output) {
        super(modid, output);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (String stem : TILE_JSONBC.getOrDefault(modid, List.of())) {
            String resourcePath = "/assets/" + modid + "/models/tile/" + stem + ".jsonbc";
            byte[] bytes;
            try (InputStream in = BcTileModelsProvider.class.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    throw new IllegalStateException(
                            "BcTileModelsProvider: shipped jsonbc resource " + resourcePath + " does not exist");
                }
                bytes = in.readAllBytes();
            } catch (IOException e) {
                throw new UncheckedIOException("BcTileModelsProvider: failed to read " + resourcePath, e);
            }
            byte[] owned = bytes;
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    ByteArrayOutputStream sink = new ByteArrayOutputStream();
                    HashingOutputStream hashedBytes = new HashingOutputStream(Hashing.sha1(), sink);
                    hashedBytes.write(owned);
                    hashedBytes.close();
                    cache.writeIfNeeded(this.paths.assetFile(modid, "models/tile", stem + ".jsonbc"), owned,
                            hashedBytes.hash());
                } catch (IOException e) {
                    throw new UncheckedIOException("BcTileModelsProvider: failed to save " + stem + ".jsonbc", e);
                }
            }));
        }
        if (ATLAS_EMITTER.equals(modid)) {
            // the shared block-atlas sprite sources (pipes M4.3, lasers/frame/drill M4.5) — see class javadoc
            futures.add(BcDatagenJson.save(cache, blocksAtlas(), paths.asset("minecraft", "atlases", "blocks"),
                    DataProvider.KEY_COMPARATOR));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    /**
     * The shared block atlas: one {@code minecraft:directory} source per BER-stitched sprite directory and one
     * {@code minecraft:single} source per directly-consumed sprite, in the shipped file's source order (pipes,
     * lasers, frame, drill).
     */
    private static JsonObject blocksAtlas() {
        validateTexture("buildcraftcore", "lasers/power_low");
        validateTexture("buildcraftbuilders", "block/frame/default");
        validateTexture("buildcraftbuilders", "block/quarry/drill");
        JsonArray sources = new JsonArray();
        sources.add(directorySource("pipes"));
        sources.add(directorySource("lasers"));
        sources.add(singleSource("buildcraftbuilders:block/frame/default"));
        sources.add(singleSource("buildcraftbuilders:block/quarry/drill"));
        JsonObject atlas = new JsonObject();
        atlas.add("sources", sources);
        return atlas;
    }

    /** {@code {"type": "minecraft:directory", "source": <stem>, "prefix": <stem> + "/"}}. */
    private static JsonObject directorySource(String stem) {
        JsonObject source = new JsonObject();
        source.addProperty("type", "minecraft:directory");
        source.addProperty("source", stem);
        source.addProperty("prefix", stem + "/");
        return source;
    }

    /** {@code {"type": "minecraft:single", "resource": <id>}}. */
    private static JsonObject singleSource(String resource) {
        JsonObject source = new JsonObject();
        source.addProperty("type", "minecraft:single");
        source.addProperty("resource", resource);
        return source;
    }

    /** Fail-closed check that one stitched sprite exists in the shipped (source-set) resources. */
    private static void validateTexture(String namespace, String texturePath) {
        if (BcTileModelsProvider.class.getResource("/assets/" + namespace + "/textures/" + texturePath + ".png")
                == null) {
            throw new IllegalStateException(
                    "BcTileModelsProvider: atlas sprite assets/" + namespace + "/textures/" + texturePath
                            + ".png does not exist in the resources");
        }
    }
}
