/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.silicon.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import buildcraft.silicon.BcSiliconBlockEntities;

/**
 * M4.16 charging table block entity (legacy counterpart:
 * {@code buildcraft.silicon.tile.TileChargingTable}, id {@code buildcraftsilicon:charging_table} unchanged).
 *
 * <p><b>M4.16 slice status &mdash; PARTIAL by design:</b> the legacy machine charges an {@code IMjContainerItem}
 * placed in its single slot (target = {@code maxPowerStored - powerStored}, drained item-by-item into the stack).
 * No {@code IMjContainerItem} item type has migrated yet (that behaviour class is a later milestone), so there is no
 * chargeable target and &mdash; per the v1 no-fake-behaviour rule &mdash; the item slot is omitted entirely. What
 * this slice ships instead is the machine's power half, fully working: laser energy accumulates in an internal
 * buffer up to {@link #BUFFER_CAPACITY} &mdash; the laser sees the free buffer room through the shared base rule
 * ({@code required = target - power}, which is why the target here is the constant cap) &mdash; and stays as the
 * visible store once full. Charging items arrives with the {@code IMjContainerItem} port.
 */
public class ChargingTableBlockEntity extends LaserTableBaseBlockEntity {

    /**
     * The internal buffer cap in &micro;MJ. The legacy table has no buffer cap of its own (its power floats while no
     * item needs charging and is drained); the slice pins one so the "energy in, visible store" evidence has a
     * defined full state.
     */
    public static final long BUFFER_CAPACITY = 1_000_000;

    public ChargingTableBlockEntity(BlockPos pos, BlockState state) {
        super(BcSiliconBlockEntities.CHARGING_TABLE.value(), pos, state);
    }

    @Override
    protected long currentTarget() {
        // the constant cap: the base derives the laser's requirement as (target - power), i.e. the free buffer room,
        // so the laser feeds exactly until the buffer is full and the store stays visible
        return BUFFER_CAPACITY;
    }

    @Override
    protected void craft(ServerLevel level) {
        // unreachable: power only reaches the target when the buffer is full, and there is nothing to craft (see
        // class javadoc)
    }

    /** Per-tick driver wired through {@code ChargingTableBlock#getTicker}. */
    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, ChargingTableBlockEntity table) {
        table.tickPower(level);
    }
}
