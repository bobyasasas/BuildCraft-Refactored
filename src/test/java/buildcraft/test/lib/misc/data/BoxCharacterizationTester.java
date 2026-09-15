/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.test.lib.misc.data;

import org.junit.Assert;
import org.junit.Test;

import buildcraft.lib.misc.data.Box;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

/**
 * Characterization tests (M0.5 baseline) for {@link Box}, the builder/filler area data structure, on the 1.20.1
 * baseline. Surprising expectations pin down CURRENT behaviour on purpose - they are regression guards for the
 * NeoForge migration, NOT statements about what the behaviour ought to be.
 */
public class BoxCharacterizationTester {

    @Test
    public void constructorNormalizesSwappedCorners() {
        Box box = new Box(new BlockPos(5, 6, 7), new BlockPos(1, 2, 3));
        Assert.assertEquals(new BlockPos(1, 2, 3), box.min());
        Assert.assertEquals(new BlockPos(5, 6, 7), box.max());
        Assert.assertEquals(new BlockPos(5, 5, 5), box.size());
    }

    @Test
    public void settersReNormalize() {
        Box box = new Box(new BlockPos(0, 0, 0), new BlockPos(2, 2, 2));
        // setting a min "beyond" the current max pushes the max out to meet it
        box.setMin(new BlockPos(3, 3, 3));
        Assert.assertEquals(new BlockPos(3, 3, 3), box.min());
        Assert.assertEquals(new BlockPos(3, 3, 3), box.max());
        // setMax replaces the max outright and pulls the min back to meet it
        box.setMax(new BlockPos(1, 1, 1));
        Assert.assertEquals(new BlockPos(1, 1, 1), box.min());
        Assert.assertEquals(new BlockPos(1, 1, 1), box.max());
    }

    @Test
    public void extendToEncompassBothGrowsTheBox() {
        Box box = new Box(new BlockPos(2, 2, 2), new BlockPos(4, 4, 4));
        box.extendToEncompassBoth(new BlockPos(0, 3, 0), new BlockPos(1, 5, 1));
        Assert.assertEquals(new BlockPos(0, 2, 0), box.min());
        Assert.assertEquals(new BlockPos(4, 5, 4), box.max());
    }

    @Test
    public void containsUsesHalfOpenBoundsOnTheMaxSide() {
        Box box = new Box(new BlockPos(0, 0, 0), new BlockPos(2, 2, 2));
        Assert.assertTrue(box.contains(new BlockPos(0, 0, 0)));
        // the max corner itself is still inside (AABB is max + 1)
        Assert.assertTrue(box.contains(new BlockPos(2, 2, 2)));
        Assert.assertFalse(box.contains(new BlockPos(3, 2, 2)));
        Assert.assertFalse(box.contains(new BlockPos(-1, 0, 0)));
        // Vec3 flavour: the open max edge excludes the exact far plane
        Assert.assertTrue(box.contains(new Vec3(2.0, 0.0, 0.0)));
        Assert.assertFalse(box.contains(new Vec3(3.0, 0.0, 0.0)));
    }

    @Test
    public void nbtRoundTrip() {
        Box box = new Box(new BlockPos(1, 2, 3), new BlockPos(4, 5, 6));
        CompoundTag nbt = box.writeToNBT();
        Box read = new Box();
        read.initialize(nbt);
        Assert.assertEquals(box.min(), read.min());
        Assert.assertEquals(box.max(), read.max());
    }

    @Test
    public void uninitialisedBoxReportsZeroSize() {
        Box box = new Box();
        Assert.assertFalse(box.isInitialized());
        // Characterization baseline: an empty box's size is ZERO rather than an error
        Assert.assertEquals(BlockPos.ZERO, box.size());
    }
}
