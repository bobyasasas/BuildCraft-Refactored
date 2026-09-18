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
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import buildcraft.builders.BcBuildersBlocks;
import buildcraft.builders.blockentity.QuarryBlockEntity;
import buildcraft.core.BcBlocks;

/**
 * The M4.5 in-game evidence rig (the same pattern as the M2.12 evidence run): a client-tick script that drives one
 * quickPlay world through every laser state this task ships and drops a screenshot per state into
 * {@code screenshots/}:
 * <ol>
 * <li>quarry with a mining area &mdash; the {@code STRIPES_WRITE} border box;</li>
 * <li>the same quarry powered &mdash; the {@code POWER_LOW} beam plus the {@code DRILL} column on the target;</li>
 * <li>a diagonal volume-marker pair (the 12-edge {@code MARKER_VOLUME_CONNECTED} box) next to a line-connected pair
 * and a 3-marker path chain (the {@code MARKER_PATH_CONNECTED} hop lines);</li>
 * <li>the line pair redstone-driven &mdash; the four {@code MARKER_VOLUME_SIGNAL} lines.</li>
 * </ol>
 *
 * <p>Every world mutation is enqueued onto the integrated server thread through
 * {@link MinecraftServer#execute(Runnable)} &mdash; the rig's tick fires on the render thread and touching the ServerLevel
 * from there races the server tick (the first rig attempt lost exactly that race: the block placed, the block entity
 * lookup right after it returned null). Lives in {@code buildcraft.core.blockentity} only to reach the protected
 * {@code connectWith} for the diagonal pair. The rig shuts the game down when done, so the run doubles as the smoke
 * pass/fail signal: {@code [M45]} lines in the log + 4 screenshots.
 *
 * <p>The rig is inert unless the run passes {@code -Dbuildcraft.m45probe=true} (the M4.6 {@code M46PipeSmokeProbe}
 * gating pattern, retrofitted in the M4.5 close-out: without the flag the tick handler returns immediately, so normal
 * play never gets its camera taken over or its world written to).
 */
@EventBusSubscriber(modid = "buildcraftcore", value = Dist.CLIENT)
public final class M45LaserSmokeProbe {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Set (and only set) by the M4.5 evidence run's JVM arguments; keeps the rig inert everywhere else. */
    private static final boolean ENABLED = Boolean.getBoolean("buildcraft.m45probe");

    /** Base wait between phases (ticks) &mdash; comfortably more than a couple of sync round trips. */
    private static final int WAIT_TICKS = 30;

    /** Safety valve for the "waiting for the first target" poll: never loop longer than ~30 s of ticks. */
    private static final int TARGET_POLL_LIMIT = 600;

