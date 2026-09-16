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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import buildcraft.core.BcBlocks;
import buildcraft.core.BuildCraftCore;
import buildcraft.core.block.StoneEngineBlock;
import buildcraft.core.blockentity.EnergyMeterBlockEntity;
import buildcraft.core.blockentity.KinesisPipeBlockEntity;
import buildcraft.core.blockentity.StoneEngineBlockEntity;
import buildcraft.core.gate.BcGateLogic;
import buildcraft.core.gate.BcGateStatements;
import buildcraft.lib.datacomponent.gate.BcGateStatement;
import buildcraft.lib.datacomponent.gate.EnumGateLogic;
import buildcraft.lib.datacomponent.gate.EnumGateMaterial;
import buildcraft.lib.datacomponent.gate.EnumGateModifier;
import buildcraft.lib.datacomponent.gate.GateVariantData;

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
        // M2.11 gate slice: trigger/action evaluation on the kinesis pipe's gate slot, with the physical redstone
        // loop (engine trigger -> redstone output -> vanilla lamp) and the BE-field assertion (wire broadcast).
        event.registerTest(
                Identifier.fromNamespaceAndPath(BuildCraftCore.MOD_ID, "gate_logic"),
                new BcGameTestInstance(
                        new TestData<>(
                                environment,
                                Identifier.fromNamespaceAndPath(BuildCraftCore.MOD_ID, "gate_test"),
                                200, // maxTicks (coal burns 1600 ticks; the trigger fires long before)
                                0, // setupTicks
                                true),
                        BcGameTests::gateLogicTest));
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

    /**
     * M2.11 gate slice test: one rig exercising both minimal triggers and both actions of the gate on the kinesis
     * pipe's gate slot. Legacy ({@code buildcraft.silicon.gate.GateLogic#resolveActions}) alignment points:
     * <ul>
     * <li>trigger {@code buildcraft:engine.stage.blue} (legacy {@code TriggerEnginePowerStage(BLUE)}, external on a
     * neighbouring engine) &mdash; fed by the coal-fired stone engine east of the pipe;</li>
     * <li>action {@code buildcraft:redstone.output} (legacy {@code ActionRedstoneOutput}) &mdash; latches the pipe's
     * redstone output on the gate face (legacy never resets it, {@code IAction#actionDeactivated} is a no-op there);
     * the physical closed loop is the vanilla redstone lamp on the gate face lighting up through the pipe block's
     * signal overrides (legacy {@code BlockPipeHolder#getSignal});</li>
     * <li>trigger {@code buildcraft:redstone.input.active} (legacy {@code TriggerRedstoneInput(true)}) &mdash; fed by
     * the redstone block above the pipe (legacy: {@code getRedstoneInput(null) = getBestNeighborSignal > 0});</li>
     * <li>action {@code buildcraft:pipe.wire.output.red} (legacy {@code ActionPipeSignal(RED)}) &mdash; asserted on
     * the pipe BE's broadcast set; the wire-network flood is transport content that has not migrated.</li>
     * </ul>
     * Layout: engine at x=1 (FACING EAST into the pipe), pipe at x=2 (gate on its EAST face), lamp at x=3, redstone
     * block on top of the pipe (y=2). The test succeeds once every link above reports active.
     */
    static void gateLogicTest(GameTestHelper helper) {
        BlockPos enginePos = new BlockPos(1, 1, 1);
        BlockPos pipePos = new BlockPos(2, 1, 1);
        BlockPos lampPos = new BlockPos(3, 1, 1);
        BlockPos powerPos = new BlockPos(2, 2, 1);
        helper.setBlock(enginePos,
                BcBlocks.ENGINE_STONE.value().defaultBlockState().setValue(StoneEngineBlock.FACING, Direction.EAST));
        helper.setBlock(pipePos, BcBlocks.PIPE_KINESIS_WOOD.value());
        helper.setBlock(lampPos, Blocks.REDSTONE_LAMP);
        helper.setBlock(powerPos, Blocks.REDSTONE_BLOCK);
        KinesisPipeBlockEntity pipe = helper.getBlockEntity(pipePos, KinesisPipeBlockEntity.class);
        // the registered gate item variant: plug_gate_iron_and_no_modifier = 2 AND slots
        pipe.attachGate(Direction.EAST,
                new GateVariantData(EnumGateLogic.AND, EnumGateMaterial.IRON, EnumGateModifier.NO_MODIFIER));
        BcGateLogic gate = pipe.getGate();
        if (gate == null) {
            helper.fail("gate did not attach to the pipe");
            return;
        }
        byte center = BcGateStatement.SIDE_CENTER;
        gate.configureSlot(0,
                new BcGateStatement(BcGateStatements.TRIGGER_ENGINE_BLUE, center),
                new BcGateStatement(BcGateStatements.ACTION_REDSTONE_OUTPUT, center));
        gate.configureSlot(1,
                new BcGateStatement(BcGateStatements.TRIGGER_REDSTONE_ACTIVE, center),
                new BcGateStatement(BcGateStatements.ACTION_PIPE_WIRE_RED, center));
        StoneEngineBlockEntity engine = helper.getBlockEntity(enginePos, StoneEngineBlockEntity.class);
        if (!engine.insertFuel(new ItemStack(Items.COAL), helper.getLevel().fuelValues())) {
            helper.fail("engine rejected a coal item");
        }
        helper.succeedWhen(() -> {
            BcGateLogic tickingGate = helper.getBlockEntity(pipePos, KinesisPipeBlockEntity.class).getGate();
            if (tickingGate == null) {
                helper.fail("gate vanished from the pipe");
            } else {
                if (!tickingGate.isTriggerOn(0)) {
                    helper.fail("engine.stage.blue trigger has not fired while the engine should be burning");
                }
                if (tickingGate.getRedstoneOutput() != 15) {
                    helper.fail("redstone.output action did not latch the gate face output at 15");
                }
                if (!helper.getBlockState(lampPos).getValue(RedstoneLampBlock.LIT)) {
                    helper.fail("the lamp on the gate face is not lit by the gate's redstone output");
                }
                if (!tickingGate.isTriggerOn(1)) {
                    helper.fail("redstone.input.active trigger has not fired while the redstone block is adjacent");
                }
                if (!tickingGate.getWireBroadcasts().contains(DyeColor.RED)) {
                    helper.fail("pipe.wire.output.red action did not broadcast the red wire");
                }
                if (!tickingGate.isOn()) {
                    helper.fail("gate isOn flag is false while both actions are active");
                }
            }
        });
    }

    private BcGameTests() {
    }
}
