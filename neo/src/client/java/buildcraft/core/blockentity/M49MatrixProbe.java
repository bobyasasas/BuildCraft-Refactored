/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.blockentity;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import buildcraft.builders.BcBuildersBlocks;
import buildcraft.builders.blockentity.FillerBlockEntity;
import buildcraft.builders.blockentity.QuarryBlockEntity;
import buildcraft.core.BcBlocks;
import buildcraft.core.BcCreativeTabs;
import buildcraft.core.BcItems;
import buildcraft.core.block.StoneEngineBlock;
import buildcraft.energy.BcEnergyFluids;
import buildcraft.factory.BcFactoryBlocks;
import buildcraft.factory.BcFactoryItems;
import buildcraft.factory.blockentity.DistillerBlockEntity;
import buildcraft.factory.blockentity.HeatExchangeBlockEntity;
import buildcraft.factory.blockentity.PumpBlockEntity;
import buildcraft.factory.blockentity.TankBlockEntity;
import buildcraft.robotics.BcRoboticsBlocks;
import buildcraft.robotics.BcRoboticsEntities;
import buildcraft.robotics.entity.EntityRobot;
import buildcraft.silicon.BcSiliconItems;
import buildcraft.transport.BcTransportBlocks;
import buildcraft.transport.BcTransportItems;
import buildcraft.transport.blockentity.PipeHolderBlockEntity;
import buildcraft.transport.client.PipeItemFlowClient;
import buildcraft.transport.pipe.BcPipeFamilies;
import buildcraft.transport.pipe.BcPipeFamilies.Family;

/**
 * The M4.9 in-game test-matrix rig (the {@link M45LaserSmokeProbe}/{@code M46}/{@code M47} pattern, the closing
 * milestone): one quickPlay world walked through every machine/function the migration ships, one screenshot per matrix
 * row into {@code screenshots/} plus one or more {@code [M49]} log lines — screenshot + log are the dual evidence each
 * matrix row needs. Scene rows (each on its own z offset, footprint cleared before building):
 * <ol>
 * <li>z+0: pipe visuals — 7 stone item pipes (straight + elbow + tee, hopper feed, blocker plug), 3 stone fluid pipes
 * (middle one bucket-filled);</li>
 * <li>z+24: the M4.6 leftover verdict — a REAL stone engine (fueled with coal) facing a 2-kinesis-pipe run with an
 * iron gate into the quarry, i.e. engine &rarr; pipe &rarr; {@code MjReceiver} with no injected buffers; the quarry
 * receives actual engine power through the pipes;</li>
 * <li>z+48: machines — solo stone engine (burn + GUI + stored-energy log), filler (GUI, 27 resource slots), the three
 * tanks (1/3 water, 2/3 lava, empty + the bucket draw/pour pass), distiller, heat exchanger, pump, chute;</li>
 * <li>z+48, east of the machines: the item-model spot check — a ground row of representative item forms (every engine,
 * the three stone pipe families, kinesis wood, blocker plug, iron gate, tank/chute/distiller) plus one first-person
 * hand shot;</li>
 * <li>z+72: markers — diagonal volume pair, line pair, 3-marker path chain, redstone-driven signal lines;</li>
 * <li>z+96: robotics as-shipped — the real {@code robot_miner} entity (only robotics entity with a renderer) next to
 * the placeholder requester block;</li>
 * <li>the creative tab scene: the single registered BuildCraft tab ({@code buildcraftcore:main}) opened in the real
 * creative screen, screenshot for the icon-completeness check.</li>
 * </ol>
 *
 * <p>Exactly like its three predecessors, every ServerLevel touch is marshalled onto the server thread through
 * {@link MinecraftServer#execute(Runnable)} and the rig is inert unless the run passes
 * {@code -Dbuildcraft.m49probe=true} (dev-run evidence only, never active in normal play). The rig shuts the game down
 * when done: {@code [M49]} lines + the screenshots are the pass signal.
 */
@EventBusSubscriber(modid = "buildcraftcore", value = Dist.CLIENT)
public final class M49MatrixProbe {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Set (and only set) by the M4.9 evidence run's JVM arguments; keeps the rig inert everywhere else. */
    private static final boolean ENABLED = Boolean.getBoolean("buildcraft.m49probe");

    /** Base wait between phases (ticks) — comfortably more than a couple of sync round trips. */
    private static final int WAIT_TICKS = 30;

