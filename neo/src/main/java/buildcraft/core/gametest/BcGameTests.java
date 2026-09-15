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
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import buildcraft.core.BcBlocks;
import buildcraft.core.BuildCraftCore;
import buildcraft.core.block.StoneEngineBlock;
import buildcraft.core.blockentity.EnergyMeterBlockEntity;
import buildcraft.core.blockentity.StoneEngineBlockEntity;

/**
 * Registers buildcraftcore's game tests into the vanilla test instance registry (task M2.2a).
 *
 * <p>Fired on the mod event bus while the test environment/test instance data registries load
 * ({@code RegisterGameTestsEvent}, see the handler wiring in {@link BuildCraftCore}). Each test pairs a structure
 * template from {@code data/buildcraftcore/structure/} with a test function from {@link BcGameTestInstance}.
 *
 * <p>Registration is deliberately skipped on the client dist: {@code minecraft:test_instance} is one of vanilla's
 * login-synchronised registries ({@code RegistryDataLoader#SYNCHRONIZED_REGISTRIES}), and a code-registered instance
 * whose codec is not an entry of the frozen {@code minecraft:test_instance_type} registry cannot be encoded for that
 * sync — the encode failure used to crash every singleplayer world join with "Failed to serialize ... /
 * BcGameTestInstance is code-registered" (found by the M2.5 login handshake probe). The tests only ever run on the
 * headless gametest server (a dedicated, server-dist process), so skipping the client dist costs nothing.
 * TODO(M3+): revisit (e.g. register a real test instance type) if tests should be runnable from a singleplayer
 * {@code /test} command.
 */
public final class BcGameTests {

    public static void onRegisterGameTests(RegisterGameTestsEvent event) {
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            return;
        }
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
        event.registerTest(
                Identifier.fromNamespaceAndPath(BuildCraftCore.MOD_ID, "kinesis_chain_transfers_power"),
                new BcGameTestInstance(
                        new TestData<>(
                                environment,
                                Identifier.fromNamespaceAndPath(BuildCraftCore.MOD_ID, "kinesis_test"),
                                200, // maxTicks: budget for burn ignition + two diffusion hops, assertion succeeds in ~15 ticks
                                0, // setupTicks
                                true),
                        BcGameTests::kinesisChainTransfersPowerTest));
        // M2.4a registry parity gate: needs no blocks, so it runs in the vanilla empty structure.
        event.registerTest(
                Identifier.fromNamespaceAndPath(BuildCraftCore.MOD_ID, "registry_parity"),
                new BcGameTestInstance(
                        new TestData<>(
                                environment,
                                Identifier.withDefaultNamespace("empty"),
                                100, // maxTicks: the checks are immediate; margin only
                                0, // setupTicks
                                true),
                        RegistryParityTest::run));
        // M2.6 data component parity gate: registration ids + ItemStack persistence shape vs legacy NBT. Immediate
        // checks, so it runs in the vanilla empty structure too.
        event.registerTest(
                Identifier.fromNamespaceAndPath(BuildCraftCore.MOD_ID, "data_components_parity"),
                new BcGameTestInstance(
                        new TestData<>(
                                environment,
                                Identifier.withDefaultNamespace("empty"),
                                100, // maxTicks: the checks are immediate; margin only
                                0, // setupTicks
                                true),
                        DataComponentsParityTest::run));
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

    /**
     * M2.2c full-chain vertical slice test: builds the complete energy path of the slice in code (no structure blocks
     * needed beyond the empty air box) &mdash; engine with {@code FACING = EAST} at x=1, kinesis pipe A at x=2, kinesis
     * pipe B at x=3, energy meter at x=4, all in one straight line. One coal is injected through the programmatic
     * {@link StoneEngineBlockEntity#insertFuel} entry point; the engine burns it, pipe A pulls the micro-MJ out of the
     * engine buffer, equalises into pipe B, and pipe B pushes into the meter. The test succeeds as soon as the meter
     * reports any lifetime received energy ({@code succeedWhen} polls each tick via
     * {@code GameTestSequence#thenWaitUntil}); a meter reading greater than zero transitively proves every link of the
     * chain, because the meter is only fed by pipe B, which is only fed by pipe A, which is only fed by the engine.
     */
    static void kinesisChainTransfersPowerTest(GameTestHelper helper) {
        BlockPos enginePos = new BlockPos(1, 1, 1);
        BlockPos pipeAPos = new BlockPos(2, 1, 1);
        BlockPos pipeBPos = new BlockPos(3, 1, 1);
        BlockPos meterPos = new BlockPos(4, 1, 1);
        helper.setBlock(enginePos,
                BcBlocks.ENGINE_STONE.value().defaultBlockState().setValue(StoneEngineBlock.FACING, Direction.EAST));
        helper.setBlock(pipeAPos, BcBlocks.PIPE_KINESIS_WOOD.value());
        helper.setBlock(pipeBPos, BcBlocks.PIPE_KINESIS_WOOD.value());
        helper.setBlock(meterPos, BcBlocks.ENERGY_METER.value());
        StoneEngineBlockEntity engine = helper.getBlockEntity(enginePos, StoneEngineBlockEntity.class);
        if (!engine.insertFuel(new ItemStack(Items.COAL), helper.getLevel().fuelValues())) {
            helper.fail("engine rejected a coal item");
        }
        helper.succeedWhen(() -> {
            EnergyMeterBlockEntity meter = helper.getBlockEntity(meterPos, EnergyMeterBlockEntity.class);
            if (meter.getTotalReceived() <= 0) {
                helper.fail("energy meter is still empty while the kinesis chain should be transferring power");
            }
        });
    }

    private BcGameTests() {
    }
}
