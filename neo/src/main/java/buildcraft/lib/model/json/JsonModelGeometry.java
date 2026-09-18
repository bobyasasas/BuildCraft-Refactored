/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.model.json;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;

/**
 * Minimal port of the legacy {@code buildcraft.lib.client.model.ModelUtil} face-baking math (M4.4): turns a cuboid
 * face (centre + radius, block space) into a {@link JsonQuad} with the legacy vertex order, UV assignment and
 * backface-culling-correct winding, plus the direction-mapping counterpart of the legacy
 * {@code MutableQuad#rotate} axis tables (used to keep {@link JsonQuad#face} in sync through rule rotations).
 */
public final class JsonModelGeometry {

    /** Legacy {@code ModelUtil#createFace(Direction, Vector3f, Vector3f, UvFaceData)}: {@code centre} and
     * {@code radius} are block-space {x, y, z} triples (radius = half the cuboid size); the UV rect is 0..1. */
    public static JsonQuad createFace(Direction face, float[] centre, float[] radius, JsonUvRect uvs) {
        float[] points = getPointsForFace(face, centre, radius);
        JsonQuad quad = new JsonQuad(face);
        if (shouldInvertForRender(face)) {
            // v0 = points[0] (minU, minV); v1 = points[1] (minU, maxV); v2 = points[2] (maxU, maxV);
            // v3 = points[3] (maxU, minV)
            setVertex(quad, 0, points, 0, uvs.minU, uvs.minV);
            setVertex(quad, 1, points, 1, uvs.minU, uvs.maxV);
            setVertex(quad, 2, points, 2, uvs.maxU, uvs.maxV);
            setVertex(quad, 3, points, 3, uvs.maxU, uvs.minV);
        } else {
            // v3 = points[0] (minU, minV); v2 = points[1] (minU, maxV); v1 = points[2] (maxU, maxV);
            // v0 = points[3] (maxU, minV)
            setVertex(quad, 3, points, 0, uvs.minU, uvs.minV);
            setVertex(quad, 2, points, 1, uvs.minU, uvs.maxV);
            setVertex(quad, 1, points, 2, uvs.maxU, uvs.maxV);
            setVertex(quad, 0, points, 3, uvs.maxU, uvs.minV);
        }
        return quad;
    }

    private static void setVertex(JsonQuad quad, int vertex, float[] points, int point, float u, float v) {
        JsonQuad.Vertex out = quad.vertices[vertex];
        out.x = points[point * 3];
        out.y = points[point * 3 + 1];
        out.z = points[point * 3 + 2];
        out.u = u;
        out.v = v;
    }

    /** Legacy {@code ModelUtil#getPointsForFace}: the four corners of the face at {@code centre + step * radius},
     * spanning the cuboid's full face. Returns a flat {x, y, z} &times; 4 array. */
    public static float[] getPointsForFace(Direction face, float[] centre, float[] radius) {
        float[] centreOfFace = { centre[0], centre[1], centre[2] };
        float[] faceAdd = { face.getStepX() * radius[0], face.getStepY() * radius[1], face.getStepZ() * radius[2] };
        centreOfFace[0] += faceAdd[0];
        centreOfFace[1] += faceAdd[1];
        centreOfFace[2] += faceAdd[2];
        float[] faceRadius = { radius[0], radius[1], radius[2] };
        if (face.getAxisDirection() == AxisDirection.POSITIVE) {
            faceRadius[0] -= faceAdd[0];
            faceRadius[1] -= faceAdd[1];
            faceRadius[2] -= faceAdd[2];
        } else {
            faceRadius[0] += faceAdd[0];
            faceRadius[1] += faceAdd[1];
            faceRadius[2] += faceAdd[2];
        }
        return getPoints(centreOfFace, faceRadius);
    }

    /** Legacy {@code ModelUtil#getPoints}: the four corners in the (u, v) order (false, false), (false, true),
     * (true, true), (true, false). */
    private static float[] getPoints(float[] centreFace, float[] faceRadius) {
        float[] array = new float[12];
        float[] point;
        point = addOrNegate(faceRadius, false, false);
        array[0] = centreFace[0] + point[0];
        array[1] = centreFace[1] + point[1];
        array[2] = centreFace[2] + point[2];
        point = addOrNegate(faceRadius, false, true);
        array[3] = centreFace[0] + point[0];
        array[4] = centreFace[1] + point[1];
        array[5] = centreFace[2] + point[2];
        point = addOrNegate(faceRadius, true, true);
        array[6] = centreFace[0] + point[0];
        array[7] = centreFace[1] + point[1];
        array[8] = centreFace[2] + point[2];
        point = addOrNegate(faceRadius, true, false);
        array[9] = centreFace[0] + point[0];
        array[10] = centreFace[1] + point[1];
        array[11] = centreFace[2] + point[2];
        return array;
    }

