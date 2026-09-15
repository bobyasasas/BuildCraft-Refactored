/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.test.builders.snapshot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Assert;
import org.junit.Test;

import buildcraft.builders.snapshot.EnumNbtCompareOperation;
import buildcraft.builders.snapshot.JsonSelector;
import buildcraft.builders.snapshot.JsonRule;
import buildcraft.builders.snapshot.NbtPath;
import buildcraft.builders.snapshot.RequiredExtractor;
import buildcraft.lib.misc.JsonUtil;
import buildcraft.lib.misc.NBTUtilBC;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * Characterization tests (M0.5 baseline) for the Gson building blocks that {@link buildcraft.builders.snapshot.RulesLoader}
 * assembles its snapshot rules from (NbtPath traversal, NBT comparison operations, JsonSelector matching) on the 1.20.1
 * baseline. Surprising expectations pin down CURRENT behaviour on purpose - they are regression guards for the NeoForge
 * migration, NOT statements about what the behaviour ought to be.
 *
 * Coverage boundary: RulesLoader itself reads its rule JSON through ForgeMod-loaded resources and caches by BlockState
 * (net.minecraftforge.fml.ModList / ForgeRegistries), so only the public deserializer parts are tested here.
 */
public class NbtRuleCharacterizationTester {

    private static final Gson PATH_GSON = new GsonBuilder()//
        .registerTypeAdapter(NbtPath.class, NbtPath.DESERIALIZER)//
        .create();

    /** The same registrations RulesLoader uses, minus the registry-dependent ones. */
    private static final Gson RULE_GSON = JsonUtil.registerNbtSerializersDeserializers(new GsonBuilder())//
        .registerTypeAdapter(EnumNbtCompareOperation.class, EnumNbtCompareOperation.DESERIALIZER)//
        .registerTypeAdapter(NbtPath.class, NbtPath.DESERIALIZER)//
        .registerTypeAdapterFactory(JsonSelector.TYPE_ADAPTER_FACTORY)//
        .create();

    private static CompoundTag makeSampleNbt() {
        CompoundTag root = new CompoundTag();
        CompoundTag inner = new CompoundTag();
        inner.putString("s", "val");
        inner.putInt("i", 7);
        inner.putDouble("d", 7.0);
        root.put("a", inner);
        ListTag list = new ListTag();
        list.add(StringTag.valueOf("e0"));
        list.add(StringTag.valueOf("e1"));
        root.put("list", list);
        root.putString("str", "text");
        return root;
    }

    @Test
    public void nbtPathTraversesCompounds() {
        NbtPath path = PATH_GSON.fromJson("[\"a\", \"s\"]", NbtPath.class);
        Assert.assertEquals(StringTag.valueOf("val"), path.get(makeSampleNbt()));
    }

    @Test
    public void nbtPathIndexesLists() {
        NbtPath path = PATH_GSON.fromJson("[\"list\", \"1\"]", NbtPath.class);
        Assert.assertEquals(StringTag.valueOf("e1"), path.get(makeSampleNbt()));
    }

    @Test
    public void nbtPathEmptyReturnsTheRootItself() {
        NbtPath path = PATH_GSON.fromJson("[]", NbtPath.class);
        CompoundTag root = makeSampleNbt();
        // Characterization baseline: the same instance is returned, not a copy
        Assert.assertTrue(root == path.get(root));
    }

    @Test
    public void nbtPathFailuresReturnNbtNull_quirk() {
        // Characterization baseline, not a correctness claim: every failure mode (missing key, out of range index,
        // non-numeric list index, descending into a scalar) yields the shared sentinel NBTUtilBC.NBT_NULL (an empty
        // CompoundTag), which is indistinguishable from a real empty compound when compared by value.
        CompoundTag root = makeSampleNbt();
        assertNbtNull(PATH_GSON.fromJson("[\"a\", \"missing\"]", NbtPath.class).get(root));
        assertNbtNull(PATH_GSON.fromJson("[\"list\", \"5\"]", NbtPath.class).get(root));
        assertNbtNull(PATH_GSON.fromJson("[\"list\", \"x\"]", NbtPath.class).get(root));
        assertNbtNull(PATH_GSON.fromJson("[\"str\", \"nope\"]", NbtPath.class).get(root));
        assertNbtNull(PATH_GSON.fromJson("[\"a\", \"i\", \"deep\"]", NbtPath.class).get(root));
    }

    private static void assertNbtNull(Tag tag) {
        Assert.assertTrue("Expected the NBTUtilBC.NBT_NULL sentinel", tag == NBTUtilBC.NBT_NULL);
    }

