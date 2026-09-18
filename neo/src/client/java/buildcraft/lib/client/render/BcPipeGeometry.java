/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.client.render;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;

/**
 * The pipe body geometry, a faithful port of the baseline
 * {@code buildcraft.transport.client.model.PipeBaseModelGenStandard} static tables onto {@link BcQuad} (the M4.6 task:
 * "管体 + 连接：按 blockstate / BE 连接状态渲染管体（中心 + 各方向导管，贴图取基线定义）"). Legacy baked these quads once per
 * connection mask into a model cache; here they are built per extract (the chunk-baked optimisation is explicitly
 * post-M4.6), which keeps the renderer state-driven and correct.
 *
 * <p>Geometry, identical to legacy:
 * <ul>
 * <li><b>Centre</b>: a 0.25..0.75 cube; every face UVs to the sprite's central circle ({@code 4/16..12/16}). Faces on
 * connected sides are omitted (the arm replaces them).</li>
 * <li><b>Arms</b>: one per connected side, centred at {@code 0.5 + step * 0.375} with radius {@code 0.125} along the
 * axis and {@code 0.25} across; the four side faces UV to the sprite's four edge bands
 * ({@code 4,0,12,4 / 4,12,12,16 / 0,4,4,12 / 12,4,16,12}) rotated per side exactly like legacy's {@code uvsRot} table;
 * the end face (flush with the neighbour) is not drawn.</li>
 * <li><b>Inside copies</b>: every face gets a reversed, darker twin (legacy {@code dupDarker}), so looking into an open
 * pipe end shows the dark interior instead of the skybox.</li>
 * </ul>
 *
 * <p>UVs stay raw (sprite-relative 0..1); the caller maps them onto the pipe's sprite with
 * {@link BcQuad#mapUv(net.minecraft.client.renderer.texture.TextureAtlasSprite)} at submit time. Vanilla's per-face
 * diffuse shading is baked into the vertex colours ({@code BcQuad#emit} writes colours as-is, the custom-geometry
 * render types don't re-derive face shading).
 */
public final class BcPipeGeometry {

    /** The pipe centre's half extent ({@code PipeBaseModelGenStandard} centre bounds 0.25..0.75). */
    private static final float CENTER = 0.25f;
    /** Arm centre offset from the block centre along the arm axis ({@code step * 0.375}). */
    private static final float ARM_OFFSET = 0.375f;
    /** Arm half-extent along its own axis (legacy radius.x/y/z = 0.125 on the axis). */
    private static final float ARM_RADIUS_AXIS = 0.125f;
    /** Arm half-extent across the axis (legacy radius = 0.25). */
    private static final float ARM_RADIUS_CROSS = 0.25f;

    /** Vanilla per-face brightness, baked into vertex colours (legacy {@code MutableQuad#setDiffuse}). */
    private static final float[] FACE_DIFFUSE = { 0.5f, 1.0f, 0.8f, 0.8f, 0.6f, 0.6f };

    /** Colour multiplier on the reversed inside copies (legacy {@code dupDarker}'s option, default 0.5). */
    private static final float INSIDE_DARKEN = 0.5f;

    /**
     * Legacy {@code uvsRot}: how many times each arm face's texture is rotated "up" per arm side, indexed
     * {@code [side.ordinal()][armFaceIndex]}.
     */
    private static final int[][] UVS_ROT = {
        { 2, 0, 3, 3 }, // DOWN
        { 0, 2, 1, 1 }, // UP
        { 2, 0, 0, 2 }, // NORTH
        { 0, 2, 2, 0 }, // SOUTH
        { 3, 3, 0, 2 }, // WEST
        { 1, 1, 2, 0 }, // EAST
    };

    /** The four sprite edge bands the arm side faces sample ({@code UvFaceData.from16} values). */
    private static final float[][] ARM_UVS = {
        { 4 / 16f, 0 / 16f, 12 / 16f, 4 / 16f },
        { 4 / 16f, 12 / 16f, 12 / 16f, 16 / 16f },
        { 0 / 16f, 4 / 16f, 4 / 16f, 12 / 16f },
        { 12 / 16f, 4 / 16f, 16 / 16f, 12 / 16f },
    };

