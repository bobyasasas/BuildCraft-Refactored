/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.datacomponent.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

/**
 * One filter slot description, registry-dispatched by its {@code kind} string exactly like the legacy statement
 * parameter system dispatches through {@code buildcraft.api.statements.StatementTypeParam#writeToNbt} (which writes a
 * {@code kind} string into the parameter compound before the parameter's own keys). This is the M2.6 replacement for
 * the in-memory-only filter hierarchy of legacy {@code buildcraft.lib.inventory.filter}.
 *
 * <p>Kinds carried over verbatim from the legacy parameter implementations (see {@link #READERS}):
 * {@code buildcraft:stack} / {@code buildcraft:stackExact}. Kinds with no legacy NBT writer (the legacy filter classes
 * were never serialized) reuse the legacy field names as keys and are marked on the record.
 */
public sealed interface BcFilterSpec permits BcFilterSpec.StackFilter, BcFilterSpec.ExactStackFilter,
    BcFilterSpec.OreFilter, BcFilterSpec.FluidFilter, BcFilterSpec.InvertedFilter {

    /** @return The dispatch key, identical to the legacy {@code IStatementParameter#getUniqueTag} where one exists. */
    String kind();

    /** Writes this spec as a legacy-style parameter compound: the {@code kind} string first, then the payload keys
     * (mirror of legacy {@code StatementTypeParam#writeToNbt}). */
    default CompoundTag writeToNbt() {
        CompoundTag nbt = WRITERS.get(kind()).apply(this);
        nbt.putString("kind", kind());
        return nbt;
    }

    /** Reads one spec from a legacy-style parameter compound. Returns null for unknown kinds, matching legacy
     * {@code StatementTypeParam#readFromNbt} (which returns null when no parameter reader is registered). */
    static BcFilterSpec read(CompoundTag nbt) {
        String kind = nbt.getStringOr("kind", "");
        Function<CompoundTag, BcFilterSpec> reader = READERS.get(kind);
        return reader == null ? null : reader.apply(nbt);
    }

    /** Registry of kind -> reader, the dispatch table replacing legacy
     * {@code buildcraft.api.statements.StatementManager#registerParameter}. Insertion ordered so the wire ids assigned
     * by {@link #writeSpecToBuf} stay deterministic. */
    Map<String, Function<CompoundTag, BcFilterSpec>> READERS = buildReaders();

    /** Registry of kind -> payload-only writer (the {@code kind} string itself is added by {@link #writeToNbt}). */
    Map<String, Function<BcFilterSpec, CompoundTag>> WRITERS = buildWriters();

    private static Map<String, Function<CompoundTag, BcFilterSpec>> buildReaders() {
        Map<String, Function<CompoundTag, BcFilterSpec>> readers = new LinkedHashMap<>();
        readers.put(StackFilter.KIND, (CompoundTag nbt) -> new StackFilter(BcItemStack.readFromNbt(nbt
            .getCompoundOrEmpty("stack"))));
        readers.put(ExactStackFilter.KIND, (CompoundTag nbt) -> new ExactStackFilter(BcItemStack.readFromNbt(nbt
            .getCompoundOrEmpty("stack"))));
        readers.put(OreFilter.KIND, BcFilterSpec::readOreFilter);
        readers.put(FluidFilter.KIND, BcFilterSpec::readFluidFilter);
        readers.put(InvertedFilter.KIND, (CompoundTag nbt) -> new InvertedFilter(read(nbt.getCompoundOrEmpty("filter"))));
        // insertion order is load-bearing: WIRE_KINDS assigns wire ids from this map's key order
        return Collections.unmodifiableMap(readers);
    }

    private static Map<String, Function<BcFilterSpec, CompoundTag>> buildWriters() {
        Map<String, Function<BcFilterSpec, CompoundTag>> writers = new LinkedHashMap<>();
        writers.put(StackFilter.KIND, (BcFilterSpec spec) -> {
            StackFilter stack = (StackFilter) spec;
            CompoundTag nbt = new CompoundTag();
            nbt.put("stack", stack.stack().writeToNbt());
            return nbt;
        });
        writers.put(ExactStackFilter.KIND, (BcFilterSpec spec) -> {
            ExactStackFilter stack = (ExactStackFilter) spec;
            CompoundTag nbt = new CompoundTag();
            nbt.put("stack", stack.stack().writeToNbt());
            return nbt;
        });
        writers.put(OreFilter.KIND, (BcFilterSpec spec) -> {
            OreFilter ore = (OreFilter) spec;
            CompoundTag nbt = new CompoundTag();
            ListTag list = new ListTag();
            for (Identifier tag : ore.ores()) {
                CompoundTag entry = new CompoundTag();
                entry.putString("tag", tag.toString());
                list.add(entry);
            }
            nbt.put("ores", list);
            return nbt;
        });
        writers.put(FluidFilter.KIND, (BcFilterSpec spec) -> {
            FluidFilter fluid = (FluidFilter) spec;
            CompoundTag nbt = new CompoundTag();
            ListTag list = new ListTag();
            for (BcFluidStack stack : fluid.fluids()) {
                list.add(stack.writeToNbt());
            }
            nbt.put("fluids", list);
            return nbt;
        });
        writers.put(InvertedFilter.KIND, (BcFilterSpec spec) -> {
            InvertedFilter inverted = (InvertedFilter) spec;
            CompoundTag nbt = new CompoundTag();
            nbt.put("filter", inverted.filter().writeToNbt());
            return nbt;
        });
        // insertion order is load-bearing: WIRE_KINDS assigns wire ids from this map's key order
        return Collections.unmodifiableMap(writers);
    }

    private static BcFilterSpec readOreFilter(CompoundTag nbt) {
        ListTag list = nbt.getListOrEmpty("ores");
        List<Identifier> ores = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            ores.add(Identifier.parse(list.getCompoundOrEmpty(i).getStringOr("tag", "minecraft:air")));
        }
        return new OreFilter(ores);
    }

    private static BcFilterSpec readFluidFilter(CompoundTag nbt) {
        ListTag list = nbt.getListOrEmpty("fluids");
        List<BcFluidStack> fluids = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            fluids.add(BcFluidStack.readFromNbt(list.getCompoundOrEmpty(i)));
        }
        return new FluidFilter(fluids);
    }

    /** Item filter matching by item identity only. Legacy counterpart:
     * {@code buildcraft.api.statements.StatementParameterItemStack} ({@code getUniqueTag} = {@code buildcraft:stack},
     * payload key {@code stack} written by {@code writeToNbt} as a legacy 1.20.1 {@code ItemStack.save} compound).
     * Matching semantics come from {@code buildcraft.lib.inventory.filter.ArrayStackFilter} (item identity only). */
    record StackFilter(String kind, BcItemStack stack) implements BcFilterSpec {

        public static final String KIND = "buildcraft:stack";

        public StackFilter(BcItemStack stack) {
            this(KIND, stack);
        }
    }

    /** Item filter matching item + damage + NBT. Legacy counterpart:
     * {@code buildcraft.core.statements.StatementParameterItemStackExact} ({@code getUniqueTag} =
     * {@code buildcraft:stackExact}, payload key {@code stack}). */
    record ExactStackFilter(String kind, BcItemStack stack) implements BcFilterSpec {

        public static final String KIND = "buildcraft:stackExact";

        public ExactStackFilter(BcItemStack stack) {
            this(KIND, stack);
        }
    }

    /** Item tag filter. Legacy counterpart: {@code buildcraft.lib.inventory.filter.OreStackFilter}, which keeps a list
     * of {@code TagKey<Item>} in its {@code ores} field. The legacy class was never serialized, so the {@code ores}
     * key reuses the field name. */
    record OreFilter(List<Identifier> ores) implements BcFilterSpec {

        public static final String KIND = "buildcraft:ore";

        @Override
        public String kind() {
            return KIND;
        }
    }

    /** Fluid filter. Legacy counterpart: {@code buildcraft.lib.inventory.filter.ArrayFluidFilter} (field
     * {@code fluids}). The legacy class was never serialized, so the {@code fluids} key reuses the field name and the
     * entries follow the Forge {@code FluidStack} NBT convention (see {@link BcFluidStack}). */
    record FluidFilter(List<BcFluidStack> fluids) implements BcFilterSpec {

        public static final String KIND = "buildcraft:fluid";

        @Override
        public String kind() {
            return KIND;
        }
    }

    /** Negating wrapper. Legacy counterpart: {@code buildcraft.lib.inventory.filter.InvertedStackFilter} (field
     * {@code filter}). The legacy class was never serialized, so the {@code filter} key reuses the field name. */
    record InvertedFilter(BcFilterSpec filter) implements BcFilterSpec {

        public static final String KIND = "buildcraft:inverted";

        @Override
        public String kind() {
            return KIND;
        }
    }

    // Wire format: a stable small kind id (insertion order of READERS) followed by the payload. Recursive payloads
    // (InvertedFilter) recurse directly, so no lazy machinery is needed.

    List<String> WIRE_KINDS = List.copyOf(READERS.keySet());

    static void writeSpecToBuf(BcFilterSpec spec, FriendlyByteBuf buf) {
        int id = WIRE_KINDS.indexOf(spec.kind());
        if (id < 0) {
            throw new IllegalStateException("Unregistered filter spec kind " + spec.kind());
        }
        buf.writeByte(id);
        switch (spec) {
            case StackFilter s -> s.stack().writeToBuf(buf);
            case ExactStackFilter s -> s.stack().writeToBuf(buf);
            case OreFilter s -> {
                buf.writeVarInt(s.ores().size());
                for (Identifier ore : s.ores()) {
                    Identifier.STREAM_CODEC.encode(buf, ore);
                }
            }
            case FluidFilter s -> {
                buf.writeVarInt(s.fluids().size());
                for (BcFluidStack fluid : s.fluids()) {
                    fluid.writeToBuf(buf);
                }
            }
            case InvertedFilter s -> writeSpecToBuf(s.filter(), buf);
        }
    }

    static BcFilterSpec readSpecFromBuf(FriendlyByteBuf buf) {
        int id = buf.readUnsignedByte();
        if (id >= WIRE_KINDS.size()) {
            throw new IllegalStateException("Unknown filter spec kind id " + id);
        }
        return switch (WIRE_KINDS.get(id)) {
            case StackFilter.KIND -> new StackFilter(BcItemStack.readFromBuf(buf));
            case ExactStackFilter.KIND -> new ExactStackFilter(BcItemStack.readFromBuf(buf));
            case OreFilter.KIND -> {
                int size = buf.readVarInt();
                List<Identifier> ores = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    ores.add(Identifier.STREAM_CODEC.decode(buf));
                }
                yield new OreFilter(ores);
            }
            case FluidFilter.KIND -> {
                int size = buf.readVarInt();
                List<BcFluidStack> fluids = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    fluids.add(BcFluidStack.readFromBuf(buf));
                }
                yield new FluidFilter(fluids);
            }
            case InvertedFilter.KIND -> new InvertedFilter(readSpecFromBuf(buf));
            default -> throw new IllegalStateException("Unhandled filter spec kind " + WIRE_KINDS.get(id));
        };
    }
}
