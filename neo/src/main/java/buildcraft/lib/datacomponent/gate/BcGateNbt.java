/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.datacomponent.gate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;

import net.minecraft.nbt.CompoundTag;

/**
 * Legacy-NBT &harr; component conversion for {@link BcGateConfig}. The compound shape is byte-compatible with the
 * legacy {@code gate_data} payload of {@code buildcraft.silicon.item.ItemGateCopier} =
 * {@code buildcraft.silicon.gate.GateLogic#writeToNbt} (minus {@code wireBroadcasts}, which the copier removes):
 * <pre>
 * {
 *   variant: { logic, material, modifier },  // ordinal bytes, GateVariant#writeToNBT
 *   connections: short,                      // AND/OR grouping bitmask
 *   "trigger[i]": { s: { kind, side }, "p": param... },  // FullStatement#writeToNbt
 *   "action[i]":  { s: { kind, side }, "p": param... },
 *   triggerOn: short, actionOn: short        // runtime glow bitmasks, kept for shape parity
 * }
 * </pre>
 */
public final class BcGateNbt {

    /** Component persistent codec: the value's NBT form is exactly the legacy {@code gate_data} compound, wrapped by
     * the vanilla {@link CompoundTag#CODEC} passthrough. */
    public static final Codec<BcGateConfig> CODEC = CompoundTag.CODEC.xmap(BcGateNbt::read, BcGateNbt::write);

    private BcGateNbt() {}

    public static CompoundTag write(BcGateConfig config) {
        CompoundTag nbt = new CompoundTag();
        nbt.put("variant", config.variant().writeToNbt());
        nbt.putShort("connections", config.connections());
        List<BcGateSlot> slots = config.slots();
        for (int i = 0; i < slots.size(); i++) {
            slots.get(i).writeTriggerToNbt(nbt, i);
            slots.get(i).writeActionToNbt(nbt, i);
        }
        nbt.putShort("triggerOn", config.triggerOn());
        nbt.putShort("actionOn", config.actionOn());
        return nbt;
    }

    public static BcGateConfig read(CompoundTag nbt) {
        GateVariantData variant = GateVariantData.readFromNbt(nbt.getCompoundOrEmpty("variant"));
        short connections = nbt.getShortOr("connections", (short) 0);
        int numSlots = variant.numSlots();
        List<BcGateSlot> slots = new ArrayList<>(numSlots);
        for (int i = 0; i < numSlots; i++) {
            // legacy only writes "trigger[i]"/"action[i]" for configured statements, and readConfigData treats a
            // missing key (or one without an "s" kind) as an empty slot
            BcGateStatement trigger = readSlotStatement(nbt, "trigger[" + i + "]");
            BcGateStatement action = readSlotStatement(nbt, "action[" + i + "]");
            slots.add(new BcGateSlot(Optional.ofNullable(trigger), Optional.ofNullable(action)));
        }
        short triggerOn = nbt.getShortOr("triggerOn", (short) 0);
        short actionOn = nbt.getShortOr("actionOn", (short) 0);
        return new BcGateConfig(variant, connections, slots, triggerOn, actionOn);
    }

    private static BcGateStatement readSlotStatement(CompoundTag nbt, String key) {
        if (!nbt.contains(key)) {
            return null;
        }
        return BcGateStatement.readFromNbt(nbt.getCompoundOrEmpty(key));
    }
}
