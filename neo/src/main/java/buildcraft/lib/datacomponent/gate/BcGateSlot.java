/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.datacomponent.gate;

import java.util.Optional;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * The trigger / action pair of one gate slot. Legacy counterpart: the
 * {@code trigger[i]} / {@code action[i]} compound pair of {@code buildcraft.silicon.gate.GateLogic#writeToNbt}, keyed
 * by the slot index; absent statements are absent optionals here, exactly like legacy only writing a compound for
 * configured statements.
 */
public record BcGateSlot(Optional<BcGateStatement> trigger, Optional<BcGateStatement> action) {

    public static final BcGateSlot EMPTY = new BcGateSlot(Optional.empty(), Optional.empty());

    public static BcGateSlot ofTrigger(BcGateStatement trigger) {
        return new BcGateSlot(Optional.of(trigger), Optional.empty());
    }

    public static BcGateSlot ofAction(BcGateStatement action) {
        return new BcGateSlot(Optional.empty(), Optional.of(action));
    }

    public void writeTriggerToNbt(CompoundTag nbt, int slot) {
        trigger.ifPresent(t -> nbt.put("trigger[" + slot + "]", t.writeToNbt()));
    }

    public void writeActionToNbt(CompoundTag nbt, int slot) {
        action.ifPresent(a -> nbt.put("action[" + slot + "]", a.writeToNbt()));
    }

    public void writeToBuf(FriendlyByteBuf buf) {
        buf.writeBoolean(trigger.isPresent());
        trigger.ifPresent(t -> t.writeToBuf(buf));
        buf.writeBoolean(action.isPresent());
        action.ifPresent(a -> a.writeToBuf(buf));
    }

    public static BcGateSlot readFromBuf(FriendlyByteBuf buf) {
        Optional<BcGateStatement> trigger = buf.readBoolean() ? Optional.of(BcGateStatement.readFromBuf(buf))
            : Optional.empty();
        Optional<BcGateStatement> action = buf.readBoolean() ? Optional.of(BcGateStatement.readFromBuf(buf))
            : Optional.empty();
        return new BcGateSlot(trigger, action);
    }
}
