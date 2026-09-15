/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.net;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import buildcraft.lib.BCLib;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Legacy counterpart: {@code buildcraft.lib.net.MessageDebugResponse} (1.20.1). Carries the two debug text columns
 * shown by the debugger item.
 *
 * <p>Wire format field set (identical to legacy): INT left-count, left-count text entries, INT right-count, right-count
 * text entries. Deviation vs legacy (deliberate, recorded for M2.5): legacy held {@code List<Component>} and encoded
 * each entry via {@code FriendlyByteBuf#writeComponent}; 26.1.2 removed that helper and the only replacement
 * ({@code ComponentSerialization}) drags vanilla registries into every class-load, so this port carries plain
 * {@code String} lines written with {@link FriendlyByteBuf#writeUtf}. TODO(M2.6+): upgrade back to components if the
 * debug display port needs formatting.
 */
public class MessageDebugResponse implements CustomPacketPayload {

    public static final Type<MessageDebugResponse> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(BCLib.MOD_ID, "message_debug_response"));

    public static final StreamCodec<FriendlyByteBuf, MessageDebugResponse> STREAM_CODEC = CustomPacketPayload.codec(
            MessageDebugResponse::write, MessageDebugResponse::read);

    public final List<String> left;
    public final List<String> right;

    public MessageDebugResponse() {
        this(List.of(), List.of());
    }

    public MessageDebugResponse(List<String> left, List<String> right) {
        this.left = Collections.unmodifiableList(new ArrayList<>(left));
        this.right = Collections.unmodifiableList(new ArrayList<>(right));
    }

    private static void write(MessageDebugResponse message, FriendlyByteBuf buf) {
        buf.writeInt(message.left.size());
        message.left.forEach(buf::writeUtf);
        buf.writeInt(message.right.size());
        message.right.forEach(buf::writeUtf);
    }

    private static MessageDebugResponse read(FriendlyByteBuf buf) {
        List<String> left = readLines(buf);
        List<String> right = readLines(buf);
        return new MessageDebugResponse(left, right);
    }

    private static List<String> readLines(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<String> lines = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            lines.add(buf.readUtf());
        }
        return lines;
    }

    /** Client-side display of the received debug columns. */
    public static void handleClient(MessageDebugResponse message, IPayloadContext context) {
        // TODO(M2.6+): port buildcraft.lib.debug.ClientDebuggables (SERVER_LEFT/SERVER_RIGHT buffers) and replay the
        // legacy handler.
        MessageManager.LOGGER.info(
                "[lib.messages] Received MessageDebugResponse ({} left, {} right lines) - client debug display not migrated yet",
                message.left.size(), message.right.size());
    }

    /** Server-side handler for the M2.5 login handshake: logs the round trip completion. */
    public static void handleServer(MessageDebugResponse message, IPayloadContext context) {
        MessageManager.LOGGER.info(
                "[lib.messages][handshake] server received MessageDebugResponse from player {} - BuildCraft network handshake complete",
                context.player().getName().getString());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
