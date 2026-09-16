/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.gametest;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;

import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeHolder;

import net.neoforged.neoforge.event.server.ServerStartedEvent;

import org.slf4j.Logger;

/**
 * M3.3 verification probe (kept intentionally): dumps the runtime registries of the 26.1.2 port into the same JSON
 * schema as the frozen 1.20.1 baseline ({@code migration/snapshots/registry-baseline.json}) so
 * {@code migration/scripts/registry_diff.py} can diff the two. Exports every section the baseline has &mdash; the five
 * registries ({@code blocks}/{@code items}/{@code block_entities}/{@code entities}/{@code fluids}) plus
 * {@code recipes} and {@code tags} (biome/block/fluid/item) &mdash; filtered to the same id scope the baseline
 * attributes to BuildCraft: the eight {@code buildcraft*} mod namespaces, plus the legacy shared {@code buildcraft}
 * namespace for tags (all 66 baseline item tags, the block and the fluid tag live there). Lists are sorted exactly
 * like the baseline. It never changes any registration or datapack behaviour &mdash; it only reads the already-loaded
 * registries, the finished {@code RecipeManager} ({@code ServerStartedEvent} fires after the resource reload, so the
 * recipe set is final) and the bound tag sets.
 *
 * <p>Gating: the probe writes only when the system property {@code buildcraft.registrydump.path} is a non-empty file
 * path (parent directories are created); empty (the default) keeps it off, so ordinary dev runs and the M3.1 CI
 * GameTests step are unaffected. Trigger:
 * {@code ./gradlew runGameTestServer --no-daemon -Pregistrydump.path=<file>}. The single evidence log line is
 * {@code Registry dump written: <path> (...)}.
 */
public final class RegistryDumpProbe {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Dump output file; empty (default) disables the probe. Wired from -Pregistrydump.path in build.gradle. */
    private static final String OUTPUT_PATH = System.getProperty("buildcraft.registrydump.path", "");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /**
     * The eight shipped mod namespaces: the id scope of the five registry sections and of {@code recipes} in the
     * baseline (identical to {@code RegistryParityTest#NAMESPACES}).
     */
    private static final Set<String> MOD_NAMESPACES = Set.of("buildcraftlib", "buildcraftcore", "buildcraftenergy",
            "buildcraftfactory", "buildcraftsilicon", "buildcrafttransport", "buildcraftbuilders",
            "buildcraftrobotics");

    /**
     * Tag ids additionally live in the legacy shared "buildcraft" namespace: the 1.20.1 baseline item/block/fluid
     * tags all use {@code buildcraft:*} ({@code buildcraftenergy:oil_gen} is the only mod-namespace tag), so the
     * tag sections use this wider scope.
     */
    private static final Set<String> TAG_NAMESPACES;

    static {
        Set<String> namespaces = new HashSet<>(MOD_NAMESPACES);
        namespaces.add("buildcraft");
        TAG_NAMESPACES = Set.copyOf(namespaces);
    }

    private RegistryDumpProbe() {
    }

