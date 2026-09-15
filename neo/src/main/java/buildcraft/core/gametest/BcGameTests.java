/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import buildcraft.core.BcBlocks;
import buildcraft.core.BuildCraftCore;

/**
 * Registers buildcraftcore's game tests into the vanilla test instance registry (task M2.2a).
 *
 * <p>Fired on the mod event bus while the test environment/test instance data registries load
 * ({@code RegisterGameTestsEvent}, see the handler wiring in {@link BuildCraftCore}). Each test pairs a structure
 * template from {@code data/buildcraftcore/structure/} with a test function from {@link BcGameTestInstance}.
 */
public final class BcGameTests {

    public static void onRegisterGameTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(BuildCraftCore.MOD_ID, "default"));
        event.registerTest(
                Identifier.fromNamespaceAndPath(BuildCraftCore.MOD_ID, "marker_smoke"),
                new BcGameTestInstance(
                        new TestData<>(
                                environment,
                                Identifier.fromNamespaceAndPath(BuildCraftCore.MOD_ID, "marker_test"),
                                200, // maxTicks
                                0, // setupTicks
                                true),
                        BcGameTests::markerSmokeTest));
    }

    /**
     * Smoke test proving structure loading, helper API and the headless gametest server all work: place the marker
     * placeholder block, assert it is really there, then succeed.
     */
    static void markerSmokeTest(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, BcBlocks.MARKER.value());
        helper.assertBlockPresent(BcBlocks.MARKER.value(), pos);
        helper.succeed();
    }

    private BcGameTests() {
    }
}
