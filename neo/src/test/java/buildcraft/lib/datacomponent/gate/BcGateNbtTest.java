/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.datacomponent.gate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import buildcraft.lib.datacomponent.DataComponentTestHelper;
import buildcraft.lib.datacomponent.filter.BcItemStack;
import net.minecraft.nbt.CompoundTag;

/**
 * M2.6 parity tests for {@link BcGateConfig}: the fixtures are hand-written reconstructions of the legacy
 * {@code gate_data} payload ({@code buildcraft.silicon.item.ItemGateCopier} storing
 * {@code buildcraft.silicon.gate.GateLogic#writeToNbt} minus {@code wireBroadcasts}); each source file is noted on the
 * fixture. Every fixture must survive NBT -> component -> NBT unchanged (key set and values) and the component stream
 * codec unchanged.
 */
public class BcGateNbtTest {

    /**
     * Legacy source: {@code buildcraft.silicon.gate.GateVariant#writeToNBT} ({@code logic}/{@code material}/
     * {@code modifier} ordinal bytes) + {@code GateLogic#writeToNbt} ({@code connections}/{@code triggerOn}/
     * {@code actionOn} shorts; per-slot compounds only for configured statements). An unconfigured basic gate
     * (CLAY_BRICK has a single slot).
     */
    private static CompoundTag fixtureBasicEmpty() {
        CompoundTag variant = new CompoundTag();
        variant.putByte("logic", (byte) 0);
        variant.putByte("material", (byte) 0);
        variant.putByte("modifier", (byte) 0);
        CompoundTag root = new CompoundTag();
        root.put("variant", variant);
        root.putShort("connections", (short) 0);
        root.putShort("triggerOn", (short) 0);
        root.putShort("actionOn", (short) 0);
        return root;
    }

    /**
     * Two-slot iron OR gate (material 2 slots / LAPIS divisor 1). Statement kinds are real legacy tags:
     * {@code buildcraft:redstone.input.active} from
     * {@code buildcraft.core.statements.TriggerRedstoneInput} and {@code buildcraft:redstone.output} from
     * {@code buildcraft.core.statements.ActionRedstoneOutput}. The {@code s} sub-compound ({@code kind} +
     * {@code side} bytes) is written by {@code buildcraft.silicon.gate.TriggerType}/{@code ActionType#writeToNbt};
     * the numbered parameter key and the {@code direction} payload byte come from
     * {@code buildcraft.lib.statement.FullStatement#writeToNbt} +
     * {@code buildcraft.core.statements.StatementParameterDirection#writeToNbt}.
     */
    private static CompoundTag fixtureIronOrGate() {
        CompoundTag root = fixtureBasicEmpty();
        root.getCompoundOrEmpty("variant").putByte("logic", (byte) 1); // OR
        root.getCompoundOrEmpty("variant").putByte("material", (byte) 1); // IRON: 2 slots
        root.getCompoundOrEmpty("variant").putByte("modifier", (byte) 1); // LAPIS: 1 trigger arg
        root.putShort("connections", (short) 1); // slots 0 and 1 grouped
        root.putShort("triggerOn", (short) 1);

        CompoundTag triggerS = new CompoundTag();
        triggerS.putString("kind", "buildcraft:redstone.input.active");
        triggerS.putByte("side", (byte) 2); // EnumPipePart NORTH
        CompoundTag trigger = new CompoundTag();
        trigger.put("s", triggerS);

        CompoundTag actionS = new CompoundTag();
        actionS.putString("kind", "buildcraft:redstone.output");
        actionS.putByte("side", (byte) 6); // EnumPipePart CENTER
        CompoundTag directionParam = new CompoundTag();
        directionParam.putString("kind", "buildcraft:pipeActionDirection");
        directionParam.putByte("direction", (byte) 1); // UP
        CompoundTag action = new CompoundTag();
        action.put("s", actionS);
        action.put("0", directionParam);

        root.put("trigger[0]", trigger);
        root.put("action[1]", action);
        return root;
    }

