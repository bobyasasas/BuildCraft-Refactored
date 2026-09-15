package buildcraft.datagen.base;

import buildcraft.api.BCModules;
import buildcraft.api.core.BCLog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.versions.forge.ForgeVersion;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * M0.3 baseline tool: exports every buildcraft registry id to
 * {@code migration/snapshots/registry-baseline.json}.
 *
 * Contents (all id lists sorted lexicographically, namespaces starting with "buildcraft" only):
 * blocks, items, block entities, entities and fluids are read from the live
 * {@link ForgeRegistries} while datagen runs; recipe and tag ids are harvested from the
 * generated data file tree ({@code data/<namespace>/recipes/**} and
 * {@code data/<namespace>/tags/<category>/**}).
 *
 * Registered last in {@link buildcraft.datagen.BCDataGenerators}, so it runs after the other
 * data providers and can harvest their output files. In M3.3 the same generator runs on the
 * migrated codebase and its output is diffed against the committed baseline.
 *
 * <p>Re-running: this jar's datagen runs through a generator whose {@code alwaysGenerate} flag
 * is false (the {@code --mod buildcraft} filter matches no mod container), so vanilla caches
 * each provider per MC version under {@code buildcraft_resources_generated/.cache} and skips
 * them on later runs. The reliable repeat procedure is:
 * {@code rm -rf buildcraft_resources_generated/.cache && ./gradlew runData}.
 * The snapshot file itself is deterministic (all id lists sorted; verified byte-identical
 * across repeated full regenerations).</p>
 */
public class BCRegistrySnapshotGenerator implements DataProvider {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String SNAPSHOT_PATH = "migration/snapshots/registry-baseline.json";
    /** json file suffix, stripped once from harvested file names to get the resource id path */
    private static final String JSON_SUFFIX = ".json";
    /** prefix marking tag categories inside the harvested map */
    private static final String TAG_KEY_PREFIX = "tags/";

    /** datagen tag folder name -> key used in the "tags" section of the snapshot */
    private static final Map<String, String> TAG_CATEGORY_KEYS = Map.of(
            "blocks", "block",
            "items", "item",
            "fluids", "fluid",
            "entity_types", "entity_type",
            "game_events", "game_event"
    );

    private final PackOutput packOutput;

    public BCRegistrySnapshotGenerator(PackOutput packOutput) {
        this.packOutput = packOutput;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        List<String> modIds = Arrays.stream(BCModules.VALUES)
                .filter(module -> module != BCModules.COMPAT)
                .map(BCModules::getModId)
                .sorted()
                .collect(Collectors.toList());

        List<String> blocks = registryIds(ForgeRegistries.BLOCKS);
        List<String> items = registryIds(ForgeRegistries.ITEMS);
        List<String> blockEntities = registryIds(ForgeRegistries.BLOCK_ENTITY_TYPES);
        List<String> entities = registryIds(ForgeRegistries.ENTITY_TYPES);
        List<String> fluids = registryIds(ForgeRegistries.FLUIDS);
        Map<String, List<String>> harvested = harvestDataFileIds();
        List<String> recipes = harvested.get("recipes");
        Map<String, List<String>> tags = new TreeMap<>();
        harvested.forEach((key, ids) -> {
            if (key.startsWith(TAG_KEY_PREFIX)) {
                tags.put(key.substring(TAG_KEY_PREFIX.length()), ids);
            }
        });

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("generator", "buildcraft.datagen.base.BCRegistrySnapshotGenerator");
        metadata.put("minecraft_version", SharedConstants.getCurrentVersion().getName());
        metadata.put("forge_version", ForgeVersion.getVersion());
        metadata.put("mod_ids", modIds);
        snapshot.put("metadata", metadata);

        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("blocks", blocks.size());
        counts.put("items", items.size());
        counts.put("block_entities", blockEntities.size());
        counts.put("entities", entities.size());
        counts.put("fluids", fluids.size());
        counts.put("recipes", recipes.size());
        tags.forEach((category, ids) -> counts.put("tags." + category, ids.size()));
        snapshot.put("counts", counts);

        snapshot.put("blocks", blocks);
        snapshot.put("items", items);
        snapshot.put("block_entities", blockEntities);
        snapshot.put("entities", entities);
        snapshot.put("fluids", fluids);
        snapshot.put("recipes", recipes);
        snapshot.put("tags", tags);

        writeSnapshot(snapshot);

        BCLog.logger.info(
                "[datagen] Registry snapshot written to {}: blocks={}, items={}, block_entities={}, entities={}, fluids={}, recipes={}, tags={}",
                SNAPSHOT_PATH, blocks.size(), items.size(), blockEntities.size(), entities.size(), fluids.size(), recipes.size(),
                tags.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue().size()).collect(Collectors.joining(", "))
        );
        return CompletableFuture.completedFuture(null);
    }

    private static List<String> registryIds(IForgeRegistry<?> registry) {
        return registry.getKeys().stream()
                .filter(id -> isBuildcraftNamespace(id.getNamespace()))
                .map(ResourceLocation::toString)
                .sorted()
                .collect(Collectors.toList());
    }

    /** All namespaces belong to one of the buildcraft mods or to the shared "buildcraft" data namespace. */
    private static boolean isBuildcraftNamespace(String namespace) {
        return namespace.equals(BCModules.BUILDCRAFT) || BCModules.isBcMod(namespace);
    }

    /**
     * Walks {@code <datagen output>/data/**\/*.json} and turns file paths into resource ids:
     * recipes from {@code data/<ns>/recipes/**} and tags from {@code data/<ns>/tags/<category>/**}
     * (the tag id is the namespace plus the path below the category folder).
     */
    private Map<String, List<String>> harvestDataFileIds() {
        TreeSet<String> recipes = new TreeSet<>();
        Map<String, TreeSet<String>> tags = new TreeMap<>();
        Path dataDir = this.packOutput.getOutputFolder().resolve("data");
        if (Files.isDirectory(dataDir)) {
            try (Stream<Path> stream = Files.walk(dataDir)) {
                stream.filter(Files::isRegularFile)
                        .filter(file -> file.getFileName().toString().endsWith(JSON_SUFFIX))
                        .forEach(file -> harvestDataFile(dataDir, file, recipes, tags));
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to walk datagen output for the registry snapshot", e);
            }
        } else {
            BCLog.logger.warn("[datagen] No data folder at {}, snapshot will contain no recipes/tags", dataDir);
        }

        Map<String, List<String>> harvested = new TreeMap<>();
        harvested.put("recipes", new ArrayList<>(recipes));
        tags.forEach((category, ids) -> harvested.put("tags/" + category, new ArrayList<>(ids)));
        return harvested;
    }

    private static void harvestDataFile(Path dataDir, Path file, TreeSet<String> recipes, Map<String, TreeSet<String>> tags) {
        Path relative = dataDir.relativize(file);
        int nameCount = relative.getNameCount();
        // <namespace>/<recipes|tags|...>/<at least one more segment>.json
        if (nameCount < 3) {
            return;
        }
        String namespace = relative.getName(0).toString();
        if (!isBuildcraftNamespace(namespace)) {
            return;
        }
        String idPath = IntStream.range(2, nameCount)
                .mapToObj(i -> relative.getName(i).toString())
                .collect(Collectors.joining("/"));
        idPath = idPath.substring(0, idPath.length() - JSON_SUFFIX.length());
        String type = relative.getName(1).toString();
        if ("recipes".equals(type)) {
            recipes.add(namespace + ":" + idPath);
        } else if ("tags".equals(type)) {
            harvestTag(namespace, idPath, tags);
        }
    }

    /** {@code idPath} is the path below {@code data/<ns>/tags/}: {@code <category segments>/<tag path>} */
    private static void harvestTag(String namespace, String idPath, Map<String, TreeSet<String>> tags) {
        String category;
        String tagPath;
        if (idPath.startsWith("worldgen/biome/")) {
            // biome tag files live under tags/worldgen/biome/ but their ids do not include the folder
            category = "biome";
            tagPath = idPath.substring("worldgen/biome/".length());
        } else {
            int split = idPath.indexOf('/');
            if (split < 0) {
                category = TAG_CATEGORY_KEYS.getOrDefault(idPath, idPath);
                tagPath = "";
            } else {
                String folder = idPath.substring(0, split);
                category = TAG_CATEGORY_KEYS.getOrDefault(folder, folder);
                tagPath = idPath.substring(split + 1);
            }
        }
        tags.computeIfAbsent(category, key -> new TreeSet<>()).add(namespace + ":" + tagPath);
    }

    private void writeSnapshot(Map<String, Object> snapshot) {
        Path outputFolder = this.packOutput.getOutputFolder();
        Path repoRoot = outputFolder.getParent();
        if (repoRoot == null) {
            throw new IllegalStateException("Cannot locate the repository root from " + outputFolder);
        }
        Path snapshotFile = repoRoot.resolve(SNAPSHOT_PATH);
        try {
            Files.createDirectories(snapshotFile.getParent());
            Files.writeString(snapshotFile, GSON.toJson(snapshot) + "\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write the registry snapshot to " + snapshotFile, e);
        }
    }

    @Override
    public String getName() {
        return "BuildCraft Registry Snapshot";
    }
}