    @Test
    public void compareOperationsCompareBySerializedJson() {
        Tag a = StringTag.valueOf("x");
        Tag b = StringTag.valueOf("x");
        Tag c = StringTag.valueOf("y");
        Assert.assertTrue(EnumNbtCompareOperation.EQ.compare(a, b));
        Assert.assertFalse(EnumNbtCompareOperation.EQ.compare(a, c));
        Assert.assertFalse(EnumNbtCompareOperation.NQE.compare(a, b));
        Assert.assertTrue(EnumNbtCompareOperation.NQE.compare(a, c));
    }

    @Test
    public void compareOperationLookupByName() {
        Assert.assertEquals(EnumNbtCompareOperation.EQ, EnumNbtCompareOperation.byName("="));
        Assert.assertEquals(EnumNbtCompareOperation.NQE, EnumNbtCompareOperation.byName("!="));
        Assert.assertEquals("=", EnumNbtCompareOperation.EQ.name);
        try {
            EnumNbtCompareOperation.byName("<");
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void jsonSelectorStringShorthandMatchesAnyNbt() {
        // A bare JSON string becomes a selector that only checks the base, with no NBT conditions
        JsonSelector selector = RULE_GSON.fromJson("'stone'", JsonSelector.class);
        CompoundTag nbt = makeSampleNbt();
        Assert.assertTrue(selector.matches("stone"::equals, nbt));
        Assert.assertFalse(selector.matches("dirt"::equals, nbt));
        // the NBT part is ignored entirely
        Assert.assertTrue(selector.matches(base -> true, nbt));
    }

    @Test
    public void jsonSelectorFullFormChecksBaseAndEveryNbtCondition() {
        // Characterization baseline, not a correctness claim: the rule format spells the equality operation "="
        // (Java-style "==" throws "Compare operation not found")
        JsonSelector selector = RULE_GSON.fromJson(
            "{\"base\": \"woot\", \"nbt\": [{\"key\": [\"a\", \"d\"], \"operation\": \"=\", \"value\": 7}]}",
            JsonSelector.class);
        CompoundTag matching = makeSampleNbt();
        CompoundTag wrongValue = makeSampleNbt();
        wrongValue.getCompound("a").putDouble("d", 8.0);
        CompoundTag missing = new CompoundTag();
        Assert.assertTrue(selector.matches("woot"::equals, matching));
        Assert.assertFalse(selector.matches(base -> true, wrongValue));
        // a missing key yields NBT_NULL, which serializes as JSON null and so never equals the value
        Assert.assertFalse(selector.matches(base -> true, missing));
    }

    @Test
    public void jsonSelectorNumbersOnlyMatchDoubleNbtValues_quirk() {
        // Characterization baseline, not a correctness claim: a bare JSON number in a rule reaches Gson as a
        // LazilyParsedNumber, so it always deserializes to a DoubleTag ("7.0"). It therefore matches a double
        // value in the block NBT but NEVER an integer value ("7" != "7.0" in the serialized comparison).
        JsonSelector seven = RULE_GSON.fromJson(
            "{\"base\": \"woot\", \"nbt\": [{\"key\": [\"a\", \"i\"], \"operation\": \"=\", \"value\": 7}]}",
            JsonSelector.class);
        Assert.assertFalse(seven.matches(base -> true, makeSampleNbt()));

        JsonSelector sevenDouble = RULE_GSON.fromJson(
            "{\"base\": \"woot\", \"nbt\": [{\"key\": [\"a\", \"d\"], \"operation\": \"=\", \"value\": 7}]}",
            JsonSelector.class);
        Assert.assertTrue(sevenDouble.matches(base -> true, makeSampleNbt()));
    }

    @Test
    public void jsonRuleIsPlainGsonBean() {
        // JsonRule itself is a plain field bag; unknown members are ignored by Gson
        JsonRule rule = RULE_GSON.fromJson("{\"ignore\": true, \"unknown_member\": 1}", JsonRule.class);
        Assert.assertTrue(rule.ignore);
        Assert.assertFalse(rule.capture);
        Assert.assertNull(rule.selectors);
    }

    @Test
    public void requiredExtractorTypeLookup() {
        Assert.assertEquals(RequiredExtractor.EnumType.CONSTANT, RequiredExtractor.EnumType.byName("constant"));
        Assert.assertEquals("item", RequiredExtractor.EnumType.ITEM.getName());
        try {
            RequiredExtractor.EnumType.byName("nope");
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
}
