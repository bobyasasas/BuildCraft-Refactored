/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.charge;

import net.minecraft.world.item.ItemStack;

/**
 * M4.17 port of the legacy {@code buildcraft.api.mj.IMjContainerItem}: an item whose stacks carry their own energy
 * store that machines (the charging table, legacy {@code TileChargingTable}) can drain into. The legacy API's three
 * stack-taking methods keep their exact shapes and semantics; the unit is the migration's slice &micro;MJ (legacy
 * {@code MjAPI.MJ = 1_000_000} &micro;J scaled by 10<sup>&minus;4</sup>, the whole-repo machine convention, see
 * {@code BcSiliconMachineLogic}). Storage itself is the implementer's business (the robot items keep their energy in
 * the vanilla {@code minecraft:custom_data} component &mdash; the modern form of the legacy item root NBT).
 */
public interface BcChargeableItem {

    /** The energy currently stored in this stack, &micro;MJ (legacy {@code getPowerStored}). */
    long getPowerStored(ItemStack stack);

    /** The energy this stack can hold at most, &micro;MJ (legacy {@code getMaxPowerStored}); 0 marks a stack that
     * cannot be charged at all (the legacy empty-board robot). */
    long getMaxPowerStored(ItemStack stack);

    /**
     * Accepts up to {@code maxReceive} &micro;MJ into this stack's store (legacy {@code receivePower}): returns the
     * accepted part &mdash; clamped by the remaining room, never overfilling &mdash; and only mutates the stack when
     * {@code simulate} is false.
     */
    long receivePower(ItemStack stack, long maxReceive, boolean simulate);

    /**
     * The shared receive clamp behind every implementation (and behind the charging table's per-tick dump, which is
     * the same math with the table buffer as the "receive" side): the accepted amount is the requested energy cut
     * down by the store's remaining room, and 0 once the store is full or nothing is offered. Keeping it here makes
     * the clamp the one pure, unit-tested definition of the interface's pacing.
     */
    static long clampReceive(long stored, long maxStored, long maxReceive) {
        long room = maxStored - stored;
        if (room <= 0 || maxReceive <= 0) {
            return 0;
        }
        return Math.min(room, maxReceive);
    }
}
