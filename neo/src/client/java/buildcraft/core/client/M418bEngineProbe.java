/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import buildcraft.core.BcBlocks;
import buildcraft.core.BuildCraftCore;
import buildcraft.core.block.EngineBlock;
import buildcraft.core.block.StoneEngineBlock;
import buildcraft.core.blockentity.EngineBlockEntity;
import buildcraft.core.blockentity.EnumPowerStage;
import buildcraft.core.blockentity.StoneEngineBlockEntity;
import buildcraft.core.client.render.BcEngineModels;
import buildcraft.energy.BcEnergyBlocks;
import buildcraft.lib.client.render.BcQuad;
import buildcraft.lib.client.render.BcVertex;

/**
 * M4.18b in-game evidence rig for the user-reported "engine colour rendering is wrong" bug (the
 * {@link M47MachineSmokeProbe}-style client-tick script, aimed at the six engines): builds one row of all six engines
 * (stone, wood, creative, iron, rf, mj_dynamo) on the quickPlay world's ground, fuels five of them (redstone block
 * beside the creative one) so the trunk stages sync, then walks a camera around every engine &mdash; front (the output
 * face), 45&deg; and side &mdash; dropping one zoomed screenshot per angle plus one wide row shot into
 * {@code screenshots/<phase>/} of the run's game directory.
 *
 * <p>The {@code <phase>} segment comes from {@code -Dbuildcraft.m418bphase=before|after} so the same rig produces the
 * fix-comparison evidence pairs. Every engine's live {@link EnumPowerStage} is logged next to each screenshot name so
 * the pixel sampler can name what it is looking at. The rig is inert unless the run passes
 * {@code -Dbuildcraft.m418bprobe=true} (dev-run evidence only, never active in normal play), and it shuts the game down
 * when done: {@code [M418b]} lines in the log + 19 screenshots are the pass signal.
 */
@EventBusSubscriber(modid = BuildCraftCore.MOD_ID, value = Dist.CLIENT)
public final class M418bEngineProbe {

    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    /** Set (and only set) by the M4.18b evidence run's JVM arguments; keeps the rig inert everywhere else. */
    private static final boolean ENABLED = Boolean.getBoolean("buildcraft.m418bprobe");
    /** Evidence subdirectory under the game dir's screenshots/ ("before" for the bug run, "after" the fix). */
    private static final String PHASE = System.getProperty("buildcraft.m418bphase", "phase");

    /** The engine row, in shot order: block supplier + per-engine fuel behaviour. */
    private enum Engine {
        STONE("stone", true),
        WOOD("wood", true),
        CREATIVE("creative", false),
        IRON("iron", true),
        RF("rf", true),
        MJ_DYNAMO("mj_dynamo", true);

        final String name;
        /** True when the engine needs a coal item inserted; false for the redstone-driven creative engine. */
        final boolean fueled;

        Engine(String name, boolean fueled) {
            this.name = name;
            this.fueled = fueled;
        }
    }

    /** One camera stop: engine index (or -1 for the wide row shot) and the offset from the engine's block pos. */
    private record Stop(int engine, double dx, double dy, double dz, float yaw, float pitch, String shot) {
    }

    private static final List<Stop> STOPS = new ArrayList<>();

    static {
        for (int i = 0; i < Engine.values().length; i++) {
            // FACING = EAST (the trunk pokes +X): front = east of the engine looking west (-X, yaw 90);
            // 45 deg = south-east looking north-west (yaw 135); side = south looking north (-Z, yaw 180).
            STOPS.add(new Stop(i, 3.6, 1.2, 0.0, 90.0F, 8.0F, "front"));
            STOPS.add(new Stop(i, 2.6, 1.5, 2.6, 135.0F, 12.0F, "deg45"));
            STOPS.add(new Stop(i, 0.0, 1.2, 3.6, 180.0F, 8.0F, "side"));
        }
    }

    private static int state = 0;
    private static int wait = 0;
    private static int groundY = -1;
    private static BlockPos anchor = null;
    private static float viewYaw;
    private static float viewPitch;

