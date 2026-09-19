/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders.client;

import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import buildcraft.core.BcBlocks;
import buildcraft.core.block.StoneEngineBlock;
import buildcraft.core.blockentity.StoneEngineBlockEntity;
import buildcraft.builders.BcBuildersBlocks;
import buildcraft.builders.BcBuildersItems;
import buildcraft.builders.blockentity.ArchitectBlockEntity;
import buildcraft.builders.blockentity.BuilderBlockEntity;
import buildcraft.builders.blockentity.ConstructionMarkerBlockEntity;
import buildcraft.builders.blockentity.LibraryBlockEntity;
import buildcraft.builders.blockentity.ReplacerBlockEntity;
import buildcraft.builders.blueprint.BlueprintData;
import buildcraft.builders.blueprint.BlueprintItems;

/**
 * The M4.17c in-game evidence rig (the {@code M416CProbe} pattern, scoped to the five blueprint-system machines now
 * carrying their real minimal behaviour): one screenshot per machine state into {@code screenshots/} plus the
 * {@code [M417]} log lines each matrix row needs. The five scenes:
 * <ol>
 * <li>marker_construction: two markers right-clicked into a pair (the real interaction path), box frame rendered
 * around a 3x3x2 stone-brick house;</li>
 * <li>architect: a blank blueprint inserted through the item capability is scanned into the marker-pair box and comes
 * out written ({@code blueprint written -> <summary>});</li>
 * <li>builder: fueled stone engine &rarr; kinesis pipe &rarr; builder ({@link buildcraft.core.blockentity.MjReceiver});
 * the written blueprint plus stone-brick resources go in through the capability and the house rises one block per
 * tick ({@code placed n/total});</li>
 * <li>library: the written blueprint moves architect &rarr; library &rarr; out &rarr; back through the item capability,
 * every move firing the {@code index} line;</li>
 * <li>replacer: a 3x3 grass field inside a marker pair swaps to dirt one cell per tick, one dirt consumed per swap
 * ({@code replaced n (total m)}).</li>
 * </ol>
 *
 * <p>Exactly like its predecessors, every ServerLevel touch is marshalled onto the server thread through
 * {@code MinecraftServer#execute} and the rig is inert unless the run passes {@code -Dbuildcraft.m417cprobe=true}
 * (dev-run evidence only, never active in normal play). The machines stand on a smooth-stone platform at
 * {@link #PLATFORM_Y} so terrain cannot intrude on the evidence screenshots.
 */
@EventBusSubscriber(modid = "buildcraftbuilders", value = Dist.CLIENT)
public final class M417CBuildersProbe {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Set (and only set) by the M4.17c evidence run's JVM arguments; keeps the rig inert everywhere else. */
    private static final boolean ENABLED = Boolean.getBoolean("buildcraft.m417cprobe");

    /** Machine deck altitude (terrain-proof: the whole rig floats on its own smooth-stone platform). */
    private static final int PLATFORM_Y = 100;
    /** Base wait between phases (ticks) — comfortably more than a couple of sync round trips. */
    private static final int WAIT_TICKS = 30;

