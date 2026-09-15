/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.datacomponent.gate;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * The gate variant triple of the M2.6 gate config schema, mirroring the {@code variant} sub-compound of
 * {@code buildcraft.silicon.gate.GateLogic#writeToNbt} (written by {@code buildcraft.silicon.gate.GateVariant#writeToNBT}
 * as three ordinal bytes: {@code logic}, {@code material}, {@code modifier}). The derived slot/argument counts use the
 * same formulas as the legacy {@code GateVariant} constructor.
 */
public record GateVariantData(EnumGateLogic logic, EnumGateMaterial material, EnumGateModifier modifier) {

    public static final GateVariantData DEFAULT = new GateVariantData(EnumGateLogic.AND, EnumGateMaterial.CLAY_BRICK,
        EnumGateModifier.NO_MODIFIER);

    public int numSlots() {
        return material.numSlots / modifier.slotDivisor;
    }

    public int numTriggerArgs() {
        return modifier.triggerParams;
    }

    public int numActionArgs() {
        return modifier.actionParams;
    }

    public CompoundTag writeToNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putByte("logic", (byte) logic.ordinal());
        nbt.putByte("material", (byte) material.ordinal());
        nbt.putByte("modifier", (byte) modifier.ordinal());
        return nbt;
    }

    public static GateVariantData readFromNbt(CompoundTag nbt) {
        return new GateVariantData(
            EnumGateLogic.getByOrdinal(nbt.getByteOr("logic", (byte) 0)),
            EnumGateMaterial.getByOrdinal(nbt.getByteOr("material", (byte) 0)),
            EnumGateModifier.getByOrdinal(nbt.getByteOr("modifier", (byte) 0)));
    }

    public void writeToBuf(FriendlyByteBuf buf) {
        buf.writeByte(logic.ordinal());
        buf.writeByte(material.ordinal());
        buf.writeByte(modifier.ordinal());
    }

    public static GateVariantData readFromBuf(FriendlyByteBuf buf) {
        return new GateVariantData(
            EnumGateLogic.getByOrdinal(buf.readUnsignedByte()),
            EnumGateMaterial.getByOrdinal(buf.readUnsignedByte()),
            EnumGateModifier.getByOrdinal(buf.readUnsignedByte()));
    }

    public static final StreamCodec<FriendlyByteBuf, GateVariantData> STREAM_CODEC = StreamCodec.ofMember(
        GateVariantData::writeToBuf, GateVariantData::readFromBuf);
}
