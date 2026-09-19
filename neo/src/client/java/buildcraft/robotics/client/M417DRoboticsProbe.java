/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.robotics.client;

import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import buildcraft.robotics.BuildCraftRobotics;
import buildcraft.robotics.BcRoboticsBlocks;
import buildcraft.robotics.blockentity.RequesterBlockEntity;
import buildcraft.robotics.blockentity.ZonePlannerBlockEntity;

/**
 * M4.17d in-game evidence rig (the {@code M416AMachineProbe} pattern, two machines later): a client-tick script that
 * drives one quickPlay world through the two robotics support machines this task ships, one screenshot per machine into
 * {@code screenshots/} plus the machines' own {@code [M417]} log lines as the dual evidence:
 * <ol>
 * <li>the requester scene: a chest holding 64 redstone + 64 glowstone dust next to the requester; the templates ask for
 * 32 redstone (pulled, matching) and 8 diamonds (never available, the shortfall log);</li>
 * <li>the zone planner scene: a single-radius zone (3) defined around the planner, bounds logged.</li>
 * </ol>
 *
 * <p>The rig is <b>restart-aware</b>, which is the persistence half of the evidence: run it once on a fresh gameDir
 * (build + pull + define, screenshots {@code m417_requester_pull}/{@code m417_zone_define}), then again on the same
 * gameDir &mdash; the saved world comes back with the requester's buffer and the planner's zone intact, announced by
 * the machines themselves ({@code restored from save}) and shot as
 * {@code m417_requester_persist}/{@code m417_zone_persist}. After the last scene the rig issues a {@code save-all} so
 * the kill of the headless client (the established exit: {@code mc.stop()} hangs without a window manager) cannot lose
 * the world state.
 *
 * <p>Inert unless the run passes {@code -Dbuildcraft.m417dprobe=true} (dev-run evidence only, never active in normal
 * play).
 */
@EventBusSubscriber(modid = BuildCraftRobotics.MOD_ID, value = Dist.CLIENT)
public final class M417DRoboticsProbe {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Set (and only set) by the M4.17d evidence run's JVM arguments; keeps the rig inert everywhere else. */
    private static final boolean ENABLED = Boolean.getBoolean("buildcraft.m417dprobe");

    /** Base wait between phases (ticks) — comfortably more than a couple of sync round trips. */
    private static final int WAIT_TICKS = 30;

    /** The strip's deterministic altitude: machines stand on a rig-built stone platform at {@code RIG_Y - 1}. */
    private static final int RIG_Y = 65;

