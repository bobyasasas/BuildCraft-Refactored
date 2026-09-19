/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.gate;

import java.util.Map;
import java.util.Set;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import buildcraft.core.BcBlocks;
import buildcraft.core.block.StoneEngineBlock;
import buildcraft.core.blockentity.KinesisPipeBlockEntity;
import buildcraft.core.blockentity.PowerTesterBlockEntity;
import buildcraft.core.blockentity.StoneEngineBlockEntity;
import buildcraft.lib.datacomponent.gate.BcGateStatement;
import buildcraft.silicon.BcSiliconItems;

/**
 * The M4.17 gate redstone evidence rig (the M49MatrixProbe pattern: a default-inert client probe armed only by
 * {@code -Dbuildcraft.m417bprobe=true}). Builds one engine &rarr; kinesis chain whose middle pipe carries a real
 * configured gate, then drives the M4.9 PARTIAL's missing live evidence: a lever flips the
 * {@code buildcraft:redstone.input.active} trigger and the rig records the dual state &mdash;
 * <ul>
 * <li><b>lever OFF</b>: the chain transmits (the power tester's {@code totalReceived} rises, gate dark);</li>
 * <li><b>lever ON</b>: the trigger fires, the {@code buildcraft:redstone.output} action latches the gate face at 15
 * (the lamp behind the repeater lights) and the slice {@code buildcraft:pipe.power.cutoff} action chokes the pipe, so
 * the tester's received counter stalls while {@code gateOn} reads true;</li>
 * <li><b>lever OFF again</b>: trigger and cutoff drop and the chain recovers, while the latch honestly holds (legacy
 * {@code ActionRedstoneOutput} never resets) &mdash; the lamp stays lit, as the {@code gate_logic} gametest asserts.</li>
 * </ul>
 *
 * <p>The lamp sits one <b>redstone repeater</b> behind the gate face, never directly on it: the repeater is a diode
 * (nothing conducts back towards the pipe). A lamp mounted straight on the gate face strong-powers from the pipe's
 * {@code getDirectSignal} (legacy {@code BlockPipeHolder} surface) and re-radiates 15 back into the pipe's trigger
 * read through the vanilla powered-conductor pass-through ({@code SignalGetter#getSignal}'s
 * {@code shouldCheckWeakPower} branch) &mdash; run 1's first-time observation: the trigger could then never drop and
 * the OFF-restore state never happened.
 *
 * <p>Every state logs a {@code [M417]} line with the gate state (server truth via the integrated server) and the
 * tester counters, and the three states are screenshotted into the gameDir's screenshots folder.
 */
@EventBusSubscriber(modid = "buildcraftcore", value = Dist.CLIENT)
public final class M417GateProbe {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Armed only by {@code -Dbuildcraft.m417bprobe=true} (default-inert, the M45/M46/M47/M49 discipline). */
    private static final boolean ENABLED = Boolean.getBoolean("buildcraft.m417bprobe");

    /** Ticks between rig steps. */
    private static final int WAIT_TICKS = 30;

    private static int state = 0;
    private static int wait = 0;
    private static BlockPos anchor = null;
    private static int groundY = -1;
    private static float viewYaw;
    private static float viewPitch;

