/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.silicon.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import buildcraft.robotics.BcRoboticsItems;
import buildcraft.silicon.BcSiliconBlocks;
import buildcraft.silicon.BuildCraftSilicon;
import buildcraft.silicon.blockentity.ChargingTableBlockEntity;
import buildcraft.silicon.blockentity.ProgrammingTableBlockEntity;

/**
 * M4.17a in-game evidence rig (the {@code M416bSiliconSmokeProbe} pattern, one milestone later): a client-tick script
 * driving one quickPlay world through the two M4.17 silicon behaviours, everything inserted and read back through the
 * real 26.1.2 item capability (no direct field shortcuts in the seed paths):
 * <ol>
 * <li>scene build: a laser + charging table pair (seeded with one robot_builder, the M4.17 chargeable item) and a
 * laser + programming table pair (seeded with one board_robot_empty), every laser battery filled through
 * {@code MjReceiver#receivePower};</li>
 * <li>charging evidence: slot + {@code minecraft:custom_data} NBT readouts at insert, mid-charge and full (the
 * legacy-paced 500,000 µMJ robot battery takes the legacy 1,250 ticks at the slice laser cap) with screenshots;</li>
 * <li>programming evidence: before (input board, empty output) and after (input consumed, output holds the
 * auto-picked programmed board &mdash; first matching {@code BcProgrammingRecipe} by id) with screenshots.</li>
 * </ol>
 *
 * <p>Everything runs through the integrated server, marshalled onto the server thread through
 * {@link MinecraftServer#execute}. The rig is inert unless the run passes {@code -Dbuildcraft.m417aprobe=true}
 * (dev-run evidence only, never active in normal play). {@code [M417]} lines in the log + the screenshots are the
 * pass signal; the launcher kills the process after the evidence lands (headless {@code mc.stop()} tears down
 * unreliably, the M4.16 lesson).
 */
@EventBusSubscriber(modid = BuildCraftSilicon.MOD_ID, value = Dist.CLIENT)
public final class M417aTableWorkProbe {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Set (and only set) by the M4.17a evidence run's JVM arguments; keeps the rig inert everywhere else. */
    private static final boolean ENABLED = Boolean.getBoolean("buildcraft.m417aprobe");

    /** Base wait between phases (ticks) — comfortably more than a couple of sync round trips. */
    private static final int WAIT_TICKS = 30;
    /** The mid-charge evidence tick: the robot battery is part-filled (not yet at the 500,000 µMJ cap). */
    private static final int CHARGE_MID_TICK = 600;
    /** The programming craft (auto-picked board_robot_bomber &mdash; first recipe by id &mdash; 12,800 legacy MJ =
     * 1,280,000 µMJ at 400 µMJ/t, the legacy 3,200 ticks) is done by ~3,210. */
    private static final int PROGRAM_DONE_TICK = 3_320;
    /** The robot charge (500,000 µMJ at 400 µMJ/t) is done by ~1,260 ticks; the final pass adds margin. */
    private static final int CHARGE_DONE_TICK = 1_500;

