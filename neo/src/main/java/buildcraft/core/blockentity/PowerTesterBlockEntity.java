/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.blockentity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.slf4j.Logger;
import buildcraft.core.BcBlockEntities;

/**
 * The M4.16 power consumer tester block entity ({@code buildcraftcore:power_tester}), replacing the M2.4a
 * {@link PlaceholderBlockEntity} under the unchanged id. Minimal faithful slice of the legacy
 * {@code buildcraft.core.tile.TilePowerConsumerTester} (frozen 8.0.x-1.20.1 tree):
 * <ul>
 * <li><b>Power input</b> &mdash; legacy: {@code IMjReceiver} through {@code MjCapabilityHelper}, always requesting
 * {@code 100000 * MjAPI.MJ} ({@link #REQUEST}, kept at the exact legacy &micro;MJ figure) and accepting every offered
 * packet ({@code receivePower} returns 0 excess) into pure counters. Slice: the same semantics through the migrated
 * plain-Java {@link MjReceiver} port &mdash; the kinesis pipe's push phase delivers into it exactly as it does into the
 * M2.12 filler/quarry machines; the NeoForge capability layer stays deferred with the rest of the MJ API migration.</li>
 * <li><b>Counters</b> &mdash; the legacy four fields ({@code lastReceived}, {@code nextTickReceived},
 * {@code lastTickReceived}, {@code totalReceived}) with the legacy {@code ITickable#update} shift
 * ({@code nextTick &rarr; lastTick, nextTick &rarr; 0}) running in {@link #serverTick}.</li>
 * <li><b>Observability</b> &mdash; legacy exposed the counters through {@code IDebuggable} (waila/F3 debug overlay).
 * Slice trim: the debug overlay framework has not migrated, so the same numbers surface as a periodic log line
 * (every {@link #LOG_INTERVAL} ticks, only while the totals actually moved) carrying the required
 * {@code "received total X µMJ"} figure.</li>
 * </ul>
 *
 * <p>The counters persist ({@code last/nt/lt/total}, the legacy NBT keys).
 */
public class PowerTesterBlockEntity extends BlockEntity implements MjReceiver {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * The legacy always-on request ({@code TilePowerConsumerTester#getPowerRequested == 100000 * MjAPI.MJ}):
     * 100,000&nbsp;MJ expressed in &micro;MJ. The tester is a debug sink &mdash; it never refuses power.
     */
    public static final long REQUEST = 100_000 * 1_000_000L;

    /** Log cadence in ticks (1 s): the slice stand-in for the legacy {@code IDebuggable} overlay. */
    public static final int LOG_INTERVAL = 20;

    /** &micro;MJ accepted by the most recent {@link #receivePower} packet (legacy {@code lastReceived}). */
    private long lastReceived;
    /** &micro;MJ accumulated since the last tick shift (legacy {@code nextTickReceived}). */
    private long nextTickReceived;
    /** {@code nextTickReceived} value of the previous tick (legacy {@code lastTickReceived}). */
    private long lastTickReceived;
    /** Lifetime &micro;MJ accepted (legacy {@code totalReceived}). */
    private long totalReceived;
    /** {@code totalReceived} value at the last emitted log line (log gating only, not persisted). */
    private long loggedTotal;

    public PowerTesterBlockEntity(BlockPos pos, BlockState state) {
        super(BcBlockEntities.POWER_TESTER.value(), pos, state);
    }

    // ---------------------------------------------------------------------
    // MjReceiver (legacy IMjReceiver semantics, see the interface javadoc)
    // ---------------------------------------------------------------------

    @Override
    public long getPowerRequested() {
        // the legacy debug sink never refuses power (no work state to run out of)
        return REQUEST;
    }

    @Override
    public long receivePower(long microJoules, boolean simulate) {
        if (!simulate) {
            this.lastReceived = microJoules;
            this.nextTickReceived += microJoules;
            this.totalReceived += microJoules;
            this.setChanged();
        }
        // legacy: every offered microjoule is accepted, nothing comes back
        return 0;
    }

    /** Current counters for tests/observability: {last packet, last tick, total}. */
    public long getLastReceived() {
        return this.lastReceived;
    }

    public long getLastTickReceived() {
        return this.lastTickReceived;
    }

    public long getTotalReceived() {
        return this.totalReceived;
    }

    // ---------------------------------------------------------------------
    // Persistence (the legacy load/save keys)
    // ---------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("last", this.lastReceived);
        output.putLong("nt", this.nextTickReceived);
        output.putLong("lt", this.lastTickReceived);
        output.putLong("total", this.totalReceived);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.lastReceived = input.getLongOr("last", 0L);
        this.nextTickReceived = input.getLongOr("nt", 0L);
        this.lastTickReceived = input.getLongOr("lt", 0L);
        this.totalReceived = input.getLongOr("total", 0L);
    }

    // ---------------------------------------------------------------------
    // Server tick: the legacy ITickable shift + the periodic evidence log
    // ---------------------------------------------------------------------

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, PowerTesterBlockEntity tester) {
        tester.lastTickReceived = tester.nextTickReceived;
        tester.nextTickReceived = 0;
        if (level.getGameTime() % LOG_INTERVAL == 0 && tester.totalReceived != tester.loggedTotal) {
            tester.loggedTotal = tester.totalReceived;
            LOGGER.info("[M416] power_tester at {} received total {} µMJ (last tick {} µMJ, last packet {} µMJ)", pos,
                tester.totalReceived, tester.lastTickReceived, tester.lastReceived);
        }
    }
}
