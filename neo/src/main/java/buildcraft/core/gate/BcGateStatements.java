/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.gate;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import buildcraft.core.blockentity.StoneEngineBlockEntity;
import buildcraft.lib.datacomponent.gate.BcGateStatement;

/**
 * The M2.11 minimal trigger/action statement set of the gate slice, resolved by legacy statement {@code kind} string
 * (the {@code s.kind} key of {@code BcGateStatement}, exactly what legacy
 * {@code buildcraft.silicon.gate.TriggerType}/{@code ActionType} persisted). Every kind below is a real 1.20.1
 * statement id; the legacy implementations and the slice mapping are:
 *
 * <ul>
 * <li><b>{@code buildcraft:redstone.input.active}</b> (trigger) &mdash; legacy
 * {@code buildcraft.core.statements.TriggerRedstoneInput(true)}: {@code IRedstoneStatementContainer#getRedstoneInput(null)}
 * &gt; 0, which legacy resolves as {@code Level#getBestNeighborSignal(pos)}. Slice: identical (no
 * {@code StatementParamGateSideOnly} parameter can be configured without the gate GUI, so the legacy default "any
 * side" evaluation is used).</li>
 * <li><b>{@code buildcraft:engine.stage.blue}</b> (trigger) &mdash; legacy
 * {@code buildcraft.core.statements.TriggerEnginePowerStage(EnumPowerStage.BLUE)}, an external trigger on the
 * neighbouring {@code TileEngineBase_BC8} ({@code getPowerStage() == BLUE}). The slice engine has no heat model, so
 * "blue" (a running, cold engine) maps to {@link StoneEngineBlockEntity#isBurning()} on the adjacent engine.</li>
 * <li><b>{@code buildcraft:redstone.output}</b> (action) &mdash; legacy
 * {@code buildcraft.core.statements.ActionRedstoneOutput}: {@code setRedstoneOutput(side, 15)}. Legacy latches the
 * value ({@code TilePipeHolder#redstoneValues} is never reset by a deactivation hook &mdash;
 * {@code IAction#actionDeactivated} is a no-op for this statement), so the slice latches too; the output only clears
 * when the gate is removed or reconfigured. The host block emits it through its signal overrides (legacy
 * {@code BlockPipeHolder#getSignal}/{@code #getDirectSignal}).</li>
 * <li><b>{@code buildcraft:pipe.wire.output.red}</b> (action) &mdash; legacy
 * {@code buildcraft.transport.statements.ActionPipeSignal(DyeColor.RED)}: {@code IWireEmitter#emitWire(RED)}, called
 * every resolution while the trigger holds (legacy {@code GateLogic#resolveActions} clears
 * {@code wireBroadcasts} first, so the broadcast follows the trigger). The wire network flood itself is transport
 * content that has not migrated; the slice keeps only the per-pipe broadcast set (visible to the renderer and
 * assertable in tests).</li>
 * <li><b>{@code buildcraft:pipe.power.cutoff}</b> (action, slice-only v1 trim, M4.17) &mdash; no legacy counterpart:
 * while active the host pipe transmits no energy (see {@link #ACTION_PIPE_POWER_CUTOFF}).</li>
 * </ul>
 *
 * <p>Unknown kinds evaluate to false / no-op, exactly like legacy reading a statement it cannot resolve (null
 * statement in the slot). Statement {@code side} bytes and parameters are accepted but ignored: the slice gate has no
 * GUI to configure them (see {@link BcGateLogic}).
 */
public final class BcGateStatements {

