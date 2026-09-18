/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.transport.client;

import java.lang.reflect.Field;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import buildcraft.core.BcBlocks;
import buildcraft.core.blockentity.KinesisPipeBlockEntity;
import buildcraft.lib.datacomponent.gate.EnumGateLogic;
import buildcraft.lib.datacomponent.gate.EnumGateMaterial;
import buildcraft.lib.datacomponent.gate.EnumGateModifier;
import buildcraft.lib.datacomponent.gate.GateVariantData;
import buildcraft.transport.BcTransportBlocks;
import buildcraft.transport.BuildCraftTransport;
import buildcraft.transport.blockentity.PipeHolderBlockEntity;
import buildcraft.transport.client.PipeItemFlowClient;
import buildcraft.transport.pipe.BcPipeFamilies;
import buildcraft.transport.pipe.BcPipeFamilies.Family;

/**
 * M4.6 in-game evidence rig (the {@code M45LaserSmokeProbe} pattern, one milestone later): a client-tick script that
 * drives one quickPlay world through every pipe visual this task ships and drops a screenshot per scene into
 * {@code screenshots/}:
 * <ol>
 * <li>the item-pipe scene: a 4-straight run with an elbow into an end chest, a T-branch over a second chest, a
 * hopper feeding items in from above and a blocker plug on the straight run &mdash; two screenshots (layout, then the
 * flow frame with travelling items visible);</li>
 * <li>the fluid-pipe scene: three stone fluid pipes over water sources, the middle one filled through
 * {@link PipeHolderBlockEntity#interactWithBucket};</li>
 * <li>the power-pipe scene: two stone power pipes feeding a wooden kinesis pipe with an iron gate. The energy buffer is
 * reflect-injected ({@code powerStored}/{@code energyStored}) instead of running live engines &mdash; the engine slice
 * belongs to another milestone's evidence and its jsonbc model loader is not this task's to exercise.</li>
 * </ol>
 *
 * <p>Everything runs through the integrated server (block placement, gate attach, bucket fill, energy injection), so
 * what the screenshots show is the update-tag sync + renderer pipeline exactly as a player would see it. Exactly like
 * the M45 rig, every ServerLevel touch is marshalled onto the server thread through
 * {@link MinecraftServer#execute(Runnable)} &mdash; the rig's tick fires on the render thread, and an off-thread
 * {@code setBlock} races the server's chunk access: the block lands but the block entity is lost. The rig is inert
 * unless the run passes {@code -Dbuildcraft.m46probe=true} (dev-run evidence only, never active in normal play), and
 * it shuts the game down when done, so the run doubles as the smoke pass/fail signal: {@code [M46]} lines in the log
 * + 4 screenshots.
 */
@EventBusSubscriber(modid = BuildCraftTransport.MOD_ID, value = Dist.CLIENT)
public final class M46PipeSmokeProbe {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Set (and only set) by the M4.6 evidence run's JVM arguments; keeps the rig inert everywhere else. */
    private static final boolean ENABLED = Boolean.getBoolean("buildcraft.m46probe");

    /** Base wait between phases (ticks) &mdash; comfortably more than a couple of sync round trips. */
    private static final int WAIT_TICKS = 30;

