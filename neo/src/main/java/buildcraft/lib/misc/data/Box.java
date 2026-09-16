/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.misc.data;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import buildcraft.lib.misc.VecUtil;

/**
 * Minimal M2.8 port of legacy {@code buildcraft.lib.misc.data.Box} (mutable integer area, {@code min} inclusive,
 * {@code max} inclusive): only the members the oil field worldgen
 * ({@code buildcraft.energy.generation.structure}) needs. The full legacy box (laser rendering, area providers,
 * NBT/network IO) migrates with its consumers in later Phase 2/3 tasks — extend this class, do not duplicate it.
 */
public class Box {

    private BlockPos min, max;

    public Box() {
        this.min = null;
        this.max = null;
    }

    public Box(BlockPos min, BlockPos max) {
        this.min = VecUtil.min(min, max);
        this.max = VecUtil.max(min, max);
    }

    public Box(BoundingBox boundingBox) {
        this(
            new BlockPos(boundingBox.minX(), boundingBox.minY(), boundingBox.minZ()),
            new BlockPos(boundingBox.maxX(), boundingBox.maxY(), boundingBox.maxZ())
        );
    }

    public boolean isInitialized() {
        return this.min != null && this.max != null;
    }

    public BlockPos min() {
        return this.min;
    }

    public BlockPos max() {
        return this.max;
    }

    /** @return The {@link AABB} equivalent, with {@link #max} made exclusive as {@link AABB} requires. */
    public AABB getBoundingBox() {
        BlockPos outerMax = this.max.offset(VecUtil.POS_ONE);
        return new AABB(this.min.getX(), this.min.getY(), this.min.getZ(), outerMax.getX(), outerMax.getY(), outerMax.getZ());
    }

    public BoundingBox getBB() {
        BlockPos outerMax = this.max.offset(VecUtil.POS_ONE);
        return new BoundingBox(this.min.getX(), this.min.getY(), this.min.getZ(), outerMax.getX(), outerMax.getY(), outerMax.getZ());
    }

    public boolean contains(Vec3 p) {
        AABB bb = getBoundingBox();
        if (p.x < bb.minX || p.x >= bb.maxX) return false;
        if (p.y < bb.minY || p.y >= bb.maxY) return false;
        if (p.z < bb.minZ || p.z >= bb.maxZ) return false;
        return true;
    }

    public boolean contains(BlockPos i) {
        return contains(new Vec3(i.getX(), i.getY(), i.getZ()));
    }

    public boolean doesIntersectWith(Box box) {
        if (isInitialized() && box.isInitialized()) {
            return min.getX() <= box.max.getX() && max.getX() >= box.min.getX()//
                && min.getY() <= box.max.getY() && max.getY() >= box.min.getY()//
                && min.getZ() <= box.max.getZ() && max.getZ() >= box.min.getZ();
        }
        return false;
    }

    public Box getIntersect(Box box) {
        if (doesIntersectWith(box)) {
            BlockPos min2 = VecUtil.max(min, box.min);
            BlockPos max2 = VecUtil.min(max, box.max);
            return new Box(min2, max2);
        }
        return null;
    }
}