    /** The shutdown state's number: one build state + one wide state + two states per stop. */
    private static final int DONE_STATE = 2 + STOPS.size() * 2;

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
        if (level == null || state > DONE_STATE) {
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
        // stop layout: state 0 = build; state 1 = wide shot; then per Stop one move state followed by one
        // shoot state (the move state's wait covers the teleport + chunk section upload before the frame);
        // finally the shutdown state.
        int movesStart = 2;
        if (state == 0) {
            onServer(mc, () -> {
                BlockPos spawn = player.blockPosition();
                anchor = new BlockPos(spawn.getX() + 24, 0, spawn.getZ());
                BlockPos probe = new BlockPos(anchor.getX() + 1, 0, anchor.getZ());
                groundY = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, probe).getY();
                mc.options.hideGui = true;
                LOGGER.info("[M418b] rig start: anchor={} groundY={} phase={}", anchor, groundY, PHASE);
                buildEngineRow(level, player);
            });
            dumpBakes();
            // long wait: fuel burn moves the stages along and one sync round trip lands the client state
            wait = 120;
        } else if (state == 1) {
            onServer(mc, () -> logStages(level, "wide shot"));
            screenshot(mc, "wide");
            wait = 20;
        } else if (state >= movesStart && state < movesStart + STOPS.size() * 2) {
            int index = (state - movesStart) / 2;
            boolean isMove = (state - movesStart) % 2 == 0;
            Stop stop = STOPS.get(index);
            if (isMove) {
                if (stop.shot().equals("front")) {
                    // first stop of an engine: log its live stage next to the screenshots that follow
                    onServer(mc, () -> logStages(level, stop.shot() + " of " + Engine.values()[stop.engine()].name));
                }
                setView(player, stop);
                wait = 12;
            } else {
                screenshot(mc, Engine.values()[stop.engine()].name + "_" + stop.shot());
                wait = 4;
            }
        } else {
            LOGGER.info("[M418b] rig done (phase {}) - shutting down", PHASE);
            state = movesStart + STOPS.size() * 2 + 1;
            mc.stop();
            return;
        }
        state++;
    }

    /** Marshals one chunk of world work onto the server thread (the M46 rig discipline). */
    private static void onServer(Minecraft mc, Runnable task) {
        mc.getSingleplayerServer().execute(task);
    }

    /** Bakes every engine model once and logs the full quad truth (resolved sprite, face, per-vertex colour/light,
     * positions) so the pixel evidence can be traced back to exact model data. Client-thread only (the bake reads the
     * resource manager and the atlas manager). */
    private static void dumpBakes() {
        String[][] models = {
            {"stone", "buildcraftenergy:models/tile/engine_stone"},//
            {"wood", "buildcraftcore:models/tile/engine_redstone"},//
            {"creative", "buildcraftcore:models/tile/engine_creative"},//
            {"iron", "buildcraftenergy:models/tile/engine_iron"},//
            {"rf", "buildcraftenergy:models/tile/engine_rf"},//
            {"mj_dynamo", "buildcraftenergy:models/tile/mj_dynamo"}//
        };
        for (String[] model : models) {
            Identifier id = Identifier.parse(model[1]);
            List<BcEngineModels.TexturedQuad> quads = BcEngineModels.bake(id, 0.35f,
                EnumPowerStage.GREEN, Direction.EAST, 15728880);
            LOGGER.info("[M418b] bake {}: {} quads", model[0], quads.size());
            for (BcEngineModels.TexturedQuad textured : quads) {
                BcQuad quad = textured.quad();
                Identifier sprite = Minecraft.getInstance().getAtlasManager().get(textured.sprite()).contents().name();
                LOGGER.info("[M418b]   quad {} sprite={} face={} shade={} color={} light={} v0={} v1={} v2={} v3={}",
                    model[0], sprite, quad.face(), quad.shade(),
                    String.format("%08x", quad.v0().color()), quad.v0().light(),//
                    pos(quad.v0()), pos(quad.v1()), pos(quad.v2()), pos(quad.v3()));
            }
        }
    }

    private static String pos(BcVertex v) {
        return String.format("(%.2f,%.2f,%.2f uv %.3f,%.3f)", v.position().x(), v.position().y(), v.position().z(),
            v.u(), v.v());
    }

    // ---------------------------------------------------------------- scenes

    private static void buildEngineRow(ServerLevel level, ServerPlayer spectator) {
        Engine[] engines = Engine.values();
        // creative mode: the camera walks at eye height next to the engines and must never die on a clip
        spectator.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        for (int i = 0; i < engines.length; i++) {
            Engine engine = engines[i];
            BlockPos pos = pos(i, 0);
            // clear air around the engine so nothing occludes the camera's view
            for (int dx = -1; dx <= 2; dx++) {
                for (int dy = 0; dy <= 2; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        level.removeBlock(pos.offset(dx, dy, dz), false);
                    }
                }
            }
            place(level, engine, pos);
            // Deterministic light for the evidence shots: this template world's server-side sky light is 0 at
            // ground level (and the client's own skylight flips between 0 and 15 per chunk), which made the BER's
            // world-light input random between runs. A light source under each engine gives every phase the same
            // stable block light through the normal server->client sync.
            level.setBlockAndUpdate(pos.below(), Blocks.GLOWSTONE.defaultBlockState());
        }
        LOGGER.info("[M418b] engine row built: 6 engines facing east at groundY={}", groundY);
    }

    private static void place(ServerLevel level, Engine engine, BlockPos pos) {
        switch (engine) {
            case STONE -> {
                level.setBlockAndUpdate(pos, BcBlocks.ENGINE_STONE.value().defaultBlockState()
                    .setValue(StoneEngineBlock.FACING, Direction.EAST));
                if (level.getBlockEntity(pos) instanceof StoneEngineBlockEntity stone) {
                    stone.insertFuel(new ItemStack(Items.COAL), level.fuelValues());
                }
            }
            case CREATIVE -> {
                level.setBlockAndUpdate(pos, BcBlocks.ENGINE_CREATIVE.value().defaultBlockState()
                    .setValue(EngineBlock.FACING, Direction.EAST));
                // the creative engine pumps on a neighbour signal: park a redstone block on its north side
                level.setBlockAndUpdate(pos.north(), Blocks.REDSTONE_BLOCK.defaultBlockState());
            }
            case MJ_DYNAMO -> {
                level.setBlockAndUpdate(pos, BcEnergyBlocks.MJ_DYNAMO.value().defaultBlockState()
                    .setValue(EngineBlock.FACING, Direction.EAST));
                if (level.getBlockEntity(pos) instanceof EngineBlockEntity dynamo) {
                    dynamo.insertFuel(new ItemStack(Items.COAL), level.fuelValues());
                }
            }
            default -> {
                var block = switch (engine) {
                    case WOOD -> BcBlocks.ENGINE_WOOD;
                    case IRON -> BcBlocks.ENGINE_IRON;
                    case RF -> BcBlocks.ENGINE_RF;
                    default -> throw new IllegalStateException("unhandled engine " + engine);
                };
                level.setBlockAndUpdate(pos, block.value().defaultBlockState()
                    .setValue(EngineBlock.FACING, Direction.EAST));
                if (level.getBlockEntity(pos) instanceof EngineBlockEntity generic) {
                    generic.insertFuel(new ItemStack(Items.COAL), level.fuelValues());
                }
            }
        }
    }

    /** Logs every engine's live stage/progress so each screenshot's expected colours are on record. */
    private static void logStages(ServerLevel level, String when) {
        Engine[] engines = Engine.values();
        for (int i = 0; i < engines.length; i++) {
            BlockPos pos = pos(i, 0);
            net.minecraft.world.level.LightLayer sky = net.minecraft.world.level.LightLayer.SKY;
            net.minecraft.world.level.LightLayer block = net.minecraft.world.level.LightLayer.BLOCK;
            LOGGER.info("[M418b.light] {} {} pos={} server: sky={} block={} loaded={}", when, engines[i].name, pos,
                level.getBrightness(sky, pos), level.getBrightness(block, pos), level.isLoaded(pos));
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                LOGGER.info("[M418b.light] {} {} pos={} client: sky={} block={}", when, engines[i].name, pos,
                    mc.level.getBrightness(sky, pos), mc.level.getBrightness(block, pos));
            }
            if (level.getBlockEntity(pos) instanceof EngineBlockEntity engine) {
                LOGGER.info("[M418b] {} {}: stage={} progress={} pumping={}", when, engines[i].name,
                    engine.getPowerStage(), String.format("%.2f", engine.getProgressClient(0.0f)),
                    engine.isPumping());
            } else if (level.getBlockEntity(pos) instanceof StoneEngineBlockEntity stone) {
                LOGGER.info("[M418b] {} {}: stage={} progress={} pumping={}", when, engines[i].name,
                    stone.getPowerStage(), String.format("%.2f", stone.getProgressClient(0.0f)),
                    stone.isPumping());
            } else {
                LOGGER.error("[M418b] {} {}: no engine BE at {} (block={})", when, engines[i].name, pos,
                    level.getBlockState(pos).getBlock());
            }
        }
    }

    // ---------------------------------------------------------------- shared helpers

    private static BlockPos pos(int x, int z) {
        return new BlockPos(anchor.getX() + x * 3, groundY, anchor.getZ() + z);
    }

    private static void setView(ServerPlayer player, Stop stop) {
        BlockPos base = stop.engine() < 0 ? pos(2, 0) : pos(stop.engine(), 0);
        viewYaw = stop.yaw();
        viewPitch = stop.pitch();
        player.teleportTo((ServerLevel) player.level(),//
            base.getX() + 0.5 + stop.dx(), base.getY() + stop.dy(), base.getZ() + 0.5 + stop.dz(),//
            Set.of(), viewYaw, viewPitch, false);
    }

    private static void screenshot(Minecraft mc, String name) {
        String file = PHASE + "/" + name + ".png";
        Screenshot.grab(mc.gameDirectory, file, mc.getMainRenderTarget(), 1,
            (Component component) -> LOGGER.info("[M418b] screenshot {}: {}", file, component.getString()));
    }
}