    /**
     * Builds the pipe body: the centre cube (minus connected faces) plus one arm per connected side, each face with
     * its darker inside twin. Empty connection set = the standalone pipe look.
     */
    public static List<BcQuad> pipeBody(EnumSet<Direction> connections) {
        List<BcQuad> quads = new ArrayList<>(12);
        for (Direction face : Direction.values()) {
            if (connections.contains(face)) {
                addArm(quads, face);
            } else {
                addCentreFace(quads, face);
            }
        }
        return quads;
    }

    /** One centre face + inside copy: full 0.25..0.75 cube face, UVs on the sprite's central circle. */
    private static void addCentreFace(List<BcQuad> quads, Direction face) {
        BcQuad quad = createFace(face, 0.5f, 0.5f, 0.5f, CENTER, CENTER, CENTER,
            4 / 16f, 4 / 16f, 12 / 16f, 12 / 16f);
        quads.add(quad);
        quads.add(insideCopy(quad));
    }

    /** One connection arm + inside copies ({@code QUADS[1][side]}): centre-offset box, four side faces, no end face. */
    private static void addArm(List<BcQuad> quads, Direction side) {
        float cx = 0.5f + side.getStepX() * ARM_OFFSET;
        float cy = 0.5f + side.getStepY() * ARM_OFFSET;
        float cz = 0.5f + side.getStepZ() * ARM_OFFSET;
        float rx = side.getAxis() == Axis.X ? ARM_RADIUS_AXIS : ARM_RADIUS_CROSS;
        float ry = side.getAxis() == Axis.Y ? ARM_RADIUS_AXIS : ARM_RADIUS_CROSS;
        float rz = side.getAxis() == Axis.Z ? ARM_RADIUS_AXIS : ARM_RADIUS_CROSS;

        int i = 0;
        for (Direction face : Direction.values()) {
            if (face.getAxis() == side.getAxis()) {
                continue;
            }
            float[] uv = ARM_UVS[i];
            BcQuad quad = createFace(face, cx, cy, cz, rx, ry, rz, uv[0], uv[1], uv[2], uv[3]);
            quad = rotateTextureUp(quad, UVS_ROT[side.ordinal()][i]);
            quads.add(quad);
            quads.add(insideCopy(quad));
            i++;
        }
    }

    /**
     * The legacy {@code ModelUtil#createFace(face, center, radius, uvs)}: computes the four face points
     * ({@code getPointsForFace}/{@code getPoints}/{@code addOrNegate}) and assigns UVs in legacy's
     * {@code shouldInvertForRender} order, keeping the exact winding the baseline quads had.
     */
    private static BcQuad createFace(Direction face, float cx, float cy, float cz, float rx, float ry, float rz,
                                     float minU, float minV, float maxU, float maxV) {
        int stepX = face.getStepX(), stepY = face.getStepY(), stepZ = face.getStepZ();
        float fax = stepX * rx, fay = stepY * ry, faz = stepZ * rz;
        // centreOfFace = center + step ∘ radius; faceRadius = radius ∓ faceAdd by axis direction
        float fcx = cx + fax, fcy = cy + fay, fcz = cz + faz;
        boolean positive = face.getAxisDirection() == AxisDirection.POSITIVE;
        float frx = positive ? rx - fax : rx + fax;
        float fry = positive ? ry - fay : ry + fay;
        float frz = positive ? rz - faz : rz + faz;
        boolean zisv = frx != 0.0f && fry == 0.0f;

        // p0..p3 = centerOfFace + addOrNegate(faceRadius, u, v) for (f,f) (f,t) (t,t) (t,f)
        float p0x = fcx + -frx, p0y = fcy + fry, p0z = fcz + (zisv ? frz : -frz);
        float p1x = fcx + -frx, p1y = fcy + -fry, p1z = fcz + (zisv ? -frz : -frz);
        float p2x = fcx + frx, p2y = fcy + -fry, p2z = fcz + (zisv ? -frz : frz);
        float p3x = fcx + frx, p3y = fcy + fry, p3z = fcz + (zisv ? frz : -frz);

        float diffuse = FACE_DIFFUSE[face.ordinal()];
        int argb = shadeArgb(diffuse);
        BcQuad quad;
        if (shouldInvertForRender(face)) {
            // legacy branch 1: v0..v3 = p0(minU,minV), p1(minU,maxV), p2(maxU,maxV), p3(maxU,minV)
            quad = new BcQuad(face, true, -1,
                vertex(p0x, p0y, p0z, minU, minV, argb),
                vertex(p1x, p1y, p1z, minU, maxV, argb),
                vertex(p2x, p2y, p2z, maxU, maxV, argb),
                vertex(p3x, p3y, p3z, maxU, minV, argb));
        } else {
            // legacy branch 2: v3..v0 = p0(minU,minV), p1(minU,maxV), p2(maxU,maxV), p3(maxU,minV)
            quad = new BcQuad(face, true, -1,
                vertex(p3x, p3y, p3z, maxU, minV, argb),
                vertex(p2x, p2y, p2z, maxU, maxV, argb),
                vertex(p1x, p1y, p1z, minU, maxV, argb),
                vertex(p0x, p0y, p0z, minU, minV, argb));
        }
        return quad;
    }

