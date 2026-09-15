/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

/**
 * Registry parity gate for the full 1.20.1 registry baseline (tasks M2.4a lib+core, M2.4b energy+factory+silicon
 * incl. fluids, M2.4c transport+builders+robotics incl. entities; test id {@code buildcraftcore:registry_parity}):
 * asserts that the frozen runtime registries contain exactly the ids the baseline attributes to the eight
 * {@code buildcraft*} namespaces.
 *
 * <p>The expected id list is a frozen copy of the baseline (blocks/items/block_entities/entities/fluids across the
 * eight namespaces) stored at {@code data/buildcraftcore/registry_parity.json}; the snapshot itself is read-only. For
 * every registry:
 * <ul>
 * <li>every baseline id must exist ({@code missing} must be empty), and</li>
 * <li>every actually registered id in the eight namespaces beyond the baseline must be in
 * {@link #EXTRA_WHITELIST} &mdash; the M2.2 slice blocks that carry the existing game tests and are not
 * part of the 1.20.1 baseline.</li>
 * </ul>
 * Any violation fails the test with the full missing/extra lists in the assertion message.
 */
public final class RegistryParityTest {

    private static final String RESOURCE_PATH = "/data/buildcraftcore/registry_parity.json";

    /** The eight namespaces, all with full baseline content since M2.4c. */
    private static final Set<String> NAMESPACES = Set.of("buildcraftlib", "buildcraftcore", "buildcraftenergy",
            "buildcraftfactory", "buildcraftsilicon", "buildcrafttransport", "buildcraftbuilders",
            "buildcraftrobotics");

    /**
     * Registered ids beyond the baseline that must stay: the M2.2 vertical slice (engine_stone also exists in the
     * baseline, listing it here keeps the check namespace-wide rather than per registry).
     */
    private static final Set<String> EXTRA_WHITELIST = Set.of("marker", "engine_stone", "pipe_kinesis_wood", "energy_meter");

    public static void run(GameTestHelper helper) {
        JsonObject baseline = readBaseline(helper);
        if (baseline == null) {
            return; // readBaseline already failed the test
        }
        List<String> problems = new ArrayList<>();
        checkRegistry(baseline.getAsJsonArray("blocks"), BuiltInRegistries.BLOCK, "blocks", problems);
        checkRegistry(baseline.getAsJsonArray("items"), BuiltInRegistries.ITEM, "items", problems);
        checkRegistry(baseline.getAsJsonArray("block_entities"), BuiltInRegistries.BLOCK_ENTITY_TYPE, "block_entities",
                problems);
        checkRegistry(baseline.getAsJsonArray("entities"), BuiltInRegistries.ENTITY_TYPE, "entities", problems);
        checkRegistry(baseline.getAsJsonArray("fluids"), BuiltInRegistries.FLUID, "fluids", problems);
        if (!problems.isEmpty()) {
            helper.fail("registry parity violated: " + String.join("; ", problems));
            return;
        }
        helper.succeed();
    }

    private static <T> void checkRegistry(JsonArray expected, Registry<T> registry, String label, List<String> problems) {
        Set<String> expectedIds = new java.util.HashSet<>();
        for (JsonElement element : expected) {
            String id = element.getAsString();
            expectedIds.add(id);
            if (!registry.containsKey(Identifier.parse(id))) {
                problems.add(label + " missing " + id);
            }
        }
        for (Identifier id : registry.keySet()) {
            if (!NAMESPACES.contains(id.getNamespace())) {
                continue;
            }
            if (expectedIds.contains(id.toString()) || EXTRA_WHITELIST.contains(id.getPath())) {
                continue;
            }
            problems.add(label + " extra " + id);
        }
    }

    /** Loads the frozen baseline copy from the classpath, failing the test when it cannot be read. */
    private static JsonObject readBaseline(GameTestHelper helper) {
        try (Reader reader = new InputStreamReader(
                RegistryParityTest.class.getResourceAsStream(RESOURCE_PATH), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException | NullPointerException | IllegalStateException e) {
            helper.fail("cannot read parity baseline resource " + RESOURCE_PATH + ": " + e);
            return null;
        }
    }

    private RegistryParityTest() {
    }
}
