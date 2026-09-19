/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.gate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

import buildcraft.lib.datacomponent.gate.BcGateStatement;
import buildcraft.lib.datacomponent.gate.EnumGateLogic;
import buildcraft.lib.datacomponent.gate.EnumGateMaterial;
import buildcraft.lib.datacomponent.gate.EnumGateModifier;
import buildcraft.lib.datacomponent.gate.GateVariantData;
import net.minecraft.world.item.DyeColor;

/**
 * M4.17 FML-less unit tests for the gate's trigger &rarr; {@code triggerOn} &rarr; action chain: the evaluation core
 * {@link BcGateLogic#tick(BcGateLogic.TriggerResolver)} with the world read injected (the redstone resolver stands in
 * for the legacy {@code TriggerRedstoneInput} input, {@code Level#getBestNeighborSignal(pipePos) > 0} &mdash; the
 * unconfigured-parameter default of {@code IRedstoneStatementContainer#getRedstoneInput(null)}). Covers the two M4.9
 * PARTIAL observables the live rig exercises: the latching {@code buildcraft:redstone.output} action (legacy latch
 * semantics, {@code IAction#actionDeactivated} is a no-op there) and the slice-only transmission cutoff (follows the
 * trigger). The in-world halves (real redstone neighbours, the pipe push phase reacting to the cutoff) are exercised
 * by the {@code gate_logic} gametest and the M4.17 evidence rig.
 */
public class BcGateLogicEvaluationTest {

    /** The registered iron gate variant ({@code plug_gate_iron_and_no_modifier}): 2 AND slots. */
    private static final GateVariantData IRON_GATE = new GateVariantData(EnumGateLogic.AND,
        EnumGateMaterial.IRON, EnumGateModifier.NO_MODIFIER);
    private static final byte CENTER = BcGateStatement.SIDE_CENTER;

    /** The legacy redstone input read as a trigger resolver: level &gt; 0 fires {@code redstone.input.active}. */
    private static BcGateLogic.TriggerResolver redstoneInput(int bestNeighborSignal) {
        return (slot, trigger) -> BcGateStatements.TRIGGER_REDSTONE_ACTIVE.equals(trigger.kind())
            && bestNeighborSignal > 0;
    }

    private static BcGateStatement statement(String kind) {
        return new BcGateStatement(kind, CENTER, Map.of());
    }

    @Test
    public void unconfiguredGateStaysDark() {
        BcGateLogic gate = new BcGateLogic(IRON_GATE);
        assertFalse(gate.tick(redstoneInput(15)));
        assertFalse(gate.isOn());
        assertEquals(0, gate.getRedstoneOutput());
        assertFalse(gate.isPowerCutoff());
        assertTrue(gate.getWireBroadcasts().isEmpty());
        for (int slot = 0; slot < gate.getNumSlots(); slot++) {
            assertFalse(gate.isTriggerOn(slot));
            assertFalse(gate.isActionOn(slot));
        }
    }

    @Test
    public void redstoneInputFiresTheTriggerAndLatchesTheOutput() {
        BcGateLogic gate = new BcGateLogic(IRON_GATE);
        gate.configureSlot(0, statement(BcGateStatements.TRIGGER_REDSTONE_ACTIVE),
            statement(BcGateStatements.ACTION_REDSTONE_OUTPUT));

        // lever ON: the trigger fires, the action activates and latches the gate face at 15
        assertTrue(gate.tick(redstoneInput(15)));
        assertTrue(gate.isTriggerOn(0));
        assertTrue(gate.isActionOn(0));
        assertTrue(gate.isOn());
        assertEquals(15, gate.getRedstoneOutput());

        // lever OFF: the trigger and glow drop, the latch holds (legacy ActionRedstoneOutput never resets)
        assertTrue(gate.tick(redstoneInput(0)));
        assertFalse(gate.isTriggerOn(0));
        assertFalse(gate.isActionOn(0));
        assertFalse(gate.isOn());
        assertEquals(15, gate.getRedstoneOutput());

        // the latch survives further evaluations until the gate is removed or reconfigured
        assertFalse(gate.tick(redstoneInput(0)));
        assertEquals(15, gate.getRedstoneOutput());
    }

    @Test
    public void transmissionCutoffFollowsTheTriggerInsteadOfLatching() {
        BcGateLogic gate = new BcGateLogic(IRON_GATE);
        gate.configureSlot(0, statement(BcGateStatements.TRIGGER_REDSTONE_ACTIVE),
            statement(BcGateStatements.ACTION_PIPE_POWER_CUTOFF));

        assertTrue(gate.tick(redstoneInput(15)));
        assertTrue(gate.isPowerCutoff());
        assertTrue(gate.isOn());

        // the cutoff is a per-resolution state: the moment the trigger drops the pipe transmits again
        assertTrue(gate.tick(redstoneInput(0)));
        assertFalse(gate.isPowerCutoff());
        assertFalse(gate.isOn());
    }

