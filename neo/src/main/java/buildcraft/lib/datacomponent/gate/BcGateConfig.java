/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.datacomponent.gate;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Gate configuration component value ({@code buildcraftlib:gate_config}): the payload the legacy
 * {@code buildcraft.silicon.item.ItemGateCopier} stores under the item NBT key {@code gate_data}, i.e.
 * {@code buildcraft.silicon.gate.GateLogic#writeToNbt} minus the {@code wireBroadcasts} runtime state (which the
 * copier explicitly removes before storing). The same compound is what the copier feeds back into
 * {@code GateLogic#readConfigData} when pasting.
 */
public record BcGateConfig(GateVariantData variant, short connections, List<BcGateSlot> slots, short triggerOn,
    short actionOn) {

    public static final BcGateConfig DEFAULT = new BcGateConfig(GateVariantData.DEFAULT, (short) 0, List.of(
        BcGateSlot.EMPTY), (short) 0, (short) 0);

    public BcGateConfig {
        slots = List.copyOf(slots);
    }

    /** Persistent codec: encodes to exactly the legacy {@code gate_data} compound shape, so an old-save
     * {@code gate_data} compound can be adopted as a component value without translation. */
    public static final Codec<BcGateConfig> CODEC = Codec.lazyInitialized(() -> BcGateNbt.CODEC);

    public static final StreamCodec<FriendlyByteBuf, BcGateConfig> STREAM_CODEC = StreamCodec.ofMember(
        BcGateConfig::writeToBuf, BcGateConfig::readFromBuf);

    public void writeToBuf(FriendlyByteBuf buf) {
        variant.writeToBuf(buf);
        buf.writeShort(connections);
        buf.writeVarInt(slots.size());
        for (BcGateSlot slot : slots) {
            slot.writeToBuf(buf);
        }
        buf.writeShort(triggerOn);
        buf.writeShort(actionOn);
    }

    public static BcGateConfig readFromBuf(FriendlyByteBuf buf) {
        GateVariantData variant = GateVariantData.readFromBuf(buf);
        short connections = buf.readShort();
        int size = buf.readVarInt();
        List<BcGateSlot> slots = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            slots.add(BcGateSlot.readFromBuf(buf));
        }
        short triggerOn = buf.readShort();
        short actionOn = buf.readShort();
        return new BcGateConfig(variant, connections, slots, triggerOn, actionOn);
    }
}
