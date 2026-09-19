/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.gate;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import buildcraft.lib.datacomponent.gate.BcGateConfig;
import buildcraft.lib.datacomponent.gate.BcGateSlot;
import buildcraft.lib.datacomponent.gate.BcGateStatement;
import buildcraft.lib.datacomponent.gate.GateVariantData;

/**
 * The M2.11 minimal runtime gate: the trigger/action evaluation core of the 1.20.1 gate system, hosted by the slice
 * kinesis pipe's block entity ({@code buildcraftcore:pipe_kinesis_wood}). Legacy counterpart:
 * {@code buildcraft.silicon.gate.GateLogic}, which lives as a pipe pluggable on a real {@code TilePipeHolder} &mdash;
 * the real transport pipe system has not migrated, so the gate rides the M2.2 slice pipe instead (recorded as a
 * migration deviation; a gate is <b>not</b> a block/BE of its own in the registry baseline, it is pipe content).
 *
 * <p>Configuration is carried by the M2.6 {@link BcGateConfig} component value (the exact legacy
 * {@code gate_data} compound shape), so a gate configured in the slice round-trips through the same bytes the legacy
 * gate copier stored. Runtime state beyond the config ({@link #isOn}, the latched redstone output and the wire
 * broadcasts) is deliberately <em>not</em> part of {@link BcGateConfig} &mdash; the component tree matches the legacy
 * copier payload, which strips {@code wireBroadcasts}; the host block entity persists the runtime bits under its own
 * keys and replicates them to clients through its update tag.
 *
 * <p>Per-tick evaluation mirrors {@code GateLogic#resolveActions} in the degenerate configuration the slice can
 * produce (no GUI to set {@code connections[]}): with every connection bit false each slot is its own group, and a
 * single-statement AND/OR group is active exactly when its trigger fired. Deviations (all recorded):
 * <ul>
 * <li>no statement parameters and no per-side variants ({@code StatementParamGateSideOnly}) &mdash; GUI content;</li>
 * <li>no wire-network flood &mdash; the broadcast set stays on this pipe;</li>
 * <li>the redstone output latches exactly like legacy {@code ActionRedstoneOutput} (see
 * {@link BcGateStatements#ACTION_REDSTONE_OUTPUT}).</li>
 * </ul>
 */
public class BcGateLogic {

    /** The gate variant (logic x material x modifier; also determines the slot count). */
    private final GateVariantData variant;
    /** One trigger/action pair per variant slot (legacy {@code GateLogic.StatementPair}). */
    private final BcGateSlot[] slots;
    /** Client-visible per-tick trigger states (legacy {@code GateLogic#triggerOn}). */
    private final boolean[] triggerOn;
    /** Client-visible per-tick action states (legacy {@code GateLogic#actionOn}). */
    private final boolean[] actionOn;
    /** True while any slot's action is being activated; drives the gate's glow (legacy {@code GateLogic#isOn}). */
    private boolean isOn;
    /** Latched redstone output level on the gate face (legacy {@code TilePipeHolder#redstoneValues} entry). */
    private int redstoneOutput;
    /**
     * True while the transmission-cutoff action holds on an active slot this tick (the M4.17
     * {@link BcGateStatements#ACTION_PIPE_POWER_CUTOFF} state; follows the trigger instead of latching &mdash; the
     * legacy action set has no pipe-side cut statement, so this slice action was purpose-built to follow
     * {@code resolveActions}' per-tick activation exactly like {@code isOn} does).
     */
    private boolean powerCutoff;
    /** Wire colours this gate currently broadcasts (legacy {@code GateLogic#wireBroadcasts}). */
    private final EnumSet<DyeColor> wireBroadcasts = EnumSet.noneOf(DyeColor.class);

    public BcGateLogic(GateVariantData variant) {
        this.variant = variant;
        this.slots = new BcGateSlot[variant.numSlots()];
        for (int i = 0; i < this.slots.length; i++) {
            this.slots[i] = BcGateSlot.EMPTY;
        }
        this.triggerOn = new boolean[this.slots.length];
        this.actionOn = new boolean[this.slots.length];
    }

    /**
     * Rebuilds a gate from its config value (the persistence path: both the host BE's {@code loadAdditional} and the
     * client's update-tag application land here). The slot list is normalised to the variant's slot count &mdash; a
     * config whose slots disagree with the variant is legacy-invalid data and gets truncated/padded like legacy's
     * array-size mismatch would.
     */
    public BcGateLogic(BcGateConfig config) {
        this.variant = config.variant();
        List<BcGateSlot> configured = config.slots();
        this.slots = new BcGateSlot[this.variant.numSlots()];
        for (int i = 0; i < this.slots.length; i++) {
            this.slots[i] = i < configured.size() ? configured.get(i) : BcGateSlot.EMPTY;
        }
        this.triggerOn = new boolean[this.slots.length];
        this.actionOn = new boolean[this.slots.length];
    }

    public GateVariantData getVariant() {
        return this.variant;
    }

    public int getNumSlots() {
        return this.slots.length;
    }

    /** The configured trigger of one slot, or empty (legacy: null statement in the slot). */
    public Optional<BcGateStatement> getTrigger(int slot) {
        return this.slots[slot].trigger();
    }

    /** The configured action of one slot, or empty (legacy: null statement in the slot). */
    public Optional<BcGateStatement> getAction(int slot) {
        return this.slots[slot].action();
    }

