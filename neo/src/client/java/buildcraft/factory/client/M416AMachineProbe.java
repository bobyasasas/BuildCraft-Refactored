/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory.client;

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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import buildcraft.core.BcBlocks;
import buildcraft.core.block.StoneEngineBlock;
import buildcraft.core.blockentity.StoneEngineBlockEntity;
import buildcraft.factory.BcFactoryBlocks;
import buildcraft.factory.BuildCraftFactory;
import buildcraft.factory.blockentity.AutoworkbenchItemBlockEntity;
import buildcraft.factory.blockentity.ChuteBlockEntity;
import buildcraft.factory.blockentity.FloodGateBlockEntity;
import buildcraft.factory.blockentity.MiningWellBlockEntity;
import buildcraft.transport.BcTransportBlocks;
import buildcraft.transport.blockentity.PipeHolderBlockEntity;
import buildcraft.transport.pipe.BcPipeFamilies;
import buildcraft.transport.pipe.BcPipeFamilies.Family;

/**
 * M4.16a in-game evidence rig (the {@code M47MachineSmokeProbe} pattern, one milestone later): a client-tick script
 * that drives one quickPlay world through the four factory machines this task ships and drops a screenshot per scene
 * into {@code screenshots/}:
 * <ol>
 * <li>the auto workbench scene: 16 oak logs pushed into the grid through the real item capability; the legacy pacing
 * self-charges 200 ticks per craft and moves log&rarr;planks results into the output slot;</li>
 * <li>the chute scenes: an upper chest's items pass through the chute into a lower chest, and (second scene) into a
 * transport item pipe whose face inbox forwards them to a side chest;</li>
 * <li>the flood gate scene: 2 buckets of water poured into the tank through the fluid capability; the gate raises a
 * source column above itself (one bucket per source block);</li>
 * <li>the mining well scenes, twice: a fueled stone engine &rarr; kinesis pipe &rarr; mining well REAL energy chain
 * (no injected buffers); well A ejects its drops through the item capability into a chest on top, well B pops them
 * into the world (the entity fallback).</li>
 * </ol>
 *
 * <p>Everything runs through the integrated server; every ServerLevel touch is marshalled onto the server thread
 * through {@link MinecraftServer#execute} (the rig's tick fires on the render thread). The rig is inert unless the run
 * passes {@code -Dbuildcraft.m416aprobe=true} (dev-run evidence only, never active in normal play), and it shuts the
 * game down when done: {@code [M416]} lines in the log + 6 screenshots are the pass signal.
 */
@EventBusSubscriber(modid = BuildCraftFactory.MOD_ID, value = Dist.CLIENT)
public final class M416AMachineProbe {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Set (and only set) by the M4.16a evidence run's JVM arguments; keeps the rig inert everywhere else. */
    private static final boolean ENABLED = Boolean.getBoolean("buildcraft.m416aprobe");

    /** Base wait between phases (ticks) — comfortably more than a couple of sync round trips. */
    private static final int WAIT_TICKS = 30;