    private static int state = 0;
    private static int wait = 0;
    private static int groundY = -1;
    private static BlockPos anchor = null;
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
        if (level == null || state > 7) {
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
                BlockPos spawn = player.blockPosition();
                anchor = new BlockPos(spawn.getX() + 24, 0, spawn.getZ());
                BlockPos scene = new BlockPos(anchor.getX() + 1, 0, anchor.getZ());
                groundY = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, scene).getY();
                mc.options.hideGui = true;
                LOGGER.info("[M46] rig start: anchor={} groundY={}", anchor, groundY);
                buildItemScene(level);
                LOGGER.info("[M46] item scene built: 7 stone item pipes (straight + elbow + tee branch),"
                    + " end chest, branch chest, hopper feed + source chest, blocker plug on the run");
                // teleport FIRST, then let a couple of sync rounds render before the layout shot
                setView(player, anchor.getX() + 1.5, groundY + 4.5, anchor.getZ() - 7.0, 22.0F);
            });
            case 1 -> {
                screenshot(mc, "m46_items_layout");
            }
            case 2 -> {
                // same camera, one flow frame later: travelling items should be visible mid-pipe
                onServer(mc, () -> logFlow(level));
                screenshot(mc, "m46_items_flow");
            }
            case 3 -> onServer(mc, () -> {
                buildFluidScene(level);
                LOGGER.info("[M46] fluid scene built: 3 stone fluid pipes, water sources below ends,"
                    + " middle pipe filled through interactWithBucket");
                buildPowerScene(level);
                LOGGER.info("[M46] power scene built: 2 stone power pipes (injected 800/400 uMJ)"
                    + " -> kinesis wood pipe with iron gate (injected 600 uMJ)");
                setView(player, anchor.getX() + 9.5, groundY + 4.0, anchor.getZ() - 7.0, 25.0F);
            });
            case 4 -> {
                onServer(mc, () -> logFluids(level));
                screenshot(mc, "m46_fluids");
            }
            case 5 -> {
                onServer(mc, () -> setView(player, anchor.getX() + 16.5, groundY + 4.0, anchor.getZ() - 7.0, 25.0F));
                wait = 2 * WAIT_TICKS;
            }
            case 6 -> {
                onServer(mc, () -> logPower(level));
                screenshot(mc, "m46_power");
                LOGGER.info("[M46] rig done - shutting down");
            }
            default -> {
                state = 8;
                mc.stop();
                return;
            }
        }
        state++;
        wait = 2 * WAIT_TICKS;
    }

    /** Marshals one chunk of world work onto the server thread (the M45 rig discipline; see class javadoc). */
    private static void onServer(Minecraft mc, Runnable task) {
        mc.getSingleplayerServer().execute(task);
    }

    /** The item scene: {@code P0..P3} straight along x (P1 is the tee hub), elbow {@code E1,E2} down to the end
     * chest, branch pipe {@code T1} down to the branch chest, hopper + source chest above {@code P0}. */
    private static void buildItemScene(ServerLevel level) {
        Family items = requireFamily("items_stone");
        int x = anchor.getX();
        int z = anchor.getZ();
        pipe(level, items, x, z);
        pipe(level, items, x + 1, z);
        pipe(level, items, x + 2, z);
        pipe(level, items, x + 3, z);
        pipe(level, items, x + 3, z + 1);
        pipe(level, items, x + 3, z + 2);
        pipe(level, items, x + 1, z + 1);
        chest(level, x + 3, z + 3);
        chest(level, x + 1, z + 2);
        // the feed: source chest on top, hopper below it (pulls from the chest, pushes into P0's up inbox)
        level.setBlockAndUpdate(new BlockPos(x, groundY + 1, z), Blocks.HOPPER.defaultBlockState());
        chestAt(level, new BlockPos(x, groundY + 2, z));
        fillChest(level, new BlockPos(x, groundY + 2, z));
        plug(level, x + 2, z);
    }

    private static void buildFluidScene(ServerLevel level) {
        Family fluids = requireFamily("fluids_stone");
        int x = anchor.getX() + 8;
        int z = anchor.getZ();
        pipe(level, fluids, x, z);
        pipe(level, fluids, x + 1, z);
        pipe(level, fluids, x + 2, z);
        level.setBlockAndUpdate(new BlockPos(x, groundY - 1, z), Blocks.WATER.defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(x + 2, groundY - 1, z), Blocks.WATER.defaultBlockState());
        BlockPos middle = new BlockPos(x + 1, groundY, z);
        if (level.getBlockEntity(middle) instanceof PipeHolderBlockEntity pipe) {
            boolean filled = pipe.interactWithBucket(new FluidStack(Fluids.WATER, 1000));
            LOGGER.info("[M46] middle fluid pipe bucket fill: {} fluid={}", filled, pipe.getFluid());
        }
    }

    /** Two power pipes into a gated kinesis pipe; the energy buffers are reflect-injected (see class javadoc). */
    private static void buildPowerScene(ServerLevel level) {
        Family power = requireFamily("power_stone");
        int x = anchor.getX() + 14;
        int z = anchor.getZ();
        pipe(level, power, x, z);
        pipe(level, power, x + 1, z);
        BlockPos kinesisPos = new BlockPos(x + 2, groundY, z);
        level.setBlockAndUpdate(kinesisPos, BcBlocks.PIPE_KINESIS_WOOD.value().defaultBlockState());
        if (level.getBlockEntity(kinesisPos) instanceof KinesisPipeBlockEntity kinesis) {
            boolean gated = kinesis.attachGate(Direction.UP, new GateVariantData(EnumGateLogic.AND,
                EnumGateMaterial.IRON, EnumGateModifier.NO_MODIFIER));
            LOGGER.info("[M46] kinesis gate attached: {}", gated);
        }
        injectField(level, new BlockPos(x, groundY, z), PipeHolderBlockEntity.class, "powerStored", 800);
        injectField(level, new BlockPos(x + 1, groundY, z), PipeHolderBlockEntity.class, "powerStored", 400);
        injectField(level, kinesisPos, KinesisPipeBlockEntity.class, "energyStored", 600);
    }

    /**
     * Reflect-sets one private long field on a block entity and pushes the BE to clients (the renderer reads these
     * buffers off the synced tag; dev-rig evidence only, which is why this bypasses the normal transfer APIs).
     */
    private static void injectField(
        ServerLevel level, BlockPos pos, Class<? extends BlockEntity> beClass, String fieldName, long value
    ) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!beClass.isInstance(be)) {
            LOGGER.error("[M46] injectField {}: expected {} got {}", pos, beClass.getSimpleName(), be);
            return;
        }
        try {
            Field field = beClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.setLong(be, value);
        } catch (ReflectiveOperationException exception) {
            LOGGER.error("[M46] injectField {} failed", pos, exception);
            return;
        }
        BlockState blockState = level.getBlockState(pos);
        level.sendBlockUpdated(pos, blockState, blockState, Block.UPDATE_CLIENTS);
        LOGGER.info("[M46] injected {} into {} at {}", value, fieldName, pos);
    }

    private static void pipe(ServerLevel level, Family family, int x, int z) {
        BlockPos pos = new BlockPos(x, groundY, z);
        boolean placed = level.setBlockAndUpdate(pos, BcTransportBlocks.PIPE_HOLDER.value().defaultBlockState());
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof PipeHolderBlockEntity pipeBe)) {
            LOGGER.error("[M46] pipe placement broken at {}: placed={} block={} be={}", pos, placed,
                level.getBlockState(pos).getBlock(), be);
            return;
        }
        pipeBe.setPipe(family, null);
    }

    private static void plug(ServerLevel level, int x, int z) {
        BlockPos pos = new BlockPos(x, groundY, z);
        if (level.getBlockEntity(pos) instanceof PipeHolderBlockEntity pipe) {
            pipe.attachPlug(Direction.UP, PipeHolderBlockEntity.PLUG_BLOCKER);
        }
    }

    private static void chest(ServerLevel level, int x, int z) {
        chestAt(level, new BlockPos(x, groundY, z));
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

    private static void setView(ServerPlayer player, double x, double y, double z, float pitch) {
        viewYaw = 0.0F;
        viewPitch = pitch;
        player.teleportTo((ServerLevel) player.level(), x, y, z, Set.of(), viewYaw, viewPitch, false);
    }

    /**
     * Flow evidence: per pipe, what the server actually has there (block + block entity class + connections + inbox)
     * and what the client mirror carries &mdash; any mismatch localises the sync break.
     */
    private static void logFlow(ServerLevel level) {
        int x = anchor.getX();
        int z = anchor.getZ();
        for (int i = 0; i < 4; i++) {
            BlockPos pos = new BlockPos(x + i, groundY, z);
            BlockEntity be = level.getBlockEntity(pos);
            LOGGER.info("[M46] item pipe {}: server block={} be={} | client travelling items = {}", pos,
                level.getBlockState(pos).getBlock(), be == null ? "null" : be.getClass().getSimpleName(),
                PipeItemFlowClient.get(pos).size());
            if (be instanceof PipeHolderBlockEntity pipe) {
                LOGGER.info("[M46]   connections={} family={} upInbox={}", pipe.connections, pipe.getFamily(),
                    pipe.getInbox(Direction.UP).getAmountAsLong(0));
            }
        }
    }

    private static void logFluids(ServerLevel level) {
        int x = anchor.getX() + 8;
        for (int i = 0; i < 3; i++) {
            BlockPos pos = new BlockPos(x + i, groundY, z());
            if (level.getBlockEntity(pos) instanceof PipeHolderBlockEntity pipe) {
                LOGGER.info("[M46] fluid pipe {}: fluid={} connections={}", pos, pipe.getFluid(), pipe.connections);
            } else {
                LOGGER.error("[M46] fluid pipe {}: no pipe BE (block={})", pos, level.getBlockState(pos).getBlock());
            }
        }
    }

    /** The fluid scene z (kept as a function so both callers agree). */
    private static int z() {
        return anchor.getZ();
    }

    private static void logPower(ServerLevel level) {
        int x = anchor.getX() + 14;
        for (int i = 0; i <= 1; i++) {
            BlockPos pos = new BlockPos(x + i, groundY, z());
            if (level.getBlockEntity(pos) instanceof PipeHolderBlockEntity pipe) {
                LOGGER.info("[M46] power pipe {}: stored={}uMJ connections={}", pos, pipe.getPowerStored(),
                    pipe.connections);
            } else {
                LOGGER.error("[M46] power pipe {}: no pipe BE (block={})", pos, level.getBlockState(pos).getBlock());
            }
        }
        BlockPos kinesisPos = new BlockPos(x + 2, groundY, z());
        if (level.getBlockEntity(kinesisPos) instanceof KinesisPipeBlockEntity kinesis) {
            LOGGER.info("[M46] kinesis pipe {}: stored={}uMJ gate={} gateOn={}", kinesisPos, kinesis.getEnergyStored(),
                kinesis.getGate() != null, kinesis.getGate() != null && kinesis.getGate().isOn());
        }
    }

    private static void screenshot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), 1,
            (Component component) -> LOGGER.info("[M46] screenshot {}: {}", name, component.getString()));
    }

    private M46PipeSmokeProbe() {
    }
}
