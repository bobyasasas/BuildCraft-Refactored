/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.datacomponent.gate;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * One configured trigger or action slot of a gate, mirroring the per-slot compounds written by
 * {@code buildcraft.lib.statement.FullStatement#writeToNbt}: the statement itself under the {@code s} key (kind string
 * + {@code side} byte, as written by {@code buildcraft.silicon.gate.TriggerType}/{@code ActionType#writeToNbt}) plus
 * the set parameters under their numeric string keys ({@code "0"}, {@code "1"}, ...).
 *
 * <p>Parameters are keyed by their argument position (not list position) so that legacy gaps - a cleared middle
 * argument, which legacy persists by simply omitting that numeric key - survive a round trip unchanged.
 */
public record BcGateStatement(String kind, byte side, Map<Integer, BcGateParam> params) {

    /** Legacy {@code EnumPipePart#getIndex()} value of {@code CENTER} ({@code face == null}); legacy writes the
     * source part index of the statement wrapper, which is this for statements without a side. */
    public static final byte SIDE_CENTER = 6;

    public BcGateStatement {
        params = Map.copyOf(params);
    }

    public BcGateStatement(String kind, byte side) {
        this(kind, side, Map.of());
    }

    public CompoundTag writeToNbt() {
        CompoundTag nbt = new CompoundTag();
        CompoundTag s = new CompoundTag();
        s.putString("kind", kind);
        s.putByte("side", side);
        nbt.put("s", s);
        for (Map.Entry<Integer, BcGateParam> entry : params.entrySet()) {
            nbt.put(String.valueOf(entry.getKey()), entry.getValue().writeToNbt());
        }
        return nbt;
    }

    /** Reads one statement slot compound. Returns null when the slot holds no statement (legacy
     * {@code FullStatement#readFromNbt} leaves the statement null when the {@code s} kind is missing or empty).
     * Unknown parameter kinds are dropped, matching legacy (which reads them as null). */
    public static BcGateStatement readFromNbt(CompoundTag nbt) {
        CompoundTag s = nbt.getCompoundOrEmpty("s");
        String kind = s.getStringOr("kind", "");
        if (kind.isEmpty()) {
            return null;
        }
        byte side = s.getByteOr("side", (byte) 0);
        Map<Integer, BcGateParam> params = new LinkedHashMap<>();
        for (String key : nbt.keySet()) {
            if (key.equals("s")) {
                continue;
            }
            int index;
            try {
                index = Integer.parseInt(key);
            } catch (NumberFormatException ignored) {
                continue;
            }
            BcGateParam param = BcGateParam.read(nbt.getCompoundOrEmpty(key));
            if (param != null) {
                params.put(index, param);
            }
        }
        return new BcGateStatement(kind, side, params);
    }

    public void writeToBuf(FriendlyByteBuf buf) {
        buf.writeUtf(kind);
        buf.writeByte(side);
        buf.writeVarInt(params.size());
        for (Map.Entry<Integer, BcGateParam> entry : params.entrySet()) {
            buf.writeVarInt(entry.getKey());
            BcGateParam.writeParamToBuf(entry.getValue(), buf);
        }
    }

    public static BcGateStatement readFromBuf(FriendlyByteBuf buf) {
        String kind = buf.readUtf();
        byte side = buf.readByte();
        int size = buf.readVarInt();
        Map<Integer, BcGateParam> params = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            int index = buf.readVarInt();
            params.put(index, BcGateParam.readParamFromBuf(buf));
        }
        return new BcGateStatement(kind, side, params);
    }
}
