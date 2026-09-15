/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.core.marker.volume;

import java.util.ArrayList;
import java.util.List;
import buildcraft.lib.BCLib;
import buildcraft.lib.net.MessageManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Legacy counterpart: {@code buildcraft.core.marker.volume.MessageVolumeBoxes} (1.20.1). Syncs every volume box to the
 * client as length-prefixed serialized blobs.
 *
 * <p>Wire format field set (identical to legacy):
 * <ul>
 * <li>INT - box count</li>
 * <li>per box: VARINT blob size, BYTE[size] serialized {@code VolumeBox}</li>
 * </ul>
 * The legacy constructor serialized each {@code VolumeBox} itself; the volume box classes migrate later, so the neo
 * payload carries the pre-serialized blobs ({@code VolumeBox#toBytes} output) unchanged.
 */
public class MessageVolumeBoxes implements CustomPacketPayload {

    public static final Type<MessageVolumeBoxes> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(BCLib.MOD_ID, "message_volume_boxes"));

    public static final StreamCodec<FriendlyByteBuf, MessageVolumeBoxes> STREAM_CODEC = CustomPacketPayload.codec(
            MessageVolumeBoxes::write, MessageVolumeBoxes::read);

    public final List<byte[]> buffers;

    public MessageVolumeBoxes(List<byte[]> buffers) {
        this.buffers = new ArrayList<>(buffers);
    }

    private static void write(MessageVolumeBoxes message, FriendlyByteBuf buf) {
        buf.writeInt(message.buffers.size());
        for (byte[] localBuffer : message.buffers) {
            buf.writeVarInt(localBuffer.length);
            buf.writeBytes(localBuffer);
        }
    }

    private static MessageVolumeBoxes read(FriendlyByteBuf buf) {
        List<byte[]> buffers = new ArrayList<>();
        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            int bytes = buf.readVarInt();
            byte[] data = new byte[bytes];
            buf.readBytes(data);
            buffers.add(data);
        }
        return new MessageVolumeBoxes(buffers);
    }

    /** Client-side volume box reconciliation. */
    public static void handleClient(MessageVolumeBoxes message, IPayloadContext context) {
        // TODO(M2.6+): port VolumeBox (decode each blob) + ClientVolumeBoxes reconciliation, then replay the legacy
        // handler (remove stale boxes, update equal ones, add new ones with Addon#onAdded).
        MessageManager.LOGGER.info(
                "[lib.messages] Received MessageVolumeBoxes ({} boxes) - client volume boxes not migrated yet",
                message.buffers.size());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
