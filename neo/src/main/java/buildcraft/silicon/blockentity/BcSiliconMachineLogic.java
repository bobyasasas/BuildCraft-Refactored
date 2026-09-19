/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.silicon.blockentity;

import org.jspecify.annotations.Nullable;

/**
 * M4.16 pure decision logic of the silicon laser family, split from the block entities on purpose (the
 * {@code FactoryMachineLogicTest} precedent): the block entity classes' class init drags vanilla's FML-bound statics
 * into the JVM, which the FML-less unit tests cannot bootstrap, while these helpers only touch plain Java values
 * (amounts, indexes, predicates) so the whole rule set is unit-testable.
 *
 * <p><b>Unit scale (the M2.2 micro-MJ slice economy):</b> the legacy machine numbers are microjoules where
 * {@code MjAPI.MJ = 1_000_000}; the slice scales every legacy MJ by 10<sup>&minus;4</sup> into &micro;MJ (legacy
 * 1&nbsp;MJ &rarr; 100&nbsp;&micro;MJ, exactly the quarry/engine slice convention), which preserves the legacy pacing
 * ratios: the laser pushes {@link #LASER_MAX_PUSH_PER_TICK} &micro;MJ/tick and a redstone chipset (legacy
 * 10,000&nbsp;MJ) takes the legacy 2,500 ticks.
 */
public final class BcSiliconMachineLogic {

    /** Legacy MJ in &micro;J ({@code MjAPI.MJ}); the recipe JSONs and the legacy tiles speak this unit. */
    public static final long LEGACY_MJ = 1_000_000;
    /** The slice scale factor: legacy N&nbsp;MJ becomes {@code N * LEGACY_MJ / SLICE_SCALE} &micro;MJ. */
    public static final long SLICE_SCALE = 10_000;

    /** Legacy {@code TileLaser#getMaxPowerPerTick()} (4 MJ) on the slice scale, in &micro;MJ per tick. */
    public static final long LASER_MAX_PUSH_PER_TICK = 4 * LEGACY_MJ / SLICE_SCALE;
    /** Legacy {@code TileLaser} battery ({@code 1024 * MjAPI.MJ}) on the slice scale, in &micro;MJ. */
    public static final long LASER_BATTERY_CAPACITY = 1024 * LEGACY_MJ / SLICE_SCALE;
    /** Legacy {@code TileAdvancedCraftingTable#POWER_REQ} (500 MJ) on the slice scale, in &micro;MJ. */
    public static final long ADVANCED_CRAFTING_POWER_REQ = 500 * LEGACY_MJ / SLICE_SCALE;

    private BcSiliconMachineLogic() {
    }

    /**
     * Maps a legacy recipe cost (the {@code requiredMicroJoules} of the migrated JSONs, in legacy &micro;J) onto the
     * slice &micro;MJ scale ({@link #SLICE_SCALE}).
     */
    public static long sliceCost(long legacyMicroJoules) {
        return legacyMicroJoules / SLICE_SCALE;
    }

    /**
     * The laser energy one tick may push into a target, in &micro;MJ &mdash; the baseline {@code TileLaser#update}
     * formula on the slice scale: the push scales linearly with the battery fill up to the maximum rate (full at
     * half capacity), clamped by the rate itself and by what the target still needs, and never more than the battery
     * holds. Zero when the target needs nothing or the battery is empty.
     */
    public static long laserPushThisTick(long stored, long capacity, long maxPerTick, long required) {
        if (required <= 0 || stored <= 0) {
            return 0;
        }
        long max = maxPerTick;
        max = max * (stored + max) / (capacity / 2);
        max = Math.min(Math.min(max, maxPerTick), required);
        return Math.min(max, stored);
    }

    /**
     * The per-slot consumption planner behind the legacy {@code TileLaserTableBase#extract(inv, items, simulate,
     * precise)}: every group (one recipe requirement) must draw its count from slots whose amount satisfies
     * {@code matcher.test(slotIndex, groupIndex)}; with {@code precise} every non-empty slot must be consumed (the
     * legacy integration-table rule that rejects unrelated extras), with {@code simulate} nothing is written. On
     * success and {@code simulate == false} the consumed amounts are subtracted from {@code slotAmounts}.
     *
     * <p>Deliberate fix over the legacy code: the baseline mutated stacks while iterating and left partial
     * consumption behind when a later group failed; the planner computes the whole plan first and only commits to an
     * all-groups success (documented M4.16 deviation, same observable contract on the success path).
     *
     * @param takenOut optional same-length output array receiving the per-slot consumed amounts (may be null)
     * @return true when every group is satisfiable (and, with {@code precise}, no slot is left over).
     */
    public static boolean extract(int[] slotAmounts, int[] groupCounts, SlotMatcher matcher, boolean simulate,
            boolean precise, int @Nullable [] takenOut) {
        int[] taken = new int[slotAmounts.length];
        for (int group = 0; group < groupCounts.length; group++) {
            int need = groupCounts[group];
            for (int slot = 0; slot < slotAmounts.length && need > 0; slot++) {
                // the available pool is the slot minus what earlier groups already claimed in this plan (one stack
                // cannot serve two requirement groups)
                int available = slotAmounts[slot] - taken[slot];
                if (available <= 0 || !matcher.test(slot, group)) {
                    continue;
                }
                int spend = Math.min(need, available);
                need -= spend;
                taken[slot] += spend;
            }
            if (need > 0) {
                return false;
            }
        }
        if (precise) {
            for (int slot = 0; slot < slotAmounts.length; slot++) {
                if (slotAmounts[slot] > 0 && taken[slot] == 0) {
                    return false;
                }
            }
        }
        if (takenOut != null) {
            // the caller asked for the plan itself (the craft paths plan with simulate=true and then replay taken[]
            // inside their own transaction), so it must be filled regardless of the simulate flag
            System.arraycopy(taken, 0, takenOut, 0, taken.length);
        }
        if (!simulate) {
            for (int slot = 0; slot < slotAmounts.length; slot++) {
                slotAmounts[slot] -= taken[slot];
            }
        }
        return true;
    }

    /** Slot/group matcher for {@link #extract}: which slots may satisfy which requirement group. */
    public interface SlotMatcher {
        boolean test(int slotIndex, int groupIndex);
    }
}
