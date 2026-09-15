/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.robotics.zone;

import buildcraft.lib.BCLib;
import buildcraft.lib.net.MessageManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Legacy counterpart: {@code buildcraft.robotics.zone.MessageZoneMapRequest} (1.20.1). Client asks the server for the
 * zone-planner map image of one chunk at a given level.
 *
 * <p>Wire format field set (identical to legacy {@code ZonePlannerMapChunkKey#toBytes}):
 * <ul>
 * <li>INT - chunk x</li>
 * <li>INT - chunk z</li>
 * <li>UTF - dimension id (legacy wrote a ResourceLocation; 26.1.2 renamed the class to Identifier)</li>
 * <li>INT - level</li>
 * </ul>
 *
 * <p>The chunk coordinates are carried as raw ints instead of a {@code ChunkPos}: {@code ChunkPos}'s static
 * initialiser pulls in {@code ChunkStatus}/{@code ChunkPyramid} (registry-backed), which cannot be class-loaded outside
 * a bootstrapped game process. Same values, same INT INT wire encoding.
 */
public class MessageZoneMapRequest implements CustomPacketPayload {

    public static final Type<MessageZoneMapRequest> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(BCLib.MOD_ID, "message_zone_map_request"));

    public static final StreamCodec<FriendlyByteBuf, MessageZoneMapRequest> STREAM_CODEC = CustomPacketPayload.codec(
            MessageZoneMapRequest::write, MessageZoneMapRequest::read);

    /** Mirrors legacy {@code ZonePlannerMapChunkKey}. */
    public record ChunkKey(int chunkX, int chunkZ, Identifier dimensionId, int level) {
        static void write(ChunkKey key, FriendlyByteBuf buf) {
            buf.writeInt(key.chunkX());
            buf.writeInt(key.chunkZ());
            buf.writeIdentifier(key.dimensionId);
            buf.writeInt(key.level);
        }

        static ChunkKey read(FriendlyByteBuf buf) {
            return new ChunkKey(buf.readInt(), buf.readInt(), buf.readIdentifier(), buf.readInt());
        }
    }

    public final ChunkKey key;

    public MessageZoneMapRequest(ChunkKey key) {
        this.key = key;
    }

    private static void write(MessageZoneMapRequest message, FriendlyByteBuf buf) {
        ChunkKey.write(message.key, buf);
    }

    private static MessageZoneMapRequest read(FriendlyByteBuf buf) {
        return new MessageZoneMapRequest(ChunkKey.read(buf));
    }

    /** Server-side map chunk lookup. */
    public static void handleServer(MessageZoneMapRequest message, IPayloadContext context) {
        // TODO(M2.6+): port ZonePlannerMapDataServer#getChunk + ZonePlannerMapChunk generation, then replay the legacy
        // handler (reply with MessageZoneMapResponse for the requested key).
        MessageManager.LOGGER.info(
                "[lib.messages] Received MessageZoneMapRequest (chunk {}, {}, dim {}, level {}) - zone planner map data not migrated yet",
                message.key.chunkX(), message.key.chunkZ(), message.key.dimensionId(), message.key.level());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
