/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.net.cache;

import buildcraft.lib.BCLib;
import buildcraft.lib.net.MessageManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Legacy counterpart: {@code buildcraft.lib.net.cache.MessageObjectCacheResponse} (1.20.1). Delivers the serialized
 * server-side values for previously requested cached object ids.
 *
 * <p>Wire format field set (identical to legacy):
 * <ul>
 * <li>BYTE - cacheId</li>
 * <li>SHORT - id count</li>
 * <li>count x [INT id, SHORT value size, BYTE[value size] value]</li>
 * </ul>
 */
public class MessageObjectCacheResponse implements CustomPacketPayload {

    public static final Type<MessageObjectCacheResponse> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(BCLib.MOD_ID, "message_object_cache_response"));

    public static final StreamCodec<FriendlyByteBuf, MessageObjectCacheResponse> STREAM_CODEC = CustomPacketPayload.codec(
            MessageObjectCacheResponse::write, MessageObjectCacheResponse::read);

    public final int cacheId;
    public final int[] ids;
    public final byte[][] values;

    public MessageObjectCacheResponse(int cacheId, int[] ids, byte[][] values) {
        this.cacheId = cacheId;
        this.ids = ids;
        this.values = values;
    }

    private static void write(MessageObjectCacheResponse message, FriendlyByteBuf buf) {
        buf.writeByte(message.cacheId);
        buf.writeShort(message.ids.length);
        for (int i = 0; i < message.ids.length; i++) {
            buf.writeInt(message.ids[i]);
            buf.writeShort(message.values[i].length);
            buf.writeBytes(message.values[i]);
        }
    }

    private static MessageObjectCacheResponse read(FriendlyByteBuf buf) {
        int cacheId = buf.readByte();
        int idCount = buf.readShort();
        int[] ids = new int[idCount];
        byte[][] values = new byte[idCount][];
        for (int i = 0; i < idCount; i++) {
            ids[i] = buf.readInt();
            values[i] = new byte[buf.readShort()];
            buf.readBytes(values[i]);
        }
        return new MessageObjectCacheResponse(cacheId, ids, values);
    }

    /** Client-side cache population. */
    public static void handleClient(MessageObjectCacheResponse message, IPayloadContext context) {
        // TODO(M2.6+): port NetworkedObjectCache#readObjectClient + BuildCraftObjectCaches, then replay the legacy
        // handler (feed each deserialized value back into the client cache).
        MessageManager.LOGGER.info(
                "[lib.messages] Received MessageObjectCacheResponse (cacheId={}, {} values) - object cache sync not migrated yet",
                message.cacheId, message.ids.length);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
