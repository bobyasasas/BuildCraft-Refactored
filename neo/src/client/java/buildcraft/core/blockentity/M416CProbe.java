/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.blockentity;

import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import buildcraft.builders.BcBuildersBlocks;
import buildcraft.core.BcBlocks;
import buildcraft.core.block.StoneEngineBlock;
import buildcraft.robotics.BcRoboticsBlocks;
import buildcraft.transport.BcTransportBlocks;
import buildcraft.transport.blockentity.FilteredBufferBlockEntity;

/**
 * The M4.16c in-game evidence rig (the {@link M49MatrixProbe} pattern, scoped down): ten machines of the
 * builders/core/transport/robotics placeholder batch, one screenshot per machine into {@code screenshots/} plus
 * {@code [M416]} log lines &mdash; the dual evidence each matrix row needs. Three of the ten carry their minimal real
 * behaviour (this task's C route), the other seven are the blueprint-system/robotics-dependent machines that ship
 * as-shipped v2 placeholders and only need place + render + right-click-no-crash evidence:
 * <ol>
 * <li>the real power tester chain: fueled stone engine &rarr; kinesis pipe &rarr; {@link PowerTesterBlockEntity}
 * ({@link MjReceiver}); the rising {@code received total} counter across a t0/t1 soak is the log evidence;</li>
 * <li>the real oil spring: {@link SpringOilBlockEntity} periodically placing oil source blocks above itself;</li>
 * <li>the real filtered buffer: hopper &rarr; {@link FilteredBufferBlockEntity} FIFO &rarr; hopper &rarr; chest, i.e.
 * items genuinely passing through the buffer (insert + release log lines);</li>
 * <li>builders: builder, architect, library, marker_construction, replacer;</li>
 * <li>robotics: requester, zone_planner.</li>
 * </ol>
 *
 * <p>Exactly like its predecessors, every ServerLevel touch is marshalled onto the server thread through
 * {@code MinecraftServer#execute} and the rig is inert unless the run passes {@code -Dbuildcraft.m416cprobe=true}
 * (dev-run evidence only, never active in normal play). The rig shuts the game down when done: the {@code [M416]}
 * lines + the 10 screenshots are the pass signal.
 */
@EventBusSubscriber(modid = "buildcraftcore", value = Dist.CLIENT)
public final class M416CProbe {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Set (and only set) by the M4.16c evidence run's JVM arguments; keeps the rig inert everywhere else. */
    private static final boolean ENABLED = Boolean.getBoolean("buildcraft.m416cprobe");

    /** Base wait between phases (ticks) — comfortably more than a couple of sync round trips. */
    private static final int WAIT_TICKS = 30;

