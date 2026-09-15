/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.datacomponent.filter;

import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Item filter component value ({@code buildcraftlib:filter}): the filter slot list, each slot holding one dispatched
 * {@link BcFilterSpec} description (null entries are not representable - an absent description is simply not in the
 * list, mirroring how legacy only wrote filter compounds for configured slots).
 *
 * <p>This generalizes the two legacy filter carriers:
 * <ul>
 * <li>the per-slot filter stacks of {@code buildcraft.transport.pipe.behaviour.PipeBehaviourDiamond} (NBT
 * {@code filters}, 9 stacks per side via {@code buildcraft.lib.tile.item.ItemHandlerSimple});</li>
 * <li>the statement-parameter filters ({@code buildcraft.lib.inventory.filter.StatementParameterStackFilter} and
 * friends) whose compounds are dispatched by {@code kind}.</li>
 * </ul>
 *
 * <p>Legacy-counterpart note: the legacy tree serializes filter stacks but never the dispatching specs, so the
 * {@code slots} list shape is the M2.6 component-level packaging; the per-slot compounds it contains are
 * byte-compatible with the legacy parameter writers.
 */
public record BcFilter(List<BcFilterSpec> slots) {

    public static final BcFilter EMPTY = new BcFilter(List.of());

    /** Persistent codec: encodes to exactly the NBT shape written by {@link BcFilterNbt#write}, so an old-save filter
     * compound can be adopted as a component value without translation. */
    public static final Codec<BcFilter> CODEC = Codec.lazyInitialized(() -> BcFilterNbt.CODEC);

    public static final StreamCodec<FriendlyByteBuf, BcFilter> STREAM_CODEC = StreamCodec.ofMember(
        BcFilter::writeToBuf, BcFilter::readFromBuf);

    public void writeToBuf(FriendlyByteBuf buf) {
        buf.writeVarInt(slots.size());
        for (BcFilterSpec slot : slots) {
            BcFilterSpec.writeSpecToBuf(slot, buf);
        }
    }

    public static BcFilter readFromBuf(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<BcFilterSpec> slots = new java.util.ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            slots.add(BcFilterSpec.readSpecFromBuf(buf));
        }
        return new BcFilter(slots);
    }
}