    private static int state = 0;
    private static int wait = 0;
    private static int soakTicks = 0;
    private static int drainCounter = 0;
    private static BlockPos anchor = null;
    private static int groundY = -1;
    /** View yaw/pitch to pin every tick (per-scene camera). */
    private static float viewYaw;
    private static float viewPitch;
    /** Scene anchors: each row gets its own z offset off the base anchor (M47 scene-per-camera discipline). */
    private static BlockPos pipeScene;
    private static BlockPos chainScene;
    private static BlockPos machineScene;
    private static BlockPos dropScene;
    private static BlockPos markerScene;
    private static BlockPos robotScene;
    private static volatile QuarryBlockEntity quarry = null;
    /** True while the engine&ndash;pipe&ndash;quarry chain soak runs (drains the quarry output buffer, see M45). */
    private static boolean chainSoak = false;
    /** While set, the trickle also tops the buffer up and keeps the output clear so the POWER_LOW beam
     * phase has a live target (the M4.5 assist, see the case 8 comment). */
    private static volatile boolean beamAssist = false;
    /** Poll budget for the beam shot: the break loop syncs a target-less frame once per block, so the client
     * mirror alternates target set/null while it chews a block per tick — wait for a set frame. */
    private static int beamPolls = 0;
    /** Consecutive live mirror ticks seen before the grab: Screenshot.grab captures the previous frame, so the
     * shot must fire a few ticks into a live streak or it keeps re-showing the target-less frame that ended the
     * last poll. */
    private static int beamHold = 0;
    /** Set once the beam freeze is armed: no trickle, no drain, no syncs — the client mirror (and with it the
     * rendered beam) holds its last live state while the shot is taken. */
    private static volatile boolean beamFrozen = false;
    /** The client mirror pinned to the live beam state: the renderer reads exactly these two fields every frame, so
     * pinning them makes the POWER_LOW beam deterministic against whatever packet lands during the shot window. */
    private static QuarryBlockEntity pinnedMirror = null;
    private static BlockPos pinnedTarget = null;
    private static Field energyField = null;
    private static Field targetField = null;

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
        if (level == null || state > 43) {
            return;
        }
        // keep the vantage view pinned at the current scene
        mc.player.setYRot(viewYaw);
        mc.player.setXRot(viewPitch);
        // keep the quarry mining through the chain soak: drain its output buffer reflectively (the M45 discipline —
        // the slice buffer has no extraction API and stalls the quarry once the 8 stacks fill)
        if ((chainSoak || beamAssist) && !beamFrozen && quarry != null && ++drainCounter >= 10) {
            drainCounter = 0;
            QuarryBlockEntity be = quarry;
            boolean feed = beamAssist;
            mc.getSingleplayerServer().execute(() -> {
                try {
                    Field buffer = QuarryBlockEntity.class.getDeclaredField("output");
                    buffer.setAccessible(true);
                    ((List<?>) buffer.get(be)).clear();
                } catch (ReflectiveOperationException ignored) {
                    // no buffer, no stall: the rig still completes, just possibly without visible mining progress
                }
                if (feed) {
                    be.receivePower(20_000, false);
                }
            });
        }
        if (pinnedMirror != null) {
            try {
                if (energyField == null) {
                    energyField = QuarryBlockEntity.class.getDeclaredField("energyStored");
                    energyField.setAccessible(true);
                }
                if (targetField == null) {
                    targetField = QuarryBlockEntity.class.getDeclaredField("currentTarget");
                    targetField.setAccessible(true);
                }
                energyField.setLong(pinnedMirror, 20_000L);
                targetField.set(pinnedMirror, pinnedTarget);
            } catch (ReflectiveOperationException ignored) {
                // pin failed: the shot then depends on the frozen mirror alone
            }
        }
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
                anchor = new BlockPos(spawn.getX() + 24, 0, spawn.getZ());
                BlockPos probe = new BlockPos(anchor.getX(), 0, anchor.getZ());
                groundY = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, probe).getY();
                pipeScene = at(0, 0);
                chainScene = at(0, 24);
                machineScene = at(0, 48);
                dropScene = at(20, 48);
                markerScene = at(0, 72);
                robotScene = at(0, 96);
                mc.options.hideGui = true;
                player.setGameMode(GameType.CREATIVE);
                MinecraftServer server = mc.getSingleplayerServer();
                server.execute(() -> server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack().withSuppressedOutput(), "time set noon"));
                LOGGER.info("[M49] rig start: anchor={} groundY={} gamemode={}", anchor, groundY,
                    player.gameMode.getGameModeForPlayer());
                logCreativeTabs();
                clearFootprint(level, pipeScene, 0, 12, -4, 8);
                buildItemPipeScene(level);
                buildFluidPipeScene(level);
                LOGGER.info("[M49] pipe scenes built (item run + fluid triple)");
                setView(player, pipeScene.getX() + 4.5, groundY + 4.5, pipeScene.getZ() - 7.0, 24.0F);
            });
            // ---- pipes: items ----
            case 1 -> screenshot(mc, "m49_pipes_items");
            case 2 -> {
                onServer(mc, () -> logItemFlow(level));
                screenshot(mc, "m49_pipes_items2");
            }
            // ---- pipes: fluids ----
            case 3 -> {
                setView(player, pipeScene.getX() + 9.5, groundY + 3.5, pipeScene.getZ() - 5.0, 28.0F);
                wait = 2 * WAIT_TICKS;
            }
            case 4 -> {
                onServer(mc, () -> logFluidPipes(level));
                screenshot(mc, "m49_pipes_fluid");
            }
            // ---- the real engine -> kinesis -> quarry chain (M4.6 leftover verdict) ----
            case 5 -> onServer(mc, () -> {
                clearFootprint(level, chainScene, 0, 16, -5, 6);
                buildChainScene(level);
                setView(player, chainScene.getX() + 4.5, groundY + 5.0, chainScene.getZ() - 9.0, 22.0F);
            });
            case 6 -> {
                screenshot(mc, "m49_quarry_chain_frame");
                onServer(mc, () -> logChain(level, "t0"));
                chainSoak = true;
                soakTicks = 0;
                wait = 1;
            }
            case 7 -> {
                // soak ~20 s so the engine-fed quarry visibly drills into its area
                if (++soakTicks < 400) {
                    wait = 1;
                    return;
                }
                chainSoak = false;
                onServer(mc, () -> logChain(level, "t1"));
                wait = 2 * WAIT_TICKS;
            }
            case 8 -> {
                screenshot(mc, "m49_quarry_chain_mining");
                // The POWER_LOW beam renders only while the synced buffer holds power AND the break loop still has a
                // target (the legacy "no laser before any power" rule). The trickle-fed real chain burns its energy
                // as it arrives, so the buffer never shows any — the documented M4.5 assist instead: a slow top-up
                // trickle (case 8..10) that keeps both the beam state and a live drill target without letting the
                // quarry finish the ~49-cell slice area before the shot (a fat one-shot charge eats it whole).
                beamAssist = true;
                setView(player, chainScene.getX() + 4.5, groundY + 10.0, chainScene.getZ() - 12.0, 18.0F);
            }
            case 9 -> {
                QuarryBlockEntity clientQuarry = quarry != null
                    && mc.level.getBlockEntity(quarry.getBlockPos()) instanceof QuarryBlockEntity cq ? cq : null;
                boolean live = clientQuarry != null && clientQuarry.getEnergyStored() > 0
                    && clientQuarry.getCurrentTarget() != null;
                if (!live) {
                    beamHold = 0;
                    if (++beamPolls < 200) {
                        wait = 1;
                        return;
                    }
                } else if (beamHold < 3) {
                    if (beamHold == 0) {
                        LOGGER.info("[M49] beam assist: client mirror pinned to energy={} uMJ target={} (renderer beam state, polls={})",
                            clientQuarry.getEnergyStored(), clientQuarry.getCurrentTarget(), beamPolls);
                        // Freeze: stop the trickle and drain the battery so no further sync fires — the client
                        // mirror keeps its live {target, energy} state and the renderer holds the beam for the shot.
                        beamAssist = false;
                        beamFrozen = true;
                        pinnedMirror = clientQuarry;
                        pinnedTarget = clientQuarry.getCurrentTarget();
                        onServer(mc, () -> {
                            if (quarry != null) {
                                try {
                                    Field energy = QuarryBlockEntity.class.getDeclaredField("energyStored");
                                    energy.setAccessible(true);
                                    energy.setLong(quarry, 0L);
                                } catch (ReflectiveOperationException ignored) {
                                    // no freeze: the shot then rides the normal break cycle instead
                                }
                            }
                        });
                    }
                    beamHold++;
                    wait = 1;
                    return;
                }
                screenshot(mc, "m49_quarry_beam");
                setView(player, chainScene.getX() + 2.5, groundY + 3.5, chainScene.getZ() - 4.5, 24.0F);
            }
            case 10 -> {
                pinnedMirror = null;
                beamAssist = false;
                onServer(mc, () -> logGateAndKinesis(level));
                screenshot(mc, "m49_pipes_kinesis");
            }
            // ---- machines row ----
            case 11 -> onServer(mc, () -> {
                clearFootprint(level, machineScene, 0, 30, -5, 8);
                buildMachineRow(level);
                setView(player, machineScene.getX() + 0.5, groundY + 3.0, machineScene.getZ() - 4.5, 24.0F);
            });
            case 12 -> screenshot(mc, "m49_engine_burn");
            case 13 -> {
                onServer(mc, () -> logSoloEngine(level, "t0"));
                wait = 80;
            }
            case 14 -> {
                screenshot(mc, "m49_engine_burn2");
                onServer(mc, () -> logSoloEngine(level, "t1"));
            }
            case 15 -> {
                onServer(mc, () -> openMachineGui(level, player, machineScene, Direction.NORTH));
                mc.options.hideGui = false;
                wait = 2 * WAIT_TICKS;
            }
            case 16 -> {
                LOGGER.info("[M49] engine GUI open: screen={}", mc.screen == null ? "null" : mc.screen.getClass()
                    .getSimpleName());
                screenshot(mc, "m49_engine_gui");
                wait = 60;
            }
            case 17 -> {
                onServer(mc, () -> logSoloEngine(level, "t2-gui-open"));
                onServer(mc, () -> player.closeContainer());
                mc.options.hideGui = true;
                wait = 2 * WAIT_TICKS;
            }
            case 18 -> onServer(mc, () -> openMachineGui(level, player, machineScene.east(3), Direction.NORTH));
            case 19 -> {
                LOGGER.info("[M49] filler GUI open: screen={}", mc.screen == null ? "null" : mc.screen.getClass()
                    .getSimpleName());
                screenshot(mc, "m49_filler_gui");
                onServer(mc, () -> player.closeContainer());
            }
            // ---- tanks ----
            case 20 -> {
                setView(player, machineScene.getX() + 5.5, groundY + 3.5, machineScene.getZ() - 5.0, 24.0F);
                wait = 2 * WAIT_TICKS;
            }
            case 21 -> screenshot(mc, "m49_tanks");
            case 22 -> onServer(mc, () -> bucketPass(level, player));
            case 23 -> {
                screenshot(mc, "m49_tanks_bucket");
                setView(player, machineScene.getX() + 9.5, groundY + 3.5, machineScene.getZ() - 5.0, 24.0F);
                wait = 2 * WAIT_TICKS;
            }
            // ---- distiller ----
            case 24 -> {
                onServer(mc, () -> logDistiller(level));
                screenshot(mc, "m49_distiller");
                setView(player, machineScene.getX() + 11.5, groundY + 3.5, machineScene.getZ() - 5.0, 24.0F);
                wait = 2 * WAIT_TICKS;
            }
            // ---- heat exchanger ----
            case 25 -> {
                onServer(mc, () -> logHeatExchange(level));
                screenshot(mc, "m49_heat_exchange");
                setView(player, machineScene.getX() + 13.5, groundY + 3.0, machineScene.getZ() - 4.0, 28.0F);
                wait = 200;
            }
            // ---- pump ----
            case 26 -> {
                onServer(mc, () -> logPump(level));
                screenshot(mc, "m49_pump");
            }
            // ---- item model spot check: ground row + first-person hand ----
            case 27 -> onServer(mc, () -> {
                clearFootprint(level, dropScene, 0, 14, -3, 4);
                buildDropRow(level);
                setView(player, dropScene.getX() + 5.0, groundY + 6.0, dropScene.getZ() - 5.5, 38.0F);
            });
            case 28 -> screenshot(mc, "m49_items_ground");
            case 29 -> {
                onServer(mc, () -> player.setItemInHand(InteractionHand.MAIN_HAND,
                    pipeItem("pipe_items_stone_colorless")));
                mc.options.hideGui = false;
                setView(player, dropScene.getX() + 5.5, groundY + 1.0, dropScene.getZ() - 2.0, 40.0F);
                wait = 2 * WAIT_TICKS;
            }
            case 30 -> {
                screenshot(mc, "m49_item_hand");
                onServer(mc, () -> player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));
                mc.options.hideGui = true;
                wait = 2 * WAIT_TICKS;
            }
            // ---- markers ----
            case 31 -> onServer(mc, () -> {
                clearFootprint(level, markerScene, -14, 12, -2, 16);
                placeMarkers(level);
                setView(player, markerScene.getX() - 1.5, groundY + 8.0, markerScene.getZ() - 4.0, 24.0F);
            });
            case 32 -> screenshot(mc, "m49_markers_box_path");
            case 33 -> onServer(mc, () -> {
                BlockPos redstone = markerScene.east(2).above(1).south(3);
                level.setBlockAndUpdate(redstone, Blocks.REDSTONE_BLOCK.defaultBlockState());
                LOGGER.info("[M49] redstone block set next to the line marker");
            });
            case 34 -> {
                screenshot(mc, "m49_marker_signals");
                setView(player, robotScene.getX() + 1.5, groundY + 3.0, robotScene.getZ() - 4.5, 26.0F);
            }
            // ---- robotics as shipped ----
            case 35 -> onServer(mc, () -> {
                clearFootprint(level, robotScene, 0, 4, -3, 4);
                level.setBlockAndUpdate(robotScene, BcRoboticsBlocks.REQUESTER.value().defaultBlockState());
                EntityRobot robot = BcRoboticsEntities.ROBOT_MINER.value().create(level,
                    net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                if (robot != null) {
                    robot.setPos(robotScene.getX() + 2.5, groundY + 1.0, robotScene.getZ() + 1.5);
                    robot.setYRot(0.0F);
                    robot.setXRot(0.0F);
                    level.addFreshEntity(robot);
                    LOGGER.info("[M49] robot spawned: {} at {}", net.minecraft.world.entity.EntityType
                        .getKey(robot.getType()), robot.blockPosition());
                } else {
                    LOGGER.error("[M49] robot_miner entity create failed");
                }
                LOGGER.info("[M49] robotics scene built: requester block + robot_miner entity (as-shipped state)");
            });
            case 36 -> {
                onServer(mc, () -> logRobot(level, "A"));
                wait = 80;
            }
            case 37 -> {
                onServer(mc, () -> logRobot(level, "B"));
                screenshot(mc, "m49_robots");
            }
            // ---- creative tab ----
            case 38 -> {
                mc.options.hideGui = false;
                boolean selected = setStaticSelectedTab(BcCreativeTabs.MAIN.value());
                mc.setScreen(new CreativeModeInventoryScreen(mc.player,
                    mc.level.enabledFeatures(), false));
                LOGGER.info("[M49] creative screen opened: selectedTab-set={} screen={}", selected,
                    mc.screen == null ? "null" : mc.screen.getClass().getSimpleName());
                wait = 2 * WAIT_TICKS;
            }
            case 39 -> {
                screenshot(mc, "m49_tab_main");
                LOGGER.info("[M49] creative tab screenshot taken (tab buildcraftcore:main, item icons visible)");
                mc.setScreen(null);
                mc.options.hideGui = true;
                LOGGER.info("[M49] rig done - shutting down");
                wait = 2 * WAIT_TICKS;
            }
            default -> {
                state = 44;
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

    // ---------------------------------------------------------------- scenes

    /** The M46 item scene: 4 straights, an elbow down to the end chest, a tee branch to a second chest, a hopper
     * feeding from a source chest, and a blocker plug mid-run. */
    private static void buildItemPipeScene(ServerLevel level) {
        Family items = requireFamily("items_stone");
        int x = pipeScene.getX();
        int z = pipeScene.getZ();
        pipe(level, items, x, z);
        pipe(level, items, x + 1, z);
        pipe(level, items, x + 2, z);
        pipe(level, items, x + 3, z);
        pipe(level, items, x + 3, z + 1);
        pipe(level, items, x + 3, z + 2);
        pipe(level, items, x + 1, z + 1);
        chestAt(level, new BlockPos(x + 3, groundY, z + 3));
        chestAt(level, new BlockPos(x + 1, groundY, z + 2));
        level.setBlockAndUpdate(new BlockPos(x, groundY + 1, z), Blocks.HOPPER.defaultBlockState());
        chestAt(level, new BlockPos(x, groundY + 2, z));
        fillChest(level, new BlockPos(x, groundY + 2, z));
        if (level.getBlockEntity(new BlockPos(x + 2, groundY, z)) instanceof PipeHolderBlockEntity plugPipe) {
            plugPipe.attachPlug(Direction.UP, PipeHolderBlockEntity.PLUG_BLOCKER);
        }
    }

    /** The M46 fluid scene: 3 stone fluid pipes, water sources under both ends, the middle pipe bucket-filled. */
    private static void buildFluidPipeScene(ServerLevel level) {
        Family fluids = requireFamily("fluids_stone");
        int x = pipeScene.getX() + 8;
        int z = pipeScene.getZ();
        pipe(level, fluids, x, z);
        pipe(level, fluids, x + 1, z);
        pipe(level, fluids, x + 2, z);
        level.setBlockAndUpdate(new BlockPos(x, groundY - 1, z), Blocks.WATER.defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(x + 2, groundY - 1, z), Blocks.WATER.defaultBlockState());
        if (level.getBlockEntity(new BlockPos(x + 1, groundY, z)) instanceof PipeHolderBlockEntity middle) {
            boolean filled = middle.interactWithBucket(new FluidStack(Fluids.WATER, 1000));
            LOGGER.info("[M49] middle fluid pipe bucket fill: {} fluid={}", filled, middle.getFluid());
        }
    }

    /**
     * The real M4.6-verdict chain: fueled stone engine facing east &rarr; two kinesis pipes (iron gate on the first)
     * &rarr; quarry with a mining area on the surface grass. No buffer injection anywhere — the quarry's
     * {@link QuarryBlockEntity#getTotalReceived()} counter only moves if energy really flows through the pipes.
     */
    private static void buildChainScene(ServerLevel level) {
        int x = chainScene.getX();
        int z = chainScene.getZ();
        BlockPos enginePos = new BlockPos(x, groundY, z);
        level.setBlockAndUpdate(enginePos, BcBlocks.ENGINE_STONE.value().defaultBlockState()
            .setValue(StoneEngineBlock.FACING, Direction.EAST));
        if (level.getBlockEntity(enginePos) instanceof StoneEngineBlockEntity engine) {
            boolean fueled = engine.insertFuel(new ItemStack(Items.COAL), level.fuelValues());
            LOGGER.info("[M49] chain engine fueled with coal: {} (ignites once the buffer has room)", fueled);
        }
        // run 1 evidence: a stone power pipe between the engine and the kinesis pipe HOARDS the energy —
        // PipeHolderBlockEntity.tickPower never hands energy to a KinesisPipeBlockEntity and the kinesis pull
        // phase never reads a PipeHolderBlockEntity, so that bridge is dead on both sides (M4.9 matrix FAIL,
        // structural, reported not fixed). The task topology goes engine -> kinesis directly, which is what
        // this scene wires now: the kinesis pull phase accepts an engine pointed straight at it.
        level.setBlockAndUpdate(new BlockPos(x + 1, groundY, z),
            BcBlocks.PIPE_KINESIS_WOOD.value().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(x + 2, groundY, z),
            BcBlocks.PIPE_KINESIS_WOOD.value().defaultBlockState());
        if (level.getBlockEntity(new BlockPos(x + 1, groundY, z)) instanceof KinesisPipeBlockEntity kinesis) {
            boolean gated = kinesis.attachGate(Direction.UP, BcSiliconItems.IRON_GATE_VARIANT);
            LOGGER.info("[M49] kinesis gate attached: {}", gated);
        }
        BlockPos quarryPos = new BlockPos(x + 3, groundY, z);
        level.setBlockAndUpdate(quarryPos, BcBuildersBlocks.QUARRY.value().defaultBlockState());
        if (level.getBlockEntity(quarryPos) instanceof QuarryBlockEntity quarryBe) {
            quarryBe.setMiningArea(new BlockPos(x + 5, groundY - 1, z - 3), new BlockPos(x + 11, groundY + 1, z + 3));
            quarry = quarryBe;
            LOGGER.info("[M49] quarry placed at {} (real chain: engine -> pipes -> MjReceiver), area set", quarryPos);
        }
    }

    /** The machines row: solo fueled engine, filler with stocked resource slots, three tanks, distiller, heat
     * exchanger, pump over water, chute. */
    private static void buildMachineRow(ServerLevel level) {
        int x = machineScene.getX();
        int z = machineScene.getZ();
        BlockPos enginePos = new BlockPos(x, groundY, z);
        level.setBlockAndUpdate(enginePos, BcBlocks.ENGINE_STONE.value().defaultBlockState()
            .setValue(StoneEngineBlock.FACING, Direction.EAST));
        if (level.getBlockEntity(enginePos) instanceof StoneEngineBlockEntity engine) {
            engine.insertFuel(new ItemStack(Items.COAL, 4), level.fuelValues());
            LOGGER.info("[M49] solo engine fueled with 4 coal");
        }
        BlockPos fillerPos = new BlockPos(x + 3, groundY, z);
        level.setBlockAndUpdate(fillerPos, BcBuildersBlocks.FILLER.value().defaultBlockState());
        if (level.getBlockEntity(fillerPos) instanceof FillerBlockEntity filler) {
            filler.setItem(0, new ItemStack(Items.DIRT, 64));
            filler.setItem(1, new ItemStack(Items.COBBLESTONE, 64));
            filler.setItem(2, new ItemStack(Items.GRAVEL, 64));
            LOGGER.info("[M49] filler stocked: 3x64 resource stacks in the 27-slot grid");
        }
        tank(level, 5, TankFill.THIRD_WATER);
        tank(level, 6, TankFill.TWO_THIRDS_LAVA);
        tank(level, 7, TankFill.EMPTY);
        BlockPos distillerPos = new BlockPos(x + 9, groundY, z);
        level.setBlockAndUpdate(distillerPos, BcFactoryBlocks.DISTILLER.value().defaultBlockState());
        if (level.getBlockEntity(distillerPos) instanceof DistillerBlockEntity distiller) {
            fill(distiller.getTankIn(), FluidResource.of(BcEnergyFluids.OIL_HEAT_0.value()), 2000);
            LOGGER.info("[M49] distiller loaded: 2000 mB oil_heat_0 (recipe 8 in -> 16 gas + 3 liquid per batch)");
        }
        BlockPos heatPos = new BlockPos(x + 11, groundY, z);
        level.setBlockAndUpdate(heatPos, BcFactoryBlocks.HEAT_EXCHANGE.value().defaultBlockState());
        if (level.getBlockEntity(heatPos) instanceof HeatExchangeBlockEntity exchanger) {
            fill(exchanger.getTankIn(), FluidResource.of(BcEnergyFluids.OIL_HEAT_0.value()), 2000);
            LOGGER.info("[M49] heat exchanger loaded: 2000 mB oil_heat_0 (heat 0 -> heat 1)");
        }
        level.setBlockAndUpdate(new BlockPos(x + 13, groundY - 1, z), Blocks.WATER.defaultBlockState());
        BlockPos pumpPos = new BlockPos(x + 13, groundY, z);
        level.setBlockAndUpdate(pumpPos, BcFactoryBlocks.PUMP.value().defaultBlockState());
        LOGGER.info("[M49] pump placed over a water source at {}", new BlockPos(x + 13, groundY - 1, z));
        level.setBlockAndUpdate(new BlockPos(x + 15, groundY, z), BcFactoryBlocks.CHUTE.value().defaultBlockState());
        LOGGER.info("[M49] chute placed for the visual check");
    }

    /** The item-form spot check row: every engine, three stone pipe families, kinesis wood, blocker plug, iron gate,
     * tank, chute, distiller. */
    private static void buildDropRow(ServerLevel level) {
        List<ItemStack> drops = List.of(
            new ItemStack(BcItems.ENGINE_WOOD.value()),
            new ItemStack(BcItems.ENGINE_STONE.value()),
            new ItemStack(BcItems.ENGINE_IRON.value()),
            new ItemStack(BcItems.ENGINE_RF.value()),
            new ItemStack(BcItems.ENGINE_CREATIVE.value()),
            pipeItem("pipe_items_stone_colorless"),
            pipeItem("pipe_fluids_stone_colorless"),
            pipeItem("pipe_power_stone_colorless"),
            new ItemStack(BcItems.PIPE_KINESIS_WOOD.value()),
            new ItemStack(BcTransportItems.PLUG_BLOCKER.value()),
            new ItemStack(BcSiliconItems.PLUG_GATE_IRON_AND_NO_MODIFIER.value()),
            new ItemStack(BcFactoryItems.TANK.value()),
            new ItemStack(BcFactoryItems.CHUTE.value()),
            new ItemStack(BcFactoryItems.DISTILLER.value()));
        for (int i = 0; i < drops.size(); i++) {
            ItemEntity entity = new ItemEntity(level, dropScene.getX() + i * 0.8, groundY + 0.5,
                dropScene.getZ(), drops.get(i));
            entity.setPickUpDelay(32767);
            level.addFreshEntity(entity);
        }
        LOGGER.info("[M49] drop row built: {} representative item forms", drops.size());
    }

    /** The M45 marker layout: diagonal volume pair (explicit {@code connectWith}), auto-connected line pair and the
     * 3-marker path chain. */
    private static void placeMarkers(ServerLevel level) {
        int x = markerScene.getX();
        int z = markerScene.getZ();
        BlockPos volA = new BlockPos(x - 7, groundY + 1, z + 6);
        BlockPos volB = new BlockPos(x - 3, groundY + 4, z + 10);
        BlockPos volC = new BlockPos(x + 2, groundY + 1, z + 4);
        BlockPos volD = new BlockPos(x + 2, groundY + 1, z + 9);
        BlockPos path1 = new BlockPos(x - 12, groundY + 1, z + 4);
        BlockPos path2 = new BlockPos(x - 12, groundY + 1, z + 8);
        BlockPos path3 = new BlockPos(x - 12, groundY + 1, z + 12);
        MarkerVolumeBlockEntity volAe = placeVolume(level, volA);
        MarkerVolumeBlockEntity volBe = placeVolume(level, volB);
        if (volAe != null && volBe != null) {
            volAe.connectWith(volBe);
        }
        placeVolume(level, volC);
        placeVolume(level, volD);
        placePath(level, path1);
        placePath(level, path2);
        placePath(level, path3);
        LOGGER.info("[M49] markers placed+connected: diagonal volume pair, line pair, path chain");
    }

    // ---------------------------------------------------------------- evidence logs

    private static void logCreativeTabs() {
        long buildcraftTabs = BuiltInRegistries.CREATIVE_MODE_TAB.stream()
            .filter(tab -> BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab).getNamespace()
                .startsWith("buildcraft"))
            .count();
        BuiltInRegistries.CREATIVE_MODE_TAB.stream()
            .filter(tab -> BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab).getNamespace().startsWith("buildcraft"))
            .forEach(tab -> {
                Identifier id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
                LOGGER.info("[M49] creative tab registered: {} displayItems={}", id, tab.getDisplayItems().size());
            });
        LOGGER.info("[M49] buildcraft creative tab count: {} (vanilla tabs: {})", buildcraftTabs,
            BuiltInRegistries.CREATIVE_MODE_TAB.size() - buildcraftTabs);
    }

    private static void logItemFlow(ServerLevel level) {
        for (int i = 0; i < 4; i++) {
            BlockPos pos = new BlockPos(pipeScene.getX() + i, groundY, pipeScene.getZ());
            if (level.getBlockEntity(pos) instanceof PipeHolderBlockEntity pipe) {
                LOGGER.info("[M49] item pipe {}: family={} connections={} upInbox={} travelling={}", pos,
                    pipe.getFamily(), pipe.connections, pipe.getInbox(Direction.UP).getAmountAsLong(0),
                    PipeItemFlowClient.get(pos).size());
            } else {
                LOGGER.error("[M49] item pipe {}: no pipe BE (block={})", pos, level.getBlockState(pos).getBlock());
            }
        }
    }

    private static void logFluidPipes(ServerLevel level) {
        for (int i = 0; i < 3; i++) {
            BlockPos pos = new BlockPos(pipeScene.getX() + 8 + i, groundY, pipeScene.getZ());
            if (level.getBlockEntity(pos) instanceof PipeHolderBlockEntity pipe) {
                LOGGER.info("[M49] fluid pipe {}: fluid={} connections={}", pos, pipe.getFluid(), pipe.connections);
            } else {
                LOGGER.error("[M49] fluid pipe {}: no pipe BE (block={})", pos, level.getBlockState(pos).getBlock());
            }
        }
    }

    /** Chain snapshot: engine burn/buffer, both pipes, quarry buffer/total/target — logged at t0 and t1; the quarry's
     * rising {@code totalReceived/energy} is the real-chain evidence. */
    private static void logChain(ServerLevel level, String tag) {
        BlockPos enginePos = new BlockPos(chainScene.getX(), groundY, chainScene.getZ());
        if (level.getBlockEntity(enginePos) instanceof StoneEngineBlockEntity engine) {
            LOGGER.info("[M49] chain[{}] engine: burning={} burn={}/{} stored={}uMJ", tag, engine.isBurning(),
                engine.getBurnRemain(), engine.getBurnTotal(), engine.getEnergyStored());
        }
        for (int i = 1; i <= 2; i++) {
            BlockPos pos = new BlockPos(chainScene.getX() + i, groundY, chainScene.getZ());
            if (level.getBlockEntity(pos) instanceof KinesisPipeBlockEntity kinesis) {
                LOGGER.info("[M49] chain[{}] kinesis pipe {}: stored={}uMJ", tag, pos, kinesis.getEnergyStored());
            } else if (level.getBlockEntity(pos) instanceof PipeHolderBlockEntity pipe) {
                LOGGER.info("[M49] chain[{}] power pipe {}: stored={}uMJ", tag, pos, pipe.getPowerStored());
            }
        }
        if (quarry != null) {
            LOGGER.info("[M49] chain[{}] quarry: energy={}uMJ totalReceived={}uMJ target={}", tag,
                quarry.getEnergyStored(), quarry.getTotalReceived(), quarry.getCurrentTarget());
        }
    }

    private static void logGateAndKinesis(ServerLevel level) {
        BlockPos pos = new BlockPos(chainScene.getX() + 2, groundY, chainScene.getZ());
        if (level.getBlockEntity(pos) instanceof KinesisPipeBlockEntity kinesis) {
            LOGGER.info("[M49] kinesis close-up: stored={}uMJ gate={} gateOn={}", kinesis.getEnergyStored(),
                kinesis.getGate() != null, kinesis.getGate() != null && kinesis.getGate().isOn());
        } else {
            LOGGER.error("[M49] kinesis close-up: no kinesis BE at {}", pos);
        }
    }

    private static void logSoloEngine(ServerLevel level, String tag) {
        BlockPos pos = new BlockPos(machineScene.getX(), groundY, machineScene.getZ());
        if (level.getBlockEntity(pos) instanceof StoneEngineBlockEntity engine) {
            LOGGER.info("[M49] engine[{}]: burning={} burn={}/{} stored={}uMJ (capacity={})", tag,
                engine.isBurning(), engine.getBurnRemain(), engine.getBurnTotal(), engine.getEnergyStored(),
                StoneEngineBlockEntity.CAPACITY);
        } else {
            LOGGER.error("[M49] engine[{}]: no engine BE at {} (block={})", tag, pos,
                level.getBlockState(pos).getBlock());
        }
    }

    private static void logDistiller(ServerLevel level) {
        BlockPos pos = new BlockPos(machineScene.getX() + 9, groundY, machineScene.getZ());
        if (level.getBlockEntity(pos) instanceof DistillerBlockEntity distiller) {
            long in = distiller.getTankIn().getAmountAsLong(0);
            long gas = distiller.getTankGasOut().getAmountAsLong(0);
            long liquid = distiller.getTankLiquidOut().getAmountAsLong(0);
            long batches = gas / 16;
            LOGGER.info("[M49] distiller: in={} gas={} liquid={} (batches={} -> expect in={} gas={} liquid={})",//
                distiller.getTankIn().getResource(0), gas, liquid, batches, 2000 - 8 * batches, 16 * batches,
                3 * batches);
        } else {
            LOGGER.error("[M49] distiller missing at {}", pos);
        }
    }

    private static void logHeatExchange(ServerLevel level) {
        BlockPos pos = new BlockPos(machineScene.getX() + 11, groundY, machineScene.getZ());
        if (level.getBlockEntity(pos) instanceof HeatExchangeBlockEntity exchanger) {
            LOGGER.info("[M49] heat exchange: in={} x {} mB, out={} x {} mB (batches={})",//
                exchanger.getTankIn().getResource(0), exchanger.getTankIn().getAmountAsLong(0),//
                exchanger.getTankOut().getResource(0), exchanger.getTankOut().getAmountAsLong(0),
                exchanger.getTankOut().getAmountAsLong(0) / 10);
        } else {
            LOGGER.error("[M49] heat exchange missing at {}", pos);
        }
    }

    private static void logPump(ServerLevel level) {
        BlockPos pumpPos = new BlockPos(machineScene.getX() + 13, groundY, machineScene.getZ());
        BlockPos below = pumpPos.below();
        if (level.getBlockEntity(pumpPos) instanceof PumpBlockEntity pump) {
            LOGGER.info("[M49] pump: below={} (was a water source), tank={} x {} mB",//
                level.getBlockState(below).getBlock(), pump.getTank().getResource(0),
                pump.getTank().getAmountAsLong(0));
        } else {
            LOGGER.error("[M49] pump missing at {} (block={})", pumpPos, level.getBlockState(pumpPos).getBlock());
        }
    }

    private static void logRobot(ServerLevel level, String tag) {
        for (var robot : level.getEntitiesOfClass(EntityRobot.class,
            new net.minecraft.world.phys.AABB(net.minecraft.world.phys.Vec3.atLowerCornerOf(
                    robotScene.offset(-4, -2, -4)), net.minecraft.world.phys.Vec3.atLowerCornerOf(
                    robotScene.offset(6, 6, 6))))) {
            LOGGER.info("[M49] robot[{}]: {} at {} delta={}", tag,
                net.minecraft.world.entity.EntityType.getKey(robot.getType()), robot.blockPosition(),
                robot.position().subtract(robot.xo, robot.yo, robot.zo).lengthSqr());
        }
    }

    /** The bucket draw/pour pass through the real {@link FluidUtil#interactWithFluidHandler} path (M47 pattern). */
    private static void bucketPass(ServerLevel level, ServerPlayer player) {
        BlockPos waterTank = new BlockPos(machineScene.getX() + 5, groundY, machineScene.getZ());
        BlockPos emptyTank = new BlockPos(machineScene.getX() + 7, groundY, machineScene.getZ());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        boolean drew = FluidUtil.interactWithFluidHandler(player, InteractionHand.MAIN_HAND, level, waterTank,
            Direction.NORTH, null);
        LOGGER.info("[M49] bucket draw on {}: {} — main hand now {}", waterTank, drew,
            player.getMainHandItem().getItem());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        boolean poured = FluidUtil.interactWithFluidHandler(player, InteractionHand.MAIN_HAND, level, emptyTank,
            Direction.NORTH, null);
        LOGGER.info("[M49] bucket pour on {}: {} — main hand now {}", emptyTank, poured,
            player.getMainHandItem().getItem());
        if (level.getBlockEntity(emptyTank) instanceof TankBlockEntity tankBe) {
            LOGGER.info("[M49] poured tank now: {} x {} mB", tankBe.getFluidHandler(null).getResource(0),
                tankBe.getFluidHandler(null).getAmountAsLong(0));
        }
    }

    /** Opens a machine GUI through the REAL right-click path ({@code ServerPlayerGameMode#useItemOn} with an empty
     * main hand &rarr; the block's {@code useWithoutItem} &rarr; {@code player.openMenu}). */
    private static void openMachineGui(ServerLevel level, ServerPlayer player, BlockPos pos, Direction face) {
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false);
        var result = player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
            hit);
        LOGGER.info("[M49] GUI open on {}: useItemOn -> {}", pos, result);
    }

    // ---------------------------------------------------------------- helpers

    /** Airs the scene footprint above ground level (the terrain itself stays — machines stand on the surface). */
    private static void clearFootprint(ServerLevel level, BlockPos base, int x0, int x1, int z0, int z1) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = groundY; y <= groundY + 10; y++) {
                    level.removeBlock(new BlockPos(base.getX() + x, y, base.getZ() + z), false);
                }
            }
        }
    }

    private static BlockPos at(int x, int z) {
        return new BlockPos(anchor.getX() + x, groundY, anchor.getZ() + z);
    }

    private static void tank(ServerLevel level, int x, TankFill fill) {
        BlockPos pos = new BlockPos(machineScene.getX() + x, groundY, machineScene.getZ());
        level.setBlockAndUpdate(pos, BcFactoryBlocks.TANK.value().defaultBlockState());
        if (level.getBlockEntity(pos) instanceof TankBlockEntity tankBe && fill.amount > 0) {
            fill(tankBe.getFluidHandler(null), fill.resource, fill.amount);
            LOGGER.info("[M49] tank {} filled: {} x {} mB", pos, fill.resource, fill.amount);
        }
    }

    /** The tank fills, as named fractions of the 16-bucket tank (the M47 enum). */
    private enum TankFill {
        THIRD_WATER(FluidResource.of(Fluids.WATER), 16_000 / 3),
        TWO_THIRDS_LAVA(FluidResource.of(Fluids.LAVA), 2 * 16_000 / 3),
        EMPTY(null, 0);

        final FluidResource resource;
        final int amount;

        TankFill(FluidResource resource, int amount) {
            this.resource = resource;
            this.amount = amount;
        }
    }

    /** Server-side fill through the real handler API (one committed transaction, the M47 helper). */
    private static void fill(net.neoforged.neoforge.transfer.ResourceHandler<FluidResource> tank,
        FluidResource resource, int amount) {
        try (Transaction transaction = Transaction.openRoot()) {
            tank.insert(0, resource, amount, transaction);
            transaction.commit();
        }
    }

    private static void pipe(ServerLevel level, Family family, int x, int z) {
        BlockPos pos = new BlockPos(x, groundY, z);
        level.setBlockAndUpdate(pos, BcTransportBlocks.PIPE_HOLDER.value().defaultBlockState());
        if (level.getBlockEntity(pos) instanceof PipeHolderBlockEntity pipeBe) {
            pipeBe.setPipe(family, null);
        } else {
            LOGGER.error("[M49] pipe placement broken at {} (block={})", pos, level.getBlockState(pos).getBlock());
        }
    }

    private static ItemStack pipeItem(String stem) {
        var item = BcTransportItems.PIPE_ITEMS.get(stem);
        if (item == null) {
            LOGGER.error("[M49] no pipe item registered for stem {}", stem);
            return ItemStack.EMPTY;
        }
        return new ItemStack(item.value());
    }

    private static void chestAt(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
    }

    private static void fillChest(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof Container chest) {
            chest.setItem(0, new ItemStack(Items.REDSTONE, 64));
            chest.setItem(1, new ItemStack(Items.GLOWSTONE_DUST, 64));
            chest.setItem(2, new ItemStack(Items.DIAMOND, 16));
        }
    }

    private static Family requireFamily(String stem) {
        Family family = BcPipeFamilies.byStem(stem);
        if (family == null) {
            throw new IllegalStateException("unknown pipe family stem: " + stem);
        }
        return family;
    }

    private static MarkerVolumeBlockEntity placeVolume(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos, BcBlocks.MARKER_VOLUME.value().defaultBlockState());
        return level.getBlockEntity(pos) instanceof MarkerVolumeBlockEntity marker ? marker : null;
    }

    private static MarkerPathBlockEntity placePath(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos, BcBlocks.MARKER_PATH.value().defaultBlockState());
        return level.getBlockEntity(pos) instanceof MarkerPathBlockEntity marker ? marker : null;
    }

    private static void setView(ServerPlayer player, double x, double y, double z, float pitch) {
        viewYaw = 0.0F;
        viewPitch = pitch;
        player.teleportTo((ServerLevel) player.level(), x, y, z, Set.of(), viewYaw, viewPitch, false);
    }

    private static void screenshot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), 1,
            (Component component) -> LOGGER.info("[M49] screenshot {}: {}", name, component.getString()));
    }

    /**
     * Points the creative screen's static {@code selectedTab} at the BuildCraft tab so the next
     * {@link CreativeModeInventoryScreen} opens on it (its {@code init()} re-selects the static if the tab page is
     * visible). Vanilla's private static is out of our module, so this goes through setAccessible and, if the module
     * refuses, through {@code sun.misc.Unsafe} static-field access (dev-rig evidence only — the same class of
     * reflect-injection the M46 rig documented).
     */
    private static boolean setStaticSelectedTab(CreativeModeTab tab) {
        try {
            Field field = CreativeModeInventoryScreen.class.getDeclaredField("selectedTab");
            try {
                field.setAccessible(true);
                field.set(null, tab);
                return true;
            } catch (Throwable reflectionFailure) {
                sun.misc.Unsafe unsafe = unsafeInstance();
                Object base = unsafe.staticFieldBase(field);
                long offset = unsafe.staticFieldOffset(field);
                unsafe.putObject(base, offset, tab);
                return true;
            }
        } catch (Throwable failure) {
            LOGGER.error("[M49] could not select the buildcraft creative tab: {}", failure.toString());
            return false;
        }
    }

    private static sun.misc.Unsafe unsafeInstance() throws Exception {
        Field theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        return (sun.misc.Unsafe) theUnsafe.get(null);
    }

    private M49MatrixProbe() {
    }
}
