/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.datacomponent.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.Test;

import buildcraft.lib.datacomponent.DataComponentTestHelper;
import net.minecraft.nbt.CompoundTag;

/**
 * M2.6 parity tests for {@link BcFilter}: every fixture compound is a hand-written reconstruction of a legacy writer
 * (source file noted per fixture), asserted to survive NBT -> component -> NBT unchanged (key set and values) and to
 * survive the component stream codec unchanged.
 */
public class BcFilterNbtTest {

    /**
     * Legacy source: {@code buildcraft.api.statements.StatementTypeParam#writeToNbt} (writes the {@code kind} string)
     * + {@code buildcraft.api.statements.StatementParameterItemStack#writeToNbt} (writes the {@code stack} key as a
     * 1.20.1 {@code ItemStack.save} compound: {@code id} string / {@code Count} byte / {@code tag} compound).
     */
    private static CompoundTag fixtureStackParam() {
        CompoundTag stack = new CompoundTag();
        stack.putString("id", "minecraft:stone");
        stack.putByte("Count", (byte) 16);
        CompoundTag tag = new CompoundTag();
        tag.putInt("Damage", 0);
        stack.put("tag", tag);
        CompoundTag slot = new CompoundTag();
        slot.putString("kind", "buildcraft:stack");
        slot.put("stack", stack);
        CompoundTag root = new CompoundTag();
        root.put("slots", listOf(slot));
        return root;
    }

    /**
     * Legacy source: {@code buildcraft.core.statements.StatementParameterItemStackExact#writeToNbt} +
     * {@code getUniqueTag} ({@code buildcraft:stackExact}).
     */
    private static CompoundTag fixtureExactStackParam() {
        CompoundTag stack = new CompoundTag();
        stack.putString("id", "minecraft:diamond_pickaxe");
        stack.putByte("Count", (byte) 1);
        CompoundTag tag = new CompoundTag();
        tag.putInt("Damage", 25);
        stack.put("tag", tag);
        CompoundTag slot = new CompoundTag();
        slot.putString("kind", "buildcraft:stackExact");
        slot.put("stack", stack);
        CompoundTag root = new CompoundTag();
        root.put("slots", listOf(slot));
        return root;
    }

    /**
     * Legacy source: {@code buildcraft.lib.inventory.filter.OreStackFilter} (list of {@code TagKey<Item>} in the
     * {@code ores} field), {@code buildcraft.lib.inventory.filter.InvertedStackFilter} (wrapping {@code filter} field)
     * and {@code buildcraft.lib.inventory.filter.ArrayFluidFilter} (fluid list following the Forge
     * {@code FluidStack} NBT convention: {@code FluidName} string / {@code Amount} int, as stored by e.g.
     * {@code buildcraft.transport.pipe.flow.PipeFlowFluids#writeToNbt}). These legacy classes had no writer, so the
     * key names reuse the legacy field names (documented on {@link BcFilterSpec}).
     */
    private static CompoundTag fixtureMixedSpecs() {
        CompoundTag ore = new CompoundTag();
        ore.putString("kind", "buildcraft:ore");
        ore.put("ores", listOf(entry("tag", "forge:ores"), entry("tag", "forge:gems/diamond")));

        CompoundTag innerStack = new CompoundTag();
        innerStack.putString("id", "minecraft:dirt");
        innerStack.putByte("Count", (byte) 1);
        CompoundTag inner = new CompoundTag();
        inner.putString("kind", "buildcraft:stack");
        inner.put("stack", innerStack);
        CompoundTag inverted = new CompoundTag();
        inverted.putString("kind", "buildcraft:inverted");
        inverted.put("filter", inner);

        CompoundTag water = new CompoundTag();
        water.putString("FluidName", "minecraft:water");
        water.putInt("Amount", 1000);
        CompoundTag lava = new CompoundTag();
        lava.putString("FluidName", "minecraft:lava");
        lava.putInt("Amount", 250);
        CompoundTag fluid = new CompoundTag();
        fluid.putString("kind", "buildcraft:fluid");
        fluid.put("fluids", listOf(water, lava));

        CompoundTag root = new CompoundTag();
        root.put("slots", listOf(ore, inverted, fluid));
        return root;
    }

    /** Boundary: an empty filter ({@code buildcraft.lib.inventory.filter.ArrayStackFilter#hasFilter} false case). */
    private static CompoundTag fixtureEmpty() {
        CompoundTag root = new CompoundTag();
        root.put("slots", new net.minecraft.nbt.ListTag());
        return root;
    }

    /**
     * Legacy source: the diamond/lapis pipe filter inventory -
     * {@code buildcraft.transport.pipe.behaviour.PipeBehaviourDiamond} (root key {@code filters}, 9 filter stacks per
     * pipe side) with the slot list written by
     * {@code buildcraft.lib.tile.item.ItemHandlerSimple#serializeNBT} (key {@code items}, one
     * {@code ItemStack.save} compound per slot, empty slots included).
     */
    private static CompoundTag fixtureDiamondFilterInventory() {
        CompoundTag cobble = new CompoundTag();
        cobble.putString("id", "minecraft:cobblestone");
        cobble.putByte("Count", (byte) 64);
        CompoundTag tag = new CompoundTag();
        tag.putInt("Damage", 0);
        cobble.put("tag", tag);
        CompoundTag empty = new CompoundTag();
        empty.putString("id", "minecraft:air");
        empty.putByte("Count", (byte) 0);
        CompoundTag glass = new CompoundTag();
        glass.putString("id", "minecraft:glass");
        glass.putByte("Count", (byte) 12);

        CompoundTag items = new CompoundTag();
        items.put("items", listOf(cobble, empty, glass));
        CompoundTag root = new CompoundTag();
        root.put("filters", items);
        return root;
    }

