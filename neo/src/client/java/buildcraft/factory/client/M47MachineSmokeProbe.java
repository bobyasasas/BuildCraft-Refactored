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
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import buildcraft.energy.BcEnergyFluids;
import buildcraft.factory.BcFactoryBlocks;
import buildcraft.factory.BuildCraftFactory;
import buildcraft.factory.blockentity.DistillerBlockEntity;
import buildcraft.factory.blockentity.HeatExchangeBlockEntity;
import buildcraft.factory.blockentity.PumpBlockEntity;
import buildcraft.factory.blockentity.TankBlockEntity;

/**
 * M4.7 in-game evidence rig (the {@code M46PipeSmokeProbe} pattern, one milestone later): a client-tick script that
 * drives one quickPlay world through every factory machine this task ships and drops a screenshot per scene into
 * {@code screenshots/}:
 * <ol>
 * <li>the tank scene: three tanks filled to 1/3 water, 2/3 lava and empty (fill level + fluid colour rendering,
 * two screenshots);</li>
 * <li>the bucket pass: an empty bucket drawn on the water tank through the real
 * {@link FluidUtil#interactWithFluidHandler} path {@code TankBlock} delegates to, then the water bucket poured into
 * the empty tank (log evidence: the hand's item turns into a water bucket and the target tank gains 1000 mB);</li>
 * <li>the distiller scene: 2000 mB of {@code oil_heat_0} in, then the recipe batches run on the server tick
 * ({@code +16 gas +3 liquid per 8 mB}) until the output windows hold visible product;</li>
 * <li>the heat exchanger scene: 2000 mB of {@code oil_heat_0} converted heat 0 &rarr; heat 1 across the two tanks;</li>
 * <li>the pump scene: one water source under the pump, consumed into the internal tank.</li>
 * </ol>
 *
 * <p>Everything runs through the integrated server; every ServerLevel touch is marshalled onto the server thread
 * through {@link MinecraftServer#execute} (the rig's tick fires on the render thread). The rig is inert unless the run
 * passes {@code -Dbuildcraft.m47probe=true} (dev-run evidence only, never active in normal play), and it shuts the game
 * down when done: {@code [M47]} lines in the log + 5 screenshots are the pass signal.
 */
@EventBusSubscriber(modid = BuildCraftFactory.MOD_ID, value = Dist.CLIENT)
public final class M47MachineSmokeProbe {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Set (and only set) by the M4.7 evidence run's JVM arguments; keeps the rig inert everywhere else. */
    private static final boolean ENABLED = Boolean.getBoolean("buildcraft.m47probe");