    private static int state = 0;
    private static int wait = 0;
    private static int groundY = -1;
    private static BlockPos anchor = null;
    /** View yaw/pitch to pin every tick (per-scene camera). */
    private static float viewYaw;
    private static float viewPitch;
    /** The upper chest's starting load (the chute evidence baseline). */
    private static int chuteChestStart = -1;

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
        if (level == null || state > 16) {
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
                LOGGER.info("[M416] rig start: anchor={} groundY={}", anchor, groundY);
                buildAll(level);
            });
            case 1 -> {
                setView(player, anchor.getX() + 8.0, groundY + 5.0, anchor.getZ() - 8.0, 0.0F, 35.0F);
                wait = 2 * WAIT_TICKS;
            }
            case 2 -> screenshot(mc, "m416_layout");
            case 3 -> onServer(mc, () -> {
                logChutes(level);
                logMiningWell(level, 10, "A"); // early soak sample: the drill depth increments across states
                setView(player, anchor.getX() + 3.5, groundY + 3.0, anchor.getZ() - 6.0, 0.0F, 30.0F);
            });
            case 4 -> screenshot(mc, "m416_chute");
            case 5 -> onServer(mc, () -> {
                logFloodGate(level);
                logMiningWell(level, 10, "A"); // mid soak sample
                setView(player, anchor.getX() + 6.5, groundY + 3.0, anchor.getZ() - 4.5, 0.0F, 35.0F);
            });
            case 6 -> screenshot(mc, "m416_flood_gate");
            case 7 -> onServer(mc, () -> {
                logMiningWell(level, 10, "A");
                setView(player, anchor.getX() + 10.5, groundY + 2.5, anchor.getZ() - 5.0, 0.0F, 30.0F);
                wait = 8 * WAIT_TICKS; // soak: the engine-fed drill eats through the topsoil
            });
            case 8 -> onServer(mc, () -> {
                logMiningWell(level, 10, "A");
                setView(player, anchor.getX() + 11.5, groundY + 1.5, anchor.getZ() - 3.5, 0.0F, 25.0F);
            });
            case 9 -> screenshot(mc, "m416_mining_well");
            case 10 -> onServer(mc, () -> {
                // wide: the well block hides its own shaft, the free pops are the visible half of this scene
                setView(player, anchor.getX() + 15.5, groundY + 2.5, anchor.getZ() - 7.0, 0.0F, 25.0F);
                wait = 8 * WAIT_TICKS; // soak well B while the camera is there
            });
            case 11 -> onServer(mc, () -> {
                logMiningWell(level, 14, "B");
                setView(player, anchor.getX() + 15.0, groundY + 1.5, anchor.getZ() - 4.0, 0.0F, 20.0F);
            });
            case 12 -> screenshot(mc, "m416_mining_well_free");
            case 13 -> onServer(mc, () -> {
                logAutoworkbench(level);
                setView(player, anchor.getX() + 0.5, groundY + 2.0, anchor.getZ() - 3.5, 0.0F, 30.0F);
            });
            case 14 -> screenshot(mc, "m416_autoworkbench");
            case 15 -> {
                onServer(mc, () -> LOGGER.info("[M416] rig done - shutting down"));
            }
            default -> {
                state = 16;
                mc.stop();
                return;
            }
        }
        state++;
        wait = WAIT_TICKS;
    }

    /** Marshals one chunk of world work onto the server thread (the M46 rig discipline; see class javadoc). */
    private static void onServer(Minecraft mc, Runnable task) {
        mc.getSingleplayerServer().execute(task);
    }

    // ---------------------------------------------------------------- scenes

    private static void buildAll(ServerLevel level) {
        // clear the strip: two air layers above the surface, plus the three basement cells the scenes need
        for (int x = 0; x <= 16; x++) {
            for (int z = -1; z <= 1; z++) {
                level.setBlockAndUpdate(pos(x, 0, z), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(pos(x, 1, z), Blocks.AIR.defaultBlockState());
            }
        }
        buildAutoworkbenchScene(level);
        buildChuteChestScene(level);
        buildChutePipeScene(level);
        buildFloodGateScene(level);
        buildMiningWellScene(level, 8, true);
        buildMiningWellScene(level, 12, false);
    }

    /** 16 oak logs into the grid through the real item capability (slot 0; the 1x1 log&rarr;planks recipe). */
    private static void buildAutoworkbenchScene(ServerLevel level) {
        BlockPos pos = pos(0, 0, 0);
        level.setBlockAndUpdate(pos, BcFactoryBlocks.AUTOWORKBENCH_ITEM.value().defaultBlockState());
        if (level.getBlockEntity(pos) instanceof AutoworkbenchItemBlockEntity bench) {
            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = bench.getInv().insert(0, ItemResource.of(new ItemStack(Items.OAK_LOG)), 16, transaction);
                transaction.commit();
                LOGGER.info("[M416] autoworkbench built at {}: capability-inserted {} oak logs into the grid", pos,
                    inserted);
            }
        }
    }

    /** chute A: upper chest (64 dirt + 64 cobblestone) &rarr; chute &rarr; lower chest, all real capability hops. */
    private static void buildChuteChestScene(ServerLevel level) {
        BlockPos upper = pos(2, 1, 0);
        BlockPos lower = pos(2, -1, 0);
        level.setBlockAndUpdate(pos(2, 0, 0), BcFactoryBlocks.CHUTE.value().defaultBlockState());
        level.setBlockAndUpdate(upper, Blocks.CHEST.defaultBlockState());
        level.setBlockAndUpdate(lower, Blocks.CHEST.defaultBlockState());
        if (level.getBlockEntity(upper) instanceof Container chest) {
            chest.setItem(0, new ItemStack(Items.DIRT, 64));
            chest.setItem(1, new ItemStack(Items.COBBLESTONE, 64));
        }
        chuteChestStart = countItems(level, upper);
        LOGGER.info("[M416] chute A built: chest(+)={} items, chute(y0), chest(-) at {}", chuteChestStart, upper);
    }

    /** chute B: upper chest &rarr; chute &rarr; transport item pipe &rarr; (face inbox &rarr; travelling item) &rarr; side chest. */
    private static void buildChutePipeScene(ServerLevel level) {
        BlockPos upper = pos(4, 1, 0);
        BlockPos pipe = pos(4, -1, 0);
        BlockPos side = pos(5, -1, 0);
        level.setBlockAndUpdate(pos(4, 0, 0), BcFactoryBlocks.CHUTE.value().defaultBlockState());
        level.setBlockAndUpdate(upper, Blocks.CHEST.defaultBlockState());
        level.setBlockAndUpdate(pipe, BcTransportBlocks.PIPE_HOLDER.value().defaultBlockState());
        level.setBlockAndUpdate(side, Blocks.CHEST.defaultBlockState());
        if (level.getBlockEntity(upper) instanceof Container chest) {
            chest.setItem(0, new ItemStack(Items.REDSTONE, 64));
        }
        Family items = BcPipeFamilies.byStem("items_stone");
        if (level.getBlockEntity(pipe) instanceof PipeHolderBlockEntity pipeBe && items != null) {
            pipeBe.setPipe(items, null);
        }
        LOGGER.info("[M416] chute B built: chest(+) at {}, chute(y0), items_stone pipe(-) at {}, side chest at {}",//
            upper, pipe, side);
    }

    /** flood gate: 2 buckets of water through the fluid capability; the gate raises its column above itself. */
    private static void buildFloodGateScene(ServerLevel level) {
        BlockPos pos = pos(6, 0, 0);
        level.setBlockAndUpdate(pos, BcFactoryBlocks.FLOOD_GATE.value().defaultBlockState());
        // containment ring (glass: the side-view evidence shot sees the raised column through it)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                level.setBlockAndUpdate(pos.offset(dx, 1, dz), Blocks.GLASS.defaultBlockState());
                level.setBlockAndUpdate(pos.offset(dx, 2, dz), Blocks.GLASS.defaultBlockState());
            }
        }
        if (level.getBlockEntity(pos) instanceof FloodGateBlockEntity gate) {
            try (Transaction transaction = Transaction.openRoot()) {
                gate.getFluidHandler(null).insert(0, FluidResource.of(Fluids.WATER), 2000, transaction);
                transaction.commit();
            }
            LOGGER.info("[M416] flood gate built at {}: {} mB water in tank (2 sources worth)", pos,
                gate.getFluidHandler(null).getAmountAsLong(0));
        }
    }

    /**
     * mining well scene: fueled stone engine facing east &rarr; kinesis pipe &rarr; mining well (the real M4.9-verdict
     * chain shape); {@code withChest} puts a drop chest on the well (capability path), otherwise drops pop free.
     */
    private static void buildMiningWellScene(ServerLevel level, int x, boolean withChest) {
        BlockPos enginePos = pos(x, 0, 0);
        BlockPos pipePos = pos(x + 1, 0, 0);
        BlockPos wellPos = pos(x + 2, 0, 0);
        level.setBlockAndUpdate(enginePos, BcBlocks.ENGINE_STONE.value().defaultBlockState()
                .setValue(StoneEngineBlock.FACING, Direction.EAST));
        if (level.getBlockEntity(enginePos) instanceof StoneEngineBlockEntity engine) {
            boolean fueled = engine.insertFuel(new ItemStack(Items.COAL), level.fuelValues());
            LOGGER.info("[M416] well {} engine fueled at {}: {} (ignites once the buffer has room)",//
                withChest ? "A" : "B", enginePos, fueled);
        }
        level.setBlockAndUpdate(pipePos, BcBlocks.PIPE_KINESIS_WOOD.value().defaultBlockState());
        level.setBlockAndUpdate(wellPos, BcFactoryBlocks.MINING_WELL.value().defaultBlockState());
        if (withChest) {
            BlockPos chestPos = pos(x + 2, 1, 0);
            level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
            LOGGER.info("[M416] well A drop chest at {} (drops route through the item capability)", chestPos);
        }
    }

    // ---------------------------------------------------------------- evidence logs

    /** The chute evidence: both scenes' item counts before/after plus the moved counter. */
    private static void logChutes(ServerLevel level) {
        BlockPos upperA = pos(2, 1, 0);
        BlockPos lowerA = pos(2, -1, 0);
        BlockPos upperB = pos(4, 1, 0);
        BlockPos sideB = pos(5, -1, 0);
        BlockPos chuteA = pos(2, 0, 0);
        LOGGER.info("[M416] chute A state: start={} now upper={} lower={} moved={}", chuteChestStart,
            countItems(level, upperA), countItems(level, lowerA),
            level.getBlockEntity(chuteA) instanceof ChuteBlockEntity chute ? chute.getMovedTotal() : -1);
        LOGGER.info("[M416] chute B state: now upper={} side={} (via items_stone pipe face inbox)",//
            countItems(level, upperB), countItems(level, sideB));
    }

    /** The flood gate evidence: tank remainder, placed sources and the resulting column. */
    private static void logFloodGate(ServerLevel level) {
        BlockPos pos = pos(6, 0, 0);
        if (level.getBlockEntity(pos) instanceof FloodGateBlockEntity gate) {
            int column = 0;
            while (level.getFluidState(pos.above(1 + column)).isSource()
                    && level.getFluidState(pos.above(1 + column)).getType().isSame(Fluids.WATER)) {
                column++;
            }
            LOGGER.info("[M416] flood gate state: tank={} x {} mB, placed={} source blocks, column height={}",//
                gate.getFluidHandler(null).getResource(0), gate.getFluidHandler(null).getAmountAsLong(0),//
                gate.getPlacedTotal(), column);
        } else {
            LOGGER.error("[M416] flood gate missing at {} (block={})", pos, level.getBlockState(pos).getBlock());
        }
    }

    /** The auto workbench evidence: grid, output and battery after the legacy-paced crafts. */
    private static void logAutoworkbench(ServerLevel level) {
        BlockPos pos = pos(0, 0, 0);
        if (level.getBlockEntity(pos) instanceof AutoworkbenchItemBlockEntity bench) {
            LOGGER.info("[M416] autoworkbench state: grid[0]={} x {} oak logs, output={} x {}, power={} / {} uMJ",//
                bench.getInv().getResource(0), bench.getInv().getAmountAsLong(0),//
                bench.getInv().getResource(9), bench.getInv().getAmountAsLong(9),//
                bench.getPowerStored(), AutoworkbenchItemBlockEntity.POWER_REQUIRED);
        } else {
            LOGGER.error("[M416] autoworkbench missing at {} (block={})", pos, level.getBlockState(pos).getBlock());
        }
    }

    /** The mining well evidence: the real energy chain's battery + the drill progress. {@code wellX} is the well itself. */
    private static void logMiningWell(ServerLevel level, int wellX, String tag) {
        BlockPos wellPos = pos(wellX, 0, 0);
        if (level.getBlockEntity(wellPos) instanceof MiningWellBlockEntity well) {
            String target = well.getCurrentTarget() == null ? "none (complete)" : well.getCurrentTarget().toString();
            LOGGER.info("[M416] mining well {} state: target={} (well y={}), broken={}, battery={} / {} uMJ"
                    + " (totalReceived={})",//
                tag, target, wellPos.getY(), well.getBlocksBroken(), well.getEnergyStored(),
                MiningWellBlockEntity.CAPACITY, well.getTotalReceived());
            if (tag.equals("A")) {
                LOGGER.info("[M416] mining well {} drop chest now holds {} items (started empty)", tag,
                    countItems(level, pos(wellX, 1, 0)));
            }
        } else {
            LOGGER.error("[M416] mining well {} missing at {} (block={})", tag, wellPos,
                level.getBlockState(wellPos).getBlock());
        }
    }

    // ---------------------------------------------------------------- shared helpers

    /** Total item count across a container's capability view (0 when absent). */
    private static int countItems(ServerLevel level, BlockPos pos) {
        ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, pos, Direction.DOWN);
        if (handler == null) {
            return 0;
        }
        int total = 0;
        for (int i = 0; i < handler.size(); i++) {
            total += handler.getAmountAsLong(i);
        }
        return total;
    }

    private static BlockPos pos(int x, int y, int z) {
        return new BlockPos(anchor.getX() + x, groundY + y, anchor.getZ() + z);
    }

    private static void setView(ServerPlayer player, double x, double y, double z, float yaw, float pitch) {
        viewYaw = yaw;
        viewPitch = pitch;
        player.teleportTo((ServerLevel) player.level(), x, y, z, Set.of(), viewYaw, viewPitch, false);
    }

    private static void screenshot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), 1,
            (Component component) -> LOGGER.info("[M416] screenshot {}: {}", name, component.getString()));
    }

    private M416AMachineProbe() {
    }
}
