/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.transport.client;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import buildcraft.factory.BcFactoryBlocks;
import buildcraft.transport.BcTransportBlocks;
import buildcraft.transport.BuildCraftTransport;
import buildcraft.transport.blockentity.PipeHolderBlockEntity;
import buildcraft.transport.pipe.BcPipeFamilies;
import buildcraft.transport.pipe.BcPipeFamilies.Family;

/**
 * M4.18c in-game evidence rig for the user-reported "管道的侧面渲染有问题" pipe-side bug (the
 * {@code M46PipeSmokeProbe} pattern, refocused on the pipe <em>side</em> faces): a client-tick script that builds every
 * adjacency the report could mean and drops a close-up screenshot per angle into the game dir's {@code screenshots/}
 * (the caller sorts them into {@code before/} and {@code after/}):
 * <ol>
 * <li>a solo pipe in mid-air (all six unconnected side faces must exist — the pixel-sampling target);</li>
 * <li>a 4-straight Z run and a 4-straight X run plus an elbow (the centre cube's unconnected E/W faces);</li>
 * <li>a pipe against glass (a transparent neighbour must not eat the touching face);</li>
 * <li>a pipe against stone/dirt (solid neighbours, no z-fighting, other faces intact);</li>
 * <li>a pipe sandwiched between a pump and a tank (machine adjacency);</li>
 * <li>four solo pipes of different materials (cobblestone, gold, diamond, clay);</li>
 * <li>a fluid pair filled through {@link PipeHolderBlockEntity#interactWithBucket} (translucent column) next to an
 * injected power pipe (the full-bright core).</li>
 * <li>three dyed {@code items_stone} pipes (colourless control / red / blue, M4.18c): the dye skin layer —
 * colourless draws no skin, red/blue draw the {@code overlay_stained} skin tinted with the dye colour.</li>
 * </ol>
 *
 * <p>Everything world-touching runs on the integrated server through {@link MinecraftServer#execute} (the M4.6 rig
 * discipline); the camera is driven per shot from a fixed {@link Shot} table with the yaw/pitch computed by
 * {@link #lookAt}. The rig is inert unless the run passes {@code -Dbuildcraft.m418cprobe=true} (dev-run evidence only,
 * never active in normal play), and it logs {@code [M418C]} lines per shot so the caller can correlate the files.
 */
@EventBusSubscriber(modid = BuildCraftTransport.MOD_ID, value = Dist.CLIENT)
public final class M418CPipeSideProbe {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Set (and only set) by the M4.18c evidence run's JVM arguments; keeps the rig inert everywhere else. */
    private static final boolean ENABLED = Boolean.getBoolean("buildcraft.m418cprobe");

    /** Base wait between phases (ticks) — comfortably more than a couple of sync round trips. */
    private static final int WAIT_TICKS = 30;

    /** Settling wait after a camera teleport before the screenshot state grabs it. */
    private static final int CAM_WAIT_TICKS = 8;

    /** The first-person camera sits this far above the teleported feet position (standing player eye height). */
    private static final double PLAYER_EYE_HEIGHT = 1.62;

    private static int state = 0;
    private static int wait = 0;
    private static int groundY = -1;
    private static BlockPos anchor = null;
    private static int shotIndex = -1;
    /** View yaw/pitch to pin every tick (per-shot camera). */
    private static float viewYaw;
    private static float viewPitch;

    /** One planned screenshot: a camera position, the world point it must look at, and the FOV to shoot with
     * ({@code 30} for face-filling close-ups, {@code 70} for scene context). */
    private record Shot(String name, double camX, double camY, double camZ, double tgtX, double tgtY, double tgtZ,
        int fov) {
    }

