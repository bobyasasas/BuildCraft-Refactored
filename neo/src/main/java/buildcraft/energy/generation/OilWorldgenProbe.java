/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.energy.generation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import buildcraft.energy.BcEnergyBlocks;

import org.slf4j.Logger;

/**
 * M2.8 verification probe (kept intentionally): proves on a freshly generated world that oil field world generation
 * really places oil blocks, by force-loading chunks in rings around world spawn and counting
 * {@code buildcraftenergy:fluid_block_oil_heat_0} blocks (the crude-oil worldgen block) via each chunk's section
 * palettes. It never changes generation semantics — it only loads chunks the normal pipeline would generate anyway.
 * The single evidence line is
 * {@code Oil worldgen scan: <N> oil blocks in <R> chunk radius (...)} with N &gt; 0 on success.
 *
 * <p>Gating: the probe is active only when the system property {@code buildcraft.oilscan.radius} is an integer
 * &gt; 0 (chunk radius around spawn; the dedicated-server run config passes 32 by default, the gametest server
 * passes 0). Optional tuning: {@code buildcraft.oilscan.chunksPerTick} (default 8),
 * {@code buildcraft.oilscan.maxTicks} (default 12000). With the legacy default probability (≈0.14 % per chunk) a
 * 32-chunk radius (~4225 candidate chunks) expects ~6 wells, and the scan stops at the first oil found.</p>
 */
public final class OilWorldgenProbe {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int RADIUS_CHUNKS = Integer.getInteger("buildcraft.oilscan.radius", 0);
    private static final int CHUNKS_PER_TICK = Integer.getInteger("buildcraft.oilscan.chunksPerTick", 8);
    private static final int MAX_TICKS = Integer.getInteger("buildcraft.oilscan.maxTicks", 12000);

    private static Scan active;

    private static final class Scan {
        final ServerLevel level;
        final ChunkPos center;
        final List<ChunkPos> order;
        final List<ChunkPos> pending = new ArrayList<>();
        int cursor;
        long totalOil;
        int scanned;
        int tick;

        Scan(ServerLevel level, ChunkPos center, int radius) {
            this.level = level;
            this.center = center;
            this.order = new ArrayList<>();
            List<int[]> offsets = new ArrayList<>();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    offsets.add(new int[] { dx, dz });
                }
            }
            // Rings outwards from the spawn chunk, so the scan can stop early.
            offsets.sort(Comparator.<int[]>comparingInt(o -> Math.max(Math.abs(o[0]), Math.abs(o[1])))
                .thenComparingInt(o -> o[0] * o[0] + o[1] * o[1]));
            for (int[] o : offsets) {
                this.order.add(new ChunkPos(center.x() + o[0], center.z() + o[1]));
            }
        }
    }

    private OilWorldgenProbe() {}

    public static void onServerStarted(ServerStartedEvent event) {
        active = null;
        if (RADIUS_CHUNKS <= 0) {
            return;
        }
        ServerLevel level = event.getServer().overworld();
        if (level == null) {
            return;
        }
        BlockPos spawn = level.getRespawnData().pos();
        ChunkPos center = new ChunkPos(spawn.getX() >> 4, spawn.getZ() >> 4);
        active = new Scan(level, center, RADIUS_CHUNKS);
        LOGGER.info(
            "Oil worldgen probe active (M2.8 verification probe): radius {} chunks around spawn chunk [{}, {}], up to {} chunksPerTick",
            RADIUS_CHUNKS, center.x(), center.z(), CHUNKS_PER_TICK);
    }

    public static void onServerTickPost(ServerTickEvent.Post event) {
        Scan scan = active;
        if (scan == null || event.getServer() != scan.level.getServer()) {
            return;
        }
        scan.tick++;
        if (scan.tick > MAX_TICKS) {
            finish(scan, "timeout after " + MAX_TICKS + " ticks");
            return;
        }

        // Issue new forced-chunk tickets.
        int issued = 0;
        while (scan.cursor < scan.order.size() && issued < CHUNKS_PER_TICK) {
            ChunkPos pos = scan.order.get(scan.cursor++);
            scan.level.setChunkForced(pos.x(), pos.z(), true);
            scan.pending.add(pos);
            issued++;
        }

        // Scan whatever tickets already materialised into full chunks, then release them.
        ServerChunkCache chunkSource = scan.level.getChunkSource();
        Iterator<ChunkPos> it = scan.pending.iterator();
        while (it.hasNext()) {
            ChunkPos pos = it.next();
            LevelChunk chunk = chunkSource.getChunkNow(pos.x(), pos.z());
            if (chunk == null) {
                continue;
            }
            scan.totalOil += countOilBlocks(chunk);
            scan.scanned++;
            it.remove();
            scan.level.setChunkForced(pos.x(), pos.z(), false);
        }

        if (scan.totalOil > 0) {
            finish(scan, "first oil found");
        } else if (scan.cursor >= scan.order.size() && scan.pending.isEmpty()) {
            finish(scan, "scan area exhausted");
        }
    }

    private static long countOilBlocks(LevelChunk chunk) {
        Block oil = BcEnergyBlocks.FLUID_BLOCK_OIL_HEAT_0.get();
        long[] count = new long[1];
        for (LevelChunkSection section : chunk.getSections()) {
            if (section.hasOnlyAir()) {
                continue;
            }
            section.getStates().count((state, n) -> {
                if (state.getBlock() == oil) {
                    count[0] += n;
                }
            });
        }
        return count[0];
    }

    private static void finish(Scan scan, String reason) {
        active = null;
        // Release any tickets that never materialised.
        for (ChunkPos pos : scan.pending) {
            scan.level.setChunkForced(pos.x(), pos.z(), false);
        }
        LOGGER.info(
            "Oil worldgen scan: {} oil blocks in {} chunk radius ({} after {} ticks, scanned {} / {} chunks around spawn chunk [{}, {}], M2.8 verification probe)",
            scan.totalOil, RADIUS_CHUNKS, reason, scan.tick, scan.scanned, scan.order.size(), scan.center.x(),
            scan.center.z());
    }
}
