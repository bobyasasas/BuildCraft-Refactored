/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.net;

import buildcraft.lib.misc.MessageUtil;
import java.util.ArrayList;
import java.util.List;
import buildcraft.lib.BCLib;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Legacy counterpart: {@code buildcraft.lib.net.MessageMarker} (1.20.1). Updates the client-side marker caches.
 *
 * <p>Wire format field set (identical to legacy):
 * <ul>
 * <li>BOOL - add</li>
 * <li>BOOL - multiple (derived on write from {@code positions.size() != 1})</li>
 * <li>BOOL - connection</li>
 * <li>SHORT - cacheId</li>
 * <li>SHORT - count (only present when multiple)</li>
 * <li>count x BC-blockpos (3 varints)</li>
 * </ul>
 */
public class MessageMarker implements CustomPacketPayload {

    public static final Type<MessageMarker> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(BCLib.MOD_ID, "message_marker"));

    public static final StreamCodec<FriendlyByteBuf, MessageMarker> STREAM_CODEC = CustomPacketPayload.codec(
            MessageMarker::write, MessageMarker::read);

    public boolean add, multiple, connection;
    public int cacheId;
    public final List<BlockPos> positions = new ArrayList<>();

    public MessageMarker() {}

    private static void write(MessageMarker message, FriendlyByteBuf buf) {
        int count = message.positions.size();
        message.multiple = count != 1;
        buf.writeBoolean(message.add);
        buf.writeBoolean(message.multiple);
        buf.writeBoolean(message.connection);
        buf.writeShort(message.cacheId);
        if (message.multiple) {
            buf.writeShort(count);
        }
        for (BlockPos pos : message.positions) {
            MessageUtil.writeBlockPos(buf, pos);
        }
    }

    private static MessageMarker read(FriendlyByteBuf buf) {
        MessageMarker message = new MessageMarker();
        message.add = buf.readBoolean();
        message.multiple = buf.readBoolean();
        message.connection = buf.readBoolean();
        message.cacheId = buf.readShort();
        int count = message.multiple ? buf.readShort() : 1;
        // Note: legacy read count into a field before the loop; kept as a local so the wire order matches exactly.
        for (int i = 0; i < count; i++) {
            message.positions.add(MessageUtil.readBlockPos(buf));
        }
        return message;
    }

    /** Client-side marker cache update. */
    public static void handleClient(MessageMarker message, IPayloadContext context) {
        // TODO(M2.6+): port buildcraft.lib.marker.MarkerCache + sub-cache handle dispatch, then replay the legacy
        // handler (which also validates the cacheId against MarkerCache.CACHES).
        MessageManager.LOGGER.info(
                "[lib.messages] Received MessageMarker (add={}, multiple={}, connection={}, cacheId={}, {} positions) - marker cache not migrated yet",
                message.add, message.multiple, message.connection, message.cacheId, message.positions.size());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
