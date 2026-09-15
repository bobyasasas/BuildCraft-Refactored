/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.net;

import java.util.UUID;
import buildcraft.lib.BCLib;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Legacy counterpart: {@code buildcraft.lib.net.MessageDebugRequest} (1.20.1). Asks the server (or, for the M2.5
 * handshake demo, the client) for debug information about a block entity or an entity.
 *
 * <p>Wire format field set (identical to legacy):
 * <ul>
 * <li>BOOL - isEntity</li>
 * <li>if isEntity: LONG+LONG - uuid</li>
 * <li>else: LONG(blockpos) - pos, VARINT - side ordinal</li>
 * </ul>
 */
public class MessageDebugRequest implements CustomPacketPayload {

    public static final Type<MessageDebugRequest> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(BCLib.MOD_ID, "message_debug_request"));

    public static final StreamCodec<FriendlyByteBuf, MessageDebugRequest> STREAM_CODEC = CustomPacketPayload.codec(
            MessageDebugRequest::write, MessageDebugRequest::read);

    public final boolean isEntity;
    public final BlockPos pos;
    public final Direction side;
    public final UUID uuid;

    public static MessageDebugRequest forBlock(BlockPos pos, Direction side) {
        return new MessageDebugRequest(false, pos, side, null);
    }

    public static MessageDebugRequest forEntity(UUID uuid) {
        return new MessageDebugRequest(true, null, null, uuid);
    }

    private MessageDebugRequest(boolean isEntity, BlockPos pos, Direction side, UUID uuid) {
        this.isEntity = isEntity;
        this.pos = pos;
        this.side = side;
        this.uuid = uuid;
    }

    private static void write(MessageDebugRequest message, FriendlyByteBuf buf) {
        buf.writeBoolean(message.isEntity);
        if (message.isEntity) {
            buf.writeUUID(message.uuid);
        } else {
            buf.writeBlockPos(message.pos);
            buf.writeEnum(message.side);
        }
    }

    private static MessageDebugRequest read(FriendlyByteBuf buf) {
        boolean isEntity = buf.readBoolean();
        if (isEntity) {
            return forEntity(buf.readUUID());
        }
        return forBlock(buf.readBlockPos(), buf.readEnum(Direction.class));
    }

    /** Server-side debug dispatch (debugger item inspecting a block entity or entity). */
    public static void handleServer(MessageDebugRequest message, IPayloadContext context) {
        // TODO(M2.6+): port IDebuggable#getDebugInfo dispatch + ItemDebugger.isShowDebugInfo guard, then replay the
        // legacy handler (reply with a MessageDebugResponse carrying the debug lines).
        MessageManager.LOGGER.info(
                "[lib.messages] Received MessageDebugRequest (isEntity={}, {}) from {} - debug logic not migrated yet",
                message.isEntity, message.isEntity ? message.uuid : message.pos, context.player().getName().getString());
    }

    /** Client-side handler used by the M2.5 login handshake: echoes a {@link MessageDebugResponse} back. */
    public static void handleClient(MessageDebugRequest message, IPayloadContext context) {
        MessageManager.LOGGER.info(
                "[lib.messages][handshake] client received MessageDebugRequest (isEntity={}, {}), replying MessageDebugResponse",
                message.isEntity, message.isEntity ? message.uuid : message.pos);
        context.reply(new MessageDebugResponse());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