    private static int state = 0;
    private static int wait = 0;
    private static int targetPolls = 0;
    private static int soakTicks = 0;
    private static int drainCounter = 0;
    /** Server-thread snapshots of the quarry state (plain cross-thread field reads proved unreliable for pacing). */
    private static volatile BlockPos serverTarget = null;
    private static volatile long serverEnergy = 0;
    private static BlockPos anchor = null;
    private static int groundY = -1;
    private static volatile QuarryBlockEntity quarry = null;

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
        if (level == null || state > 6) {
            return;
        }
        // keep the vantage view pinned: south (+z) and slightly down at the scene
        mc.player.setYRot(0.0F);
        mc.player.setXRot(18.0F);
        // The slice quarry stalls once its 8-stack output buffer fills (8 blocks of grass/dirt, i.e. seconds into
        // the run). The buffer has no extraction API, so the rig drains it reflectively on the server thread to
        // keep the beam phase alive for the whole screenshot walk.
        if (quarry != null && ++drainCounter >= 10) {
            drainCounter = 0;
                QuarryBlockEntity be = quarry;
                mc.getSingleplayerServer().execute(() -> {
                    try {
                        Field buffer = QuarryBlockEntity.class.getDeclaredField("output");
                        buffer.setAccessible(true);
                        ((List<?>) buffer.get(be)).clear();
                    } catch (ReflectiveOperationException ignored) {
                        // no buffer, no stall: the rig still completes, just possibly without the beam phase
                    }
                    be.receivePower(6400, false);
                    serverTarget = be.getCurrentTarget();
                    serverEnergy = be.getEnergyStored();
                });
        }
        if (--wait > 0) {
            return;
        }
        ServerPlayer player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
        if (player == null) {
            return;
        }
        switch (state) {
            case 0 -> {
                BlockPos spawn = player.blockPosition();
                anchor = new BlockPos(spawn.getX(), 0, spawn.getZ());
                groundY = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn).getY();
                mc.options.hideGui = true;
                // daylight for the evidence shots (the reused save drifts toward sunset across runs)
                MinecraftServer server = mc.getSingleplayerServer();
                server.execute(() -> server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack().withSuppressedOutput(), "time set noon"));
                player.teleportTo(level, anchor.getX() + 0.5, groundY + 10, anchor.getZ() - 12, Set.of(), 0.0F,
                    18.0F, false);
                LOGGER.info("[M45] rig start: anchor={} groundY={}", anchor, groundY);
            }
            case 1 -> {
                if (quarry == null) {
                    placeQuarry(level);
                    wait = 10;
                    return;
                }
                // quarry placed and area set (see placeQuarry): give the update tag a moment, then screenshot
            }
            case 2 -> {
                screenshot(mc, "m45_quarry_frame");
                MinecraftServer server = mc.getSingleplayerServer();
                QuarryBlockEntity be = quarry;
                // trickle the power (640/tick through the drain top-ups) so the layer outlasts the walk: the
                // slice burns MAX_POWER_PER_TICK = 51,200/tick when fed fully, one full grass layer in ~6 s
                server.execute(() -> be.receivePower(6400, false));
                LOGGER.info("[M45] quarry powered: area={}..{} target={}", quarry.getAreaMin(), quarry.getAreaMax(),
                    quarry.getCurrentTarget());
            }
            case 3 -> {
                // once the beam is alive, soak ~15 s so the drill marches a few rows into the area: on the first
                // scan row it hugs the frame's front edge (and its corner posts) and the beam is edge-on
                if (serverTarget != null && serverEnergy > 0) {
                    if (++soakTicks < 300) {
                        wait = 1;
                        return;
                    }
                } else if (++targetPolls < TARGET_POLL_LIMIT) {
                    // still charging the first block: stay in this state and re-check shortly
                    wait = 1;
                    return;
                }
                LOGGER.info("[M45] quarry mining: target={} energy={} (polls={}, soak={})",
                    serverTarget, serverEnergy, targetPolls, soakTicks);
                wait = 2 * WAIT_TICKS;
                state++;
                return;
            }
            case 4 -> {
                screenshot(mc, "m45_quarry_mining");
                placeMarkers(level);
                LOGGER.info("[M45] markers placed+connected: volumeBox diagonal pair, volumeLine z-pair,"
                    + " pathChain triple");
            }
            case 5 -> {
                screenshot(mc, "m45_markers_box_path");
                BlockPos redstone = new BlockPos(anchor.getX() + 6, groundY + 3, anchor.getZ() + 4);
                mc.getSingleplayerServer().execute(() -> level.setBlockAndUpdate(redstone,
                    Blocks.REDSTONE_BLOCK.defaultBlockState()));
                LOGGER.info("[M45] redstone block set next to the line marker");
            }
            default -> {
                screenshot(mc, "m45_marker_signals");
                LOGGER.info("[M45] rig done - shutting down");
                state = 7;
                mc.stop();
                return;
            }
        }
        state++;
        wait = 2 * WAIT_TICKS;
    }

    /** Places the quarry on the server thread and wires its mining area; retried until the block entity really is
     * there (defensive — even enqueued work orders operations, it never races them). */
    private static void placeQuarry(ServerLevel level) {
        BlockPos quarryPos = new BlockPos(anchor.getX() + 2, groundY, anchor.getZ() + 8);
        // the slice scan works the area top-down (top layer first), so the first mined cells are the surface grass
        // layer at groundY; the shallow areaMin keeps the whole worked volume above ground, in view of the camera.
        BlockPos areaMin = new BlockPos(anchor.getX() - 3, groundY - 1, anchor.getZ() + 3);
        BlockPos areaMax = new BlockPos(anchor.getX() + 7, groundY + 1, anchor.getZ() + 13);
        level.getServer().execute(() -> {
            level.setBlockAndUpdate(quarryPos, BcBuildersBlocks.QUARRY.value().defaultBlockState());
            if (level.getBlockEntity(quarryPos) instanceof QuarryBlockEntity be) {
                be.setMiningArea(areaMin, areaMax);
                quarry = be;
                LOGGER.info("[M45] quarry placed at {} with area {}..{}", quarryPos, areaMin, areaMax);
            } else {
                LOGGER.error("[M45] quarry block entity missing at {} - will retry", quarryPos);
            }
        });
    }

    /** Places both marker groups on the server thread: the diagonal volume pair (explicit {@code connectWith},
     * legacy corner connection), the line pair (auto-connects on load through the line scan) and the path chain
     * (also auto-connected). */
    private static void placeMarkers(ServerLevel level) {
        BlockPos volAPos = new BlockPos(anchor.getX() - 7, groundY + 1, anchor.getZ() + 6);
        BlockPos volBPos = new BlockPos(anchor.getX() - 3, groundY + 4, anchor.getZ() + 10);
        BlockPos volCPos = new BlockPos(anchor.getX() + 6, groundY + 3, anchor.getZ() + 5);
        BlockPos volDPos = new BlockPos(anchor.getX() + 6, groundY + 3, anchor.getZ() + 11);
        BlockPos path1 = new BlockPos(anchor.getX() - 11, groundY + 1, anchor.getZ() + 4);
        BlockPos path2 = new BlockPos(anchor.getX() - 11, groundY + 1, anchor.getZ() + 8);
        BlockPos path3 = new BlockPos(anchor.getX() - 11, groundY + 1, anchor.getZ() + 12);
        level.getServer().execute(() -> {
            MarkerVolumeBlockEntity volA = placeVolume(level, volAPos);
            MarkerVolumeBlockEntity volB = placeVolume(level, volBPos);
            if (volA != null && volB != null) {
                volA.connectWith(volB);
            }
            placeVolume(level, volCPos);
            placeVolume(level, volDPos);
            placePath(level, path1);
            placePath(level, path2);
            placePath(level, path3);
        });
    }

    private static MarkerVolumeBlockEntity placeVolume(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos, BcBlocks.MARKER_VOLUME.value().defaultBlockState());
        return level.getBlockEntity(pos) instanceof MarkerVolumeBlockEntity marker ? marker : null;
    }

    private static MarkerPathBlockEntity placePath(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos, BcBlocks.MARKER_PATH.value().defaultBlockState());
        return level.getBlockEntity(pos) instanceof MarkerPathBlockEntity marker ? marker : null;
    }

    private static void screenshot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), 1,
            (Component component) -> LOGGER.info("[M45] screenshot {}: {}", name, component.getString()));
    }

    private M45LaserSmokeProbe() {
    }
}