    public static void onServerStarted(ServerStartedEvent event) {
        if (OUTPUT_PATH.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        JsonArray blocks = registryIds(BuiltInRegistries.BLOCK, MOD_NAMESPACES);
        JsonArray items = registryIds(BuiltInRegistries.ITEM, MOD_NAMESPACES);
        JsonArray blockEntities = registryIds(BuiltInRegistries.BLOCK_ENTITY_TYPE, MOD_NAMESPACES);
        JsonArray entities = registryIds(BuiltInRegistries.ENTITY_TYPE, MOD_NAMESPACES);
        JsonArray fluids = registryIds(BuiltInRegistries.FLUID, MOD_NAMESPACES);
        JsonArray recipes = recipeIds(server);
        JsonArray biomeTags = tagIds(server.registryAccess().lookupOrThrow(Registries.BIOME));
        JsonArray blockTags = tagIds(BuiltInRegistries.BLOCK);
        JsonArray fluidTags = tagIds(BuiltInRegistries.FLUID);
        JsonArray itemTags = tagIds(BuiltInRegistries.ITEM);

        JsonObject dump = new JsonObject();
        dump.add("metadata", metadata());
        dump.add("counts", counts(blocks, items, blockEntities, entities, fluids, recipes, biomeTags, blockTags,
                fluidTags, itemTags));
        dump.add("blocks", blocks);
        dump.add("items", items);
        dump.add("block_entities", blockEntities);
        dump.add("entities", entities);
        dump.add("fluids", fluids);
        dump.add("recipes", recipes);
        JsonObject tags = new JsonObject();
        tags.add("biome", biomeTags);
        tags.add("block", blockTags);
        tags.add("fluid", fluidTags);
        tags.add("item", itemTags);
        dump.add("tags", tags);

        try {
            Path out = Path.of(OUTPUT_PATH);
            if (out.getParent() != null) {
                Files.createDirectories(out.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
                GSON.toJson(dump, writer);
            }
        } catch (IOException e) {
            // Fail loudly but do not take the run down: registry_diff.py treats a missing dump as a harness error.
            LOGGER.error("Registry dump FAILED for {}: {} (M3.3 verification probe)", OUTPUT_PATH, e.toString(), e);
            return;
        }
        // Evidence line; the vanilla-scoped totals prove the dump ran after recipes loaded and tags were bound
        // (a too-early hook would show 0 there), while the buildcraft-scoped ones are the exported sections.
        LOGGER.info(
                "Registry dump written: {} (blocks {}, items {}, block_entities {}, entities {}, fluids {}, recipes {},"
                        + " tags.biome {}, tags.block {}, tags.fluid {}, tags.item {}; server totals for sanity:"
                        + " recipes {}, tags block/fluid/item/biome {}/{}/{}/{}) — M3.3 verification probe",
                OUTPUT_PATH, blocks.size(), items.size(), blockEntities.size(), entities.size(), fluids.size(),
                recipes.size(), biomeTags.size(), blockTags.size(), fluidTags.size(), itemTags.size(), totalRecipes(
                        server),
                BuiltInRegistries.BLOCK.listTagIds().count(), BuiltInRegistries.FLUID.listTagIds().count(),
                BuiltInRegistries.ITEM.listTagIds().count(),
                server.registryAccess().lookupOrThrow(Registries.BIOME).listTagIds().count());
    }

    /** All ids of one registry in the given namespaces, sorted (baseline lists are sorted too). */
    private static JsonArray registryIds(Registry<?> registry, Set<String> namespaces) {
        TreeSet<String> ids = new TreeSet<>();
        for (Identifier id : registry.keySet()) {
            if (namespaces.contains(id.getNamespace())) {
                ids.add(id.toString());
            }
        }
        return toArray(ids);
    }

    /** The currently loaded recipe ids of the eight mod namespaces; {@code ServerStartedEvent} guarantees the set is final. */
    private static JsonArray recipeIds(MinecraftServer server) {
        TreeSet<String> ids = new TreeSet<>();
        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            Identifier id = holder.id().identifier();
            if (MOD_NAMESPACES.contains(id.getNamespace())) {
                ids.add(id.toString());
            }
        }
        return toArray(ids);
    }

    /** The bound tag ids of one registry in the tag namespaces (baseline tags are id lists, not member lists). */
    private static JsonArray tagIds(Registry<?> registry) {
        TreeSet<String> ids = new TreeSet<>();
        registry.listTagIds().forEach(tag -> {
            Identifier id = tag.location();
            if (TAG_NAMESPACES.contains(id.getNamespace())) {
                ids.add(id.toString());
            }
        });
        return toArray(ids);
    }

    private static int totalRecipes(MinecraftServer server) {
        return server.getRecipeManager().getRecipes().size();
    }

    private static JsonObject metadata() {
        JsonObject metadata = new JsonObject();
        metadata.addProperty("generator", "buildcraft.core.gametest.RegistryDumpProbe");
        metadata.addProperty("minecraft_version", SharedConstants.getCurrentVersion().name());
        JsonArray modIds = new JsonArray();
        new TreeSet<>(MOD_NAMESPACES).forEach(modIds::add);
        metadata.add("mod_ids", modIds);
        metadata.addProperty("note", "M3.3 runtime dump of the 26.1.2 registries; same schema as the frozen 1.20.1"
                + " baseline migration/snapshots/registry-baseline.json (see migration/scripts/registry_diff.py)");
        return metadata;
    }

    private static JsonObject counts(JsonArray blocks, JsonArray items, JsonArray blockEntities, JsonArray entities,
            JsonArray fluids, JsonArray recipes, JsonArray biomeTags, JsonArray blockTags, JsonArray fluidTags,
            JsonArray itemTags) {
        JsonObject counts = new JsonObject();
        counts.addProperty("blocks", blocks.size());
        counts.addProperty("items", items.size());
        counts.addProperty("block_entities", blockEntities.size());
        counts.addProperty("entities", entities.size());
        counts.addProperty("fluids", fluids.size());
        counts.addProperty("recipes", recipes.size());
        counts.addProperty("tags.biome", biomeTags.size());
        counts.addProperty("tags.block", blockTags.size());
        counts.addProperty("tags.fluid", fluidTags.size());
        counts.addProperty("tags.item", itemTags.size());
        return counts;
    }

    private static JsonArray toArray(Iterable<String> ids) {
        JsonArray array = new JsonArray();
        ids.forEach(array::add);
        return array;
    }
}