    private static final List<Shot> SHOTS = new ArrayList<>();

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
        if (level == null || state > 2 * SHOTS.size() + 2) {
            return;
        }
        // keep the vantage view pinned at the current shot
        mc.player.setYRot(viewYaw);
        mc.player.setXRot(viewPitch);
        if (--wait > 0) {
            return;
        }
        ServerPlayer player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
        if (player == null) {
            return;
        }
        if (state == 0) {
            onServer(mc, () -> {
                BlockPos spawn = player.blockPosition();
                anchor = new BlockPos(spawn.getX() + 24, 0, spawn.getZ());
                BlockPos scene = new BlockPos(anchor.getX() + 2, 0, anchor.getZ());
                groundY = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, scene).getY();
                mc.options.hideGui = true;
                // pin the daylight (M4.18c: comparable pixel evidence across runs — the lightmap must not drift)
                level.getServer().getCommands().performPrefixedCommand(
                    level.getServer().createCommandSourceStack(), "gamerule advance_time false");
                level.getServer().getCommands().performPrefixedCommand(
                    level.getServer().createCommandSourceStack(), "time set 6000");
                LOGGER.info("[M418C] rig start: anchor={} groundY={}", anchor, groundY);
                buildScenes(level);
            });
            // the build task runs on the server thread — let it land (and the blocks sync) before shooting
            wait = WAIT_TICKS;
            state++;
            return;
        }
        if (state % 2 == 1) {
            // odd states: move the camera to the next shot's vantage
            if (SHOTS.isEmpty()) {
                // the build task hasn't landed yet — keep waiting
                wait = WAIT_TICKS;
                return;
            }
            shotIndex++;
            if (shotIndex >= SHOTS.size()) {
                LOGGER.info("[M418C] rig done - shutting down");
                state = 2 * SHOTS.size() + 3;
                mc.stop();
                return;
            }
            Shot shot = SHOTS.get(shotIndex);
            lookAt(player, shot.camX(), shot.camY(), shot.camZ(), shot.tgtX(), shot.tgtY(), shot.tgtZ());
            mc.options.fov().set(shot.fov());
            wait = CAM_WAIT_TICKS;
        } else {
            // even states: the camera has settled — grab the shot
            Shot shot = SHOTS.get(shotIndex);
            LOGGER.info("[M418C] screenshot {} (cam {},{},{} -> {},{},{}, fov {})", shot.name(), shot.camX(),
                shot.camY(), shot.camZ(), shot.tgtX(), shot.tgtY(), shot.tgtZ(), shot.fov());
            screenshot(mc, "m418c_" + shot.name());
            wait = WAIT_TICKS;
        }
        state++;
    }

    // ---------------------------------------------------------------- scenes

    /** Builds every adjacency scene at once (all shots then just walk a camera around one loaded area). */
    private static void buildScenes(ServerLevel level) {
        int ax = anchor.getX();
        int az = anchor.getZ();

        // A: solo pipe in mid-air — all six side faces unconnected (the pixel-sampling target);
        // plus a 2-high stand pillar 3 south of it for the top-down shot (never orthogonally adjacent to a pipe)
        pipe(level, "items_stone", ax, az);
        level.setBlockAndUpdate(new BlockPos(ax, groundY + 1, az + 3), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(ax, groundY + 2, az + 3), Blocks.STONE.defaultBlockState());
        // B: straight run along Z — every pipe exposes unconnected E/W centre faces
        for (int dz = 6; dz <= 9; dz++) {
            pipe(level, "items_stone", ax, az + dz);
        }
        // C: straight run along X — the classic pipe line (N/S centre faces exposed)
        for (int dx = 4; dx <= 7; dx++) {
            pipe(level, "items_stone", ax + dx, az);
        }
        // D: elbow — corner connects N+E, so its W centre face is an unconnected side face
        pipe(level, "items_stone", ax + 4, az + 6);
        pipe(level, "items_stone", ax + 4, az + 7);
        pipe(level, "items_stone", ax + 5, az + 7);
        // E: pipe against glass (transparent neighbours W+S must not eat the touching faces)
        pipe(level, "items_stone", ax + 10, az);
        level.setBlockAndUpdate(new BlockPos(ax + 9, groundY, az), Blocks.GLASS.defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(ax + 10, groundY, az + 1), Blocks.GLASS.defaultBlockState());
        // F: pipe against solids (stone W, dirt S) — occlusion, never a hole
        pipe(level, "items_stone", ax + 10, az + 5);
        level.setBlockAndUpdate(new BlockPos(ax + 9, groundY, az + 5), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(ax + 10, groundY, az + 6), Blocks.DIRT.defaultBlockState());
        // G: machine sandwich — pump W, tank E
        level.setBlockAndUpdate(new BlockPos(ax + 13, groundY, az), BcFactoryBlocks.PUMP.value().defaultBlockState());
        pipe(level, "items_stone", ax + 14, az);
        level.setBlockAndUpdate(new BlockPos(ax + 15, groundY, az), BcFactoryBlocks.TANK.value().defaultBlockState());
        // H: material spread — one solo pipe per family, air-gapped
        pipe(level, "items_cobblestone", ax + 13, az + 5);
        pipe(level, "items_gold", ax + 15, az + 5);
        pipe(level, "items_diamond", ax + 17, az + 5);
        pipe(level, "items_clay", ax + 19, az + 5);
        // I: fluids + power — filled fluid pipe (translucent column), injected power pipe (core), empty end pipe
        pipe(level, "fluids_stone", ax + 22, az);
        pipe(level, "power_stone", ax + 23, az);
        pipe(level, "fluids_stone", ax + 24, az);
        if (level.getBlockEntity(new BlockPos(ax + 22, groundY, az)) instanceof PipeHolderBlockEntity fluidPipe) {
            boolean filled = fluidPipe.interactWithBucket(new FluidStack(Fluids.WATER, 1000));
            LOGGER.info("[M418C] fluid pipe at dx=22 filled: {} fluid={}", filled, fluidPipe.getFluid());
        }
        injectField(level, new BlockPos(ax + 23, groundY, az), PipeHolderBlockEntity.class, "powerStored", 800);
        // J: dyed trio — colourless control / red / blue items_stone (the M4.18c dye skin layer), air-gapped
        pipe(level, "items_stone", ax + 13, az + 9, null);
        pipe(level, "items_stone", ax + 15, az + 9, DyeColor.RED);
        pipe(level, "items_stone", ax + 17, az + 9, DyeColor.BLUE);

        planShots(ax, az);
        LOGGER.info("[M418C] scenes built: solo pipe, 2 straight runs, elbow, glass/solid/machine neighbours,"
            + " 4 materials, fluid+power pair, dyed trio (none/red/blue)");
    }

    /** The camera walk (anchor-relative in the X/Z plane, feet heights relative to the ground): a face-filling
     * close-up plus a context shot per scene. Every close-up stands <em>on the ground</em> (or on the little stone
     * pillar built in {@link #buildScenes}) so the player neither falls nor clips between teleport and shot. */
    private static void planShots(int ax, int az) {
        // solo pipe: one face-filling close-up per side + a pillar-top view (pillar stands at dz=+3, see scenes)
        shot("solo_south", 0.5, 0.0, 2.3, 0.5, 0.5, 0.5, 30);
        shot("solo_east", 2.3, 0.0, 0.5, 0.5, 0.5, 0.5, 30);
        shot("solo_west", -2.3, 0.0, 0.5, 0.5, 0.5, 0.5, 30);
        shot("solo_north", 0.5, 0.0, -2.3, 0.5, 0.5, 0.5, 30);
        shot("solo_top", 0.5, 3.0, 3.5, 0.5, 0.5, 0.5, 30);
        // straight runs: close-ups of the exposed E/W centre faces + context
        shot("linez_east", 2.2, 0.0, 6.8, 0.5, 0.5, 6.9, 30);
        shot("linez_west", -2.2, 0.0, 8.2, 0.5, 0.5, 8.1, 30);
        shot("linez_wide", -5.0, 0.0, 7.5, 0.5, 0.7, 7.5, 70);
        shot("linex_south", 4.8, 0.0, 2.2, 4.9, 0.5, 0.5, 30);
        shot("linex_wide", 5.5, 0.0, 4.6, 5.5, 0.7, 0.5, 70);
        // elbow: the corner's unconnected W face + a diagonal overview
        shot("elbow_west", 2.6, 0.0, 6.5, 4.5, 0.5, 6.5, 30);
        shot("elbow_diag", 1.8, 0.0, 3.8, 4.5, 0.6, 6.0, 70);
        // glass neighbour: shoot the pipe's W face straight through the glass block
        shot("glass_west", 7.7, 0.0, 0.5, 10.25, 0.45, 0.5, 30);
        shot("glass_wide", 5.8, 0.0, 0.5, 10.25, 0.45, 0.5, 70);
        // solid neighbours: the E face (against air) + the N face + context
        shot("solid_east", 12.6, 0.0, 5.5, 10.5, 0.45, 5.5, 30);
        shot("solid_north", 10.5, 0.0, 3.1, 10.5, 0.5, 5.5, 30);
        // machines: south close-up of the pipe between pump and tank + top context from the tank's roof
        shot("mach_south", 14.5, 0.0, 2.7, 14.5, 0.5, 0.5, 30);
        shot("mach_top", 15.5, 1.0, 0.5, 14.5, 0.6, 0.5, 30);
        // materials: wide spread + a cobblestone close-up
        shot("mats_south", 16.5, 0.0, 8.8, 16.5, 0.4, 5.5, 70);
        shot("mats_close", 13.5, 0.0, 7.4, 13.5, 0.45, 5.5, 30);
        // fluids + power
        shot("fluid_south", 23.5, 0.0, 2.5, 23.5, 0.5, 0.5, 30);
        shot("fluid_wide", 23.5, 0.0, 4.8, 23.5, 0.7, 0.5, 70);
        shot("fluid_east", 26.4, 0.0, 0.5, 24.3, 0.45, 0.5, 30);
        shot("power_close", 21.9, 0.0, 0.5, 23.5, 0.45, 0.5, 30);
        // dyed trio (none/red/blue at dx=13/15/17, dz=9): south close-ups + a wide
        shot("dye_none", 13.5, 0.0, 11.8, 13.5, 0.5, 9.5, 30);
        shot("dye_red", 15.5, 0.0, 11.8, 15.5, 0.5, 9.5, 30);
        shot("dye_blue", 17.5, 0.0, 11.8, 17.5, 0.5, 9.5, 30);
        shot("dye_wide", 15.5, 0.0, 14.0, 15.5, 0.5, 9.5, 70);
    }

    private static void shot(String name, double dx, double y, double dz, double tx, double ty, double tz, int fov) {
        SHOTS.add(new Shot(name, anchor.getX() + dx, groundY + y, anchor.getZ() + dz,
            anchor.getX() + tx, groundY + ty, anchor.getZ() + tz, fov));
    }

    /**
     * Reflect-sets one private long field on a block entity and pushes the BE to clients (the M4.6 rig pattern; dev-rig
     * evidence only, which is why this bypasses the normal transfer APIs).
     */
    private static void injectField(
        ServerLevel level, BlockPos pos, Class<? extends BlockEntity> beClass, String fieldName, long value
    ) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!beClass.isInstance(be)) {
            LOGGER.error("[M418C] injectField {}: expected {} got {}", pos, beClass.getSimpleName(), be);
            return;
        }
        try {
            Field field = beClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.setLong(be, value);
        } catch (ReflectiveOperationException exception) {
            LOGGER.error("[M418C] injectField {} failed", pos, exception);
            return;
        }
        BlockState blockState = level.getBlockState(pos);
        level.sendBlockUpdated(pos, blockState, blockState, Block.UPDATE_CLIENTS);
        LOGGER.info("[M418C] injected {} into {} at {}", value, fieldName, pos);
    }

    private static void pipe(ServerLevel level, String familyStem, int x, int z) {
        pipe(level, familyStem, x, z, null);
    }

    /** Places one pipe of the family, optionally dyed ({@code null} = the colourless variant). */
    private static void pipe(ServerLevel level, String familyStem, int x, int z, DyeColor colour) {
        Family family = BcPipeFamilies.byStem(familyStem);
        BlockPos pos = new BlockPos(x, groundY, z);
        level.setBlockAndUpdate(pos, BcTransportBlocks.PIPE_HOLDER.value().defaultBlockState());
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof PipeHolderBlockEntity pipeBe)) {
            LOGGER.error("[M418C] pipe placement broken at {}: block={} be={}", pos,
                level.getBlockState(pos).getBlock(), be);
            return;
        }
        pipeBe.setPipe(family, colour);
    }

    // ---------------------------------------------------------------- shared helpers

    /** Marshals one chunk of world work onto the server thread (the M4.6 rig discipline; see class javadoc). */
    private static void onServer(Minecraft mc, Runnable task) {
        mc.getSingleplayerServer().execute(task);
    }

    /** Teleports the rig player to the vantage and pins the view onto the target. The pitch is computed from the
     * player's <em>eye</em> (feet + 1.62), because that is what the first-person camera renders through. */
    private static void lookAt(ServerPlayer player, double x, double y, double z, double tx, double ty, double tz) {
        double dx = tx - x;
        double dy = ty - (y + PLAYER_EYE_HEIGHT);
        double dz = tz - z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        viewYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        viewPitch = (float) -Math.toDegrees(Math.atan2(dy, horiz));
        player.teleportTo((ServerLevel) player.level(), x, y, z, Set.of(), viewYaw, viewPitch, false);
    }

    private static void screenshot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), 1,
            (Component component) -> LOGGER.info("[M418C] saved {}: {}", name, component.getString()));
    }

    private M418CPipeSideProbe() {
    }
}
