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
 * Legacy counterpart: {@code buildcraft.lib.net.cache.MessageObjectCacheRequest} (1.20.1). Signifies a client to server
 * request for the value of a cached object, given its ID.
 *
 * <p>Wire format field set (identical to legacy):
 * <ul>
 * <li>BYTE - cacheId</li>
 * <li>SHORT - id count</li>
 * <li>count x INT - requested ids</li>
 * </ul>
 */
public class MessageObjectCacheRequest implements CustomPacketPayload {

    public static final Type<MessageObjectCacheRequest> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(BCLib.MOD_ID, "message_object_cache_request"));

    public static final StreamCodec<FriendlyByteBuf, MessageObjectCacheRequest> STREAM_CODEC = CustomPacketPayload.codec(
            MessageObjectCacheRequest::write, MessageObjectCacheRequest::read);

    public final int cacheId;
    public final int[] ids;

    public MessageObjectCacheRequest(int cacheId, int[] ids) {
        this.cacheId = cacheId;
        this.ids = ids;
        if (ids.length > Short.MAX_VALUE) {
            throw new IllegalStateException("Tried to request too many ID's! (" + ids.length + ")");
        }
    }

    private static void write(MessageObjectCacheRequest message, FriendlyByteBuf buf) {
        buf.writeByte(message.cacheId);
        buf.writeShort(message.ids.length);
        for (int id : message.ids) {
            buf.writeInt(id);
        }
    }

    private static MessageObjectCacheRequest read(FriendlyByteBuf buf) {
        int cacheId = buf.readByte();
        int idCount = buf.readShort();
        int[] ids = new int[idCount];
        for (int i = 0; i < idCount; i++) {
            ids[i] = buf.readInt();
        }
        return new MessageObjectCacheRequest(cacheId, ids);
    }

    /** Server-side cache lookup and response assembly. */
    public static void handleServer(MessageObjectCacheRequest message, IPayloadContext context) {
        // TODO(M2.6+): port BuildCraftObjectCaches + NetworkedObjectCache#writeObjectServer, then replay the legacy
        // handler (serialize each requested object into a MessageObjectCacheResponse).
        MessageManager.LOGGER.info(
                "[lib.messages] Received MessageObjectCacheRequest (cacheId={}, {} ids) - object cache sync not migrated yet",
                message.cacheId, message.ids.length);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
