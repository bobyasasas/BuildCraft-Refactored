/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.datacomponent.gate;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import buildcraft.lib.datacomponent.filter.BcItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * One gate statement argument, registry-dispatched by its {@code kind} string. This is the M2.6 replacement for the
 * parameter half of the legacy gate statement compounds: {@code buildcraft.lib.statement.StatementTypeParam} writes
 * {@code kind = IStatementParameter#getUniqueTag} into the parameter compound followed by the parameter's own keys, and
 * {@code StatementManager} dispatches readers by that kind string.
 *
 * <p>Every kind below is carried over verbatim from a legacy parameter implementation, including its payload keys.
 */
public sealed interface BcGateParam permits BcGateParam.StackParam, BcGateParam.ColorParam,
    BcGateParam.DirectionParam, BcGateParam.RedstoneLevelParam {

    /** @return The dispatch key, identical to the legacy {@code IStatementParameter#getUniqueTag}. */
    String kind();

    /** Writes this param as a legacy-style parameter compound: the {@code kind} string first, then the payload keys
     * (mirror of legacy {@code StatementTypeParam#writeToNbt}). */
    default CompoundTag writeToNbt() {
        CompoundTag nbt = WRITERS.get(kind()).apply(this);
        nbt.putString("kind", kind());
        return nbt;
    }

    /** Reads one param from a legacy-style parameter compound. Returns null for unknown kinds, matching legacy
     * {@code StatementTypeParam#readFromNbt} (which returns null when no reader is registered). */
    static BcGateParam read(CompoundTag nbt) {
        String kind = nbt.getStringOr("kind", "");
        Function<CompoundTag, BcGateParam> reader = READERS.get(kind);
        return reader == null ? null : reader.apply(nbt);
    }

    /** Registry of kind -> reader (the dispatch table replacing the {@code StatementManager#registerParameter} calls
     * for the parameter types used by gate statements). */
    Map<String, Function<CompoundTag, BcGateParam>> READERS = buildReaders();

    /** Registry of kind -> payload-only writer (the {@code kind} string is added by {@link #writeToNbt}). */
    Map<String, Function<BcGateParam, CompoundTag>> WRITERS = buildWriters();

    private static Map<String, Function<CompoundTag, BcGateParam>> buildReaders() {
        Map<String, Function<CompoundTag, BcGateParam>> readers = new LinkedHashMap<>();
        readers.put(StackParam.KIND_STACK, nbt -> new StackParam(StackParam.KIND_STACK, BcItemStack.readFromNbt(nbt
            .getCompoundOrEmpty("stack"))));
        readers.put(StackParam.KIND_STACK_EXACT, nbt -> new StackParam(StackParam.KIND_STACK_EXACT, BcItemStack
            .readFromNbt(nbt.getCompoundOrEmpty("stack"))));
        readers.put(ColorParam.KIND_TRIGGER, nbt -> new ColorParam(ColorParam.KIND_TRIGGER, nbt.getByteOr("color",
            (byte) 0)));
        readers.put(ColorParam.KIND_ACTION, nbt -> new ColorParam(ColorParam.KIND_ACTION, nbt.getByteOr("color",
            (byte) 0)));
        readers.put(DirectionParam.KIND, nbt -> new DirectionParam(nbt.getByteOr("direction", (byte) -1)));
        readers.put(RedstoneLevelParam.KIND, RedstoneLevelParam::readFromNbt);
        // insertion order is load-bearing: WIRE_KINDS assigns wire ids from this map's key order
        return Collections.unmodifiableMap(readers);
    }

    private static Map<String, Function<BcGateParam, CompoundTag>> buildWriters() {
        Map<String, Function<BcGateParam, CompoundTag>> writers = new LinkedHashMap<>();
        writers.put(StackParam.KIND_STACK, BcGateParam::writeStackPayload);
        writers.put(StackParam.KIND_STACK_EXACT, BcGateParam::writeStackPayload);
        writers.put(ColorParam.KIND_TRIGGER, BcGateParam::writeColorPayload);
        writers.put(ColorParam.KIND_ACTION, BcGateParam::writeColorPayload);
        writers.put(DirectionParam.KIND, param -> {
            DirectionParam direction = (DirectionParam) param;
            CompoundTag nbt = new CompoundTag();
            // legacy omits the key entirely for the null-direction state
            if (direction.direction() != DirectionParam.NULL_DIRECTION) {
                nbt.putByte("direction", direction.direction());
            }
            return nbt;
        });
        writers.put(RedstoneLevelParam.KIND, param -> {
            RedstoneLevelParam level = (RedstoneLevelParam) param;
            CompoundTag nbt = new CompoundTag();
            nbt.putByte("l", level.level());
            nbt.putByte("mi", level.minLevel());
            nbt.putByte("ma", level.maxLevel());
            return nbt;
        });
        // insertion order is load-bearing: WIRE_KINDS assigns wire ids from this map's key order
        return Collections.unmodifiableMap(writers);
    }

    private static CompoundTag writeStackPayload(BcGateParam param) {
        StackParam stack = (StackParam) param;
        CompoundTag nbt = new CompoundTag();
        nbt.put("stack", stack.stack().writeToNbt());
        return nbt;
    }

    private static CompoundTag writeColorPayload(BcGateParam param) {
        ColorParam color = (ColorParam) param;
        CompoundTag nbt = new CompoundTag();
        nbt.putByte("color", color.color());
        return nbt;
    }

    /** Item stack argument. Legacy counterparts: {@code buildcraft.api.statements.StatementParameterItemStack}
     * ({@code buildcraft:stack}) and {@code buildcraft.core.statements.StatementParameterItemStackExact}
     * ({@code buildcraft:stackExact}); both write the payload under the {@code stack} key as a legacy 1.20.1
     * {@code ItemStack.save} compound. The kind stays a field because legacy has two unique tags with the same shape. */
    record StackParam(String kind, BcItemStack stack) implements BcGateParam {

        public static final String KIND_STACK = "buildcraft:stack";
        public static final String KIND_STACK_EXACT = "buildcraft:stackExact";
    }

    /** Wire colour argument (a {@code DyeColor} id byte). Legacy counterparts:
     * {@code buildcraft.transport.statements.TriggerParameterSignal} ({@code buildcraft:pipeWireTrigger}) and
     * {@code buildcraft.transport.statements.ActionParameterSignal} ({@code buildcraft:pipeWireAction}); both write
     * the payload under the {@code color} key ({@code nbt.putByte("color", ...)}). */
    record ColorParam(String kind, byte color) implements BcGateParam {

        public static final String KIND_TRIGGER = "buildcraft:pipeWireTrigger";
        public static final String KIND_ACTION = "buildcraft:pipeWireAction";
    }

    /** Direction argument ({@code Direction#get3DDataValue} byte). Legacy counterpart:
     * {@code buildcraft.core.statements.StatementParameterDirection} ({@code buildcraft:pipeActionDirection}, payload
     * key {@code direction}). A legacy direction parameter may carry a null direction (the legacy writer omits the
     * {@code direction} key then); that state is encoded as the documented sentinel {@code -1}. */
    record DirectionParam(byte direction) implements BcGateParam {

        public static final String KIND = "buildcraft:pipeActionDirection";

        /** Sentinel for the legacy null-direction state (legacy simply omits the {@code direction} key). */
        public static final byte NULL_DIRECTION = -1;

        public DirectionParam {
            if (direction < NULL_DIRECTION) {
                throw new IllegalArgumentException("direction out of range: " + direction);
            }
        }

        public DirectionParam(net.minecraft.core.Direction direction) {
            this(direction == null ? NULL_DIRECTION : (byte) direction.get3DDataValue());
        }

        @Override
        public String kind() {
            return KIND;
        }
    }

    /** Redstone level argument. Legacy counterpart:
     * {@code buildcraft.core.statements.StatementParameterRedstoneLevel} ({@code buildcraft:redstoneLevel}), which
     * writes the bytes {@code l} (level), {@code mi} (min level) and {@code ma} (max level). */
    record RedstoneLevelParam(byte level, byte minLevel, byte maxLevel) implements BcGateParam {

        public static final String KIND = "buildcraft:redstoneLevel";

        @Override
        public String kind() {
            return KIND;
        }

        static RedstoneLevelParam readFromNbt(CompoundTag nbt) {
            return new RedstoneLevelParam(nbt.getByteOr("l", (byte) 0), nbt.getByteOr("mi", (byte) 0), nbt.getByteOr(
                "ma", (byte) 15));
        }
    }

    // Wire format: stable small kind id (insertion order of READERS) followed by the payload.

    List<String> WIRE_KINDS = List.copyOf(READERS.keySet());

    byte WIRE_STACK = 0;
    byte WIRE_STACK_EXACT = 1;
    byte WIRE_COLOR_TRIGGER = 2;
    byte WIRE_COLOR_ACTION = 3;
    byte WIRE_DIRECTION = 4;
    byte WIRE_REDSTONE_LEVEL = 5;

    static void writeParamToBuf(BcGateParam param, FriendlyByteBuf buf) {
        if (param instanceof StackParam stack) {
            buf.writeByte(stack.kind().equals(StackParam.KIND_STACK) ? WIRE_STACK : WIRE_STACK_EXACT);
            stack.stack().writeToBuf(buf);
        } else if (param instanceof ColorParam color) {
            buf.writeByte(color.kind().equals(ColorParam.KIND_TRIGGER) ? WIRE_COLOR_TRIGGER : WIRE_COLOR_ACTION);
            buf.writeByte(color.color());
        } else if (param instanceof DirectionParam direction) {
            buf.writeByte(WIRE_DIRECTION);
            buf.writeByte(direction.direction());
        } else if (param instanceof RedstoneLevelParam level) {
            buf.writeByte(WIRE_REDSTONE_LEVEL);
            buf.writeByte(level.level());
            buf.writeByte(level.minLevel());
            buf.writeByte(level.maxLevel());
        } else {
            throw new IllegalStateException("Unhandled gate param " + param);
        }
    }

    static BcGateParam readParamFromBuf(FriendlyByteBuf buf) {
        byte id = buf.readByte();
        return switch (id) {
            case WIRE_STACK -> new StackParam(StackParam.KIND_STACK, BcItemStack.readFromBuf(buf));
            case WIRE_STACK_EXACT -> new StackParam(StackParam.KIND_STACK_EXACT, BcItemStack.readFromBuf(buf));
            case WIRE_COLOR_TRIGGER -> new ColorParam(ColorParam.KIND_TRIGGER, buf.readByte());
            case WIRE_COLOR_ACTION -> new ColorParam(ColorParam.KIND_ACTION, buf.readByte());
            case WIRE_DIRECTION -> new DirectionParam(buf.readByte());
            case WIRE_REDSTONE_LEVEL -> new RedstoneLevelParam(buf.readByte(), buf.readByte(), buf.readByte());
            default -> throw new IllegalStateException("Unknown gate param kind id " + id);
        };
    }
}
