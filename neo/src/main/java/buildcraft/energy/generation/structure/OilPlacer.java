/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.energy.generation.structure;

import buildcraft.lib.misc.data.Box;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;

/**
 * M2.8 port of legacy {@code buildcraft.energy.generation.structure.OilPlacer} (1.20.1): runs every oil structure
 * part for the chunk currently generating (clipped to the chunk's {@link BoundingBox}) and finally feeds the total
 * oil block count to the spring (legacy {@code TileSpringOil#totalSources}).
 */
public final class OilPlacer {
    private final LevelAccessor level;
    private final List<OilGenStructurePart> structurePieces;
    private final Box box;

    public OilPlacer(WorldGenLevel level, List<OilGenStructurePart> structurePieces, BoundingBox bounds) {
        this.level = level;
        this.structurePieces = structurePieces;
        this.box = new Box(bounds);
    }

    public void place() {
        LevelAccessor world = this.level;
        OilGenStructurePart.Spring spring = null;
        for (OilGenStructurePart struct : structurePieces) {
            struct.generate(world, box);
            if (struct instanceof OilGenStructurePart.Spring) {
                spring = (OilGenStructurePart.Spring) struct;
            }
        }
        if (spring != null && box.contains(spring.pos)) {
            int count = 0;
            for (OilGenStructurePart struct : structurePieces) {
                count += struct.countOilBlocks();
            }
            spring.generate(world, count);
        }
    }
}
