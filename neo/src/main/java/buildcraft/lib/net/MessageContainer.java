/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.net;

import buildcraft.lib.BCLib;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Legacy counterpart: {@code buildcraft.lib.net.MessageContainer} (1.20.1). Wrapper payload that routes GUI container
 * sub-messages to the open {@code ContainerBC_Neptune}.
 *
 * <p>Wire format field set (identical to legacy):
 * <ul>
 * <li>INT - windowId</li>
 * <li>USHORT - msgId (read unsigned)</li>
 * <li>USHORT - payload size</li>
 * <li>BYTE[size] - payload</li>
 * </ul>
 */
public class MessageContainer implements CustomPacketPayload {

    public static final Type<MessageContainer> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(BCLib.MOD_ID, "message_container"));

    /** Encodes {@code msgId} with its original unsigned-short range and prefixes the blob with its length. */
    public static final StreamCodec<FriendlyByteBuf, MessageContainer> STREAM_CODEC = CustomPacketPayload.codec(
            MessageContainer::write, MessageContainer::read);

    public final int windowId;
    public final int msgId;
    public final byte[] payload;

    public MessageContainer(int windowId, int msgId, byte[] payload) {
        this.windowId = windowId;
        this.msgId = msgId;
        this.payload = payload;
    }

    private static void write(MessageContainer message, FriendlyByteBuf buf) {
        buf.writeInt(message.windowId);
        buf.writeShort(message.msgId);
        buf.writeShort(message.payload.length);
        buf.writeBytes(message.payload);
    }

    private static MessageContainer read(FriendlyByteBuf buf) {
        int windowId = buf.readInt();
        int msgId = buf.readUnsignedShort();
        int payloadSize = buf.readUnsignedShort();
        byte[] payload = new byte[payloadSize];
        buf.readBytes(payload);
        return new MessageContainer(windowId, msgId, payload);
    }

    /** Client-side dispatch into the open BuildCraft container. */
    public static void handleClient(MessageContainer message, IPayloadContext context) {
        // TODO(M2.6+): port ContainerBC_Neptune#readMessage (GUI container sub-message routing) and the
        // MessageUtil#ensureEmpty completeness check, then replay the legacy handler here.
        MessageManager.LOGGER.info(
                "[lib.messages] Received MessageContainer for window {} msg {} ({} bytes) - container logic not migrated yet",
                message.windowId, message.msgId, message.payload.length);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