    /**
     * Configures one slot's statements (the programmatic stand-in for the gate GUI's statement pickers; both may be
     * null to clear). Reconfiguration resets the runtime state, including the latched redstone output &mdash; the
     * legacy output array belongs to the pipe and only clears on gate removal, but a reconfigured gate never keeps a
     * stale latch in the slice (GUI-less test/rig path).
     */
    public void configureSlot(int slot, BcGateStatement trigger, BcGateStatement action) {
        this.slots[slot] = new BcGateSlot(Optional.ofNullable(trigger), Optional.ofNullable(action));
        this.isOn = false;
        this.redstoneOutput = 0;
        this.powerCutoff = false;
        this.wireBroadcasts.clear();
        for (int i = 0; i < this.triggerOn.length; i++) {
            this.triggerOn[i] = false;
            this.actionOn[i] = false;
        }
    }

    public boolean isOn() {
        return this.isOn;
    }

    /** The latched redstone output level (0..15) emitted on the gate face; legacy latch semantics, see class javadoc. */
    public int getRedstoneOutput() {
        return this.redstoneOutput;
    }

    /** Latches the redstone output (legacy {@code IRedstoneStatementContainer#setRedstoneOutput(side, value)}). */
    public void latchRedstoneOutput(int level) {
        this.redstoneOutput = level;
    }

    /**
     * True while the transmission-cutoff action held on the last evaluation (read by the host pipe's push phase; see
     * {@link BcGateStatements#ACTION_PIPE_POWER_CUTOFF}). Follows the trigger &mdash; it is recomputed from scratch
     * every tick exactly like {@link #isOn()}, never latched.
     */
    public boolean isPowerCutoff() {
        return this.powerCutoff;
    }

    /** Arms the transmission cutoff for the current evaluation (called from {@code BcGateStatements#runAction}). */
    public void armPowerCutoff() {
        this.powerCutoff = true;
    }

    /**
     * Restores the glow flag without running an evaluation &mdash; the client-side restore path from the host's update
     * tag (the server recomputes {@link #isOn} on every tick, so this never runs on a ticking server gate).
     */
    public void restoreClientGlow(boolean on) {
        this.isOn = on;
    }

    /** The wire colours currently broadcast by this gate (live view; legacy {@code GateLogic#isEmitting}). */
    public EnumSet<DyeColor> getWireBroadcasts() {
        return this.wireBroadcasts;
    }

    /** Adds a wire colour to the broadcast set (legacy {@code IWireEmitter#emitWire(colour)}). */
    public void emitWire(DyeColor colour) {
        this.wireBroadcasts.add(colour);
    }

    public boolean isTriggerOn(int slot) {
        return this.triggerOn[slot];
    }

    public boolean isActionOn(int slot) {
        return this.actionOn[slot];
    }

    /**
     * One evaluation pass, called from the host's server tick (legacy {@code GateLogic#onTick} &rarr;
     * {@code #resolveActions}). The trigger truth comes from the world through
     * {@link BcGateStatements#isTriggerActive}; see {@link #tick(TriggerResolver)} for the evaluation contract.
     */
    public boolean tick(ServerLevel level, BlockPos pos, Direction gateSide) {
        return this.tick((slot, trigger) -> BcGateStatements.isTriggerActive(trigger, level, pos, gateSide));
    }

    /**
     * The evaluation core over an injected trigger resolver &mdash; the same pass as {@link #tick(ServerLevel, BlockPos,
     * Direction)} with the world read factored out, which is what makes the trigger &rarr; {@code triggerOn} &rarr;
     * output-latch chain unit-testable without a level (M4.17; the legacy counterpart
     * {@code GateLogic#resolveActions} hard-wires {@code TriggerWrapper#isTriggerActive}). The resolver is called once
     * per configured trigger, in slot order.
     */
    public boolean tick(TriggerResolver triggerResolver) {
        boolean previousOn = this.isOn;
        EnumSet<DyeColor> previousWires = EnumSet.copyOf(this.wireBroadcasts);

        this.isOn = false;
        this.powerCutoff = false;
        this.wireBroadcasts.clear();

        for (int slot = 0; slot < this.slots.length; slot++) {
            this.triggerOn[slot] = false;
            this.actionOn[slot] = false;
            BcGateStatement trigger = this.slots[slot].trigger().orElse(null);
            if (trigger != null && triggerResolver.isActive(slot, trigger)) {
                this.triggerOn[slot] = true;
            }
            // Degenerate group evaluation (all connection bits false): each slot is its own group, and a
            // single-statement AND/OR group is active iff the trigger fired (GateLogic#resolveActions).
            boolean groupActive = this.triggerOn[slot];
            BcGateStatement action = this.slots[slot].action().orElse(null);
            this.actionOn[slot] = groupActive;
            if (groupActive && action != null) {
                this.isOn = true;
                BcGateStatements.runAction(action, this);
            }
        }

        return previousOn != this.isOn || !previousWires.equals(this.wireBroadcasts);
    }

    /** The world-read half of a trigger evaluation (see {@link #tick(TriggerResolver)}). */
    @FunctionalInterface
    public interface TriggerResolver {

        /** True when the world currently satisfies this slot's trigger (legacy {@code TriggerWrapper}). */
        boolean isActive(int slot, BcGateStatement trigger);
    }

    /** The config value this gate's configuration round-trips as (the legacy {@code gate_data} compound shape). */
    public BcGateConfig toConfig() {
        short triggerBits = 0;
        short actionBits = 0;
        for (int i = 0; i < this.slots.length; i++) {
            if (this.triggerOn[i]) {
                triggerBits |= (short) (1 << i);
            }
            if (this.actionOn[i]) {
                actionBits |= (short) (1 << i);
            }
        }
        // connections: all false (the slice cannot configure groups), matching the legacy default
        return new BcGateConfig(this.variant, (short) 0, List.of(this.slots), triggerBits, actionBits);
    }
}