    /** Legacy {@code TriggerRedstoneInput(true)} ({@code buildcraft.core.BCCoreStatements#TRIGGER_REDSTONE_ACTIVE}). */
    public static final String TRIGGER_REDSTONE_ACTIVE = "buildcraft:redstone.input.active";
    /** Legacy {@code TriggerEnginePowerStage(EnumPowerStage.BLUE)} ({@code BCCoreStatements#TRIGGER_POWER_STAGES}). */
    public static final String TRIGGER_ENGINE_BLUE = "buildcraft:engine.stage.blue";
    /** Legacy {@code ActionRedstoneOutput} ({@code BCCoreStatements#ACTION_REDSTONE}). */
    public static final String ACTION_REDSTONE_OUTPUT = "buildcraft:redstone.output";
    /** Legacy {@code ActionPipeSignal(DyeColor.RED)} ({@code BCTransportStatements#ACTION_PIPE_SIGNAL}). */
    public static final String ACTION_PIPE_WIRE_RED = "buildcraft:pipe.wire.output.red";
    /**
     * <b>Slice-only v1 trim (no legacy statement id &mdash; M4.17):</b> while active, the host pipe transmits no
     * energy ({@code KinesisPipeBlockEntity} stops its push phase). Legacy gates shape pipe power only indirectly:
     * the limiter pipes' {@code ActionIronPowerLimit}/{@code ActionDiamondPowerLimit} shift a limiter pipe's max
     * output (a different, unmigrated pipe), and the classic player recipe is the gate's redstone output switching a
     * neighbouring engine off. The wooden kinesis gate host has neither a limiter behaviour nor a neighbour engine
     * hook in the slice, so this purpose-built id stands in for the "gate chokes the pipe" play pattern the M4.17
     * evidence rig requires; like {@link #ACTION_PIPE_WIRE_RED} it follows the trigger (re-armed per resolution,
     * never latched).
     */
    public static final String ACTION_PIPE_POWER_CUTOFF = "buildcraft:pipe.power.cutoff";

    private BcGateStatements() {
    }

    /**
     * Evaluates one trigger for the gate attached at {@code pos} on face {@code gateSide}. Legacy counterpart:
     * {@code TriggerWrapper#isTriggerActive} inside {@code GateLogic#resolveActions}.
     */
    public static boolean isTriggerActive(BcGateStatement trigger, ServerLevel level, BlockPos pos, Direction gateSide) {
        return switch (trigger.kind()) {
            // legacy: getRedstoneInput(null) -> Level#getBestNeighborSignal(pipePos) > 0
            case TRIGGER_REDSTONE_ACTIVE -> level.getBestNeighborSignal(pos) > 0;
            // legacy: external trigger on the neighbour engine, getPowerStage() == BLUE;
            // slice engine has no heat stages, "blue" = producing = burning
            case TRIGGER_ENGINE_BLUE -> isAdjacentEngineBurning(level, pos);
            default -> false;
        };
    }

    /** Any directly adjacent block entity is a burning stone engine (the slice's engine-stage stand-in). */
    private static boolean isAdjacentEngineBurning(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(pos.relative(direction)) instanceof StoneEngineBlockEntity engine
                    && engine.isBurning()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Runs one action for the gate attached at {@code pos} on face {@code gateSide}. Legacy counterpart:
     * {@code ActionWrapper#actionActivate} + the {@code PipeEventActionActivate} fire inside
     * {@code GateLogic#resolveActions} (the pipe-event hook has no slice listener &mdash; no extraction pipes exist
     * yet &mdash; so it is not carried over). Every statement of the current minimal set resolves against the gate's
     * own state alone (the cutoff arms, the latch and the wire broadcast &mdash; none reads the world), so the legacy
     * {@code (container, parameters)} activation arguments are not carried down; a world-reading statement would
     * bring them back.
     */
    public static void runAction(BcGateStatement action, BcGateLogic gate) {
        switch (action.kind()) {
            // legacy: setRedstoneOutput(side, 15), latched (no deactivation reset)
            case ACTION_REDSTONE_OUTPUT -> gate.latchRedstoneOutput(15);
            // legacy: IWireEmitter#emitWire(RED), re-emitted every resolution while the trigger holds
            case ACTION_PIPE_WIRE_RED -> gate.emitWire(DyeColor.RED);
            // slice-only v1 trim: arms the pipe's transmission cutoff for this resolution (follows the trigger)
            case ACTION_PIPE_POWER_CUTOFF -> gate.armPowerCutoff();
            default -> {
            }
        }
    }

    /** The DyeColor values the slice statement set can broadcast (just RED, see {@link #ACTION_PIPE_WIRE_RED}). */
    public static EnumSet<DyeColor> broadcastableWires() {
        return EnumSet.of(DyeColor.RED);
    }
}