    private static BlockPos enginePos;
    private static BlockPos gatedPipePos;
    private static BlockPos testerPos;
    private static BlockPos leverPos;
    private static BlockPos repeaterPos;
    private static BlockPos lampPos;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) {
            return;
        }
        ServerLevel level = mc.getSingleplayerServer().getLevel(Level.OVERWORLD);
        if (level == null || state > 10) {
            return;
        }
        // keep the vantage view pinned
        mc.player.setYRot(viewYaw);
        mc.player.setXRot(viewPitch);
        if (--wait > 0) {
            return;
        }
        ServerPlayer player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
        if (player == null) {
            return;
        }
        switch (state) {
            case 0 -> onServer(mc, () -> {
                BlockPos spawn = player.blockPosition();
                anchor = new BlockPos(spawn.getX() + 48, 0, spawn.getZ());
                groundY = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos(anchor.getX(), 0, anchor.getZ())).getY();
                int x = anchor.getX();
                int z = anchor.getZ();
                enginePos = new BlockPos(x, groundY, z);
                gatedPipePos = new BlockPos(x + 2, groundY, z);
                testerPos = new BlockPos(x + 4, groundY, z);
                repeaterPos = new BlockPos(x + 2, groundY, z + 1);
                lampPos = new BlockPos(x + 2, groundY, z + 2);
                leverPos = new BlockPos(x + 2, groundY, z - 1);
                clearFootprint(level, x - 2, x + 6, z - 3, z + 8);
                buildScene(level);
                // pin the daylight: the dual-state shots must differ by rig state, not by the sun
                MinecraftServer server = mc.getSingleplayerServer();
                server.execute(() -> server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack().withSuppressedOutput(), "time set noon"));
                // elevated wide vantage: lever (z-1), gated pipe, repeater (z+1) and lamp (z+2) all in frame
                setView(player, x + 2.5, groundY + 4.5, z + 10.5, 180.0F, 30.0F);
                LOGGER.info("[M417] rig start: anchor={} groundY={} gate@{} south face, lever north, repeater+lamp behind the gate face, camera at {}",
                    anchor, groundY, gatedPipePos, new BlockPos(x + 2, groundY + 4, z + 10));
            });
            // ---- lever OFF: the plain M4.9 chain must transmit ----
            case 1 -> onServer(mc, () -> logState(level, "off t0 (lever OFF)"));
            case 2 -> {
                onServer(mc, () -> logState(level, "off t1 (lever OFF)"));
                screenshot(mc, "m417b_state_off.png");
            }
            // ---- lever ON: trigger fires -> lamp latches + pipe chokes ----
            case 3 -> onServer(mc, () -> {
                setLever(level, true);
                LOGGER.info("[M417] lever flipped ON at {}", leverPos);
            });
            case 4 -> {
                onServer(mc, () -> logState(level, "on t0 (lever ON)"));
                screenshot(mc, "m417b_state_on.png");
                wait = 100;
            }
            case 5 -> onServer(mc, () -> logState(level, "on t1 (lever ON)"));
            // ---- lever OFF again: trigger/cutoff recover, the latch holds (legacy) ----
            case 6 -> onServer(mc, () -> {
                setLever(level, false);
                LOGGER.info("[M417] lever flipped OFF again at {}", leverPos);
            });
            case 7 -> {
                onServer(mc, () -> logState(level, "restore t0 (lever OFF again)"));
                wait = 100;
            }
            case 8 -> {
                onServer(mc, () -> logState(level, "restore t1 (lever OFF again)"));
                screenshot(mc, "m417b_state_off_restore.png");
            }
            case 9 -> {
                LOGGER.info("[M417] rig done - shutting down");
                wait = 2 * WAIT_TICKS;
            }
            default -> {
                state = 11;
                mc.stop();
                return;
            }
        }
        state++;
        wait = Math.max(wait, WAIT_TICKS);
    }

    /** Marshals one chunk of world work onto the server thread (the M45 rig discipline). */
    private static void onServer(Minecraft mc, Runnable task) {
        mc.getSingleplayerServer().execute(task);
    }

    // ---------------------------------------------------------------- scene

    /**
     * The dual-state rig: fueled stone engine facing east &rarr; kinesis pipe &rarr; <b>gated</b> kinesis pipe
     * (iron AND gate on the south face, slot 0 redstone-input&rarr;redstone-output, slot 1
     * redstone-input&rarr;pipe-power-cutoff) &rarr; kinesis pipe &rarr; power tester. The lamp stands one redstone
     * repeater behind the gate face (repeater FACING north = its input reads the gate face, output feeds the lamp
     * south; the diode blocks the powered-conductor feedback that a directly-mounted lamp would radiate back into the
     * trigger read); the floor lever sits on the opposite (north) side of the gated pipe &mdash; the legacy
     * {@code getRedstoneInput(null)} default reads the best neighbour signal of the whole pipe, any side.
     */
    private static void buildScene(ServerLevel level) {
        int x = anchor.getX();
        int z = anchor.getZ();
        level.setBlockAndUpdate(enginePos, BcBlocks.ENGINE_STONE.value().defaultBlockState()
            .setValue(StoneEngineBlock.FACING, Direction.EAST));
        if (level.getBlockEntity(enginePos) instanceof StoneEngineBlockEntity engine) {
            boolean fueled = engine.insertFuel(new ItemStack(Items.COAL, 4), level.fuelValues());
            LOGGER.info("[M417] chain engine fueled with 4 coal: {}", fueled);
        }
        for (int i = 1; i <= 3; i++) {
            level.setBlockAndUpdate(new BlockPos(x + i, groundY, z),
                BcBlocks.PIPE_KINESIS_WOOD.value().defaultBlockState());
        }
        if (level.getBlockEntity(gatedPipePos) instanceof KinesisPipeBlockEntity pipe) {
            boolean attached = pipe.attachGate(Direction.SOUTH, BcSiliconItems.IRON_GATE_VARIANT);
            BcGateLogic gate = pipe.getGate();
            byte center = BcGateStatement.SIDE_CENTER;
            gate.configureSlot(0, new BcGateStatement(BcGateStatements.TRIGGER_REDSTONE_ACTIVE, center, Map.of()),
                new BcGateStatement(BcGateStatements.ACTION_REDSTONE_OUTPUT, center, Map.of()));
            gate.configureSlot(1, new BcGateStatement(BcGateStatements.TRIGGER_REDSTONE_ACTIVE, center, Map.of()),
                new BcGateStatement(BcGateStatements.ACTION_PIPE_POWER_CUTOFF, center, Map.of()));
            LOGGER.info("[M417] gate attached: {} (variant {}, slots: redstone.active -> redstone.output | redstone.active -> pipe.power.cutoff)",
                attached, gate.getVariant());
        } else {
            LOGGER.error("[M417] no kinesis BE at {} (block={})", gatedPipePos, level.getBlockState(gatedPipePos));
        }
        level.setBlockAndUpdate(testerPos, BcBlocks.POWER_TESTER.value().defaultBlockState());
        // 26.1.2 repeater semantics: FACING points at the INPUT (DiodeBlock#getInputSignal reads pos.relative(FACING))
        // and the output leaves through the opposite side — so FACING=NORTH reads the gate face and feeds the lamp south
        level.setBlockAndUpdate(repeaterPos, Blocks.REPEATER.defaultBlockState()
            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        level.setBlockAndUpdate(lampPos, Blocks.REDSTONE_LAMP.defaultBlockState());
        setLever(level, false);
        LOGGER.info("[M417] scene built: engine {} -> pipes x3 -> tester {}; gate face {} -> repeater {} -> lamp {}, lever {} north",
            enginePos, testerPos, gatedPipePos, repeaterPos, lampPos, leverPos);
    }

    private static void setLever(ServerLevel level, boolean on) {
        BlockState lever = Blocks.LEVER.defaultBlockState()
            .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
            .setValue(BlockStateProperties.POWERED, on);
        level.setBlockAndUpdate(leverPos, lever);
    }

    private static void clearFootprint(ServerLevel level, int x0, int x1, int z0, int z1) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = groundY; y <= groundY + 10; y++) {
                    level.removeBlock(new BlockPos(x, y, z), false);
                }
            }
        }
    }

    // ---------------------------------------------------------------- evidence

    /**
     * The dual-state snapshot: lever position, gate trigger/latch/cutoff state (server truth through the integrated
     * server's level), the lamp, the three pipe buffers and the tester's received counters. Logged once per state so
     * the OFF/OFF-restore lines show the counters rising and the ON lines show them frozen.
     */
    private static void logState(ServerLevel level, String tag) {
        boolean leverOn = level.getBlockState(leverPos).getValue(BlockStateProperties.POWERED);
        boolean lampLit = level.getBlockState(lampPos).getValue(RedstoneLampBlock.LIT);
        String gateState = "none";
        long testerTotal = -1;
        long testerLastTick = -1;
        if (level.getBlockEntity(gatedPipePos) instanceof KinesisPipeBlockEntity pipe && pipe.getGate() != null) {
            BcGateLogic gate = pipe.getGate();
            gateState = "gateOn=" + gate.isOn() + " trigger0=" + gate.isTriggerOn(0) + " trigger1=" + gate.isTriggerOn(1)
                + " redstoneOut=" + gate.getRedstoneOutput() + " cutoff=" + gate.isPowerCutoff()
                + " wireRed=" + gate.getWireBroadcasts().contains(DyeColor.RED);
        }
        if (level.getBlockEntity(testerPos) instanceof PowerTesterBlockEntity tester) {
            testerTotal = tester.getTotalReceived();
            testerLastTick = tester.getLastTickReceived();
        }
        LOGGER.info("[M417] {}: lever={} lampLit={} {} | pipes A/B/C stored: {} / {} / {} uMJ | tester total={} lastTick={} uMJ",
            tag, leverOn ? "ON" : "OFF", lampLit, gateState,
            stored(level, anchor.getX() + 1), stored(level, anchor.getX() + 2), stored(level, anchor.getX() + 3),
            testerTotal, testerLastTick);
    }

    private static long stored(ServerLevel level, int x) {
        return level.getBlockEntity(new BlockPos(x, groundY, anchor.getZ())) instanceof KinesisPipeBlockEntity pipe
            ? pipe.getEnergyStored()
            : -1;
    }

    private static void setView(ServerPlayer player, double x, double y, double z, float yaw, float pitch) {
        viewYaw = yaw;
        viewPitch = pitch;
        player.teleportTo((ServerLevel) player.level(), x, y, z, Set.of(), viewYaw, viewPitch, false);
    }

    private static void screenshot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), 1,
            (Component component) -> LOGGER.info("[M417] screenshot {}: {}", name, component.getString()));
    }

    private M417GateProbe() {
    }
}
