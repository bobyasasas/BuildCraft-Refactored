/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.datacomponent.filter;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/**
 * Legacy-NBT &harr; component conversion for {@link BcFilter} (task M2.6 acceptance: the serialization layer must
 * round-trip old-save structures unchanged).
 *
 * <p>Component NBT shape (a list of dispatched parameter compounds, each byte-compatible with the legacy
 * {@code StatementTypeParam} parameter writers):
 * <pre>{ slots: [ { kind: "buildcraft:stack", stack: { id, Count, tag } }, ... ] }</pre>
 *
 * <p>Also carries the diamond-pipe filter inventory form ({@code items} list of legacy {@code ItemStack.save}
 * compounds) used by {@code buildcraft.transport.pipe.behaviour.PipeBehaviourDiamond} (NBT key {@code filters}, 9
 * stacks per pipe side) with the list written by
 * {@code buildcraft.lib.tile.item.ItemHandlerSimple#serializeNBT}.
 */
public final class BcFilterNbt {

    /** Component persistent codec: the value's NBT form is exactly the legacy-compatible compound, wrapped by the
     * vanilla {@link CompoundTag#CODEC} passthrough (the same mechanism vanilla uses for compound-carrying components
     * such as {@code minecraft:custom_data}). */
    public static final Codec<BcFilter> CODEC = CompoundTag.CODEC.xmap(BcFilterNbt::read, BcFilterNbt::write);

    private BcFilterNbt() {}

    public static CompoundTag write(BcFilter filter) {
        CompoundTag nbt = new CompoundTag();
        ListTag slots = new ListTag();
        for (BcFilterSpec slot : filter.slots()) {
            slots.add(slot.writeToNbt());
        }
        nbt.put("slots", slots);
        return nbt;
    }

    public static BcFilter read(CompoundTag nbt) {
        ListTag slots = nbt.getListOrEmpty("slots");
        List<BcFilterSpec> parsed = new ArrayList<>(slots.size());
        for (int i = 0; i < slots.size(); i++) {
            BcFilterSpec spec = BcFilterSpec.read(slots.getCompoundOrEmpty(i));
            if (spec != null) {
                parsed.add(spec);
            }
        }
        return new BcFilter(parsed);
    }

    // Diamond pipe filter inventory ({filters: {items: [...]}} in legacy tile NBT)

    public static CompoundTag writeStacks(List<BcItemStack> stacks) {
        CompoundTag nbt = new CompoundTag();
        ListTag items = new ListTag();
        for (BcItemStack stack : stacks) {
            items.add(stack.writeToNbt());
        }
        nbt.put("items", items);
        return nbt;
    }

    public static List<BcItemStack> readStacks(CompoundTag nbt) {
        ListTag items = nbt.getListOrEmpty("items");
        List<BcItemStack> stacks = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            stacks.add(BcItemStack.readFromNbt(items.getCompoundOrEmpty(i)));
        }
        return stacks;
    }
}