    private static int state = 0;
    private static int wait = 0;
    private static int tickCounter = 0;
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
        if (level == null || state > 20) {
            return;
        }
        // keep the vantage view pinned at the current scene
        mc.player.setYRot(viewYaw);
        mc.player.setXRot(viewPitch);
        tickCounter++;
        if (anchor != null && tickCounter % 40 == 0) {
            // the rig is the power plant: keep every scene laser's battery topped up (server thread)
            onServer(mc, () -> refillLasers(level));
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
                // +64 in z keeps the rig clear of the M4.16 probe's leftover scenes around this world spawn
                anchor = new BlockPos(spawn.getX() + 24, 0, spawn.getZ() + 64);
                BlockPos scene = new BlockPos(anchor.getX() + 1, 0,
                    anchor.getZ());
                groundY = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, scene).getY();
                mc.options.hideGui = true;
                LOGGER.info("[M417] rig start: anchor={} groundY={} tick={}", anchor, groundY, tickCounter);
                buildScenes(level);
                setView(player, anchor.getX() + 3.5, groundY + 5.5, anchor.getZ() - 10.0, 35.0F);
                wait = 120 - WAIT_TICKS;
            });
            case 1 -> {
                // insert-time + early charge state (both tables are still filling at this point)
                onServer(mc, () -> {
                    logCharging(level, "insert/early");
                    logProgramming(level, "before");
                });
                screenshot(mc, "m417_programming_before");
                wait = CHARGE_MID_TICK - 120 - WAIT_TICKS;
            }
            case 2 -> onServer(mc, () -> setView(player, anchor.getX() + 1.5, groundY + 2.5, anchor.getZ() - 4.0,
                25.0F));
            case 3 -> {
                // the robot battery is part-filled here: the charging mid evidence (the programming craft, the
                // legacy 3,200 ticks, is still far from done at tick ~600)
                screenshot(mc, "m417_charging_mid");
                onServer(mc, () -> logCharging(level, "mid-charge"));
                wait = PROGRAM_DONE_TICK - tickCounter - WAIT_TICKS;
            }
            case 4 -> {
                onServer(mc, () -> setView(player, anchor.getX() + 5.5, groundY + 2.5, anchor.getZ() - 4.0, 25.0F));
                wait = 2 * WAIT_TICKS;
            }
            case 5 -> {
                // the programming craft (1,280,000 µMJ) is done by now: the after half of the table's pair
                onServer(mc, () -> logProgramming(level, "after"));
                screenshot(mc, "m417_programming_after");
                LOGGER.info("[M417] waiting until tick {} for the final pass", CHARGE_DONE_TICK);
                wait = CHARGE_DONE_TICK - tickCounter - WAIT_TICKS;
            }
            case 6 -> {
                onServer(mc, () -> {
                    logCharging(level, "final");
                    logProgramming(level, "final");
                });
                wait = 2 * WAIT_TICKS;
            }
            case 7 -> {
                onServer(mc, () -> setView(player, anchor.getX() + 3.5, groundY + 5.5, anchor.getZ() - 10.0, 30.0F));
                wait = 2 * WAIT_TICKS;
            }
            case 8 -> screenshot(mc, "m417_final_overview");
            case 9 -> {
                onServer(mc, () -> setView(player, anchor.getX() + 1.5, groundY + 2.5, anchor.getZ() - 4.0, 25.0F));
                wait = 2 * WAIT_TICKS;
            }
            case 10 -> screenshot(mc, "m417_charging_full");
            case 11 -> {
                LOGGER.info("[M417] rig done at tick {} - evidence landed, shutting down", tickCounter);
                state = 21;
                mc.stop();
                return;
            }
            default -> {
                return;
            }
        }
        state++;
        wait = Math.max(wait, WAIT_TICKS);
    }

    /** Marshals one chunk of world work onto the server thread (the M416 rig discipline; see class javadoc). */
    private static void onServer(Minecraft mc, Runnable task) {
        mc.getSingleplayerServer().execute(task);
    }

    // ---------------------------------------------------------------- scenes

    /** Laser/table pair x offsets on the anchor row (the table sits directly east of its laser). */
    private static final int CHARGING_LASER_X = 0;
    private static final int CHARGING_TABLE_X = 1;
    private static final int PROGRAMMING_LASER_X = 4;
    private static final int PROGRAMMING_TABLE_X = 5;

    private static void buildScenes(ServerLevel level) {
        level.setBlockAndUpdate(pos(CHARGING_LASER_X), BcSiliconBlocks.LASER.value().defaultBlockState());
        level.setBlockAndUpdate(pos(CHARGING_TABLE_X), BcSiliconBlocks.CHARGING_TABLE.value().defaultBlockState());
        level.setBlockAndUpdate(pos(PROGRAMMING_LASER_X), BcSiliconBlocks.LASER.value().defaultBlockState());
        level.setBlockAndUpdate(pos(PROGRAMMING_TABLE_X), BcSiliconBlocks.PROGRAMMING_TABLE.value()
            .defaultBlockState());
        LOGGER.info("[M417] scenes built: charging pair at x={}..{}, programming pair at x={}..{}",//
            CHARGING_LASER_X, CHARGING_TABLE_X, PROGRAMMING_LASER_X, PROGRAMMING_TABLE_X);
        seedCharging(level);
        seedProgramming(level);
        refillLasers(level);
    }

    /** Inserts one robot_builder through the real item capability (the chargeable half of M4.17). */
    private static void seedCharging(ServerLevel level) {
        BlockPos table = pos(CHARGING_TABLE_X);
        ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, table,
            Direction.UP);
        ItemStack robot = new ItemStack(BcRoboticsItems.ROBOT_BUILDER.get());
        if (handler == null || insert(handler, robot) != 1) {
            LOGGER.error("[M417] charging seed FAILED at {}: handler={} (is the capability registered?)", table,
                handler);
            return;
        }
        logCharging(level, "insert");
    }

    /** Inserts one board_robot_empty through the real item capability (the programming input). */
    private static void seedProgramming(ServerLevel level) {
        BlockPos table = pos(PROGRAMMING_TABLE_X);
        ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, table,
            Direction.UP);
        ItemStack board = new ItemStack(BcRoboticsItems.BOARD_ROBOT_EMPTY.get());
        if (handler == null || insert(handler, board) != 1) {
            LOGGER.error("[M417] programming seed FAILED at {}: handler={} (is the capability registered?)", table,
                handler);
        }
    }

    /** Tops every scene laser's battery up through the real receiver entry point (the rig is the power plant). */
    private static void refillLasers(ServerLevel level) {
        for (int x : new int[] { CHARGING_LASER_X, PROGRAMMING_LASER_X }) {
            if (level.getBlockEntity(pos(x)) instanceof buildcraft.silicon.blockentity.LaserBlockEntity laser) {
                long requested = laser.getPowerRequested();
                if (requested > 0) {
                    laser.receivePower(requested, false);
                }
            }
        }
    }

    // ---------------------------------------------------------------- evidence logs

    private static void logCharging(ServerLevel level, String phase) {
        BlockPos table = pos(CHARGING_TABLE_X);
        if (level.getBlockEntity(table) instanceof ChargingTableBlockEntity charging) {
            ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, table,
                Direction.UP);
            String slot;
            if (handler != null && handler.getAmountAsLong(0) > 0) {
                ItemStack stack = handler.getResource(0).toStack((int) handler.getAmountAsLong(0));
                var data = stack.get(DataComponents.CUSTOM_DATA);
                slot = stack.getItem() + " x" + stack.getCount() + " custom_data="
                    + (data == null ? "{}" : data.copyTag().toString());
            } else {
                slot = "empty";
            }
            LOGGER.info("[M417] charging @ {} ({}): buffer={}µMJ slot={} [capability={}]", table, phase,
                charging.getPower(), slot, handler != null ? "exposed" : "MISSING");
        } else {
            LOGGER.error("[M417] charging table missing at {} ({})", table, phase);
        }
    }

    private static void logProgramming(ServerLevel level, String phase) {
        BlockPos table = pos(PROGRAMMING_TABLE_X);
        if (level.getBlockEntity(table) instanceof ProgrammingTableBlockEntity programming) {
            ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, table,
                Direction.UP);
            String input = slotText(handler, ProgrammingTableBlockEntity.SLOT_INPUT);
            String output = slotText(handler, ProgrammingTableBlockEntity.SLOT_OUTPUT);
            LOGGER.info("[M417] programming @ {} ({}): power={}µMJ active={} input=[{}] output=[{}]"
                + " [capability={}]", table, phase, programming.getPower(),//
                programming.getActive() == null ? "-" : programming.getActive().id().identifier(), input, output,//
                handler != null ? "exposed" : "MISSING");
        } else {
            LOGGER.error("[M417] programming table missing at {} ({})", table, phase);
        }
    }

    private static String slotText(@Nullable ResourceHandler<ItemResource> handler, int slot) {
        if (handler == null || handler.getAmountAsLong(slot) <= 0) {
            return "empty";
        }
        ItemStack stack = handler.getResource(slot).toStack((int) handler.getAmountAsLong(slot));
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return stack.getItem() + " x" + stack.getCount()
            + (data == null ? "" : " custom_data=" + data.copyTag().toString());
    }

    // ---------------------------------------------------------------- shared helpers

    private static BlockPos pos(int x) {
        return new BlockPos(anchor.getX() + x, groundY, anchor.getZ());
    }

    /** Server-side insert through the real handler API (fills from slot 0; one committed transaction). */
    private static int insert(ResourceHandler<ItemResource> inv, ItemStack stack) {
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = 0;
            for (int slot = 0; slot < inv.size() && inserted < stack.getCount(); slot++) {
                inserted += inv.insert(slot, ItemResource.of(stack), stack.getCount() - inserted, transaction);
            }
            if (inserted == stack.getCount()) {
                transaction.commit();
            }
            return inserted;
        }
    }

    private static void setView(ServerPlayer player, double x, double y, double z, float pitch) {
        viewYaw = 0.0F;
        viewPitch = pitch;
        player.teleportTo((ServerLevel) player.level(), x, y, z, java.util.Set.of(), viewYaw, viewPitch, false);
    }

    private static void screenshot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), 1,
            (Component component) -> LOGGER.info("[M417] screenshot {}: {}", name, component.getString()));
    }

    private M417aTableWorkProbe() {
    }
}