    /** Base wait between phases (ticks) — comfortably more than a couple of sync round trips. */
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
                BlockPos spawn = player.blockPosition();
                anchor = new BlockPos(spawn.getX() + 24, 0, spawn.getZ());
                BlockPos scene = new BlockPos(anchor.getX() + 1, 0, anchor.getZ());
                groundY = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, scene).getY();
                mc.options.hideGui = true;
                LOGGER.info("[M47] rig start: anchor={} groundY={}", anchor, groundY);
                buildTankScene(level);
                LOGGER.info("[M47] tank scene built: three tanks (1/3 water, 2/3 lava, empty)");
                setView(player, anchor.getX() + 1.5, groundY + 2.5, anchor.getZ() - 4.0, 25.0F);
            });
            case 1 -> {
                screenshot(mc, "m47_tanks");
            }
            case 2 -> onServer(mc, () -> bucketPass(level, player));
            case 3 -> {
                screenshot(mc, "m47_bucket_pour");
            }
            case 4 -> onServer(mc, () -> {
                buildDistillerScene(level);
                buildHeatExchangeScene(level);
                buildPumpScene(level);
                // one long wait: the machines batch on the server tick while the world runs
                wait = 600 - WAIT_TICKS;
                setView(player, anchor.getX() + 4.5, groundY + 2.5, anchor.getZ() - 4.0, 25.0F);
            });
            case 5 -> {
                onServer(mc, () -> logDistiller(level));
                screenshot(mc, "m47_distiller");
            }
            case 6 -> {
                onServer(mc, () -> setView(player, anchor.getX() + 6.5, groundY + 2.5, anchor.getZ() - 4.0, 25.0F));
                wait = 2 * WAIT_TICKS;
            }
            case 7 -> {
                onServer(mc, () -> logHeatExchange(level));
                screenshot(mc, "m47_heat_exchange");
            }
            case 8 -> {
                onServer(mc, () -> setView(player, anchor.getX() + 9.5, groundY + 2.5, anchor.getZ() - 4.0, 25.0F));
                wait = 2 * WAIT_TICKS;
            }
            case 9 -> {
                onServer(mc, () -> logPump(level));
                screenshot(mc, "m47_pump");
                LOGGER.info("[M47] rig done - shutting down");
            }
            default -> {
                state = 11;
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

    private static void buildTankScene(ServerLevel level) {
        tank(level, 0, TankFill.THIRD_WATER);
        tank(level, 1, TankFill.TWO_THIRDS_LAVA);
        tank(level, 2, TankFill.EMPTY);
    }

    private static void tank(ServerLevel level, int x, TankFill fill) {
        BlockPos pos = pos(x, 0);
        level.setBlockAndUpdate(pos, BcFactoryBlocks.TANK.value().defaultBlockState());
        if (level.getBlockEntity(pos) instanceof TankBlockEntity tankBe && fill.amount > 0) {
            fill(tankBe.getFluidHandler(null), fill.resource, fill.amount);
            LOGGER.info("[M47] tank {} filled: {} x {} mB", pos, fill.resource, fill.amount);
        }
    }

    /** The tank fills, as named fractions of the 16-bucket tank ({@code TileTank} capacity). */
    private enum TankFill {
        THIRD_WATER(FluidResource.of(Fluids.WATER), 16_000 / 3),
        TWO_THIRDS_LAVA(FluidResource.of(Fluids.LAVA), 2 * 16_000 / 3),
        EMPTY(null, 0);

        final FluidResource resource;
        final int amount;

        TankFill(FluidResource resource, int amount) {
            this.resource = resource;
            this.amount = amount;
        }
    }

    /**
     * The bucket evidence, through the exact code path {@code TankBlock#useItemOn} delegates to: draw water from the
     * first tank with an empty bucket, then pour the water bucket into the empty tank.
     */
    private static void bucketPass(ServerLevel level, ServerPlayer player) {
        BlockPos waterTank = pos(0, 0);
        BlockPos emptyTank = pos(2, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        boolean drew = FluidUtil.interactWithFluidHandler(player, InteractionHand.MAIN_HAND, level, waterTank,
            Direction.NORTH, null);
        LOGGER.info("[M47] bucket draw on {}: {} — main hand now {}", waterTank, drew,
            player.getMainHandItem().getItem());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        boolean poured = FluidUtil.interactWithFluidHandler(player, InteractionHand.MAIN_HAND, level, emptyTank,
            Direction.NORTH, null);
        LOGGER.info("[M47] bucket pour on {}: {} — main hand now {}", emptyTank, poured,
            player.getMainHandItem().getItem());
        if (level.getBlockEntity(emptyTank) instanceof TankBlockEntity tankBe) {
            LOGGER.info("[M47] poured tank now: {} x {} mB", tankBe.getFluidHandler(null).getResource(0),
                tankBe.getFluidHandler(null).getAmountAsLong(0));
        }
    }

    private static void buildDistillerScene(ServerLevel level) {
        BlockPos pos = pos(4, 0);
        level.setBlockAndUpdate(pos, BcFactoryBlocks.DISTILLER.value().defaultBlockState());
        if (level.getBlockEntity(pos) instanceof DistillerBlockEntity distiller) {
            fill(distiller.getTankIn(), FluidResource.of(BcEnergyFluids.OIL_HEAT_0.value()), 2000);
            LOGGER.info("[M47] distiller scene built at {}: 2000 mB oil_heat_0 in (recipe 8 in -> 16 gas + 3 liquid)",
                pos);
        }
    }

    private static void buildHeatExchangeScene(ServerLevel level) {
        BlockPos pos = pos(6, 0);
        level.setBlockAndUpdate(pos, BcFactoryBlocks.HEAT_EXCHANGE.value().defaultBlockState());
        if (level.getBlockEntity(pos) instanceof HeatExchangeBlockEntity exchanger) {
            fill(exchanger.getTankIn(), FluidResource.of(BcEnergyFluids.OIL_HEAT_0.value()), 2000);
            LOGGER.info("[M47] heat exchange scene built at {}: 2000 mB oil_heat_0 in (heat 0 -> heat 1)", pos);
        }
    }

    private static void buildPumpScene(ServerLevel level) {
        BlockPos waterPos = new BlockPos(anchor.getX() + 9, groundY - 1, anchor.getZ());
        level.setBlockAndUpdate(waterPos, Blocks.WATER.defaultBlockState());
        BlockPos pumpPos = pos(9, 0);
        level.setBlockAndUpdate(pumpPos, BcFactoryBlocks.PUMP.value().defaultBlockState());
        LOGGER.info("[M47] pump scene built: pump {} over a water source at {}", pumpPos, waterPos);
    }

    /** Server-side fill through the real handler API (one committed transaction). */
    private static void fill(net.neoforged.neoforge.transfer.ResourceHandler<FluidResource> tank,
        FluidResource resource, int amount) {
        try (Transaction transaction = Transaction.openRoot()) {
            tank.insert(0, resource, amount, transaction);
            transaction.commit();
        }
    }

    // ---------------------------------------------------------------- evidence logs

    private static void logDistiller(ServerLevel level) {
        BlockPos pos = pos(4, 0);
        if (level.getBlockEntity(pos) instanceof DistillerBlockEntity distiller) {
            LOGGER.info("[M47] distiller state: in={} x {}, gasOut={} x {}, liquidOut={} x {}",//
                distiller.getTankIn().getResource(0), distiller.getTankIn().getAmountAsLong(0),//
                distiller.getTankGasOut().getResource(0), distiller.getTankGasOut().getAmountAsLong(0),//
                distiller.getTankLiquidOut().getResource(0), distiller.getTankLiquidOut().getAmountAsLong(0));
        } else {
            LOGGER.error("[M47] distiller missing at {} (block={})", pos, level.getBlockState(pos).getBlock());
        }
    }

    private static void logHeatExchange(ServerLevel level) {
        BlockPos pos = pos(6, 0);
        if (level.getBlockEntity(pos) instanceof HeatExchangeBlockEntity exchanger) {
            LOGGER.info("[M47] heat exchange state: in={} x {}, out={} x {}",//
                exchanger.getTankIn().getResource(0), exchanger.getTankIn().getAmountAsLong(0),//
                exchanger.getTankOut().getResource(0), exchanger.getTankOut().getAmountAsLong(0));
        } else {
            LOGGER.error("[M47] heat exchange missing at {} (block={})", pos, level.getBlockState(pos).getBlock());
        }
    }

    private static void logPump(ServerLevel level) {
        BlockPos pumpPos = pos(9, 0);
        BlockPos below = pumpPos.below();
        if (level.getBlockEntity(pumpPos) instanceof PumpBlockEntity pump) {
            LOGGER.info("[M47] pump state: below={} (was a water source), tank={} x {} mB",//
                level.getBlockState(below).getBlock(), pump.getTank().getResource(0),
                pump.getTank().getAmountAsLong(0));
        } else {
            LOGGER.error("[M47] pump missing at {} (block={})", pumpPos, level.getBlockState(pumpPos).getBlock());
        }
    }

    // ---------------------------------------------------------------- shared helpers

    private static BlockPos pos(int x, int z) {
        return new BlockPos(anchor.getX() + x, groundY, anchor.getZ() + z);
    }

    private static void setView(ServerPlayer player, double x, double y, double z, float pitch) {
        viewYaw = 0.0F;
        viewPitch = pitch;
        player.teleportTo((ServerLevel) player.level(), x, y, z, Set.of(), viewYaw, viewPitch, false);
    }

    private static void screenshot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), 1,
            (Component component) -> LOGGER.info("[M47] screenshot {}: {}", name, component.getString()));
    }

    private M47MachineSmokeProbe() {
    }
}