    private static int state = 0;
    private static int wait = 0;
    private static BlockPos anchor = null;
    /** View yaw/pitch to pin every tick (per-scene camera). */
    private static float viewYaw;
    private static float viewPitch;
    /** Scene anchors: each machine scene gets its own z offset off the base anchor. */
    private static BlockPos sceneA;
    private static BlockPos sceneB;
    private static BlockPos sceneC;
    private static BlockPos sceneD;
    /** The blueprint the architect wrote (scene A output), carried into the builder/library scenes. */
    private static ItemStack writtenBlueprint = ItemStack.EMPTY;

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
        if (level == null || state > 22) {
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
                anchor = new BlockPos(spawn.getX() + 300, PLATFORM_Y, spawn.getZ());
                sceneA = at(0, 0);
                sceneB = at(0, 48);
                sceneC = at(0, 96);
                sceneD = at(0, 144);
                mc.options.hideGui = true;
                player.setGameMode(GameType.CREATIVE);
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                mc.getSingleplayerServer().execute(() -> mc.getSingleplayerServer().getCommands()
                    .performPrefixedCommand(
                        mc.getSingleplayerServer().createCommandSourceStack().withSuppressedOutput(), "time set noon"));
                for (BlockPos scene : new BlockPos[] { sceneA, sceneB, sceneC, sceneD }) {
                    floor(level, scene, -2, 14, -8, 10);
                    clearFootprint(level, scene, -2, 14, -8, 10);
                }
                LOGGER.info("[M417] rig start: anchor={} platformY={}", anchor, PLATFORM_Y);
                setView(player, sceneA.getX() + 2.5, PLATFORM_Y + 3.5, sceneA.getZ() - 6.0, 30.0F);
            });
            // ---- scene A: marker pair + stone-brick house + architect ----
            case 1 -> onServer(mc, () -> {
                buildMarkerScene(level);
                setView(player, sceneA.getX() + 2.5, PLATFORM_Y + 3.5, sceneA.getZ() - 6.0, 30.0F);
            });
            case 2 -> onServer(mc, () -> pairMarker(level, player, sceneA, "scene A"));
            case 3 -> {
                screenshot(mc, "m417_marker_construction.png");
                onServer(mc, () -> insertBlankBlueprint(level));
            }
            case 4 -> {
                onServer(mc, () -> {
                    if (level.getBlockEntity(architectPos()) instanceof ArchitectBlockEntity architect) {
                        architect.logStatus();
                        LOGGER.info("[M417] rig: architect scanning={} (waiting for the scan to finish)",//
                            architect.isScanning());
                    }
                });
                wait = 100 - WAIT_TICKS; // 50 cells at 8/tick plus slack
            }
            case 5 -> onServer(mc, () -> {
                ItemStack written = extractOne(level, architectPos());
                BlueprintData data = BlueprintItems.readData(written);
                if (data == null) {
                    LOGGER.error("[M417] rig: architect produced NO readable blueprint (got {})",//
                        written.isEmpty() ? "nothing" : written.getItem());
                } else {
                    writtenBlueprint = written.copy();
                    LOGGER.info("[M417] rig: architect output verified: {}", data.summary());
                    data.blockCounts().forEach((id, count) -> LOGGER.info("[M417] rig: blueprint needs {} x{}", id,
                        count));
                }
            });
            case 6 -> screenshot(mc, "m417_architect.png");
            // ---- scene B: engine -> kinesis -> builder ----
            case 7 -> onServer(mc, () -> {
                buildBuilderScene(level);
                setView(player, sceneB.getX() + 3.5, PLATFORM_Y + 3.5, sceneB.getZ() - 6.0, 30.0F);
            });
            case 8 -> {
                screenshot(mc, "m417_builder_before.png");
                onServer(mc, () -> fuelEngine(level));
            }
            case 9 -> {
                onServer(mc, () -> {
                    if (level.getBlockEntity(builderPos()) instanceof BuilderBlockEntity builder) {
                        builder.logStatus();
                    }
                });
                wait = 600 - WAIT_TICKS; // stone engines ramp with heat: give the chain time to reach full output
            }
            case 10 -> {
                onServer(mc, () -> {
                    if (level.getBlockEntity(builderPos()) instanceof BuilderBlockEntity builder) {
                        builder.logStatus();
                    }
                });
                wait = 900 - WAIT_TICKS; // second window: the receipt rate is ~8k uMJ per placement, 17 need ~70s total
            }
            case 11 -> onServer(mc, () -> {
                if (level.getBlockEntity(builderPos()) instanceof BuilderBlockEntity builder) {
                    builder.logStatus();
                    int counted = 0;
                    // the blueprint's cells sit at local (1..3, 0..1, 1..3) of the 5x2x5 snapshot: the house box is
                    // origin+(1,0,1)..origin+(3,1,3), NOT origin..origin+(2,1,2) (the run-3 check box missed half)
                    BlockPos from = builder.buildOrigin().offset(1, 0, 1);
                    BlockPos to = builder.buildOrigin().offset(3, 1, 3);
                    for (BlockPos cell : BlockPos.betweenClosed(from, to)) {
                        if (level.getBlockState(cell).is(Blocks.STONE_BRICKS)) {
                            counted++;
                        }
                    }
                    LOGGER.info("[M417] rig: builder region check: {} stone brick blocks in {}x{}x{} (finished={})",//
                        counted, 3, 2, 3, builder.isFinished());
                }
            });
            case 12 -> screenshot(mc, "m417_builder_after.png");
            // ---- scene C: library stores the written blueprint ----
            case 13 -> onServer(mc, () -> {
                placeLibrary(level);
                setView(player, sceneC.getX() + 0.5, PLATFORM_Y + 3.5, sceneC.getZ() - 5.0, 30.0F);
            });
            case 14 -> onServer(mc, () -> storeThenSwapBlueprint(level, player));
            case 15 -> screenshot(mc, "m417_library.png");
            // ---- scene D: replacer swaps grass to dirt inside a marker pair ----
            case 16 -> onServer(mc, () -> {
                buildReplacerScene(level);
                setView(player, sceneD.getX() + 3.5, PLATFORM_Y + 3.5, sceneD.getZ() - 6.0, 30.0F);
            });
            case 17 -> onServer(mc, () -> pairMarker(level, player, sceneD, "scene D"));
            case 18 -> {
                // the before shot must land before the fuel arrives (the box pickup takes <=20 ticks, then the walk
                // swaps at 1/tick): with the replacer unfueled nothing can have replaced yet
                screenshot(mc, "m417_replacer_before.png");
                onServer(mc, () -> fuelReplacer(level));
                wait = 200 - WAIT_TICKS; // box pickup <=20 ticks + 9 swaps at 1/tick, plus margin
            }
            case 19 -> onServer(mc, () -> {
                if (level.getBlockEntity(replacerPos()) instanceof ReplacerBlockEntity replacer) {
                    replacer.logStatus();
                    int grass = 0;
                    int dirt = 0;
                    for (BlockPos cell : BlockPos.betweenClosed(
                        sceneD.offset(1, 0, 1), sceneD.offset(3, 0, 3))) {
                        if (level.getBlockState(cell).is(Blocks.GRASS_BLOCK)) {
                            grass++;
                        }
                        if (level.getBlockState(cell).is(Blocks.DIRT)) {
                            dirt++;
                        }
                    }
                    LOGGER.info("[M417] rig: replacer field check: {} grass left, {} dirt placed (expect 0/9)", grass,
                        dirt);
                }
            });
            case 20 -> screenshot(mc, "m417_replacer_after.png");
            case 21 -> {
                LOGGER.info("[M417] rig done - all five machine scenes complete");
                wait = 2 * WAIT_TICKS;
            }
            default -> {
                state = 23;
                // headless runs (no window manager) hang in mc.stop(); the evidence is on disk by now, so exit hard
                Thread killer = new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                    LOGGER.info("[M417] rig exiting");
                    Runtime.getRuntime().halt(0);
                }, "m417c-rig-exit");
                killer.setDaemon(true);
                killer.start();
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

    /** Scene A: the marker pair around the 3x3x2 stone-brick house plus the architect two blocks east of the box. */
    private static void buildMarkerScene(ServerLevel level) {
        level.setBlockAndUpdate(sceneA, BcBuildersBlocks.MARKER_CONSTRUCTION.value().defaultBlockState());
        level.setBlockAndUpdate(sceneA.offset(4, 1, 4),
            BcBuildersBlocks.MARKER_CONSTRUCTION.value().defaultBlockState());
        LOGGER.info("[M417] markers placed at {} and {} (pairing on right-click)", sceneA, sceneA.offset(4, 1, 4));
        // the 3x3x2 house: full bottom ring plus the top ring (17 stone bricks), inside the pair box, markers free
        int blocks = 0;
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                level.setBlockAndUpdate(new BlockPos(sceneA.getX() + x, PLATFORM_Y, sceneA.getZ() + z),
                    Blocks.STONE_BRICKS.defaultBlockState());
                blocks++;
            }
        }
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                boolean edge = x == 1 || x == 3 || z == 1 || z == 3;
                if (edge) {
                    level.setBlockAndUpdate(new BlockPos(sceneA.getX() + x, PLATFORM_Y + 1, sceneA.getZ() + z),
                        Blocks.STONE_BRICKS.defaultBlockState());
                    blocks++;
                }
            }
        }
        LOGGER.info("[M417] house built: {} stone brick blocks (3x3x2)", blocks);
        level.setBlockAndUpdate(architectPos(), BcBuildersBlocks.ARCHITECT.value().defaultBlockState());
        LOGGER.info("[M417] architect placed at {}", architectPos());
    }

    /** Scene B: stone engine (unfuelled until the "before" shot) -> wooden kinesis -> builder; blueprint and stone
     * brick resources go in through the item capability. */
    private static void buildBuilderScene(ServerLevel level) {
        BlockPos enginePos = sceneB;
        level.setBlockAndUpdate(enginePos, BcBlocks.ENGINE_STONE.value().defaultBlockState()
            .setValue(StoneEngineBlock.FACING, Direction.EAST));
        level.setBlockAndUpdate(sceneB.offset(1, 0, 0), BcBlocks.PIPE_KINESIS_WOOD.value().defaultBlockState());
        level.setBlockAndUpdate(builderPos(), BcBuildersBlocks.BUILDER.value().defaultBlockState());
        LOGGER.info("[M417] builder chain placed: engine {} -> kinesis {} -> builder {} (real energy chain)",//
            enginePos, sceneB.offset(1, 0, 0), builderPos());
        if (level.getBlockEntity(builderPos()) instanceof BuilderBlockEntity builder) {
            int blueprintIn = insertAll(level, builderPos(), writtenBlueprint);
            int resourceIn = insertAll(level, builderPos(), new ItemStack(Items.STONE_BRICKS, 32));
            LOGGER.info("[M417] builder loaded through capability: blueprint {} ({}) + {} stone bricks, origin {}",//
                blueprintIn, builder.getBlueprint() == null ? "unreadable" : "readable", resourceIn,//
                builder.buildOrigin());
        }
    }

    /** Scene C: the library, one block on the platform. */
    private static void placeLibrary(ServerLevel level) {
        level.setBlockAndUpdate(sceneC, BcBuildersBlocks.LIBRARY.value().defaultBlockState());
        LOGGER.info("[M417] library placed at {}", sceneC);
    }

    /** Scene D: the marker pair around the 3x3 grass field plus the replacer east of the box. */
    private static void buildReplacerScene(ServerLevel level) {
        level.setBlockAndUpdate(sceneD, BcBuildersBlocks.MARKER_CONSTRUCTION.value().defaultBlockState());
        level.setBlockAndUpdate(sceneD.offset(4, 0, 4),
            BcBuildersBlocks.MARKER_CONSTRUCTION.value().defaultBlockState());
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                level.setBlockAndUpdate(new BlockPos(sceneD.getX() + x, PLATFORM_Y, sceneD.getZ() + z),
                    Blocks.GRASS_BLOCK.defaultBlockState());
            }
        }
        LOGGER.info("[M417] grass field built: 9 grass blocks (3x3) inside markers {} / {}", sceneD,
            sceneD.offset(4, 0, 4));
        level.setBlockAndUpdate(replacerPos(), BcBuildersBlocks.REPLACER.value().defaultBlockState());
        LOGGER.info("[M417] replacer placed at {}", replacerPos());
    }

    // ---------------------------------------------------------------- per-machine actions + evidence

    private static BlockPos architectPos() {
        return sceneA.offset(7, 0, 2);
    }

    private static BlockPos builderPos() {
        return sceneB.offset(2, 0, 0);
    }

    private static BlockPos replacerPos() {
        return sceneD.offset(7, 0, 2);
    }

    /** Right-clicks the scene's first marker through the real interaction path, then makes sure the pair exists. */
    private static void pairMarker(ServerLevel level, ServerPlayer player, BlockPos scene, String tag) {
        rightClick(level, player, scene, "marker_construction (" + tag + ")");
        if (level.getBlockEntity(scene) instanceof ConstructionMarkerBlockEntity marker) {
            if (!marker.isConnected()) {
                marker.tryConnect(); // interaction-path fallback: the pairing itself is the evidence, not the click
                LOGGER.info("[M417] rig: marker pairing retried through tryConnect()");
            }
            LOGGER.info("[M417] rig: marker pair verified: connected={}, peer={}", marker.isConnected(),
                marker.getPeerPos());
            if (marker.getPeerPos() != null && level.getBlockEntity(marker.getPeerPos())
                instanceof ConstructionMarkerBlockEntity peer) {
                peer.logStatus();
            }
        }
    }

    /** Feeds the architect a blank blueprint through its item capability (the automation path). */
    private static void insertBlankBlueprint(ServerLevel level) {
        ItemStack blank = new ItemStack(BcBuildersItems.SNAPSHOT_BLUEPRINT.get());
        int inserted = insertAll(level, architectPos(), blank);
        if (level.getBlockEntity(architectPos()) instanceof ArchitectBlockEntity architect) {
            architect.logStatus();
        }
        LOGGER.info("[M417] rig: blank blueprint insert through capability: {} accepted (of 1)", inserted);
    }

    /** Lights the engine: coal in, the engine ignites and starts pushing MJ down the kinesis pipe. */
    private static void fuelEngine(ServerLevel level) {
        BlockPos enginePos = sceneB;
        if (level.getBlockEntity(enginePos) instanceof StoneEngineBlockEntity engine) {
            boolean fueled = engine.insertFuel(new ItemStack(Items.COAL, 4), level.fuelValues());
            LOGGER.info("[M417] rig: engine fueled: {} (ignites once the buffer has room)", fueled);
        } else {
            LOGGER.error("[M417] rig: engine missing at {}", enginePos);
        }
    }

    /** Feeds the replacer: one grass block into the from slot, nine dirt into the to buffer (the capability path). */
    private static void fuelReplacer(ServerLevel level) {
        int from = insertAll(level, replacerPos(), new ItemStack(Blocks.GRASS_BLOCK));
        int to = insertAll(level, replacerPos(), new ItemStack(Items.DIRT, 9));
        if (level.getBlockEntity(replacerPos()) instanceof ReplacerBlockEntity replacer) {
            replacer.logStatus();
        }
        LOGGER.info("[M417] rig: replacer loaded through capability: from={} grass, to={} dirt", from, to);
    }

    /** Library scene: the written blueprint goes in through the capability (the index line fires by itself), one
     * real right-click prints the index again, then one blueprint comes back out and returns (store + retrieve). */
    private static void storeThenSwapBlueprint(ServerLevel level, ServerPlayer player) {
        int stored = insertAll(level, sceneC, writtenBlueprint);
        LOGGER.info("[M417] rig: blueprint into library capability: {} accepted (of 1)", stored);
        rightClick(level, player, sceneC, "library");
        ItemStack out = extractOne(level, sceneC);
        LOGGER.info("[M417] rig: blueprint out of library capability: {} ({})", out.isEmpty() ? "NOTHING" : "ok",
            BlueprintItems.readData(out) == null ? "unreadable" : "readable");
        int back = insertAll(level, sceneC, out);
        LOGGER.info("[M417] rig: blueprint back on the shelf: {} accepted", back);
    }

    // ---------------------------------------------------------------- capability + interaction helpers

    /** Inserts a whole stack through the block's item capability (every slot tried, transactional). */
    private static int insertAll(ServerLevel level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, pos, Direction.UP);
        if (handler == null) {
            LOGGER.error("[M417] rig: NO item capability at {}", pos);
            return 0;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = 0;
            for (int slot = 0; slot < handler.size() && inserted < stack.getCount(); slot++) {
                inserted += handler.insert(slot, ItemResource.of(stack), stack.getCount() - inserted, transaction);
            }
            if (inserted > 0) {
                transaction.commit();
            }
            return inserted;
        }
    }

    /** Extracts one item from the block's item capability (first non-empty slot), or empty. */
    private static ItemStack extractOne(ServerLevel level, BlockPos pos) {
        ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, pos, Direction.UP);
        if (handler == null) {
            LOGGER.error("[M417] rig: NO item capability at {}", pos);
            return ItemStack.EMPTY;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            for (int slot = 0; slot < handler.size(); slot++) {
                ItemResource resource = handler.getResource(slot);
                if (resource.isEmpty() || handler.getAmountAsLong(slot) <= 0) {
                    continue;
                }
                if (handler.extract(slot, resource, 1, transaction) > 0) {
                    transaction.commit();
                    return resource.toStack(1);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * The real interaction path ({@code ServerPlayerGameMode#useItemOn} with an empty main hand): the result is logged
     * and the run continues — any crash here would fail the rig.
     */
    private static void rightClick(ServerLevel level, ServerPlayer player, BlockPos pos, String name) {
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
        var result = player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
        LOGGER.info("[M417] right-click {} at {}: useItemOn -> {} (no crash)", name, pos, result);
    }

    // ---------------------------------------------------------------- platform helpers

    /** Airs the scene footprint above the platform (the platform itself stays). */
    private static void clearFootprint(ServerLevel level, BlockPos base, int x0, int x1, int z0, int z1) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = PLATFORM_Y; y <= PLATFORM_Y + 12; y++) {
                    level.removeBlock(new BlockPos(base.getX() + x, y, base.getZ() + z), false);
                }
            }
        }
    }

    private static BlockPos at(int x, int z) {
        return new BlockPos(anchor.getX() + x, PLATFORM_Y, anchor.getZ() + z);
    }

    private static void setView(ServerPlayer player, double x, double y, double z, float pitch) {
        viewYaw = 0.0F;
        viewPitch = pitch;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.teleportTo((ServerLevel) player.level(), x, y, z, Set.of(), viewYaw, viewPitch, false);
    }

    /** Lays the scene floor slab one block under machine level (the platform the whole rig stands on). */
    private static void floor(ServerLevel level, BlockPos base, int x0, int x1, int z0, int z1) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                level.setBlockAndUpdate(new BlockPos(base.getX() + x, PLATFORM_Y - 1, base.getZ() + z),
                    Blocks.SMOOTH_STONE.defaultBlockState());
            }
        }
    }

    private static void screenshot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), 1,
            (Component component) -> LOGGER.info("[M417] screenshot {}: {}", name, component.getString()));
    }

    private M417CBuildersProbe() {
    }
}