    /**
     * Four-slot gold AND gate with diamond modifier (8 slots / divisor 2), exercising: a redstone level parameter
     * (bytes {@code l}/{@code mi}/{@code ma}, kind {@code buildcraft:redstoneLevel}, from
     * {@code buildcraft.core.statements.StatementParameterRedstoneLevel}), a wire colour parameter (byte
     * {@code color}, kind {@code buildcraft:pipeWireTrigger}, from
     * {@code buildcraft.transport.statements.TriggerParameterSignal#writeToNbt}), a stack parameter (kind
     * {@code buildcraft:stack}, from {@code buildcraft.api.statements.StatementParameterItemStack#writeToNbt}), and a
     * parameter gap (cleared middle argument - legacy persists the gap by omitting the numeric key, and the conversion
     * must keep it).
     */
    private static CompoundTag fixtureGoldAndGate() {
        CompoundTag root = fixtureBasicEmpty();
        root.getCompoundOrEmpty("variant").putByte("material", (byte) 3); // GOLD: 8 slots / 2 = 4
        root.getCompoundOrEmpty("variant").putByte("modifier", (byte) 3); // DIAMOND: 3 args per side
        root.putShort("connections", (short) 5); // bits 0 and 2 -> (0,1) and (2,3) grouped

        // slot 0: action with redstone level param
        CompoundTag action0 = new CompoundTag();
        CompoundTag action0S = new CompoundTag();
        action0S.putString("kind", "buildcraft:redstone.output");
        action0S.putByte("side", (byte) 6);
        action0.put("s", action0S);
        CompoundTag redstoneParam = new CompoundTag();
        redstoneParam.putString("kind", "buildcraft:redstoneLevel");
        redstoneParam.putByte("l", (byte) 9);
        redstoneParam.putByte("mi", (byte) 0);
        redstoneParam.putByte("ma", (byte) 15);
        action0.put("0", redstoneParam);
        root.put("action[0]", action0);

        // slot 1: trigger with wire colour param
        CompoundTag trigger1 = new CompoundTag();
        CompoundTag trigger1S = new CompoundTag();
        trigger1S.putString("kind", "buildcraft:redstone.input.inactive");
        trigger1S.putByte("side", (byte) 4); // WEST
        trigger1.put("s", trigger1S);
        CompoundTag colorParam = new CompoundTag();
        colorParam.putString("kind", "buildcraft:pipeWireTrigger");
        colorParam.putByte("color", (byte) 3);
        trigger1.put("0", colorParam);
        root.put("trigger[1]", trigger1);

        // slot 2: trigger whose first argument is cleared (gap: only "1" exists) and a stack parameter
        CompoundTag trigger2 = new CompoundTag();
        CompoundTag trigger2S = new CompoundTag();
        trigger2S.putString("kind", "buildcraft:inventory.match"); // statement tag shape of the legacy inventory triggers
        trigger2S.putByte("side", (byte) 5); // EAST
        trigger2.put("s", trigger2S);
        CompoundTag stackParam = new CompoundTag();
        stackParam.putString("kind", "buildcraft:stack");
        CompoundTag stack = new CompoundTag();
        stack.putString("id", "minecraft:iron_ingot");
        stack.putByte("Count", (byte) 3);
        stackParam.put("stack", stack);
        trigger2.put("1", stackParam); // gap at position 0
        root.put("trigger[2]", trigger2);

        root.putShort("triggerOn", (short) 2); // slot 1 active
        root.putShort("actionOn", (short) 1); // slot 0 active
        return root;
    }

    @Test
    public void basicEmptyGateRoundTrips() {
        CompoundTag nbt = fixtureBasicEmpty();
        BcGateConfig config = BcGateNbt.read(nbt);
        assertEquals(EnumGateLogic.AND, config.variant().logic());
        assertEquals(EnumGateMaterial.CLAY_BRICK, config.variant().material());
        assertEquals(1, config.slots().size());
        assertEquals(Optional.empty(), config.slots().get(0).trigger());
        assertEquals(0, config.variant().numTriggerArgs());
        DataComponentTestHelper.assertNbtEquals(nbt, BcGateNbt.write(config));
    }

