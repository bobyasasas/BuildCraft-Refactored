/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.net;

import java.util.UUID;
import buildcraft.lib.BCLib;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Legacy counterpart: {@code buildcraft.lib.net.MessageUpdateEntity} (1.20.1). Delivers an opaque payload blob to a
 * specific entity, which decodes it via {@code IPayloadReceiver#receivePayload}.
 *
 * <p>Wire format field set (identical to legacy):
 * <ul>
 * <li>LONG+LONG - entity uuid</li>
 * <li>MEDIUM(3-byte unsigned) - payload size</li>
 * <li>BYTE[size] - payload</li>
 * </ul>
 */
public class MessageUpdateEntity implements CustomPacketPayload {

    public static final Type<MessageUpdateEntity> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(BCLib.MOD_ID, "message_update_entity"));

    public static final StreamCodec<FriendlyByteBuf, MessageUpdateEntity> STREAM_CODEC = CustomPacketPayload.codec(
            MessageUpdateEntity::write, MessageUpdateEntity::read);

    public final UUID uuid;
    public final byte[] payload;

    public MessageUpdateEntity(Entity entity, byte[] payload) {
        this.uuid = entity.getUUID();
        this.payload = payload;
        if (payload.length > 1 << 24) {
            throw new IllegalStateException("Can't write out " + getPayloadSize() + " bytes!");
        }
    }

    public MessageUpdateEntity(UUID uuid, byte[] payload) {
        this.uuid = uuid;
        this.payload = payload;
    }

    public int getPayloadSize() {
        return payload == null ? 0 : payload.length;
    }

    private static void write(MessageUpdateEntity message, FriendlyByteBuf buf) {
        buf.writeUUID(message.uuid);
        int length = message.payload.length;
        buf.writeMedium(length);
        buf.writeBytes(message.payload);
    }

    private static MessageUpdateEntity read(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        int size = buf.readUnsignedMedium();
        byte[] payload = new byte[size];
        buf.readBytes(payload);
        return new MessageUpdateEntity(uuid, payload);
    }

    /** Server-side dispatch into {@code IPayloadReceiver} entities. */
    public static void handleServer(MessageUpdateEntity message, IPayloadContext context) {
        // TODO(M2.6+): port IPayloadReceiver + the entity lookup (level().getEntity(uuid)) dispatch, then replay the
        // legacy handler.
        MessageManager.LOGGER.info(
                "[lib.messages] Received MessageUpdateEntity for {} ({} bytes) - entity payload dispatch not migrated yet",
                message.uuid, message.getPayloadSize());
    }

    /** Client-side dispatch into {@code IPayloadReceiver} entities. */
    public static void handleClient(MessageUpdateEntity message, IPayloadContext context) {
        // TODO(M2.6+): port IPayloadReceiver + the entity lookup (level().getEntity(uuid)) dispatch, then replay the
        // legacy handler (warn on dropped messages for unknown entities).
        MessageManager.LOGGER.info(
                "[lib.messages] Received MessageUpdateEntity for {} ({} bytes) - entity payload dispatch not migrated yet",
                message.uuid, message.getPayloadSize());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
