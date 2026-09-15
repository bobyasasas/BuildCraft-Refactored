/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.test.builders.snapshot;

import org.junit.Assert;
import org.junit.Test;

import buildcraft.builders.snapshot.Snapshot;
import buildcraft.builders.snapshot.Template;
import net.minecraft.core.BlockPos;

/**
 * Characterization tests (M0.5 baseline) for the index math of {@link Snapshot} on the 1.20.1 baseline. The index
 * layout is pinned with concrete values on purpose - it is a regression guard for the NeoForge migration, NOT a
 * statement about what the layout ought to be.
 */
public class SnapshotIndexCharacterizationTester {
    private static final BlockPos SIZE = new BlockPos(6, 4, 8);

    @Test
    public void posToIndexFormula() {
        // index = ((z * sizeY) + y) * sizeX + x  =>  ((1 * 4) + 2) * 6 + 3 = 39
        Assert.assertEquals(39, Snapshot.posToIndex(SIZE, new BlockPos(3, 2, 1)));
        Assert.assertEquals(0, Snapshot.posToIndex(SIZE, BlockPos.ZERO));
        // last valid position maps to the last index (product - 1)
        Assert.assertEquals(6 * 4 * 8 - 1, Snapshot.posToIndex(SIZE, new BlockPos(5, 3, 7)));
    }

    @Test
    public void indexToPosFormula() {
        Assert.assertEquals(new BlockPos(3, 2, 1), Snapshot.indexToPos(SIZE, 39));
        Assert.assertEquals(BlockPos.ZERO, Snapshot.indexToPos(SIZE, 0));
        Assert.assertEquals(new BlockPos(5, 3, 7), Snapshot.indexToPos(SIZE, 6 * 4 * 8 - 1));
    }

    @Test
    public void dataSizeIsTheProductOfTheDimensions() {
        Assert.assertEquals(192, Snapshot.getDataSize(SIZE));
        Assert.assertEquals(1, Snapshot.getDataSize(new BlockPos(1, 1, 1)));
    }

    @Test
    public void roundTripOnAsymmetricSize() {
        BlockPos size = new BlockPos(3, 5, 7);
        for (int z = 0; z < size.getZ(); z++) {
            for (int y = 0; y < size.getY(); y++) {
                for (int x = 0; x < size.getX(); x++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    Assert.assertEquals(pos, Snapshot.indexToPos(size, Snapshot.posToIndex(size, pos)));
                }
            }
        }
    }

    @Test
    public void instanceHelpersMatchStaticHelpers() {
        Template template = new Template();
        template.size = SIZE;
        Assert.assertEquals(Snapshot.posToIndex(SIZE, new BlockPos(3, 2, 1)), template.posToIndex(new BlockPos(3, 2, 1)));
        Assert.assertEquals(Snapshot.indexToPos(SIZE, 39), template.indexToPos(39));
        Assert.assertEquals(Snapshot.getDataSize(SIZE), template.getDataSize());
    }

    @Test
    public void indexOrderIsXFastestThenYThenZ() {
        // x varies fastest, then y, then z (z-major "pages" of X*Y layers)
        Assert.assertEquals(1, Snapshot.posToIndex(new BlockPos(2, 1, 2), new BlockPos(1, 0, 0)));
        Assert.assertEquals(2, Snapshot.posToIndex(new BlockPos(2, 1, 2), new BlockPos(0, 0, 1)));
        Assert.assertEquals(3, Snapshot.posToIndex(new BlockPos(2, 1, 2), new BlockPos(1, 0, 1)));
    }
}
