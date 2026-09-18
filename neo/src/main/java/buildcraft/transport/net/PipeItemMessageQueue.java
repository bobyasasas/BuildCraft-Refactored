/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.transport.net;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import buildcraft.lib.net.MessageManager;
import buildcraft.transport.blockentity.PipeHolderBlockEntity;

/**
 * Server-side batching queue for travelling pipe items (legacy counterpart:
 * {@code buildcraft.transport.net.MessageMultiPipeItem} sender + {@code PipeTransportFlowItems} batching). Pipes mark
 * themselves dirty whenever their travelling item set changes ({@code PipeHolderBlockEntity#itemsDirty}); the batch
 * accumulates over one server tick and is flushed to each affected dimension at the end of the tick (the
 * {@code ServerTickEvent.Post} listener registered by {@code BuildCraftTransport}).
 *
 * <p><b>Client semantics (replace per pipe):</b> every entry in a batch <em>replaces</em> the client-side item list for
 * that pipe position, exactly like legacy's handler. A pipe whose items vanished sends an empty list once, which
 * clears the client mirror; unchanged pipes are not resent.
 */
public final class PipeItemMessageQueue {

    /** One batch per dimension, created lazily by {@link #appendPipe} and cleared every tick. */
    private static final Map<ResourceKey<Level>, MessageMultiPipeItem> BATCHES = new HashMap<>();

    private PipeItemMessageQueue() {}

    /** Adds the pipe's current travelling items to this tick's batch (called by the pipe's server tick). */
    public static void appendPipe(PipeHolderBlockEntity pipe) {
        Level level = pipe.getLevel();
        if (level == null) {
            return;
        }
        MessageMultiPipeItem message = BATCHES.computeIfAbsent(level.dimension(), key -> new MessageMultiPipeItem());
        pipe.appendItemData(message);
    }

    /** Flushes the batch (called at the end of every server tick). */
    public static void onServerTickPost(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        if (BATCHES.isEmpty()) {
            return;
        }
        for (Map.Entry<ResourceKey<Level>, MessageMultiPipeItem> entry : BATCHES.entrySet()) {
            ServerLevel level = event.getServer().getLevel(entry.getKey());
            if (level != null && !entry.getValue().items.isEmpty()) {
                MessageManager.sendToDimension(entry.getValue(), level);
            }
        }
        BATCHES.clear();
    }
}
