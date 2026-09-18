/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.client.render.laser;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.phys.Vec3;

/**
 * Builds the laser edges around an axis-aligned box (M4.5 port of legacy
 * {@code buildcraft.lib.client.render.laser.LaserBoxRenderer}): with {@code center} the edge lines run through the
 * box's block centres and an axis only gets its lines when the box spans more than one block along it &mdash; exactly
 * the visibility rules {@code LaserBoxRenderer#makeLaserBox} applies to the marker-volume and quarry frame boxes.
 */
public final class BcLaserBoxRenderer {

    private static final double RENDER_SCALE = 1 / 16.05;

    /**
     * Appends one {@link BcLaserData} per box edge line. The caller bakes them through {@link BcLaserBaker}.
     *
     * @param min the box's minimum block position (inclusive)
     * @param max the box's maximum block position (inclusive)
     * @param center true to run the lines through the block centres (markers, quarry frame boxes); false to run them
     *            along the box's outer corner
     */
    public static List<BcLaserData> makeLaserBox(BlockPos min, BlockPos max, BcLaserType type, boolean center) {
        boolean renderX = center ? boxSize(min, max, Axis.X) > 1 : true;
        boolean renderY = center ? boxSize(min, max, Axis.Y) > 1 : true;
        boolean renderZ = center ? boxSize(min, max, Axis.Z) > 1 : true;

        Vec3 vecMin = new Vec3(min.getX(), min.getY(), min.getZ()).add(center ? new Vec3(0.5, 0.5, 0.5) : Vec3.ZERO);
        Vec3 vecMax = new Vec3(max.getX(), max.getY(), max.getZ())
            .add(center ? new Vec3(0.5, 0.5, 0.5) : new Vec3(1, 1, 1));

        // [x][y][z] with 0 = min side, 1 = max side (legacy vecs table)
        Vec3[][][] vecs = new Vec3[2][2][2];
        for (int xi = 0; xi < 2; xi++) {
            for (int yi = 0; yi < 2; yi++) {
                for (int zi = 0; zi < 2; zi++) {
                    vecs[xi][yi][zi] = new Vec3(
                        xi == 0 ? vecMin.x : vecMax.x,
                        yi == 0 ? vecMin.y : vecMax.y,
                        zi == 0 ? vecMin.z : vecMax.z);
                }
            }
        }

        List<BcLaserData> datas = new ArrayList<>();

        if (renderX) {
            datas.add(makeLaser(type, vecs[0][0][0], vecs[1][0][0], Axis.X));
            if (renderY) {
                datas.add(makeLaser(type, vecs[0][1][0], vecs[1][1][0], Axis.X));
                if (renderZ) {
                    datas.add(makeLaser(type, vecs[0][1][1], vecs[1][1][1], Axis.X));
                }
            }
            if (renderZ) {
                datas.add(makeLaser(type, vecs[0][0][1], vecs[1][0][1], Axis.X));
            }
        }
        if (renderY) {
            datas.add(makeLaser(type, vecs[0][0][0], vecs[0][1][0], Axis.Y));
            if (renderX) {
                datas.add(makeLaser(type, vecs[1][0][0], vecs[1][1][0], Axis.Y));
                if (renderZ) {
                    datas.add(makeLaser(type, vecs[1][0][1], vecs[1][1][1], Axis.Y));
                }
            }
            if (renderZ) {
                datas.add(makeLaser(type, vecs[0][0][1], vecs[0][1][1], Axis.Y));
            }
        }
        if (renderZ) {
            datas.add(makeLaser(type, vecs[0][0][0], vecs[0][0][1], Axis.Z));
            if (renderX) {
                datas.add(makeLaser(type, vecs[1][0][0], vecs[1][0][1], Axis.Z));
                if (renderY) {
                    datas.add(makeLaser(type, vecs[1][1][0], vecs[1][1][1], Axis.Z));
                }
            }
            if (renderY) {
                datas.add(makeLaser(type, vecs[0][1][0], vecs[0][1][1], Axis.Z));
            }
        }
        return datas;
    }

    /** Legacy {@code Box#size()}: {@code max - min + 1}, so a single-block span measures 1 and renders no lines. */
    private static int boxSize(BlockPos min, BlockPos max, Axis axis) {
        return switch (axis) {
            case X -> max.getX() - min.getX() + 1;
            case Y -> max.getY() - min.getY() + 1;
            case Z -> max.getZ() - min.getZ() + 1;
        };
    }

    /** Legacy {@code LaserBoxRenderer#makeLaser}: both ends pushed 1/16 outward along the edge's axis. */
    private static BcLaserData makeLaser(BcLaserType type, Vec3 min, Vec3 max, Axis axis) {
        Direction faceForMin = facing(axis, true);
        Direction faceForMax = facing(axis, false);
        Vec3 one = min.add(new Vec3(
            faceForMin.getStepX(), faceForMin.getStepY(), faceForMin.getStepZ()).scale(1 / 16.0));
        Vec3 two = max.add(new Vec3(
            faceForMax.getStepX(), faceForMax.getStepY(), faceForMax.getStepZ()).scale(1 / 16.0));
        return new BcLaserData(type, one, two, RENDER_SCALE);
    }

    private static Direction facing(Axis axis, boolean negative) {
        return switch (axis) {
            case X -> negative ? Direction.WEST : Direction.EAST;
            case Y -> negative ? Direction.DOWN : Direction.UP;
            case Z -> negative ? Direction.NORTH : Direction.SOUTH;
        };
    }

    private BcLaserBoxRenderer() {
    }
}
