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
 * Legacy counterpart: {@code buildcraft.robotics.zone.MessageZoneMapResponse} (1.20.1). Delivers the zone-planner map
 * image of one chunk (16x16 cells of height+map-colour pairs).
 *
 * <p>Wire format field set (identical to legacy {@code ZonePlannerMapChunk#write}):
 * <ul>
 * <li>the {@link MessageZoneMapRequest} key fields (INT x, INT z, UTF dimension, INT level)</li>
 * <li>256 cells, scanned x-major then z: INT -1 for "no block", else INT posY + INT colour</li>
 * </ul>
 */
public class MessageZoneMapResponse implements CustomPacketPayload {

    public static final int GRID_SIZE = 16;

    public static final Type<MessageZoneMapResponse> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(BCLib.MOD_ID, "message_zone_map_response"));

    public static final StreamCodec<FriendlyByteBuf, MessageZoneMapResponse> STREAM_CODEC = CustomPacketPayload.codec(
            MessageZoneMapResponse::write, MessageZoneMapResponse::read);

    /** Mirrors legacy {@code ZonePlannerMapChunk}: flat 16x16 grids with -1 as the "absent" sentinel. */
    public record ChunkData(int[] posY, int[] colour) {
        public static final int ABSENT = -1;

        public ChunkData {
            if (posY.length != GRID_SIZE * GRID_SIZE || colour.length != GRID_SIZE * GRID_SIZE) {
                throw new IllegalArgumentException("Zone map chunk data must be " + GRID_SIZE + "x" + GRID_SIZE);
            }
        }

        public static ChunkData absent() {
            int[] posY = new int[GRID_SIZE * GRID_SIZE];
            int[] colour = new int[GRID_SIZE * GRID_SIZE];
            java.util.Arrays.fill(posY, ABSENT);
            java.util.Arrays.fill(colour, ABSENT);
            return new ChunkData(posY, colour);
        }

        static void write(ChunkData data, FriendlyByteBuf buf) {
            for (int i = 0; i < GRID_SIZE * GRID_SIZE; i++) {
                if (data.posY[i] == ABSENT) {
                    buf.writeInt(ABSENT);
                } else {
                    buf.writeInt(data.posY[i]);
                    buf.writeInt(data.colour[i]);
                }
            }
        }

        static ChunkData read(FriendlyByteBuf buf) {
            int[] posY = new int[GRID_SIZE * GRID_SIZE];
            int[] colour = new int[GRID_SIZE * GRID_SIZE];
            for (int x = 0; x < GRID_SIZE; x++) {
                for (int z = 0; z < GRID_SIZE; z++) {
                    int index = x * GRID_SIZE + z;
                    posY[index] = buf.readInt();
                    if (posY[index] > 0) {
                        colour[index] = buf.readInt();
                    } else {
                        posY[index] = ABSENT;
                        colour[index] = ABSENT;
                    }
                }
            }
            return new ChunkData(posY, colour);
        }
    }

    public final MessageZoneMapRequest.ChunkKey key;
    public final ChunkData data;

    public MessageZoneMapResponse(MessageZoneMapRequest.ChunkKey key, ChunkData data) {
        this.key = key;
        this.data = data;
    }

    private static void write(MessageZoneMapResponse message, FriendlyByteBuf buf) {
        MessageZoneMapRequest.ChunkKey.write(message.key, buf);
        ChunkData.write(message.data, buf);
    }

    private static MessageZoneMapResponse read(FriendlyByteBuf buf) {
        return new MessageZoneMapResponse(MessageZoneMapRequest.ChunkKey.read(buf), ChunkData.read(buf));
    }

    /** Client-side zone map cache population. */
    public static void handleClient(MessageZoneMapResponse message, IPayloadContext context) {
        // TODO(M2.6+): port ZonePlannerMapDataClient#onChunkReceived, then replay the legacy handler (store the chunk
        // image for the zone planner map renderer).
        MessageManager.LOGGER.info(
                "[lib.messages] Received MessageZoneMapResponse (chunk {}, {}) - zone planner map data not migrated yet",
                message.key.chunkX(), message.key.chunkZ());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
