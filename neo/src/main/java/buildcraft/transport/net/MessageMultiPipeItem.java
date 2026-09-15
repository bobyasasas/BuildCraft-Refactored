/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.transport.net;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import buildcraft.lib.BCLib;
import buildcraft.lib.net.MessageManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Legacy counterpart: {@code buildcraft.transport.net.MessageMultiPipeItem} (1.20.1). Batch-syncs items travelling
 * through item pipes to the client renderer.
 *
 * <p>Wire format field set (identical to legacy):
 * <ul>
 * <li>SHORT - block count (capped at MAX_POSITIONS on write)</li>
 * <li>per block: LONG-packed blockpos, BYTE item count (capped at MAX_ITEMS_PER_PIPE), items[]</li>
 * <li>per item: VARINT stackId, BYTE stackCount, BOOL toCenter, VARINT side ordinal, BOOL+VARINT nullable colour
 * ordinal, BYTE timeToDest</li>
 * </ul>
 */
public class MessageMultiPipeItem implements CustomPacketPayload {

    public static final Type<MessageMultiPipeItem> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(BCLib.MOD_ID, "message_multi_pipe_item"));

    public static final StreamCodec<FriendlyByteBuf, MessageMultiPipeItem> STREAM_CODEC = CustomPacketPayload.codec(
            MessageMultiPipeItem::write, MessageMultiPipeItem::read);

    public static final int MAX_ITEMS_PER_PIPE = 10;
    public static final int MAX_POSITIONS = 4000;

    /** Mirrors legacy {@code MessageMultiPipeItem.TravellingItemData}. */
    public static final class TravellingItemData {
        public final int stackId;
        public final byte stackCount;
        public final boolean toCenter;
        public final Direction side;
        public final @Nullable DyeColor colour;
        public final byte timeToDest;

        public TravellingItemData(int stackId, byte stackCount, boolean toCenter, Direction side, @Nullable DyeColor colour, byte timeToDest) {
            this.stackId = stackId;
            this.stackCount = stackCount;
            this.toCenter = toCenter;
            this.side = side;
            this.colour = colour;
            this.timeToDest = timeToDest;
        }

        static TravellingItemData read(FriendlyByteBuf buf) {
            int stackId = buf.readVarInt();
            byte stackCount = buf.readByte();
            boolean toCenter = buf.readBoolean();
            Direction side = buf.readEnum(Direction.class);
            DyeColor colour = buildcraft.lib.misc.MessageUtil.readEnumOrNull(buf, DyeColor.class);
            byte timeToDest = buf.readByte();
            return new TravellingItemData(stackId, stackCount, toCenter, side, colour, timeToDest);
        }

        void write(FriendlyByteBuf buf) {
            buf.writeVarInt(stackId);
            buf.writeByte(stackCount);
            buf.writeBoolean(toCenter);
            buf.writeEnum(side);
            buildcraft.lib.misc.MessageUtil.writeEnumOrNull(buf, colour);
            buf.writeByte(timeToDest);
        }
    }

    public final Map<BlockPos, List<TravellingItemData>> items = new LinkedHashMap<>();

    private static void write(MessageMultiPipeItem message, FriendlyByteBuf buf) {
        int blockCount = Math.min(message.items.size(), MAX_POSITIONS);
        buf.writeShort(blockCount);
        int blockIndex = 0;
        for (Map.Entry<BlockPos, List<TravellingItemData>> entry : message.items.entrySet()) {
            buf.writeBlockPos(entry.getKey());
            List<TravellingItemData> list = entry.getValue();
            int itemCount = Math.min(list.size(), MAX_ITEMS_PER_PIPE);
            buf.writeByte(itemCount);
            for (int i = 0; i < itemCount; i++) {
                list.get(i).write(buf);
            }
            if (++blockIndex >= blockCount) {
                break;
            }
        }
    }

    private static MessageMultiPipeItem read(FriendlyByteBuf buf) {
        MessageMultiPipeItem message = new MessageMultiPipeItem();
        int blockCount = buf.readShort();
        for (int b = 0; b < blockCount; b++) {
            BlockPos pos = buf.readBlockPos();
            List<TravellingItemData> posItems = new ArrayList<>();
            message.items.put(pos, posItems);
            int itemCount = buf.readUnsignedByte();
            for (int i = 0; i < itemCount; i++) {
                posItems.add(TravellingItemData.read(buf));
            }
        }
        return message;
    }

    /** Adds one travelling item, honouring the legacy per-pipe and per-message caps. */
    public void append(BlockPos pos, int stackId, byte stackCount, boolean toCenter, Direction side, DyeColor colour, byte timeToDest) {
        List<TravellingItemData> list = items.get(pos);
        if (list == null) {
            if (items.size() >= MAX_POSITIONS) {
                return;
            }
            list = new ArrayList<>();
            items.put(pos, list);
        }
        if (list.size() >= MAX_ITEMS_PER_PIPE) {
            return;
        }
        list.add(new TravellingItemData(stackId, stackCount, toCenter, side, colour, timeToDest));
    }

    /** Client-side travelling item renderer feed. */
    public static void handleClient(MessageMultiPipeItem message, IPayloadContext context) {
        // TODO(M2.6+): port PipeFlowItems#handleClientReceviedItems + the pipe lookup, then replay the legacy handler.
        MessageManager.LOGGER.info(
                "[lib.messages] Received MessageMultiPipeItem ({} pipes) - client pipe flow rendering not migrated yet",
                message.items.size());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
