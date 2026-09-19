/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.silicon.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import buildcraft.robotics.BcRoboticsItems;
import buildcraft.silicon.BcSiliconBlocks;
import buildcraft.silicon.BuildCraftSilicon;
import buildcraft.silicon.blockentity.AdvancedCraftingTableBlockEntity;
import buildcraft.silicon.blockentity.AssemblyTableBlockEntity;
import buildcraft.silicon.blockentity.BcSiliconMachineLogic;
import buildcraft.silicon.blockentity.ChargingTableBlockEntity;
import buildcraft.silicon.blockentity.IntegrationTableBlockEntity;
import buildcraft.silicon.blockentity.LaserBlockEntity;
import buildcraft.silicon.blockentity.ProgrammingTableBlockEntity;

/**
 * M4.16b in-game evidence rig (the {@code M47MachineSmokeProbe} pattern, one milestone later): a client-tick script
 * that drives one quickPlay world through the six silicon machines and drops a screenshot per scene into
 * {@code screenshots/}. One laser feeds each table (the M4.16 direct-neighbour push):
 * <ol>
 * <li>scene build: 5 laser + table pairs (assembly/integration/charging/advanced-crafting/programming), inputs
 * seeded (redstone dust, robot board + base, one oak log) and every laser battery filled directly through
 * {@code MjReceiver#receivePower} (no pipes in this slice's scene &mdash; the rig is the power plant, the kinesis
 * feed is the already-proven M4.9 path);</li>
 * <li>mid-charge overview + per-table close-ups while the lasers push;</li>
 * <li>a real right-click on the programming table (v2 placeholder: must stay crash-free);</li>
 * <li>after the legacy-paced charges complete (a chipset takes the legacy 2,500 ticks at the slice-scaled rates),
 * the final state log carries the craft evidence ([M416] craft lines from the machines + inventories).</li>
 * </ol>
 *
 * <p>Everything runs through the integrated server; every ServerLevel touch is marshalled onto the server thread
 * through {@link MinecraftServer#execute}. The rig is inert unless the run passes {@code -Dbuildcraft.m416bprobe=true}
 * (dev-run evidence only, never active in normal play), and it shuts the game down when done: {@code [M416]} lines in
 * the log + the screenshots are the pass signal.
 */
@EventBusSubscriber(modid = BuildCraftSilicon.MOD_ID, value = Dist.CLIENT)
public final class M416bSiliconSmokeProbe {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Set (and only set) by the M4.16b evidence run's JVM arguments; keeps the rig inert everywhere else. */
    private static final boolean ENABLED = Boolean.getBoolean("buildcraft.m416bprobe");

