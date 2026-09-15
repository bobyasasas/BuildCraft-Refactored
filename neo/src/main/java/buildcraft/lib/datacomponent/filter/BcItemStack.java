/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.datacomponent.filter;

import java.util.Optional;

import io.netty.buffer.ByteBuf;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * Registry-free stand-in for a serialized {@code ItemStack}, shaped exactly like the legacy 1.20.1
 * {@code ItemStack.save(CompoundTag)} output that {@code buildcraft.lib.tile.item.ItemHandlerSimple#serializeNBT} and
 * the statement parameter writers ({@code buildcraft.api.statements.StatementParameterItemStack#writeToNbt}) store
 * into NBT: an {@code id} string, a {@code Count} byte and an optional {@code tag} compound.
 *
 * <p>M2.6 keeps this record deliberately free of {@link net.minecraft.world.item.Item} registry lookups so the
 * serialization round-trips stay testable on a plain JUnit JVM (same constraint as the M2.5 payload tests). The
 * content-migration tasks can replace this with a real {@code ItemStack}-based component once items are live.
 */
public record BcItemStack(Identifier id, int count, Optional<CompoundTag> tag) {

    public static final Identifier AIR_ID = Identifier.withDefaultNamespace("air");

    /** The empty stack: legacy {@code ItemStack.EMPTY} serializes as {@code {id:"minecraft:air", Count:0b}}. */
    public static final BcItemStack EMPTY = new BcItemStack(AIR_ID, 0, Optional.empty());

    public static BcItemStack of(String id, int count) {
        return new BcItemStack(Identifier.parse(id), count, Optional.empty());
    }

    public boolean isEmpty() {
        return count <= 0 || AIR_ID.equals(id);
    }

    /** Writes the legacy 1.20.1 {@code ItemStack.save} key set ({@code id} string, {@code Count} byte, optional
     * {@code tag} compound) into the given compound. */
    public CompoundTag writeToNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", id.toString());
        nbt.putByte("Count", (byte) count);
        tag.ifPresent(t -> nbt.put("tag", t));
        return nbt;
    }

    public static BcItemStack readFromNbt(CompoundTag nbt) {
        Identifier id = Identifier.parse(nbt.getStringOr("id", AIR_ID.toString()));
        int count = nbt.getByteOr("Count", (byte) 0);
        Optional<CompoundTag> tag = nbt.getCompound("tag");
        return new BcItemStack(id, count, tag);
    }

    /** Wire format (our own choice, the legacy wire never carried filter stacks): identifier, {@code Count} byte,
     * boolean tag header and an optional NBT compound. */
    public void writeToBuf(FriendlyByteBuf buf) {
        Identifier.STREAM_CODEC.encode(buf, id);
        buf.writeByte(count);
        buf.writeBoolean(tag.isPresent());
        tag.ifPresent(buf::writeNbt);
    }

    public static BcItemStack readFromBuf(FriendlyByteBuf buf) {
        Identifier id = Identifier.STREAM_CODEC.decode(buf);
        int count = buf.readByte();
        Optional<CompoundTag> tag = buf.readBoolean() ? Optional.ofNullable(buf.readNbt()) : Optional.empty();
        return new BcItemStack(id, count, tag);
    }

    public static final StreamCodec<FriendlyByteBuf, BcItemStack> STREAM_CODEC = StreamCodec.ofMember(
        BcItemStack::writeToBuf, BcItemStack::readFromBuf);
}
