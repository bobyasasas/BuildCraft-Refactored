/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.misc;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.network.codec.StreamEncoder;

/**
 * Partial port of the legacy {@code buildcraft.lib.misc.MessageUtil} (task M2.5 network layer): only the wire helpers
 * that the 16 payload classes actually need.
 *
 * <p>TODO(M2.6+): grow this class as the remaining legacy helpers migrate (delayed tasks, GUI opening, block state
 * serialisation). The method signatures intentionally match the legacy ones so the full port can slot in.
 */
public final class MessageUtil {

    private MessageUtil() {}

    /** Legacy wire format: three varints for x, y and z (NOT the vanilla long-packed block position). */
    public static void writeBlockPos(FriendlyByteBuf buffer, BlockPos pos) {
        buffer.writeVarInt(pos.getX());
        buffer.writeVarInt(pos.getY());
        buffer.writeVarInt(pos.getZ());
    }

    /** Legacy wire format: three varints for x, y and z (NOT the vanilla long-packed block position). */
    public static BlockPos readBlockPos(FriendlyByteBuf buffer) {
        return new BlockPos(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    /**
     * {@link FriendlyByteBuf#writeEnum(Enum)} can only write *actual* enum values - so not null. This method allows for
     * writing an enum value, or null (legacy wire format: presence boolean followed by the varint ordinal).
     */
    public static <E extends Enum<E>> void writeEnumOrNull(FriendlyByteBuf buffer, E value) {
        if (value == null) {
            buffer.writeBoolean(false);
        } else {
            buffer.writeBoolean(true);
            buffer.writeEnum(value);
        }
    }

    /**
     * {@link FriendlyByteBuf#readEnum(Class)} can only read *actual* enum values - so not null. This method allows for
     * reading an enum value, or null (legacy wire format: presence boolean followed by the varint ordinal).
     */
    public static <E extends Enum<E>> E readEnumOrNull(FriendlyByteBuf buffer, Class<E> clazz) {
        if (buffer.readBoolean()) {
            return buffer.readEnum(clazz);
        } else {
            return null;
        }
    }

    /** Adapts the BC block position helpers to the {@link StreamEncoder} shape for reusable codecs. */
    public static StreamEncoder<FriendlyByteBuf, BlockPos> blockPosEncoder() {
        return (buf, pos) -> writeBlockPos(buf, pos);
    }

    /** Adapts the BC block position helpers to the {@link StreamDecoder} shape for reusable codecs. */
    public static StreamDecoder<FriendlyByteBuf, BlockPos> blockPosDecoder() {
        return MessageUtil::readBlockPos;
    }

    /** Returns the given {@link ByteBuf} as a {@link FriendlyByteBuf} (legacy {@code MessageUtil.asPacketBuffer}). */
    public static FriendlyByteBuf asPacketBuffer(ByteBuf buf) {
        if (buf instanceof FriendlyByteBuf friendly) {
            return friendly;
        }
        return new FriendlyByteBuf(buf);
    }
}