    /** Base wait between phases (ticks) — comfortably more than a couple of sync round trips. */
    private static final int WAIT_TICKS = 30;
    /** The legacy-paced chipset charge (1,000,000 µMJ at 400 µMJ/t) plus margin, until the final evidence pass. */
    private static final int FINAL_WAIT = 2_800;

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
                anchor = new BlockPos(spawn.getX() + 24, 0, spawn.getZ());
                BlockPos scene = new BlockPos(anchor.getX() + 1, 0, anchor.getZ());
                groundY = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, scene).getY();
                mc.options.hideGui = true;
                LOGGER.info("[M416] rig start: anchor={} groundY={}", anchor, groundY);
                buildScenes(level);
                setView(player, anchor.getX() + 7.5, groundY + 6.5, anchor.getZ() - 11.0, 35.0F);
                // long cook: the legacy pacing does the crafting while the world runs
                wait = 100 - WAIT_TICKS;
            });
            case 1 -> {
                onServer(mc, () -> logAll(level, "mid-charge"));
                screenshot(mc, "m416_overview");
            }
            case 2 -> onServer(mc, () -> rightClickProgrammingTable(level, player));
            case 3 -> {
                onServer(mc, () -> setView(player, anchor.getX() + 1.5, groundY + 2.5, anchor.getZ() - 4.0, 25.0F));
                wait = 2 * WAIT_TICKS;
            }
            case 4 -> screenshot(mc, "m416_assembly");
            case 5 -> onServer(mc, () -> setView(player, anchor.getX() + 4.5, groundY + 2.5, anchor.getZ() - 4.0, 25.0F));
            case 6 -> screenshot(mc, "m416_integration");
            case 7 -> onServer(mc, () -> setView(player, anchor.getX() + 7.5, groundY + 2.5, anchor.getZ() - 4.0, 25.0F));
            case 8 -> screenshot(mc, "m416_charging");
            case 9 -> onServer(mc, () -> setView(player, anchor.getX() + 10.5, groundY + 2.5, anchor.getZ() - 4.0, 25.0F));
            case 10 -> screenshot(mc, "m416_advanced_crafting");
            case 11 -> onServer(mc, () -> setView(player, anchor.getX() + 13.5, groundY + 2.5, anchor.getZ() - 4.0, 25.0F));
            case 12 -> screenshot(mc, "m416_programming");
            case 13 -> onServer(mc, () -> {
                LOGGER.info("[M416] waiting {} ticks for the legacy-paced charges to complete", FINAL_WAIT);
                wait = FINAL_WAIT - WAIT_TICKS;
            });
            case 14 -> {
                onServer(mc, () -> logAll(level, "final"));
                screenshot(mc, "m416_final_overview");
            }
            case 15 -> {
                LOGGER.info("[M416] rig done - shutting down");
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

    /** Marshals one chunk of world work onto the server thread (the M47 rig discipline; see class javadoc). */
    private static void onServer(Minecraft mc, Runnable task) {
        mc.getSingleplayerServer().execute(task);
    }

    // ---------------------------------------------------------------- scenes

    /** Laser/table pair positions on the anchor row (x offsets; the table sits directly east of its laser). */
    private static final int[] LASER_X = {0, 3, 6, 9, 12};
    private static final int[] TABLE_X = {1, 4, 7, 10, 13};

    private static void buildScenes(ServerLevel level) {
        placePair(level, 0, BcSiliconBlocks.ASSEMBLY_TABLE.value().defaultBlockState());
        placePair(level, 1, BcSiliconBlocks.INTEGRATION_TABLE.value().defaultBlockState());
        placePair(level, 2, BcSiliconBlocks.CHARGING_TABLE.value().defaultBlockState());
        placePair(level, 3, BcSiliconBlocks.ADVANCED_CRAFTING_TABLE.value().defaultBlockState());
        // the fifth pair: laser + programming table (v2 placeholder, right-click evidence only)
        level.setBlockAndUpdate(pos(LASER_X[4], 0), BcSiliconBlocks.LASER.value().defaultBlockState());
        level.setBlockAndUpdate(pos(TABLE_X[4], 0), BcSiliconBlocks.PROGRAMMING_TABLE.value().defaultBlockState());
        LOGGER.info("[M416] scenes built: 5 laser+table pairs at x={}", TABLE_X);
        seedAssembly(level);
        seedIntegration(level);
        seedAdvancedCrafting(level);
        refillLasers(level);
    }

    private static void placePair(ServerLevel level, int pair, net.minecraft.world.level.block.state.BlockState table) {
        level.setBlockAndUpdate(pos(LASER_X[pair], 0), BcSiliconBlocks.LASER.value().defaultBlockState());
        level.setBlockAndUpdate(pos(TABLE_X[pair], 0), table);
    }

    private static void seedAssembly(ServerLevel level) {
        BlockPos table = pos(TABLE_X[0], 0);
        if (level.getBlockEntity(table) instanceof AssemblyTableBlockEntity assembly) {
            insert(assembly.getInv(), new ItemStack(Items.REDSTONE, 3));
            LOGGER.info("[M416] assembly seeded at {}: 3x redstone dust (recipe redstone_chipset: 1 dust -> 1 chipset,"
                + " 10,000,000,000µJ = 1,000,000µMJ slice)", table);
        }
    }

    private static void seedIntegration(ServerLevel level) {
        BlockPos table = pos(TABLE_X[1], 0);
        if (level.getBlockEntity(table) instanceof IntegrationTableBlockEntity integration) {
            try (Transaction transaction = Transaction.openRoot()) {
                integration.getInv().insert(IntegrationTableBlockEntity.SLOT_TARGET,
                    net.neoforged.neoforge.transfer.item.ItemResource.of(new ItemStack(BcRoboticsItems.ROBOT_BASE.get())),
                    1, transaction);
                integration.getInv().insert(IntegrationTableBlockEntity.SLOTS_TO_INTEGRATE_FIRST,
                    net.neoforged.neoforge.transfer.item.ItemResource.of(
                        new ItemStack(BcRoboticsItems.BOARD_ROBOT_BOMBER.get())), 1, transaction);
                transaction.commit();
            }
            LOGGER.info("[M416] integration seeded at {}: robot_base (slot 0) + board_robot_bomber (slot 1)"
                + " (recipe robot_bomber: 5,000,000,000µJ = 500,000µMJ slice)", table);
        }
    }

    private static void seedAdvancedCrafting(ServerLevel level) {
        BlockPos table = pos(TABLE_X[3], 0);
        if (level.getBlockEntity(table) instanceof AdvancedCraftingTableBlockEntity crafting) {
            try (Transaction transaction = Transaction.openRoot()) {
                crafting.getInv().insert(4,
                    net.neoforged.neoforge.transfer.item.ItemResource.of(new ItemStack(Items.OAK_LOG)), 1, transaction);
                transaction.commit();
            }
            LOGGER.info("[M416] advanced crafting seeded at {}: 1x oak_log in the grid centre (1 log -> 4 planks,"
                + " flat 50,000µMJ = legacy 500MJ)", table);
        }
    }

    /** Tops every scene laser's battery up through the real receiver entry point (the rig is the power plant). */
    private static void refillLasers(ServerLevel level) {
        for (int x : LASER_X) {
            if (level.getBlockEntity(pos(x, 0)) instanceof LaserBlockEntity laser) {
                long requested = laser.getPowerRequested();
                if (requested > 0) {
                    long excess = laser.receivePower(requested, false);
                    LOGGER.debug("[M416] laser {} topped up by {}µMJ (excess {})", pos(x, 0), requested - excess,
                        excess);
                }
            }
        }
    }

    private static void rightClickProgrammingTable(ServerLevel level, ServerPlayer player) {
        BlockPos table = pos(TABLE_X[4], 0);
        if (level.getBlockEntity(table) instanceof ProgrammingTableBlockEntity) {
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(table), Direction.NORTH, table, false);
            InteractionResult result = player.gameMode.useItemOn(player, level, ItemStack.EMPTY,
                InteractionHand.MAIN_HAND, hit);
            LOGGER.info("[M416] programming table right-click at {}: block={}, result={} (v2 placeholder:"
                + " crash-free no-op expected)", table, level.getBlockState(table).getBlock(), result);
        } else {
            LOGGER.error("[M416] programming table missing at {}", table);
        }
    }

    // ---------------------------------------------------------------- evidence logs

    private static void logAll(ServerLevel level, String phase) {
        LOGGER.info("[M416] === {} state pass ===", phase);
        logLaser(level, LASER_X[0]);
        if (level.getBlockEntity(pos(TABLE_X[0], 0)) instanceof AssemblyTableBlockEntity assembly) {
            StringBuilder contents = new StringBuilder();
            for (int i = 0; i < assembly.getInv().size(); i++) {
                var stack = assembly.getInv().getResource(i);
                long amount = assembly.getInv().getAmountAsLong(i);
                if (amount > 0) {
                    contents.append(stack).append(" x").append(amount).append("; ");
                }
            }
            LOGGER.info("[M416] assembly @ {}: power={}µMJ target={}µMJ active={} inv=[{}]",//
                assembly.getBlockPos(), assembly.getPower(), assembly.getActive() == null ? 0
                    : BcSiliconMachineLogic.sliceCost(assembly.getActive().value().requiredMicroJoules),//
                assembly.getActive() == null ? "-" : assembly.getActive().id().identifier(), contents);
        }
        logLaser(level, LASER_X[1]);
        if (level.getBlockEntity(pos(TABLE_X[1], 0)) instanceof IntegrationTableBlockEntity integration) {
            LOGGER.info("[M416] integration @ {}: power={}µMJ target={}µMJ active={}",//
                integration.getBlockPos(), integration.getPower(), integration.getActive() == null ? 0
                    : BcSiliconMachineLogic.sliceCost(integration.getActive().value().requiredMicroJoules),//
                integration.getActive() == null ? "-" : integration.getActive().id().identifier());
        }
        logLaser(level, LASER_X[2]);
        if (level.getBlockEntity(pos(TABLE_X[2], 0)) instanceof ChargingTableBlockEntity charging) {
            LOGGER.info("[M416] charging @ {}: buffer={}µMJ / {}µMJ (PARTIAL slice: no IMjContainerItem item"
                + " migrated yet, energy-in + visible store only)",//
                charging.getBlockPos(), charging.getPower(), ChargingTableBlockEntity.BUFFER_CAPACITY);
        }
        logLaser(level, LASER_X[3]);
        if (level.getBlockEntity(pos(TABLE_X[3], 0)) instanceof AdvancedCraftingTableBlockEntity crafting) {
            StringBuilder contents = new StringBuilder();
            for (int i = 0; i < crafting.getInv().size(); i++) {
                var stack = crafting.getInv().getResource(i);
                long amount = crafting.getInv().getAmountAsLong(i);
                if (amount > 0) {
                    contents.append(stack).append(" x").append(amount).append("; ");
                }
            }
            LOGGER.info("[M416] advanced crafting @ {}: power={}µMJ target={}µMJ recipe={} inv=[{}]",//
                crafting.getBlockPos(), crafting.getPower(), crafting.getCachedRecipe() == null ? 0
                    : BcSiliconMachineLogic.ADVANCED_CRAFTING_POWER_REQ,//
                crafting.getCachedRecipe() == null ? "-" : crafting.cachedRecipeId(), contents);
        }
        if (level.getBlockEntity(pos(TABLE_X[4], 0)) instanceof ProgrammingTableBlockEntity) {
            LOGGER.info("[M416] programming @ {}: placed, v2 placeholder (no behaviour by design)", pos(TABLE_X[4], 0));
        }
    }

    private static void logLaser(ServerLevel level, int x) {
        if (level.getBlockEntity(pos(x, 0)) instanceof LaserBlockEntity laser) {
            LOGGER.info("[M416] laser @ {}: battery={}µMJ totalPushed={}µMJ",//
                laser.getBlockPos(), laser.getBattery(), laser.getTotalPushed());
        }
    }

    // ---------------------------------------------------------------- shared helpers

    private static BlockPos pos(int x, int z) {
        return new BlockPos(anchor.getX() + x, groundY, anchor.getZ() + z);
    }

    /** Server-side insert through the real handler API (fills from slot 0; one committed transaction). */
    private static void insert(net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource> inv,
        ItemStack stack) {
        try (Transaction transaction = Transaction.openRoot()) {
            int left = stack.getCount();
            for (int slot = 0; slot < inv.size() && left > 0; slot++) {
                left -= inv.insert(slot, net.neoforged.neoforge.transfer.item.ItemResource.of(stack), left,
                    transaction);
            }
            transaction.commit();
        }
    }

    private static void setView(ServerPlayer player, double x, double y, double z, float pitch) {
        viewYaw = 0.0F;
        viewPitch = pitch;
        player.teleportTo((ServerLevel) player.level(), x, y, z, java.util.Set.of(), viewYaw, viewPitch, false);
    }

    private static void screenshot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), 1,
            (Component component) -> LOGGER.info("[M416] screenshot {}: {}", name, component.getString()));
    }

    private M416bSiliconSmokeProbe() {
    }
}