    private static BcVertex vertex(float x, float y, float z, float u, float v, int argb) {
        return new BcVertex(new org.joml.Vector3f(x, y, z), u, v, argb, 15728880);
    }

    /** ARGB white scaled by the diffuse factor (legacy {@code colourf(diffuse, diffuse, diffuse, 1)}). */
    private static int shadeArgb(float diffuse) {
        int c = Math.round(diffuse * 255.0f);
        return (0xFF << 24) | (c << 16) | (c << 8) | c;
    }

    /** Legacy {@code ModelUtil#shouldInvertForRender}: NEGATIVE axis faces, except the Z axis flips it. */
    private static boolean shouldInvertForRender(Direction face) {
        boolean flip = face.getAxisDirection() == AxisDirection.NEGATIVE;
        if (face.getAxis() == Axis.Z) {
            flip = !flip;
        }
        return flip;
    }

    /** Legacy {@code dupDarker}'s twin: reversed winding + opposite normal, darkened, unshaded (no diffuse twice). */
    private static BcQuad insideCopy(BcQuad quad) {
        BcQuad reversed = new BcQuad(quad.face().getOpposite(), false, quad.tintIndex(),
            quad.v3(), quad.v2(), quad.v1(), quad.v0());
        return reversed.multiplyColor(INSIDE_DARKEN, INSIDE_DARKEN, INSIDE_DARKEN, 1.0f);
    }

    /** Legacy {@code MutableQuad#rotateTextureUp(n)}: cyclically shifts the four vertex UVs. */
    public static BcQuad rotateTextureUp(BcQuad quad, int times) {
        switch (times & 3) {
            case 0:
                return quad;
            case 1: {
                // v0 <- v1 <- v2 <- v3 <- old v0
                float[] t = uvOf(quad, 0);
                quad = quad.withVertexUv(0, uvOf(quad, 1)[0], uvOf(quad, 1)[1]);
                quad = quad.withVertexUv(1, uvOf(quad, 2)[0], uvOf(quad, 2)[1]);
                quad = quad.withVertexUv(2, uvOf(quad, 3)[0], uvOf(quad, 3)[1]);
                return quad.withVertexUv(3, t[0], t[1]);
            }
            case 2: {
                float[] t0 = uvOf(quad, 0), t1 = uvOf(quad, 1);
                quad = quad.withVertexUv(0, uvOf(quad, 2)[0], uvOf(quad, 2)[1]);
                quad = quad.withVertexUv(1, uvOf(quad, 3)[0], uvOf(quad, 3)[1]);
                quad = quad.withVertexUv(2, t0[0], t0[1]);
                return quad.withVertexUv(3, t1[0], t1[1]);
            }
            default: {
                // case 3: v3 <- v2 <- v1 <- v0 <- old v3
                float[] t = uvOf(quad, 3);
                quad = quad.withVertexUv(3, uvOf(quad, 2)[0], uvOf(quad, 2)[1]);
                quad = quad.withVertexUv(2, uvOf(quad, 1)[0], uvOf(quad, 1)[1]);
                quad = quad.withVertexUv(1, uvOf(quad, 0)[0], uvOf(quad, 0)[1]);
                return quad.withVertexUv(0, t[0], t[1]);
            }
        }
    }

    private static float[] uvOf(BcQuad quad, int index) {
        BcVertex v = quad.vertex(index);
        return new float[] { v.u(), v.v() };
    }

    private BcPipeGeometry() {
    }
}
