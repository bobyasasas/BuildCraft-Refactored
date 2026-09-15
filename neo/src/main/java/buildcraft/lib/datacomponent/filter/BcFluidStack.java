/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.datacomponent.filter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * Registry-free stand-in for a serialized {@code FluidStack}, used by the fluid filter description
 * ({@link BcFilterSpec.FluidFilterSpec legacy {@code buildcraft.lib.inventory.filter.ArrayFluidFilter}}). The NBT keys
 * {@code FluidName} (string) and {@code Amount} (int) follow the 1.20.1 Forge {@code FluidStack#writeToNBT} convention
 * that legacy code relies on wherever fluid stacks hit NBT (for example
 * {@code buildcraft.transport.pipe.flow.PipeFlowFluids#writeToNbt} storing {@code currentFluid.writeToNBT} under the
 * {@code fluid} key).
 */
public record BcFluidStack(Identifier fluid, int amount) {

    public CompoundTag writeToNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("FluidName", fluid.toString());
        nbt.putInt("Amount", amount);
        return nbt;
    }

    public static BcFluidStack readFromNbt(CompoundTag nbt) {
        Identifier fluid = Identifier.parse(nbt.getStringOr("FluidName", "minecraft:water"));
        int amount = nbt.getIntOr("Amount", 0);
        return new BcFluidStack(fluid, amount);
    }

    public void writeToBuf(FriendlyByteBuf buf) {
        Identifier.STREAM_CODEC.encode(buf, fluid);
        buf.writeVarInt(amount);
    }

    public static BcFluidStack readFromBuf(FriendlyByteBuf buf) {
        Identifier fluid = Identifier.STREAM_CODEC.decode(buf);
        int amount = buf.readVarInt();
        return new BcFluidStack(fluid, amount);
    }

    public static final StreamCodec<FriendlyByteBuf, BcFluidStack> STREAM_CODEC = StreamCodec.ofMember(
        BcFluidStack::writeToBuf, BcFluidStack::readFromBuf);
}
