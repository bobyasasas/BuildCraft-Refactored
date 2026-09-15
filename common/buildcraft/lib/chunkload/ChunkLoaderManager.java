/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.chunkload;

import buildcraft.lib.BCLib;
import buildcraft.lib.BCLibConfig;
import buildcraft.lib.misc.data.WorldPos;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.common.world.ForgeChunkManager.TicketHelper;

import java.util.*;

public class ChunkLoaderManager {
    private static final Map<WorldPos, Pair<LongSet, LongSet>> TICKETS = new HashMap<>();

    /**
     * This should be called in {@link BlockEntity#clearRemoved()}, if a tile entity might be able to load. A check is
     * performed to see if the config allows it
     */
    public static <T extends BlockEntity & IChunkLoadingTile> void loadChunksForTile(T tile) {
        if (!(tile.getLevel() instanceof ServerLevel)) {
            return;
        }
        if (!canLoadFor(tile)) {
            releaseChunksFor(tile);
            return;
        }
        updateChunksFor(tile);
    }

    public static <T extends BlockEntity & IChunkLoadingTile> void releaseChunksFor(T tile) {
        if (!(tile.getLevel() instanceof ServerLevel)) {
            return;
        }
        Pair<LongSet, LongSet> removed = TICKETS.remove(new WorldPos(tile));
        // NullPointerException: Cannot invoke "com.mojang.datafixers.util.Pair.getSecond()" because "removed" is null
        if (removed != null) {
            removed.getSecond().forEach(cp -> unforceChunk((ServerLevel) tile.getLevel(), tile.getBlockPos(), new ChunkPos(cp)));
        }
    }

    private static <T extends BlockEntity & IChunkLoadingTile> void updateChunksFor(T tile) {
        if (!(tile.getLevel() instanceof ServerLevel)) {
            return;
        }
        WorldPos wPos = new WorldPos(tile);
        Pair<LongSet, LongSet> ticket = TICKETS.get(wPos);
        if (ticket == null) {
//            ticket = ForgeChunkManager.requestTicket(
//                    BCLib.INSTANCE,
//                    tile.getLevel(),
//                    ForgeChunkManager.Type.NORMAL
            ticket = new Pair<>(new LongOpenHashSet(), new LongOpenHashSet());
            TICKETS.put(wPos, ticket);
        }
        Set<ChunkPos> chunks = getChunksToLoad(tile);
        for (Long pos : ticket.getSecond()) {
            if (!chunks.contains(new ChunkPos(pos))) {
                unforceChunk((ServerLevel) tile.getLevel(), tile.getBlockPos(), new ChunkPos(pos));
            }
        }
        for (ChunkPos pos : chunks) {
            if (!ticket.getSecond().contains(pos.toLong())) {
                forceChunk((ServerLevel) tile.getLevel(), tile.getBlockPos(), pos);
                ticket.getSecond().add(pos.toLong());
            }
        }
    }

    public static boolean unforceChunk(ServerLevel world, BlockPos owner, ChunkPos chunkPos) {
        return ForgeChunkManager.forceChunk(world, BCLib.MODID, owner, chunkPos.x, chunkPos.z, false, true);
    }

    public static boolean forceChunk(ServerLevel world, BlockPos owner, ChunkPos chunkPos) {
        return ForgeChunkManager.forceChunk(world, BCLib.MODID, owner, chunkPos.x, chunkPos.z, true, true);
    }

    public static <T extends BlockEntity & IChunkLoadingTile> Set<ChunkPos> getChunksToLoad(T tile) {
        Set<ChunkPos> chunksToLoad = tile.getChunksToLoad();
        Set<ChunkPos> chunkPoses = new HashSet<>(chunksToLoad != null ? chunksToLoad : Collections.emptyList());
        chunkPoses.add(new ChunkPos(tile.getBlockPos()));
        return chunkPoses;
    }

    public static void rebindTickets(ServerLevel world, TicketHelper ticketHelper) {
        Map<BlockPos, Pair<LongSet, LongSet>> tickets = ticketHelper.getBlockTickets();
        TICKETS.clear();
        if (BCLibConfig.chunkLoadingLevel != BCLibConfig.ChunkLoaderLevel.NONE) {
            for (BlockPos pos : tickets.keySet()) {
                if (pos == null) {
                    ticketHelper.removeAllTickets(pos);
                    continue;
                }
                WorldPos wPos = new WorldPos(world, pos);
                if (TICKETS.containsKey(wPos)) {
                    // and should not be duplicated WorldPos added into TICKETS
                    ticketHelper.removeAllTickets(pos);
                    continue;
                }
                BlockEntity tile = world.getBlockEntity(pos);
                if (tile == null || !(tile instanceof IChunkLoadingTile) || !canLoadFor((IChunkLoadingTile) tile)) {
                    TICKETS.remove(wPos);
                    ticketHelper.removeAllTickets(pos);
                    continue;
                }
                TICKETS.put(wPos, tickets.get(pos));
                for (ChunkPos chunkPos : getChunksToLoad((BlockEntity & IChunkLoadingTile) tile)) {
                    ForgeChunkManager.forceChunk(world, BCLib.MODID, tile.getBlockPos(), chunkPos.x, chunkPos.z, true, true);
                }
            }
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean canLoadFor(IChunkLoadingTile tile) {
        return BCLibConfig.chunkLoadingLevel.canLoad(tile.getLoadType());
    }
}
