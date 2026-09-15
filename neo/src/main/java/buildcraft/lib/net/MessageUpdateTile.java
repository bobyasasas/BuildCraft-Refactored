/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.net;

import buildcraft.lib.misc.MessageUtil;
import buildcraft.lib.BCLib;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Legacy counterpart: {@code buildcraft.lib.net.MessageUpdateTile} (1.20.1). Delivers an opaque payload blob to the
 * block entity at a position, which decodes it via {@code IPayloadReceiver#receivePayload}.
 *
 * <p>Wire format field set (identical to legacy):
 * <ul>
 * <li>BC-blockpos (3 varints) - pos</li>
 * <li>MEDIUM(3-byte unsigned) - payload size</li>
 * <li>BYTE[size] - payload</li>
 * </ul>
 */
public class MessageUpdateTile implements CustomPacketPayload {

    public static final Type<MessageUpdateTile> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(BCLib.MOD_ID, "message_update_tile"));

    public static final StreamCodec<FriendlyByteBuf, MessageUpdateTile> STREAM_CODEC = CustomPacketPayload.codec(
            MessageUpdateTile::write, MessageUpdateTile::read);

    public final BlockPos pos;
    public final byte[] payload;

    public MessageUpdateTile(BlockPos pos, byte[] payload) {
        this.pos = pos;
        this.payload = payload;
        if (getPayloadSize() > 1 << 24) {
            throw new IllegalStateException("Can't write out " + getPayloadSize() + " bytes!");
        }
    }

    public int getPayloadSize() {
        return payload == null ? 0 : payload.length;
    }

    private static void write(MessageUpdateTile message, FriendlyByteBuf buf) {
        MessageUtil.writeBlockPos(buf, message.pos);
        int length = message.payload.length;
        buf.writeMedium(length);
        buf.writeBytes(message.payload);
    }

    private static MessageUpdateTile read(FriendlyByteBuf buf) {
        BlockPos pos = MessageUtil.readBlockPos(buf);
        int size = buf.readUnsignedMedium();
        byte[] payload = new byte[size];
        buf.readBytes(payload);
        return new MessageUpdateTile(pos, payload);
    }

    /** Server-side dispatch into {@code IPayloadReceiver} tiles. */
    public static void handleServer(MessageUpdateTile message, IPayloadContext context) {
        // TODO(M2.6+): port IPayloadReceiver + TileBC_Neptune receive dispatch, then replay the legacy handler.
        MessageManager.LOGGER.info(
                "[lib.messages] Received MessageUpdateTile at {} ({} bytes) - tile payload dispatch not migrated yet",
                message.pos, message.getPayloadSize());
    }

    /** Client-side dispatch into {@code IPayloadReceiver} tiles. */
    public static void handleClient(MessageUpdateTile message, IPayloadContext context) {
        // TODO(M2.6+): port IPayloadReceiver + TileBC_Neptune receive dispatch (and the warning for non-receiving
        // tiles), then replay the legacy handler.
        MessageManager.LOGGER.info(
                "[lib.messages] Received MessageUpdateTile at {} ({} bytes) - tile payload dispatch not migrated yet",
                message.pos, message.getPayloadSize());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