    @Test
    public void ironOrGateRoundTrips() {
        CompoundTag nbt = fixtureIronOrGate();
        BcGateConfig config = BcGateNbt.read(nbt);
        assertEquals(EnumGateLogic.OR, config.variant().logic());
        assertEquals(2, config.variant().numSlots());
        assertEquals(1, config.connections());
        BcGateStatement trigger = config.slots().get(0).trigger().orElseThrow();
        assertEquals("buildcraft:redstone.input.active", trigger.kind());
        assertEquals(2, trigger.side());
        BcGateStatement action = config.slots().get(1).action().orElseThrow();
        BcGateParam.DirectionParam direction = (BcGateParam.DirectionParam) action.params().get(0);
        assertEquals(1, direction.direction());
        DataComponentTestHelper.assertNbtEquals(nbt, BcGateNbt.write(config));
    }

    @Test
    public void goldAndGateRoundTripsWithParameterGap() {
        CompoundTag nbt = fixtureGoldAndGate();
        BcGateConfig config = BcGateNbt.read(nbt);
        assertEquals(4, config.variant().numSlots());
        assertEquals(5, config.connections());
        BcGateStatement action0 = config.slots().get(0).action().orElseThrow();
        BcGateParam.RedstoneLevelParam level = (BcGateParam.RedstoneLevelParam) action0.params().get(0);
        assertEquals((byte) 9, level.level());
        BcGateStatement trigger2 = config.slots().get(2).trigger().orElseThrow();
        // the cleared middle argument must stay at position 1 (gap preserved)
        assertEquals(java.util.Set.of(1), trigger2.params().keySet());
        assertTrue(trigger2.params().get(1) instanceof BcGateParam.StackParam);
        DataComponentTestHelper.assertNbtEquals(nbt, BcGateNbt.write(config));
    }

    /** Missing keys default exactly like legacy: {@code GateVariant(CompoundTag)} clamps via
     * {@code getByOrdinal}, {@code CompoundTag#getShort} reads 0. */
    @Test
    public void emptyCompoundReadsDefaults() {
        BcGateConfig config = BcGateNbt.read(new CompoundTag());
        assertEquals(EnumGateLogic.AND, config.variant().logic());
        assertEquals(EnumGateMaterial.CLAY_BRICK, config.variant().material());
        assertEquals(EnumGateModifier.NO_MODIFIER, config.variant().modifier());
        assertEquals(0, config.connections());
        assertEquals(1, config.slots().size());
    }

    /** Unknown statement parameter kinds are dropped (read as null in legacy). */
    @Test
    public void unknownParamKindIsDropped() {
        CompoundTag slot = new CompoundTag();
        CompoundTag s = new CompoundTag();
        s.putString("kind", "buildcraft:redstone.output");
        s.putByte("side", (byte) 6);
        slot.put("s", s);
        CompoundTag param = new CompoundTag();
        param.putString("kind", "buildcraft:someFutureParam");
        param.putByte("color", (byte) 1);
        slot.put("0", param);
        BcGateStatement statement = BcGateStatement.readFromNbt(slot);
        assertEquals(Map.of(), statement.params());
    }

    /** A statement slot without an {@code s} kind reads as no statement, like legacy
     * {@code FullStatement#readFromNbt} with a null statement. */
    @Test
    public void missingKindReadsNoStatement() {
        CompoundTag slot = new CompoundTag();
        slot.putByte("side", (byte) 0);
        assertNull(BcGateStatement.readFromNbt(slot));
    }

    /** Component codec round trip (buffer) for all fixtures built as values. */
    @Test
    public void gateStreamCodecRoundTrips() {
        List<BcGateConfig> configs = List.of(
            BcGateNbt.read(fixtureBasicEmpty()),
            BcGateNbt.read(fixtureIronOrGate()),
            BcGateNbt.read(fixtureGoldAndGate()),
            BcGateConfig.DEFAULT);
        for (BcGateConfig config : configs) {
            assertEquals(config, DataComponentTestHelper.bufRoundTrip(BcGateConfig.STREAM_CODEC, config));
        }
    }
}
