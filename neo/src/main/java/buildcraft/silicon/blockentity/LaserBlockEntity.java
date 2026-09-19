/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.silicon.blockentity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import buildcraft.core.blockentity.MjReceiver;
import buildcraft.silicon.BcSiliconBlockEntities;

/**
 * M4.16 laser block entity (replaces the M2.4 {@code PlaceholderBlockEntity} under the unchanged id
 * {@code buildcraftsilicon:laser}; legacy counterpart: {@code buildcraft.silicon.tile.TileLaser}).
 *
 * <p><b>Legacy audit and the slice mapping:</b>
 * <ul>
 * <li><b>Power input</b> &mdash; legacy: {@code MjBattery(1024 * MjAPI.MJ)} fed through
 * {@code MjCapabilityHelper(new MjBatteryReceiver(battery))}, i.e. the battery always accepts while not full
 * ({@code getPowerRequested} = {@code capacity - stored}, work or not). Slice: the same receiver semantics through
 * {@link MjReceiver} (pipes can push; the evidence rig injects directly) with the battery on the M2.2
 * &times;10<sup>&minus;4</sup> scale ({@link BcSiliconMachineLogic#LASER_BATTERY_CAPACITY}).</li>
 * <li><b>Targeting</b> &mdash; legacy: a 6-block cone in the laser's facing scanned for {@code ILaserTarget} blocks,
 * re-scanned on local block updates, targets needing power picked at random every 10&ndash;20 ticks. Slice: the
 * M4.16 simplification &mdash; the six direct neighbours are scanned every tick for {@link LaserTarget} block
 * entities of this module needing power, random pick among them (no facing property on the slice block, no
 * cone volume, no local-update subscription).</li>
 * <li><b>Push</b> &mdash; legacy per tick: {@code max = min(min(rate * (stored + rate) / (capacity / 2), rate),
 * required)} pulled from the battery, the un-accepted excess refunded; kept verbatim through
 * {@link BcSiliconMachineLogic#laserPushThisTick}.</li>
 * </ul>
 *
 * <p>v1 trims (documented, behaviour-completing later milestones): no laser-beam renderer (the M4.5 laser rendering
 * library covers quarry/markers; the silicon beam is a later task), no {@code averagePower} statistics (legacy GUI
 * data, GUIs are the second batch) and no {@code laserPos} render state.
 *
 * <p>Client sync: the M4.7 Beacon pattern &mdash; the update tag is {@link #saveCustomOnly} and the tick loop pushes
 * it at {@link #SYNC_INTERVAL} while pushing power (the battery level reaches the client for a future renderer).
 */
public class LaserBlockEntity extends BlockEntity implements MjReceiver {

    /** Server-side sync cadence while the laser is pushing power (the M4.7 machine pattern). */
    public static final int SYNC_INTERVAL = 20;

    /** Internal battery, &micro;MJ (legacy {@code 1024 MJ} scaled, see class javadoc). */
    private long battery;
    /** Lifetime energy pushed into targets, &micro;MJ (evidence/test assertion counter). */
    private long totalPushed;
    /** Ticks since the last client sync while active. */
    private int syncTimer;

    public LaserBlockEntity(BlockPos pos, BlockState state) {
        super(BcSiliconBlockEntities.LASER.value(), pos, state);
    }

    public long getBattery() {
        return this.battery;
    }

    public long getTotalPushed() {
        return this.totalPushed;
    }

    // ---------------------------------------------------------------------
    // MjReceiver (legacy MjBatteryReceiver semantics: the battery accepts while not full)
    // ---------------------------------------------------------------------

    @Override
    public long getPowerRequested() {
        return BcSiliconMachineLogic.LASER_BATTERY_CAPACITY - this.battery;
    }

    @Override
    public long receivePower(long microJoules, boolean simulate) {
        long accepted = Math.min(microJoules, BcSiliconMachineLogic.LASER_BATTERY_CAPACITY - this.battery);
        if (!simulate && accepted > 0) {
            this.battery += accepted;
            this.syncToClients();
        }
        return microJoules - accepted;
    }

    // ---------------------------------------------------------------------
    // Server tick: target pick + the legacy per-tick push
    // ---------------------------------------------------------------------

    /** Per-tick work logic, wired through {@code LaserBlock#getTicker}. */
    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, LaserBlockEntity laser) {
        LaserTarget target = laser.findTarget(level, pos);
        if (target == null) {
            return;
        }
        long required = target.getRequiredLaserPower();
        long push = BcSiliconMachineLogic.laserPushThisTick(laser.battery,
            BcSiliconMachineLogic.LASER_BATTERY_CAPACITY, BcSiliconMachineLogic.LASER_MAX_PUSH_PER_TICK, required);
        if (push <= 0) {
            return;
        }
        laser.battery -= push;
        long excess = target.receiveLaserPower(push);
        laser.battery += excess;
        laser.totalPushed += push - excess;
        if (++laser.syncTimer >= SYNC_INTERVAL) {
            laser.syncToClients();
        }
    }

    /**
     * The six-neighbour scan (the M4.16 direct-connection simplification, see class javadoc): block entities of this
     * module acting as {@link LaserTarget}s that still need power; random pick among them like the legacy
     * {@code randomlyChooseTargetPos}.
     */
    private @Nullable LaserTarget findTarget(ServerLevel level, BlockPos pos) {
        List<LaserTarget> needing = new ArrayList<>(6);
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(pos.relative(direction)) instanceof LaserTarget target
                && target.getRequiredLaserPower() > 0 && !target.isInvalidTarget()) {
                needing.add(target);
            }
        }
        if (needing.isEmpty()) {
            return null;
        }
        return needing.get(level.getRandom().nextInt(needing.size()));
    }

    // ---------------------------------------------------------------------
    // Persistence + client sync (Beacon pattern)
    // ---------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("bc_battery", this.battery);
        output.putLong("bc_total_pushed", this.totalPushed);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.battery = input.getLongOr("bc_battery", 0L);
        this.totalPushed = input.getLongOr("bc_total_pushed", 0L);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Marks the state dirty and pushes it to clients (Beacon-pattern update tag, see class javadoc). */
    private void syncToClients() {
        this.setChanged();
        this.syncTimer = 0;
        if (this.level instanceof ServerLevel serverLevel) {
            BlockState state = this.getBlockState();
            serverLevel.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }
}
