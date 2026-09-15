/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.transport.wire;

import java.util.LinkedHashMap;
import java.util.Map;
import buildcraft.lib.BCLib;
import buildcraft.lib.net.MessageManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Legacy counterpart: {@code buildcraft.transport.wire.MessageWireSystemsPowered} (1.20.1). Marks which synced wire
 * systems are currently powered on the client.
 *
 * <p>Wire format field set (identical to legacy):
 * <ul>
 * <li>INT - entry count</li>
 * <li>per entry: INT wires hash code, BOOL powered</li>
 * </ul>
 */
public class MessageWireSystemsPowered implements CustomPacketPayload {

    public static final Type<MessageWireSystemsPowered> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(BCLib.MOD_ID, "message_wire_systems_powered"));

    public static final StreamCodec<FriendlyByteBuf, MessageWireSystemsPowered> STREAM_CODEC = CustomPacketPayload.codec(
            MessageWireSystemsPowered::write, MessageWireSystemsPowered::read);

    public final Map<Integer, Boolean> hashesPowered;

    public MessageWireSystemsPowered(Map<Integer, Boolean> hashesPowered) {
        this.hashesPowered = new LinkedHashMap<>(hashesPowered);
    }

    private static void write(MessageWireSystemsPowered message, FriendlyByteBuf buf) {
        buf.writeInt(message.hashesPowered.size());
        message.hashesPowered.forEach((wiresHashCode, powered) -> {
            buf.writeInt(wiresHashCode);
            buf.writeBoolean(powered);
        });
    }

    private static MessageWireSystemsPowered read(FriendlyByteBuf buf) {
        Map<Integer, Boolean> hashesPowered = new LinkedHashMap<>();
        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            hashesPowered.put(buf.readInt(), buf.readBoolean());
        }
        return new MessageWireSystemsPowered(hashesPowered);
    }

    /** Client-side powered-state application onto the synced wire systems. */
    public static void handleClient(MessageWireSystemsPowered message, IPayloadContext context) {
        // TODO(M2.6+): port WireManager#poweredClient + the client wire-system lookup, then replay the legacy handler
        // (set/clear the powered flag per wire part of the matching system).
        MessageManager.LOGGER.info(
                "[lib.messages] Received MessageWireSystemsPowered ({} entries) - client wire systems not migrated yet",
                message.hashesPowered.size());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
