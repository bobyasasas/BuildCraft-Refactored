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
import buildcraft.builders.BcBuildersBlocks;
import buildcraft.builders.blockentity.FillerBlockEntity;
import buildcraft.builders.blockentity.QuarryBlockEntity;
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
import buildcraft.robotics.BcRoboticsEntities;
import buildcraft.robotics.entity.EntityRobot;
import buildcraft.robotics.zone.BoxZone;

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
        // M2.12 filler slice: full powered chain engine -> kinesis pipe -> filler, one buildcraft:fill cycle over a
        // 2x2x2 area. Lives in the core suite (like gate_logic, which is silicon content) because the slice's energy
        // chain and test infrastructure are core content; the exercised machines are builders content.
        event.registerTest(
                Identifier.fromNamespaceAndPath(BuildCraftCore.MOD_ID, "filler_cycle"),
                new BcGameTestInstance(
                        new TestData<>(
                                environment,
                                Identifier.fromNamespaceAndPath(BuildCraftCore.MOD_ID, "filler_test"),
                                // 8 dirt x 4800 uMJ = 38400 uMJ at the slice engine's 100 uMJ/t production
                                600, // maxTicks
                                0, // setupTicks
                                true),
                        BcGameTests::fillerCycleTest));
        // M2.12 quarry slice: the same powered chain feeding a quarry that mines a 2x2x1 stone area into its output
        // buffer (frame blocks, lasers and markers are not part of the slice).
        event.registerTest(
                Identifier.fromNamespaceAndPath(BuildCraftCore.MOD_ID, "quarry_cycle"),
                new BcGameTestInstance(
                        new TestData<>(
                                environment,
                                Identifier.fromNamespaceAndPath(BuildCraftCore.MOD_ID, "quarry_test"),
                                // 4 stone x 8000 uMJ = 32000 uMJ at the slice engine's 100 uMJ/t production
                                600, // maxTicks
                                0, // setupTicks
                                true),
                        BcGameTests::quarryCycleTest));
        // M2.13 robot slice: one summoned robot_miner mines the two iron ores of its work zone and parks done.
        event.registerTest(
                Identifier.fromNamespaceAndPath(BuildCraftCore.MOD_ID, "robot_task"),
                new BcGameTestInstance(
                        new TestData<>(
                                environment,
                                Identifier.fromNamespaceAndPath(BuildCraftCore.MOD_ID, "robot_test"),
                                // ~80 ticks of flight + two 12-tick breaks at the slice pace; 400 = ample margin
                                400, // maxTicks
                                0, // setupTicks
                                true),
                        BcGameTests::robotTaskTest));
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

    /**
     * M2.12 filler cycle test: the full powered build chain of the slice &mdash; stone engine &rarr; kinesis pipe
     * &rarr; filler &mdash; completing one {@code buildcraft:fill} cycle over a 2&times;2&times;2 area. Legacy
     * ({@code buildcraft.builders.tile.TileFiller}) alignment points:
     * <ul>
     * <li>power enters through the legacy {@code IMjReceiver} semantics ({@code MjBatteryReceiver}: request
     * {@code capacity - stored}, receive, return the excess) &mdash; here via the pipe's {@code MjReceiver} push into
     * the filler battery (legacy battery: 16,000 MJ; slice: 1,600,000 &micro;MJ on the M2.2 &times;10&#8315;&#8308;
     * scale);</li>
     * <li>the pattern is legacy {@code PatternFill} (unique tag {@code buildcraft:fill} = "every cell solid"), the
     * simplest of the frozen tree's 20+ filler patterns and the only one migrated;</li>
     * <li>the work area replaces the legacy marker/volume box discovery (not migrated) as programmatic BE fields;</li>
     * <li>the resources ride the single-stack slice stand-in for the legacy 27-slot {@code invResources} (block items
     * only, and the build stalls without them exactly like the legacy "missing resources" stall);</li>
     * <li>each placement is powered ({@code 3,200 &micro;MJ &times; (hardness + 1)} per block, the slice form of the
     * legacy {@code computeBlockBreakPower} formula &mdash; the 8.x tree dropped per-block drain, the slice restores it
     * so "powered building" stays testable).</li>
     * </ul>
     * Layout: engine at x=1 (FACING EAST into the pipe), pipe at x=2, filler at x=3, work area (4,1,1)..(5,2,2). The
     * test succeeds once the filler reports finished, has drawn power through the pipe, and every area cell is dirt.
     */
    static void fillerCycleTest(GameTestHelper helper) {
        BlockPos enginePos = new BlockPos(1, 1, 1);
        BlockPos pipePos = new BlockPos(2, 1, 1);
        BlockPos fillerPos = new BlockPos(3, 1, 1);
        BlockPos areaMin = new BlockPos(4, 1, 1);
        BlockPos areaMax = new BlockPos(5, 2, 2);
        helper.setBlock(enginePos,
                BcBlocks.ENGINE_STONE.value().defaultBlockState().setValue(StoneEngineBlock.FACING, Direction.EAST));
        helper.setBlock(pipePos, BcBlocks.PIPE_KINESIS_WOOD.value());
        helper.setBlock(fillerPos, BcBuildersBlocks.FILLER.value());
        FillerBlockEntity filler = helper.getBlockEntity(fillerPos, FillerBlockEntity.class);
        // legacy PatternFill unique tag
        if (!filler.setPattern("buildcraft:fill")) {
            helper.fail("filler rejected the buildcraft:fill pattern");
            return;
        }
        filler.setWorkArea(helper.absolutePos(areaMin), helper.absolutePos(areaMax));
        if (!filler.insertResource(new ItemStack(Items.DIRT, 8)).isEmpty()) {
            helper.fail("filler rejected a dirt stack for its resource buffer");
            return;
        }
        StoneEngineBlockEntity engine = helper.getBlockEntity(enginePos, StoneEngineBlockEntity.class);
        if (!engine.insertFuel(new ItemStack(Items.COAL), helper.getLevel().fuelValues())) {
            helper.fail("engine rejected a coal item");
            return;
        }
        helper.succeedWhen(() -> {
            FillerBlockEntity tickingFiller = helper.getBlockEntity(fillerPos, FillerBlockEntity.class);
            if (tickingFiller.getTotalReceived() <= 0) {
                helper.fail("filler has not received any power through the kinesis pipe");
            }
            if (!tickingFiller.isFinished()) {
                helper.fail("filler has not finished the buildcraft:fill cycle yet");
            }
            for (BlockPos pos : BlockPos.betweenClosed(areaMin, areaMax)) {
                if (helper.getBlockState(pos).getBlock() != Blocks.DIRT) {
                    helper.fail("fill area cell " + pos + " was not filled with dirt");
                }
            }
        });
    }

    /**
     * M2.12 quarry cycle test: the same powered chain &mdash; stone engine &rarr; kinesis pipe &rarr; quarry &mdash;
     * mining a 2&times;2&times;1 stone area "into the output". Legacy
     * ({@code buildcraft.builders.tile.TileQuarry}) alignment points:
     * <ul>
     * <li>power enters through the legacy {@code IMjReceiver} semantics into the quarry battery (legacy 24,000 MJ /
     * slice 2,400,000 &micro;MJ on the M2.2 &times;10&#8315;&#8308; scale) and is spent per tick like
     * {@code TaskBreakBlock}: the active target accumulates {@code battery.extractPower} until it reaches its target
     * cost, with the overshoot refunded (all three behaviours live in {@code QuarryBlockEntity#tickWork});</li>
     * <li>the per-block cost is legacy {@code BlockUtil.computeBlockBreakPower = 16 MJ * (hardness + 1) * 2} on the
     * slice scale ({@code 3,200 &micro;MJ * (hardness + 1)} &mdash; stone: 8,000 &micro;MJ);</li>
     * <li>breaking uses the legacy tool parity ({@code breakBlockAndGetDrops(DIAMOND_PICKAXE)}) and the legacy
     * {@code canMine} guards (no air, no fluids, breakable only);</li>
     * <li>the drops land in the internal output buffer &mdash; the slice stand-in for the legacy push-to-acceptor
     * ({@code InventoryUtil.addToBestAcceptor}); the legacy quarry itself has no inventory;</li>
     * <li>the mining area replaces the legacy volume-marker discovery, and the legacy frame-block system
     * ({@code TaskAddFrame}), lasers and drill entities are not part of the slice.</li>
     * </ul>
     * Layout: engine at x=1 (FACING EAST into the pipe), pipe at x=2, quarry at x=3, mining area (4,1,1)..(5,1,2)
     * pre-filled with stone. The test succeeds once the quarry reports finished, has drawn power through the pipe,
     * every area cell is air again and the output buffer holds the four cobblestone drops.
     */
    static void quarryCycleTest(GameTestHelper helper) {
        BlockPos enginePos = new BlockPos(1, 1, 1);
        BlockPos pipePos = new BlockPos(2, 1, 1);
        BlockPos quarryPos = new BlockPos(3, 1, 1);
        BlockPos areaMin = new BlockPos(4, 1, 1);
        BlockPos areaMax = new BlockPos(5, 1, 2);
        helper.setBlock(enginePos,
                BcBlocks.ENGINE_STONE.value().defaultBlockState().setValue(StoneEngineBlock.FACING, Direction.EAST));
        helper.setBlock(pipePos, BcBlocks.PIPE_KINESIS_WOOD.value());
        helper.setBlock(quarryPos, BcBuildersBlocks.QUARRY.value());
        for (BlockPos pos : BlockPos.betweenClosed(areaMin, areaMax)) {
            helper.setBlock(pos, Blocks.STONE);
        }
        QuarryBlockEntity quarry = helper.getBlockEntity(quarryPos, QuarryBlockEntity.class);
        quarry.setMiningArea(helper.absolutePos(areaMin), helper.absolutePos(areaMax));
        StoneEngineBlockEntity engine = helper.getBlockEntity(enginePos, StoneEngineBlockEntity.class);
        if (!engine.insertFuel(new ItemStack(Items.COAL), helper.getLevel().fuelValues())) {
            helper.fail("engine rejected a coal item");
            return;
        }
        helper.succeedWhen(() -> {
            QuarryBlockEntity tickingQuarry = helper.getBlockEntity(quarryPos, QuarryBlockEntity.class);
            if (tickingQuarry.getTotalReceived() <= 0) {
                helper.fail("quarry has not received any power through the kinesis pipe");
            }
            if (!tickingQuarry.isFinished()) {
                helper.fail("quarry has not finished mining the area yet");
            }
            int cobbleCount = 0;
            for (ItemStack drop : tickingQuarry.getOutput()) {
                if (drop.is(Items.COBBLESTONE)) {
                    cobbleCount += drop.getCount();
                }
            }
            if (cobbleCount != 4) {
                helper.fail("quarry output buffer holds " + cobbleCount + " cobblestone instead of 4");
            }
            for (BlockPos pos : BlockPos.betweenClosed(areaMin, areaMax)) {
                if (!helper.getBlockState(pos).isAir()) {
                    helper.fail("mined area cell " + pos + " is not air");
                }
            }
        });
    }

    /**
     * M2.13 robot task test: one summoned {@code buildcraftrobotics:robot_miner} completes a full mining task inside
     * its work zone. Legacy ({@code buildcraft.robotics}) alignment points:
     * <ul>
     * <li>the robot is the M2.13 slice {@code EntityRobot} under the unchanged placeholder id
     * {@code buildcraftrobotics:robot_miner} (legacy registers one type per board; the miner board
     * {@code BoardRobotMiner} is the slice board);</li>
     * <li>the work zone is the slice {@link BoxZone} stand-in for the legacy zone-planner bitmap
     * ({@code IZone#getZoneToWork}); the robot only mines inside it, in the slice's deterministic scan order
     * (legacy: random {@code BlockScannerZoneRandom});</li>
     * <li>targets are legacy-miner semantics: ore blocks ({@code BoardRobotMiner#isExpectedBlock} = ores; slice:
     * the vanilla {@code #minecraft:iron_ores} tag, see {@link EntityRobot});</li>
     * <li>the task walks the collapsed legacy AI tree search &rarr; straight-line flight &rarr; break (per-tick
     * damage + per-tick AI power costs, legacy {@code AIRobotSearchBlock}/{@code AIRobotStraightMoveTo}/
     * {@code AIRobotBreak} on the &times;10&#8315;&#8304; slice scale) and latches
     * {@code DONE} once the zone is exhausted (legacy: {@code AIRobotGotoSleep} at the dock &mdash; no dock in the
     * slice);</li>
     * <li>power enters entity-directly through the legacy {@code IMjReceiver} request/accept/excess semantics
     * (the slice's station-less closed loop; legacy recharges at docking stations, which have no placeholder id to
     * activate).</li>
     * </ul>
     * Layout: robot summoned at (1,1,1), work zone (4,1,1)..(5,1,2) with iron ore at (4,1,1) and (5,1,2). The test
     * succeeds once the robot reports DONE, has drained its battery doing the work, both ore cells are air again
     * and the robot carries the two raw iron drops.
     */
    static void robotTaskTest(GameTestHelper helper) {
        BlockPos robotPos = new BlockPos(1, 1, 1);
        BlockPos oreAPos = new BlockPos(4, 1, 1);
        BlockPos oreBPos = new BlockPos(5, 1, 2);
        helper.setBlock(oreAPos, Blocks.IRON_ORE);
        helper.setBlock(oreBPos, Blocks.IRON_ORE);
        EntityRobot robot = helper.spawn(BcRoboticsEntities.ROBOT_MINER.value(), robotPos);
        robot.setWorkZone(new BoxZone(helper.absolutePos(oreAPos), helper.absolutePos(oreBPos)));
        // legacy IMjReceiver semantics: full charge through the entity-direct-power entry point
        robot.receivePower(EntityRobot.BATTERY_CAPACITY, false);
        if (robot.getPowerRequested() != 0) {
            helper.fail("robot refused a full battery charge");
            return;
        }
        helper.succeedWhen(() -> {
            if (robot.getTaskState() != EntityRobot.RobotTaskState.DONE) {
                helper.fail("robot has not finished mining its work zone yet (state " + robot.getTaskState() + ")");
            }
            if (robot.getEnergyStored() >= EntityRobot.BATTERY_CAPACITY) {
                helper.fail("robot battery was never drained by the task");
            }
            int rawIron = 0;
            for (ItemStack stack : robot.getCarried()) {
                if (stack.is(Items.RAW_IRON)) {
                    rawIron += stack.getCount();
                }
            }
            if (rawIron != 2) {
                helper.fail("robot carries " + rawIron + " raw iron instead of the 2 mined ores");
            }
            for (BlockPos pos : BlockPos.betweenClosed(oreAPos, oreBPos)) {
                if (!helper.getBlockState(pos).isAir()) {
                    helper.fail("work zone cell " + pos + " was not mined");
                }
            }
        });
    }

    private BcGameTests() {
    }
}