    private static int state = 0;
    private static int wait = 0;
    private static int groundY = -1;
    private static BlockPos anchor = null;
    /** True when this run found the run-A scene already in the save (the run-B persistence pass). */
    private static boolean persistenceRun = false;
    /** View yaw/pitch to pin every tick (per-scene camera). */
    private static float viewYaw;
    private static float viewPitch;

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
            case 0 -> onServer(mc, () -> {
                // the shared world spawn (not the player's current position: run B respawns where run A left
                // off, which would drift the anchor) plus a distinctive z offset so the rig never collides with
                // any other scene left at the spawn anchor in a reused world (z+2048: virgin ground, the earlier
                // z+1024 strip saw experiments whose leftovers the fresh-world copy cannot be trusted against)
                BlockPos spawn = level.getRespawnData().pos();
                anchor = new BlockPos(spawn.getX() + 24, 0, spawn.getZ() + 2048);
                // force the far strip's chunk to generate/load now; the platform pass writes its block data next
                // state (block data is fully writable once the chunk exists)
                level.getChunk(anchor.getX() >> 4, anchor.getZ() >> 4);
                // spectator: this void world's spawn drops the player into the sky (fall + death loop), and a
                // flying spectator keeps the camera deterministic while rendering the scene just the same
                ServerPlayer rigPlayer = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                if (rigPlayer != null) {
                    rigPlayer.setGameMode(GameType.SPECTATOR);
                }
                mc.options.hideGui = true;
            });
            case 1 -> onServer(mc, () -> {
                // the rig builds its own platform: this world's ground truth is mostly VOID (the gametest save's
                // far field has no terrain at all, which once dropped a whole scene into the air), so nothing is
                // left to natural terrain — the strip is carved deterministically at RIG_Y over a stone platform
                BlockPos prior = findPriorRequester(level);
                persistenceRun = prior != null;
                if (prior != null) {
                    anchor = new BlockPos(prior.getX(), prior.getY(), prior.getZ());
                    groundY = prior.getY();
                } else {
                    buildPlatform(level);
                    anchor = new BlockPos(anchor.getX(), RIG_Y, anchor.getZ());
                    groundY = RIG_Y;
                }
                buildScenes(level);
                LOGGER.info("[M417] rig start: anchor={} groundY={} persistenceRun={}", anchor, groundY,
                    persistenceRun);
            });
            case 2 -> {
                setView(player, anchor.getX() + 2.0, groundY + 4.0, anchor.getZ() - 7.0, 0.0F, 30.0F);
                wait = 2 * WAIT_TICKS;
            }
            case 3 -> screenshot(mc, "m417_layout");
            case 4 -> {
                // soak: let the requester run its pull passes (one per 20 ticks, two+ passes land here)
                setView(player, anchor.getX() + 1.0, groundY + 3.0, anchor.getZ() - 4.5, 0.0F, 32.0F);
                wait = 8 * WAIT_TICKS;
            }
            case 5 -> onServer(mc, () -> logRequester(level, "A"));
            case 6 -> {
                screenshot(mc, persistenceRun ? "m417_requester_persist" : "m417_requester_pull");
                onServer(mc, () -> logRequester(level, "B"));
                wait = 2 * WAIT_TICKS;
            }
            case 7 -> {
                setView(player, anchor.getX() + 5.0, groundY + 3.5, anchor.getZ() - 5.0, 0.0F, 30.0F);
                wait = 2 * WAIT_TICKS;
            }
            case 8 -> onServer(mc, () -> {
                if (level.getBlockEntity(zonePlannerPos()) instanceof ZonePlannerBlockEntity planner) {
                    planner.logZoneState();
                }
            });
            case 9 -> {
                screenshot(mc, persistenceRun ? "m417_zone_persist" : "m417_zone_define");
                onServer(mc, () -> {
                    MinecraftServer server = mc.getSingleplayerServer();
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "save-all");
                    LOGGER.info("[M417] rig done - world saved for the persistence pass, shutting down");
                });
                wait = 3 * WAIT_TICKS;
            }
            default -> {
                LOGGER.info("[M417] rig finished (headless exit: the harness kills the client)");
                state = 11;
                return;
            }
        }
        state++;
        wait = Math.max(wait, WAIT_TICKS);
    }

    /** Marshals one chunk of world work onto the server thread (the M416 rig discipline). */
    private static void onServer(Minecraft mc, Runnable task) {
        mc.getSingleplayerServer().execute(task);
    }

    // ---------------------------------------------------------------- scenes

    private static BlockPos requesterPos() {
        return new BlockPos(anchor.getX(), groundY, anchor.getZ());
    }

    private static BlockPos chestPos() {
        return new BlockPos(anchor.getX() + 1, groundY, anchor.getZ());
    }

    private static BlockPos zonePlannerPos() {
        return new BlockPos(anchor.getX() + 4, groundY, anchor.getZ());
    }

    /**
     * The rig's deterministic ground: a stone platform at {@code RIG_Y - 1}, wide enough to also carry the camera
     * stand-ins south of the strip (the player teleports land there instead of falling into the void), plus a cleared
     * band above it. Overwrites silently — safe to re-run.
     */
    private static void buildPlatform(ServerLevel level) {
        for (int x = -2; x <= 8; x++) {
            for (int z = -9; z <= 1; z++) {
                level.setBlockAndUpdate(new BlockPos(anchor.getX() + x, RIG_Y - 1, anchor.getZ() + z),
                    Blocks.STONE.defaultBlockState());
                for (int y = RIG_Y; y <= RIG_Y + 15; y++) {
                    level.removeBlock(new BlockPos(anchor.getX() + x, y, anchor.getZ() + z), false);
                }
            }
        }
    }

    /** A leftover rig requester from an earlier run, found by its block in a +-8 y window around the strip. */
    private static BlockPos findPriorRequester(ServerLevel level) {
        for (int y = RIG_Y - 8; y <= RIG_Y + 8; y++) {
            BlockPos pos = new BlockPos(anchor.getX(), y, anchor.getZ());
            if (level.getBlockState(pos).is(BcRoboticsBlocks.REQUESTER.value())) {
                return pos;
            }
        }
        return null;
    }

    /**
     * Builds both scenes (idempotent: a persistence run finds them by block and only re-reads them). The chest holds
     * 64 redstone + 64 glowstone dust; the templates ask for 32 redstone (matching, gets pulled) and 8 diamonds
     * (never available, the shortfall line).
     */
    private static void buildScenes(ServerLevel level) {
        // persistence runs find the previous scene and must NOT touch it (the requester's buffer and lifetime
        // counter are the run-B evidence; the chest keeps its remaining redstone). Only a fresh build carves.
        if (level.getBlockState(requesterPos()).is(BcRoboticsBlocks.REQUESTER.value())) {
            if (level.getBlockEntity(requesterPos()) instanceof RequesterBlockEntity requester) {
                LOGGER.info("[M417] requester scene found from the previous run: buffered {} item(s), lifetime"
                    + " pulled {}", countBuffer(requester), requester.getPulledTotal());
            } else {
                LOGGER.info("[M417] requester scene found from the previous run (block only at {})", requesterPos());
            }
        } else {
            // fresh build on the rig platform (void world: nothing natural to carve around, and the platform pass
            // already cleared the band above the stone — just settle the machines on it)
            level.setBlockAndUpdate(requesterPos(), BcRoboticsBlocks.REQUESTER.value().defaultBlockState());
            if (level.getBlockEntity(requesterPos()) instanceof RequesterBlockEntity fresh) {
                fresh.setRequestTemplate(0, new ItemStack(Items.REDSTONE, 32));
                fresh.setRequestTemplate(1, new ItemStack(Items.DIAMOND, 8));
                LOGGER.info("[M417] requester scene built at {}: template slot 0 = 32x minecraft:redstone,"
                    + " slot 1 = 8x minecraft:diamond", requesterPos());
            }
        }
        if (!level.getBlockState(chestPos()).is(Blocks.CHEST)) {
            level.setBlockAndUpdate(chestPos(), Blocks.CHEST.defaultBlockState());
        }
        if (level.getBlockEntity(chestPos()) instanceof Container chest && chest.isEmpty()) {
            chest.setItem(0, new ItemStack(Items.REDSTONE, 64));
            chest.setItem(1, new ItemStack(Items.GLOWSTONE_DUST, 64));
            LOGGER.info("[M417] chest stocked at {}: 64x minecraft:redstone + 64x minecraft:glowstone_dust", chestPos());
        }
        if (level.getBlockState(zonePlannerPos()).is(BcRoboticsBlocks.ZONE_PLANNER.value())) {
            if (level.getBlockEntity(zonePlannerPos()) instanceof ZonePlannerBlockEntity planner) {
                planner.getZone(); // the lazy restore announce (run B)
            }
            LOGGER.info("[M417] zone planner scene found from the previous run at {}", zonePlannerPos());
        } else {
            level.setBlockAndUpdate(zonePlannerPos(), BcRoboticsBlocks.ZONE_PLANNER.value().defaultBlockState());
            if (level.getBlockEntity(zonePlannerPos()) instanceof ZonePlannerBlockEntity fresh) {
                fresh.setZone(zonePlannerPos(), 3);
            }
        }
    }

    // ---------------------------------------------------------------- evidence logs

    private static void logRequester(ServerLevel level, String tag) {
        BlockPos pos = requesterPos();
        if (level.getBlockEntity(pos) instanceof RequesterBlockEntity requester) {
            LOGGER.info("[M417] requester[{}]: template0={}x{}, buffered0={}, template1={}x{}, buffered1={};"
                + " chest redstone left={}, lifetime pulled={}", tag, requester.getRequestTemplate(0).getCount(),
                requester.getRequestTemplate(0).getItem(), requester.getInv().getAmountAsLong(0),
                requester.getRequestTemplate(1).getCount(), requester.getRequestTemplate(1).getItem(),
                requester.getInv().getAmountAsLong(1), chestRedstone(level), requester.getPulledTotal());
        } else {
            LOGGER.error("[M417] requester[{}]: no requester BE at {} (block={})", tag, pos,
                level.getBlockState(pos).getBlock());
        }
    }

    private static int chestRedstone(ServerLevel level) {
        if (level.getBlockEntity(chestPos()) instanceof Container chest) {
            int total = 0;
            for (int i = 0; i < chest.getContainerSize(); i++) {
                if (chest.getItem(i).is(Items.REDSTONE)) {
                    total += chest.getItem(i).getCount();
                }
            }
            return total;
        }
        return -1;
    }

    private static long countBuffer(RequesterBlockEntity requester) {
        long total = 0;
        for (int i = 0; i < RequesterBlockEntity.NB_ITEMS; i++) {
            total += requester.getInv().getAmountAsLong(i);
        }
        return total;
    }

    // ---------------------------------------------------------------- helpers

    private static void setView(ServerPlayer player, double x, double y, double z, float yaw, float pitch) {
        viewYaw = yaw;
        viewPitch = pitch;
        player.teleportTo((ServerLevel) player.level(), x, y, z, Set.of(), viewYaw, viewPitch, false);
    }

    private static void screenshot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), 1,
            (Component component) -> LOGGER.info("[M417] screenshot {}: {}", name, component.getString()));
    }

    private M417DRoboticsProbe() {
    }
}