    @Test
    public void stackParamRoundTripsThroughComponent() {
        CompoundTag nbt = fixtureStackParam();
        BcFilter filter = BcFilterNbt.read(nbt);
        assertEquals(1, filter.slots().size());
        BcFilterSpec.StackFilter slot = (BcFilterSpec.StackFilter) filter.slots().get(0);
        assertEquals("buildcraft:stack", slot.kind());
        assertEquals("minecraft:stone", slot.stack().id().toString());
        assertEquals(16, slot.stack().count());
        DataComponentTestHelper.assertNbtEquals(nbt, BcFilterNbt.write(filter));
    }

    @Test
    public void exactStackParamRoundTripsThroughComponent() {
        CompoundTag nbt = fixtureExactStackParam();
        BcFilter filter = BcFilterNbt.read(nbt);
        BcFilterSpec.ExactStackFilter slot = (BcFilterSpec.ExactStackFilter) filter.slots().get(0);
        assertEquals("buildcraft:stackExact", slot.kind());
        assertEquals(25, slot.stack().tag().orElseThrow().getIntOr("Damage", 0));
        DataComponentTestHelper.assertNbtEquals(nbt, BcFilterNbt.write(filter));
    }

    @Test
    public void mixedSpecsRoundTripThroughComponent() {
        CompoundTag nbt = fixtureMixedSpecs();
        BcFilter filter = BcFilterNbt.read(nbt);
        assertEquals(3, filter.slots().size());
        BcFilterSpec.OreFilter ore = (BcFilterSpec.OreFilter) filter.slots().get(0);
        assertEquals(List.of("forge:ores", "forge:gems/diamond"),
            ore.ores().stream().map(Object::toString).toList());
        assertTrue(filter.slots().get(1) instanceof BcFilterSpec.InvertedFilter);
        BcFilterSpec.FluidFilter fluid = (BcFilterSpec.FluidFilter) filter.slots().get(2);
        assertEquals(1000, fluid.fluids().get(0).amount());
        DataComponentTestHelper.assertNbtEquals(nbt, BcFilterNbt.write(filter));
    }

    @Test
    public void emptyFilterRoundTripsThroughComponent() {
        CompoundTag nbt = fixtureEmpty();
        BcFilter filter = BcFilterNbt.read(nbt);
        assertEquals(0, filter.slots().size());
        DataComponentTestHelper.assertNbtEquals(nbt, BcFilterNbt.write(filter));
    }

    /** A spec compound written by the component must equal the legacy parameter compound shape ({@code kind} string
     * plus payload keys), so old readers can consume new data. */
    @Test
    public void writtenSlotMatchesLegacyParameterCompoundShape() {
        BcFilterSpec spec = new BcFilterSpec.ExactStackFilter(
            new BcItemStack(net.minecraft.resources.Identifier.parse("minecraft:diamond_pickaxe"), 1,
                Optional.empty()));
        CompoundTag written = spec.writeToNbt();

        CompoundTag expected = new CompoundTag();
        expected.putString("kind", "buildcraft:stackExact");
        CompoundTag stack = new CompoundTag();
        stack.putString("id", "minecraft:diamond_pickaxe");
        stack.putByte("Count", (byte) 1);
        expected.put("stack", stack);

        DataComponentTestHelper.assertNbtEquals(expected, written);
    }

    /** Unknown kinds read as null, exactly like legacy {@code StatementTypeParam#readFromNbt}. */
    @Test
    public void unknownKindReadsNull() {
        CompoundTag slot = new CompoundTag();
        slot.putString("kind", "buildcraft:someFutureFilter");
        assertNull(BcFilterSpec.read(slot));
    }

    /** Diamond pipe filter inventory: component stack list conversion keeps the exact
     * {@code ItemHandlerSimple#serializeNBT} shape (including empty slots). */
    @Test
    public void diamondFilterInventoryRoundTrips() {
        CompoundTag nbt = fixtureDiamondFilterInventory();
        List<BcItemStack> stacks = BcFilterNbt.readStacks(nbt.getCompoundOrEmpty("filters"));
        assertEquals(3, stacks.size());
        assertEquals(64, stacks.get(0).count());
        assertTrue(stacks.get(1).isEmpty());
        assertEquals(12, stacks.get(2).count());

        CompoundTag written = new CompoundTag();
        written.put("filters", BcFilterNbt.writeStacks(stacks));
        DataComponentTestHelper.assertNbtEquals(nbt, written);
    }

    /** Component codec round trip (buffer) for all fixtures built as values. */
    @Test
    public void filterStreamCodecRoundTrips() {
        List<BcFilter> filters = List.of(
            BcFilterNbt.read(fixtureStackParam()),
            BcFilterNbt.read(fixtureExactStackParam()),
            BcFilterNbt.read(fixtureMixedSpecs()),
            BcFilterNbt.read(fixtureEmpty()),
            BcFilter.EMPTY);
        for (BcFilter filter : filters) {
            assertEquals(filter, DataComponentTestHelper.bufRoundTrip(BcFilter.STREAM_CODEC, filter));
        }
    }

    private static net.minecraft.nbt.ListTag listOf(CompoundTag... tags) {
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (CompoundTag tag : tags) {
            list.add(tag);
        }
        return list;
    }

    private static CompoundTag entry(String key, String value) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString(key, value);
        return nbt;
    }
}
