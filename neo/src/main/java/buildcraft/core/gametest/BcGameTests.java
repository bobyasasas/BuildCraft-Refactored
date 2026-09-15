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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import buildcraft.core.BcBlocks;
import buildcraft.core.BuildCraftCore;
import buildcraft.core.blockentity.StoneEngineBlockEntity;

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
        event.registerTest(
                Identifier.fromNamespaceAndPath(BuildCraftCore.MOD_ID, "engine_stone_produces_power"),
                new BcGameTestInstance(
                        new TestData<>(
                                environment,
                                Identifier.fromNamespaceAndPath(BuildCraftCore.MOD_ID, "engine_test"),
                                200, // maxTicks (coal burns 1600 ticks; the first µMJ appear long before)
                                0, // setupTicks
                                true),
                        BcGameTests::engineStoneProducesPowerTest));
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

    /**
     * M2.2b vertical slice test for the stone engine: place the engine block, inject one coal through the programmatic
     * {@link StoneEngineBlockEntity#insertFuel} entry point, then wait for the server tick loop to ignite the fuel and
     * produce energy. Fuel is resolved through the real vanilla fuel table (coal = 1600 ticks), so this test also pins
     * the fuel-to-<micro>MJ conversion.
     */
    static void engineStoneProducesPowerTest(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, BcBlocks.ENGINE_STONE.value());
        StoneEngineBlockEntity engine = helper.getBlockEntity(pos, StoneEngineBlockEntity.class);
        if (!engine.insertFuel(new ItemStack(Items.COAL), helper.getLevel().fuelValues())) {
            helper.fail("engine rejected a coal item");
        }
        helper.succeedWhen(() -> {
            StoneEngineBlockEntity tickingEngine = helper.getBlockEntity(pos, StoneEngineBlockEntity.class);
            if (tickingEngine.getEnergyStored() <= 0) {
                helper.fail("engine buffer is still empty while fuel should be burning");
            }
            if (tickingEngine.extractEnergy(StoneEngineBlockEntity.POWER_PER_TICK, true) <= 0) {
                helper.fail("extractEnergy(simulate) could not pull the freshly produced energy");
            }
        });
    }

    private BcGameTests() {
    }
}