    @Test
    public void oneTriggerDrivesBothTheLampAndTheCutoff() {
        BcGateLogic gate = new BcGateLogic(IRON_GATE);
        gate.configureSlot(0, statement(BcGateStatements.TRIGGER_REDSTONE_ACTIVE),
            statement(BcGateStatements.ACTION_REDSTONE_OUTPUT));
        gate.configureSlot(1, statement(BcGateStatements.TRIGGER_REDSTONE_ACTIVE),
            statement(BcGateStatements.ACTION_PIPE_POWER_CUTOFF));

        assertTrue(gate.tick(redstoneInput(15)));
        assertTrue(gate.isTriggerOn(0) && gate.isTriggerOn(1));
        assertEquals(15, gate.getRedstoneOutput());
        assertTrue(gate.isPowerCutoff());

        assertTrue(gate.tick(redstoneInput(0)));
        assertFalse(gate.isTriggerOn(0) || gate.isTriggerOn(1));
        assertFalse(gate.isPowerCutoff());
        assertEquals(15, gate.getRedstoneOutput());
    }

    @Test
    public void reconfigurationResetsTheLatchedState() {
        BcGateLogic gate = new BcGateLogic(IRON_GATE);
        gate.configureSlot(0, statement(BcGateStatements.TRIGGER_REDSTONE_ACTIVE),
            statement(BcGateStatements.ACTION_REDSTONE_OUTPUT));
        gate.tick(redstoneInput(15));
        assertEquals(15, gate.getRedstoneOutput());

        gate.configureSlot(0, null, null);
        assertEquals(0, gate.getRedstoneOutput());
        assertFalse(gate.isOn());
        assertFalse(gate.isPowerCutoff());
        assertFalse(gate.tick(redstoneInput(15)));
    }

    @Test
    public void wireBroadcastsFollowTheTriggerLikeLegacy() {
        BcGateLogic gate = new BcGateLogic(IRON_GATE);
        gate.configureSlot(0, statement(BcGateStatements.TRIGGER_REDSTONE_ACTIVE),
            statement(BcGateStatements.ACTION_PIPE_WIRE_RED));

        assertTrue(gate.tick(redstoneInput(15)));
        assertTrue(gate.getWireBroadcasts().contains(DyeColor.RED));

        assertTrue(gate.tick(redstoneInput(0)));
        assertFalse(gate.getWireBroadcasts().contains(DyeColor.RED));
    }

    @Test
    public void changeFlagTracksOnlyTheClientVisibleState() {
        BcGateLogic gate = new BcGateLogic(IRON_GATE);
        gate.configureSlot(0, statement(BcGateStatements.TRIGGER_REDSTONE_ACTIVE),
            statement(BcGateStatements.ACTION_REDSTONE_OUTPUT));

        // isOn flip -> sync; unchanged re-evaluation -> no sync; the latch alone does not re-sync
        assertTrue(gate.tick(redstoneInput(15)));
        assertFalse(gate.tick(redstoneInput(15)));
        assertTrue(gate.tick(redstoneInput(0)));
        assertFalse(gate.tick(redstoneInput(0)));

        // wire broadcast set changes re-sync (legacy sendResolveData/wire paths)
        BcGateLogic wired = new BcGateLogic(IRON_GATE);
        wired.configureSlot(0, statement(BcGateStatements.TRIGGER_REDSTONE_ACTIVE),
            statement(BcGateStatements.ACTION_PIPE_WIRE_RED));
        assertTrue(wired.tick(redstoneInput(15)));
        assertTrue(wired.tick(redstoneInput(0)));
    }

    @Test
    public void unknownKindsEvaluateToFalseAndNoOp() {
        BcGateLogic gate = new BcGateLogic(IRON_GATE);
        gate.configureSlot(0, statement("buildcraft:not.a.real.trigger"),
            statement("buildcraft:not.a.real.action"));

        // the unknown trigger never fires, so its unknown action never runs either
        assertFalse(gate.tick(redstoneInput(15)));
        assertFalse(gate.isOn());
        assertEquals(0, gate.getRedstoneOutput());
        assertFalse(gate.isPowerCutoff());
        assertTrue(gate.getWireBroadcasts().isEmpty());
    }
}