    private static int state = 0;
    private static int wait = 0;
    private static int groundY = -1;
    private static BlockPos anchor = null;
    /** View yaw/pitch to pin every tick (per-scene camera). */
    private static float viewYaw;
    private static float viewPitch;
    /** Scene anchors: each machine family gets its own z offset off the base anchor (M49 scene discipline). */
    private static BlockPos coreScene;
    private static BlockPos transportScene;
    private static BlockPos buildersScene;
    private static BlockPos roboticsScene;

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
        if (level == null || state > 17) {
            return;
        }
        // keep the vantage view pinned at the current scene
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
            // ---- core: the real power tester chain + the real oil spring ----
            case 0 -> onServer(mc, () -> {
                BlockPos spawn = player.blockPosition();
                // +200 x off spawn: this evidence world is the M4.9 rig save copy, whose scenes sit at spawn+24 —
                // the offset keeps the two rigs' footprints disjoint
                anchor = new BlockPos(spawn.getX() + 200, 0, spawn.getZ());
                // void-aware ground level: the +200 area has no terrain at all (heightmap sags to min build height),
                // so the machines ride one block above it and each scene lays its own floor slab at min height
                BlockPos probe = new BlockPos(anchor.getX(), 0, anchor.getZ());
                int terrain = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, probe).getY();
                groundY = terrain <= level.getMinY() ? level.getMinY() + 1 : terrain;
                coreScene = at(0, 0);
                transportScene = at(0, 32);
                buildersScene = at(0, 64);
                roboticsScene = at(0, 96);
                mc.options.hideGui = true;
                // the spawn column can be void in this scratch world — if the rig player fell while the world was
                // still joining, revive here so the teleport below actually takes (dead players ignore teleports)
                if (player.isDeadOrDying()) {
                    player.setHealth(player.getMaxHealth());
                    player.clearFire();
                    LOGGER.info("[M416] revived the rig player (fell out of the void world before the rig armed)");
                }
                player.setGameMode(GameType.CREATIVE);
                // creative flight: no landing between teleports, no way to fall out of the world mid-rig
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                mc.getSingleplayerServer().execute(() -> mc.getSingleplayerServer().getCommands()
                    .performPrefixedCommand(
                        mc.getSingleplayerServer().createCommandSourceStack().withSuppressedOutput(), "time set noon"));
                LOGGER.info("[M416] rig start: anchor={} groundY={}", anchor, groundY);
                floor(level, coreScene, -2, 14, -8, 8);
                clearFootprint(level, coreScene, 0, 14, -5, 8);
                buildTesterChain(level);
                buildOilSpring(level);
                setView(player, coreScene.getX() + 2.5, groundY + 3.5, coreScene.getZ() - 6.0, 28.0F);
            });
            case 1 -> {
                onServer(mc, () -> logTesterChain(level, "t0"));
                wait = 240 - WAIT_TICKS; // ~12 s soak: the engine feeds the chain while the tester counts
            }
            case 2 -> onServer(mc, () -> logTesterChain(level, "t1"));
            case 3 -> {
                screenshot(mc, "m416_power_tester.png");
                onServer(mc, () -> setView(player, coreScene.getX() + 8.5, groundY + 2.5, coreScene.getZ() - 4.5,
                        28.0F));
                wait = 2 * WAIT_TICKS;
            }
            case 4 -> {
                onServer(mc, () -> logOilSpring(level));
                screenshot(mc, "m416_spring_oil.png");
            }
            // ---- transport: the real filtered buffer pass-through ----
            case 5 -> onServer(mc, () -> {
                floor(level, transportScene, -2, 6, -8, 8);
                clearFootprint(level, transportScene, 0, 6, -5, 8);
                buildBufferPass(level);
                setView(player, transportScene.getX() + 1.5, groundY + 3.5, transportScene.getZ() - 5.0, 28.0F);
            });
            case 6 -> {
                onServer(mc, () -> logBufferPass(level, "mid"));
                screenshot(mc, "m416_filtered_buffer.png"); // mid-transit: the buffer should be holding items right now
                wait = 400 - WAIT_TICKS;
            }
            case 7 -> onServer(mc, () -> logBufferPass(level, "final"));
            // ---- builders: five v2 placeholders, place + render + right-click ----
            case 8 -> onServer(mc, () -> {
                floor(level, buildersScene, -2, 14, -6, 8);
                clearFootprint(level, buildersScene, 0, 16, -5, 8);
                placeBuilders(level);
                setView(player, buildersScene.getX() + 0.5, groundY + 1.5, buildersScene.getZ() - 2.5, 30.0F);
            });
            case 9 -> {
                screenshot(mc, "m416_builder.png");
                onServer(mc, () -> {
                    rightClick(level, player, buildersScene, "builder");
                    setView(player, buildersScene.getX() + 3.5, groundY + 1.5, buildersScene.getZ() - 2.5, 30.0F);
                });
            }
            case 10 -> {
                screenshot(mc, "m416_architect.png");
                onServer(mc, () -> {
                    rightClick(level, player, buildersScene.offset(3, 0, 0), "architect");
                    setView(player, buildersScene.getX() + 6.5, groundY + 1.5, buildersScene.getZ() - 2.5, 30.0F);
                });
            }
            case 11 -> {
                screenshot(mc, "m416_library.png");
                onServer(mc, () -> {
                    rightClick(level, player, buildersScene.offset(6, 0, 0), "library");
                    setView(player, buildersScene.getX() + 9.5, groundY + 1.5, buildersScene.getZ() - 2.5, 30.0F);
                });
            }
            case 12 -> {
                screenshot(mc, "m416_marker_construction.png");
                onServer(mc, () -> {
                    rightClick(level, player, buildersScene.offset(9, 0, 0), "marker_construction");
                    setView(player, buildersScene.getX() + 12.5, groundY + 1.5, buildersScene.getZ() - 2.5, 30.0F);
                });
            }
            case 13 -> {
                screenshot(mc, "m416_replacer.png");
                onServer(mc, () -> {
                    rightClick(level, player, buildersScene.offset(12, 0, 0), "replacer");
                    // over to robotics
                    floor(level, roboticsScene, -2, 6, -6, 8);
                    clearFootprint(level, roboticsScene, 0, 6, -5, 8);
                    placeRobotics(level);
                    setView(player, roboticsScene.getX() + 0.5, groundY + 1.5, roboticsScene.getZ() - 2.5, 30.0F);
                });
            }
            // ---- robotics: two v2 placeholders ----
            case 14 -> {
                screenshot(mc, "m416_requester.png");
                onServer(mc, () -> {
                    rightClick(level, player, roboticsScene, "requester");
                    setView(player, roboticsScene.getX() + 3.5, groundY + 1.5, roboticsScene.getZ() - 2.5, 30.0F);
                });
            }
            case 15 -> {
                screenshot(mc, "m416_zone_planner.png");
                onServer(mc, () -> rightClick(level, player, roboticsScene.offset(3, 0, 0), "zone_planner"));
            }
            case 16 -> {
                LOGGER.info("[M416] rig done - shutting down");
                wait = 2 * WAIT_TICKS;
            }
            default -> {
                state = 18;
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

    /**
     * The real power tester chain (the M4.9 verified topology): stone engine facing east &rarr; wooden kinesis pipe
     * &rarr; the power tester's {@link MjReceiver}. No injection anywhere — the tester's
     * {@code received total} counter only moves if energy really flows through the pipe.
     */
    private static void buildTesterChain(ServerLevel level) {
        int x = coreScene.getX();
        int z = coreScene.getZ();
        BlockPos enginePos = new BlockPos(x, groundY, z);
        level.setBlockAndUpdate(enginePos, BcBlocks.ENGINE_STONE.value().defaultBlockState()
            .setValue(StoneEngineBlock.FACING, Direction.EAST));
        if (level.getBlockEntity(enginePos) instanceof StoneEngineBlockEntity engine) {
            boolean fueled = engine.insertFuel(new ItemStack(Items.COAL, 4), level.fuelValues());
            LOGGER.info("[M416] chain engine fueled: {} (ignites once the buffer has room)", fueled);
        }
        level.setBlockAndUpdate(new BlockPos(x + 1, groundY, z),
            BcBlocks.PIPE_KINESIS_WOOD.value().defaultBlockState());
        BlockPos testerPos = new BlockPos(x + 2, groundY, z);
        level.setBlockAndUpdate(testerPos, BcBlocks.POWER_TESTER.value().defaultBlockState());
        LOGGER.info("[M416] power_tester placed at {} (real chain: engine -> kinesis -> MjReceiver)", testerPos);
    }

    /** The real oil spring, a few blocks east of the chain so the oil puddle stays out of it. */
    private static void buildOilSpring(ServerLevel level) {
        BlockPos springPos = new BlockPos(coreScene.getX() + 8, groundY, coreScene.getZ());
        level.setBlockAndUpdate(springPos, BcBlocks.SPRING_OIL.value().defaultBlockState());
        LOGGER.info("[M416] spring_oil placed at {} (real spring: places oil sources above itself every {} ticks)",
            springPos, SpringOilBlockEntity.TICK_RATE);
    }

    /**
     * The filtered buffer pass: bottom hopper (faces east, pulls from the buffer above, pushes into the chest) &rarr;
     * the real filtered buffer &rarr; top hopper (faces down, holds the payload and pushes it into the buffer). Items
     * must show up in the chest having passed through the buffer's FIFO.
     */
    private static void buildBufferPass(ServerLevel level) {
        int x = transportScene.getX();
        int z = transportScene.getZ();
        BlockPos bottomHopper = new BlockPos(x, groundY, z);
        level.setBlockAndUpdate(bottomHopper, Blocks.HOPPER.defaultBlockState()
            .setValue(HopperBlock.FACING, Direction.EAST));
        BlockPos bufferPos = bottomHopper.above();
        level.setBlockAndUpdate(bufferPos, BcTransportBlocks.FILTERED_BUFFER.value().defaultBlockState());
        LOGGER.info("[M416] filtered_buffer placed at {} (real FIFO buffer, item capability on every side)", bufferPos);
        BlockPos topHopper = bufferPos.above();
        level.setBlockAndUpdate(topHopper, Blocks.HOPPER.defaultBlockState()
            .setValue(HopperBlock.FACING, Direction.DOWN));
        if (level.getBlockEntity(topHopper) instanceof Container topContainer) {
            topContainer.setItem(0, new ItemStack(Items.DIRT, 16));
            LOGGER.info("[M416] top hopper loaded with 16 dirt (pushing down into the buffer)");
        }
        BlockPos chest = bottomHopper.east();
        level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
        LOGGER.info("[M416] chest placed at {} (the pass-through destination)", chest);
        var capability = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,
            bufferPos, Direction.UP);
        LOGGER.info("[M416] filtered_buffer item capability (up side): {}", capability == null
            ? "NULL (hoppers cannot see the buffer!)" : capability.getClass().getSimpleName());
    }

    /** The five builders v2 placeholders, three blocks apart (legacy {@code Tile*} behaviour, not migrated this task). */
    private static void placeBuilders(ServerLevel level) {
        placeV2(level, buildersScene, BcBuildersBlocks.BUILDER.value().defaultBlockState(), "builder", "TileBuilder");
        placeV2(level, buildersScene.offset(3, 0, 0), BcBuildersBlocks.ARCHITECT.value().defaultBlockState(),
            "architect", "TileArchitect");
        placeV2(level, buildersScene.offset(6, 0, 0), BcBuildersBlocks.LIBRARY.value().defaultBlockState(),
            "library", "TileLibrary");
        placeV2(level, buildersScene.offset(9, 0, 0), BcBuildersBlocks.MARKER_CONSTRUCTION.value().defaultBlockState(),
            "marker_construction", "TileMarker");
        placeV2(level, buildersScene.offset(12, 0, 0), BcBuildersBlocks.REPLACER.value().defaultBlockState(),
            "replacer", "TileReplacer");
    }

    /** The two robotics v2 placeholders. */
    private static void placeRobotics(ServerLevel level) {
        placeV2(level, roboticsScene, BcRoboticsBlocks.REQUESTER.value().defaultBlockState(), "requester",
            "TileRequester");
        placeV2(level, roboticsScene.offset(3, 0, 0), BcRoboticsBlocks.ZONE_PLANNER.value().defaultBlockState(),
            "zone_planner", "TileZonePlanner");
    }

    private static void placeV2(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state,
            String name, String legacyTile) {
        level.setBlockAndUpdate(pos, state);
        LOGGER.info("[M416] {} placed at {} (v2-PLACEHOLDER: legacy {} blueprint/robotics behaviour not migrated)",
            name, pos, legacyTile);
    }

    // ---------------------------------------------------------------- evidence logs

    /** Chain snapshot at t0/t1: engine burn, kinesis buffer, tester counters — the rising total is the evidence. */
    private static void logTesterChain(ServerLevel level, String tag) {
        BlockPos enginePos = new BlockPos(coreScene.getX(), groundY, coreScene.getZ());
        if (level.getBlockEntity(enginePos) instanceof StoneEngineBlockEntity engine) {
            LOGGER.info("[M416] chain[{}] engine: burning={} burn={}/{} stored={}µMJ", tag, engine.isBurning(),
                engine.getBurnRemain(), engine.getBurnTotal(), engine.getEnergyStored());
        } else {
            LOGGER.error("[M416] chain[{}] engine missing at {}", tag, enginePos);
        }
        BlockPos pipePos = new BlockPos(coreScene.getX() + 1, groundY, coreScene.getZ());
        if (level.getBlockEntity(pipePos) instanceof KinesisPipeBlockEntity kinesis) {
            LOGGER.info("[M416] chain[{}] kinesis pipe: stored={}µMJ", tag, kinesis.getEnergyStored());
        } else {
            LOGGER.error("[M416] chain[{}] kinesis missing at {}", tag, pipePos);
        }
        BlockPos testerPos = new BlockPos(coreScene.getX() + 2, groundY, coreScene.getZ());
        if (level.getBlockEntity(testerPos) instanceof PowerTesterBlockEntity tester) {
            LOGGER.info("[M416] chain[{}] power_tester: received total {}µMJ (last tick {}µMJ, last packet {}µMJ)",
                tag, tester.getTotalReceived(), tester.getLastTickReceived(), tester.getLastReceived());
        } else {
            LOGGER.error("[M416] chain[{}] power_tester missing at {}", tag, testerPos);
        }
    }

    /** The spring evidence: the block above the spring must be an oil source by now (the [M416] placement lines come
     * from {@link SpringOilBlockEntity} itself). */
    private static void logOilSpring(ServerLevel level) {
        BlockPos springPos = new BlockPos(coreScene.getX() + 8, groundY, coreScene.getZ());
        BlockPos above = springPos.above();
        Identifier fluid = BuiltInRegistries.FLUID.getKey(level.getFluidState(above).getType());
        LOGGER.info("[M416] spring_oil check: block above {} = {} (fluid {}, source={})", springPos,
            level.getBlockState(above).getBlock(), fluid, level.getFluidState(above).isSource());
    }

    /** Pass-through snapshot at mid/final: payload in the top hopper, buffer FIFO, chest — locates a stalled hop. */
    private static void logBufferPass(ServerLevel level, String tag) {
        int topHopper = containerTotal(level, new BlockPos(transportScene.getX(), groundY + 2, transportScene.getZ()));
        String buffer = level.getBlockEntity(new BlockPos(transportScene.getX(), groundY + 1, transportScene.getZ()))
            instanceof FilteredBufferBlockEntity b ? b.describeContents() : "missing";
        int chest = containerTotal(level, new BlockPos(transportScene.getX() + 1, groundY, transportScene.getZ()));
        LOGGER.info("[M416] buffer pass[{}]: top hopper {} dirt, buffer [{}], chest {} item(s) (expect 16)", tag,
            topHopper, buffer, chest);
    }

    /** Total item count in the container BE at pos, or -1 if there is no container there. */
    private static int containerTotal(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof Container container) {
            int total = 0;
            for (int i = 0; i < container.getContainerSize(); i++) {
                total += container.getItem(i).getCount();
            }
            return total;
        }
        return -1;
    }

    /**
     * The v2 right-click evidence through the REAL interaction path ({@code ServerPlayerGameMode#useItemOn} with an
     * empty main hand): the result is logged and the run continues — any crash here would fail the rig.
     */
    private static void rightClick(ServerLevel level, ServerPlayer player, BlockPos pos, String name) {
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
        var result = player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
        LOGGER.info("[M416] right-click {} at {}: useItemOn -> {} (no crash)", name, pos, result);
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

    private static void setView(ServerPlayer player, double x, double y, double z, float pitch) {
        viewYaw = 0.0F;
        viewPitch = pitch;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.teleportTo((ServerLevel) player.level(), x, y, z, Set.of(), viewYaw, viewPitch, false);
    }

    /**
     * Lays the scene floor slab one block under machine level — the anchor sits in void terrain, so without this
     * every scene floats over the abyss (and the first vantage drop killed the rig's first run).
     */
    private static void floor(ServerLevel level, BlockPos base, int x0, int x1, int z0, int z1) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                level.setBlockAndUpdate(new BlockPos(base.getX() + x, groundY - 1, base.getZ() + z),
                    Blocks.SMOOTH_STONE.defaultBlockState());
            }
        }
    }

    private static void screenshot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), 1,
            (Component component) -> LOGGER.info("[M416] screenshot {}: {}", name, component.getString()));
    }

    private M416CProbe() {
    }
}
