/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.transport.wire;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import buildcraft.lib.BCLib;
import buildcraft.lib.net.MessageManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Legacy counterpart: {@code buildcraft.transport.wire.MessageWireSystems} (1.20.1). Syncs the visual wire systems
 * attached to pipe holders to the client.
 *
 * <p>Wire format field set (identical to legacy):
 * <ul>
 * <li>INT - system count</li>
 * <li>per system: INT wires hash code, INT element count, elements[]</li>
 * <li>per element: INT type ordinal, BC-blockpos (3 varints), INT wire-part ordinal (type 0) or INT emitter-side
 * ordinal (type 1)</li>
 * </ul>
 * Legacy modelled elements as {@code WireSystem.WireElement}; the full wire-system classes migrate later, so the
 * elements travel as a data record here.
 */
public class MessageWireSystems implements CustomPacketPayload {

    public static final Type<MessageWireSystems> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(BCLib.MOD_ID, "message_wire_systems"));

    public static final StreamCodec<FriendlyByteBuf, MessageWireSystems> STREAM_CODEC = CustomPacketPayload.codec(
            MessageWireSystems::write, MessageWireSystems::read);

    /** Mirrors legacy {@code WireSystem.WireElement}: type ordinal + block position + type-specific ordinal detail. */
    public record WireElementData(int type, BlockPos blockPos, int detail) {
        public static final int TYPE_WIRE_PART = 0;
        public static final int TYPE_EMITTER_SIDE = 1;
    }

    public final Map<Integer, List<WireElementData>> wireSystems;

    public MessageWireSystems(Map<Integer, List<WireElementData>> wireSystems) {
        this.wireSystems = new LinkedHashMap<>(wireSystems);
    }

    private static void write(MessageWireSystems message, FriendlyByteBuf buf) {
        buf.writeInt(message.wireSystems.size());
        message.wireSystems.forEach((wiresHashCode, elements) -> {
            buf.writeInt(wiresHashCode);
            buf.writeInt(elements.size());
            for (WireElementData element : elements) {
                buf.writeInt(element.type());
                buildcraft.lib.misc.MessageUtil.writeBlockPos(buf, element.blockPos());
                buf.writeInt(element.detail());
            }
        });
    }

    private static MessageWireSystems read(FriendlyByteBuf buf) {
        Map<Integer, List<WireElementData>> wireSystems = new LinkedHashMap<>();
        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            int wiresHashCode = buf.readInt();
            int localCount = buf.readInt();
            List<WireElementData> elements = new ArrayList<>(localCount);
            for (int j = 0; j < localCount; j++) {
                int type = buf.readInt();
                BlockPos pos = buildcraft.lib.misc.MessageUtil.readBlockPos(buf);
                int detail = buf.readInt();
                elements.add(new WireElementData(type, pos, detail));
            }
            wireSystems.put(wiresHashCode, elements);
        }
        return new MessageWireSystems(wireSystems);
    }

    /** Client-side wire system render cache update. */
    public static void handleClient(MessageWireSystems message, IPayloadContext context) {
        // TODO(M2.6+): port WireSystem/WireElement + ClientWireSystems, then replay the legacy handler (replace the
        // whole client-side wire system map).
        MessageManager.LOGGER.info(
                "[lib.messages] Received MessageWireSystems ({} systems) - client wire systems not migrated yet",
                message.wireSystems.size());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