    /** Legacy {@code ModelUtil#addOrNegate}. */
    private static float[] addOrNegate(float[] coord, boolean u, boolean v) {
        boolean zisv = coord[0] != 0 && coord[1] == 0;
        float x = coord[0] * (u ? 1 : -1);
        float y = coord[1] * (v ? -1 : 1);
        float z = coord[2] * (zisv ? (v ? -1 : 1) : (u ? 1 : -1));
        return new float[] { x, y, z };
    }

    /** Legacy {@code ModelUtil#shouldInvertForRender}: true when the legacy baker swapped the vertex/UV assignment
     * order for this face (negative axes, un-flipped for Z). */
    public static boolean shouldInvertForRender(Direction face) {
        boolean flip = face.getAxisDirection() == AxisDirection.NEGATIVE;
        if (face.getAxis() == Axis.Z) {
            flip = !flip;
        }
        return flip;
    }

    /**
     * Maps {@code face} through the same rotation that legacy {@code MutableQuad#rotate(from, to, ...)} applied to
     * every vertex normal, so rotated quads keep a correct outward normal. Implemented by pushing the face's unit
     * vector through the identical axis-case table and matching the result against the six unit vectors (no reliance
     * on direction-construction APIs).
     */
    public static Direction rotateDirection(Direction from, Direction to, Direction face) {
        int[] v = { face.getStepX(), face.getStepY(), face.getStepZ() };
        // @formatter:off
        switch (from.getAxis()) {
            case X: {
                int mult = from.getStepX();
                switch (to.getAxis()) {
                    case X: rotateY_180(v); break;
                    case Y: rotateZ_90(v, mult * to.getStepY()); break;
                    case Z: rotateY_90(v, mult * to.getStepZ()); break;
                }
                break;
            }
            case Y: {
                int mult = from.getStepY();
                switch (to.getAxis()) {
                    case X: rotateZ_90(v, -mult * to.getStepX()); break;
                    case Y: rotateZ_180(v); break;
                    case Z: rotateX_90(v, mult * to.getStepZ()); break;
                }
                break;
            }
            case Z: {
                int mult = -from.getStepZ();
                switch (to.getAxis()) {
                    case X: rotateY_90(v, mult * to.getStepX()); break;
                    case Y: rotateX_90(v, mult * to.getStepY()); break;
                    case Z: rotateY_180(v); break;
                }
                break;
            }
        }
        // @formatter:on
        for (Direction d : Direction.values()) {
            if (d.getStepX() == v[0] && d.getStepY() == v[1] && d.getStepZ() == v[2]) {
                return d;
            }
        }
        throw new IllegalStateException(//
            "Rotated the face " + face + " into a non-unit vector (" + v[0] + ", " + v[1] + ", " + v[2] + ")");
    }

    /** Legacy {@code MutableVertex#rotateX_90(scale)} on a unit-vector triple. */
    private static void rotateX_90(int[] v, int scale) {
        int ym = scale;
        int zm = -ym;
        int t = v[1] * ym;
        v[1] = v[2] * zm;
        v[2] = t;
    }

    /** Legacy {@code MutableVertex#rotateY_90(scale)} on a unit-vector triple. */
    private static void rotateY_90(int[] v, int scale) {
        int xm = scale;
        int zm = -xm;
        int t = v[0] * xm;
        v[0] = v[2] * zm;
        v[2] = t;
    }

    /** Legacy {@code MutableVertex#rotateZ_90(scale)} on a unit-vector triple. */
    private static void rotateZ_90(int[] v, int scale) {
        int xm = scale;
        int ym = -xm;
        int t = v[0] * xm;
        v[0] = v[1] * ym;
        v[1] = t;
    }

    private static void rotateY_180(int[] v) {
        v[0] = -v[0];
        v[2] = -v[2];
    }

    private static void rotateZ_180(int[] v) {
        v[0] = -v[0];
        v[1] = -v[1];
    }

    private JsonModelGeometry() {
    }
}
