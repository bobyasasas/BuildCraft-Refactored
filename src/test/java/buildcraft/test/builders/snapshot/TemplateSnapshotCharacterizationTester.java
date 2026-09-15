/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.test.builders.snapshot;

import java.util.BitSet;

import org.junit.Assert;
import org.junit.Test;

import buildcraft.api.core.InvalidInputDataException;
import buildcraft.api.enums.EnumSnapshotType;
import buildcraft.builders.snapshot.Blueprint;
import buildcraft.builders.snapshot.Snapshot;
import buildcraft.builders.snapshot.Template;
import buildcraft.lib.misc.HashUtil;
import buildcraft.test.VanillaSetupBaseTester;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * Characterization tests (M0.5 baseline) for the pure data-structure part of {@link Template} / {@link Snapshot} (NBT
 * round-trip, key hashing, invert/copy) on the 1.20.1 baseline. Surprising expectations pin down CURRENT behaviour on
 * purpose - they are regression guards for the NeoForge migration, NOT statements about what the behaviour ought to be.
 *
 * Coverage boundary: the block/entity palette of {@link Blueprint} and the schematic managers need live registries or
 * a Level, so only the registry-independent parts are tested here.
 */
public class TemplateSnapshotCharacterizationTester extends VanillaSetupBaseTester {

    private static Template makeTemplate(int dataBits) {
        Template template = new Template();
        template.size = new BlockPos(2, 2, 2);
        template.facing = Direction.NORTH;
        template.offset = BlockPos.ZERO;
        template.data = new BitSet(Snapshot.getDataSize(template.size));
        for (int i = 0; i < dataBits; i++) {
            template.data.set(i);
        }
        return template;
    }

    @Test
    public void nbtRoundTripPreservesEverything() throws InvalidInputDataException {
        Template template = makeTemplate(3);
        CompoundTag nbt = Snapshot.writeToNBT(template);
        Assert.assertTrue(nbt.contains("type"));

        Snapshot read = Snapshot.readFromNBT(nbt);
        Assert.assertTrue(read instanceof Template);
        Assert.assertEquals(template.size, read.size);
        Assert.assertEquals(template.offset, read.offset);
        Assert.assertEquals(template.data, ((Template) read).data);
    }

    @Test
    public void oversizedDataIsRejected() {
        Template template = makeTemplate(0);
        // data claims 9 bits but the 2x2x2 size only allows 8
        template.data = new BitSet(9);
        template.data.set(8);
        CompoundTag nbt = Snapshot.writeToNBT(template);
        try {
            Snapshot.readFromNBT(nbt);
            Assert.fail("Expected InvalidInputDataException");
        } catch (InvalidInputDataException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("Serialized data has length of 9"));
        }
    }

    @Test
    public void snapshotFactoryReturnsTheRightTypes() {
        Assert.assertTrue(Snapshot.create(EnumSnapshotType.TEMPLATE) instanceof Template);
        Assert.assertTrue(Snapshot.create(EnumSnapshotType.BLUEPRINT) instanceof Blueprint);
        Assert.assertEquals(EnumSnapshotType.TEMPLATE, new Template().getType());
        Assert.assertEquals(EnumSnapshotType.BLUEPRINT, new Blueprint().getType());
    }

    @Test
    public void invertFlipsEveryBit() {
        Template template = makeTemplate(3); // bits 0..2 set out of 8
        template.invert();
        BitSet expected = new BitSet(8);
        expected.set(3, 8);
        Assert.assertEquals(expected, template.data);
    }

    @Test
    public void computeKeyIsDeterministicAndContentSensitive() {
        Template a1 = makeTemplate(3);
        Template a2 = makeTemplate(3);
        Template b = makeTemplate(1);
        a1.computeKey();
        a2.computeKey();
        b.computeKey();
        Assert.assertEquals(a1.key, a2.key);
        Assert.assertEquals(a1.key.hashCode(), a2.key.hashCode());
        Assert.assertNotEquals(a1.key, b.key);
        // the hash is a SHA-256 digest (32 bytes), serialized as 64 lowercase hex chars
        Assert.assertEquals(32, a1.key.hash.length);
        Assert.assertEquals(64, a1.key.toString().length());
        Assert.assertEquals(HashUtil.convertHashToString(a1.key.hash), a1.key.toString());
    }

    @Test
    public void keyIgnoresItsOwnPreviousValue() {
        // computeKey strips the "key" member before hashing, so re-computing on a copy of a keyed template keeps
        // the same hash instead of chaining
        Template template = makeTemplate(3);
        template.computeKey();
        Template copy = template.copy();
        copy.computeKey();
        Assert.assertEquals(template.key, copy.key);
    }

    @Test
    public void keyNbtRoundTrip() {
        Template template = makeTemplate(3);
        template.computeKey();
        Snapshot.Key key = new Snapshot.Key(template.key.serializeNBT());
        Assert.assertEquals(template.key, key);
        Assert.assertNull(new Snapshot.Key(new CompoundTag()).header);
    }

    @Test
    public void copyIsIndependent() {
        Template template = makeTemplate(3);
        template.computeKey();
        Template copy = template.copy();
        copy.data.set(7);
        Assert.assertFalse(template.data.get(7));
        // the copy keeps the same geometry and hash
        Assert.assertEquals(template.size, copy.size);
        Assert.assertEquals(template.key, copy.key);
    }

    @Test
    public void filledTemplateBoundsChecks() {
        Template template = makeTemplate(0);
        Template.FilledTemplate filled = template.getFilledTemplate();
        filled.set(1, 1, 1, true);
        Assert.assertTrue(filled.get(1, 1, 1));
        Assert.assertEquals(new BlockPos(1, 1, 1), filled.getMax());
        try {
            filled.get(2, 0, 0);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("pos"));
        }
        // a Template serialized without a key still carries the "key" compound member for the round trip
        CompoundTag nbt = template.serializeNBT();
        Assert.assertEquals(Tag.TAG_COMPOUND, nbt.get("key").getId());
    }
}
