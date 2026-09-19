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
 * {@link BcQuad#mapUv(net.minecraft.client.renderer.texture.TextureAtlasSprite)} at submit time. Face shading is
 * <em>not</em> baked into the vertex colours any more: the 26.1.2 entity render pipelines the pipe is drawn with
 * ({@code Sheets#cutoutBlockSheet()}/{@code translucentBlockSheet()}, both {@code minecraft_mix_light}) already apply
 * the directional diffuse from the emitted per-vertex normal, so baking legacy's {@code setDiffuse} table here too
 * squared it ({@code texel × diffuse²}, M4.18c pixel evidence). Vertex colours carry only explicit tints now (the
 * inside-copy darkening and the dye skin's colour, set by the caller).
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
    /** How far the dyed skin sits inside the pipe surface (legacy {@code PipeBaseModelGenStandard#colourOffset}). */
    private static final float SKIN_INSET = 0.01f;

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

    /**
     * Builds the dyed pipe's translucent skin (baseline {@code PipeBaseModelGenStandard} {@code QUADS_COLOURED} +
     * {@code generateTranslucent}): one skin face per body face, each inset {@code 0.01} into the pipe (legacy
     * {@code faceOffset} = opposite face's normal × {@code colourOffset}) and paired with an undarkened reversed twin
     * ({@code createDoubleFace}/{@code dupInverted}) so the skin shows from both sides. Quads come out flat white —
     * the caller tints them with the pipe's dye colour and draws them on the translucent layer.
     */
    public static List<BcQuad> colouredSkin(EnumSet<Direction> connections) {
        List<BcQuad> quads = new ArrayList<>(24);
        for (Direction face : Direction.values()) {
            if (connections.contains(face)) {
                float[] center = armCenter(face);
                float[] radius = armRadius(face);
                int i = 0;
                for (Direction armSide : Direction.values()) {
                    if (armSide.getAxis() == face.getAxis()) {
                        continue;
                    }
                    BcQuad quad = insetForSkin(armFace(face, armSide, center, radius, i));
                    quads.add(quad);
                    quads.add(reversedCopy(quad));
                    i++;
                }
            } else {
                BcQuad quad = insetForSkin(centreFace(face));
                quads.add(quad);
                quads.add(reversedCopy(quad));
            }
        }
        return quads;
    }

    /** One centre face + inside copy: full 0.25..0.75 cube face, UVs on the sprite's central circle. */
    private static void addCentreFace(List<BcQuad> quads, Direction face) {
        BcQuad quad = centreFace(face);
        quads.add(quad);
        quads.add(insideCopy(quad));
    }

    private static BcQuad centreFace(Direction face) {
        return createFace(face, 0.5f, 0.5f, 0.5f, CENTER, CENTER, CENTER,
            4 / 16f, 4 / 16f, 12 / 16f, 12 / 16f);
    }

    /** One connection arm + inside copies ({@code QUADS[1][side]}): centre-offset box, four side faces, no end face. */
    private static void addArm(List<BcQuad> quads, Direction side) {
        float[] center = armCenter(side);
        float[] radius = armRadius(side);
        int i = 0;
        for (Direction face : Direction.values()) {
            if (face.getAxis() == side.getAxis()) {
                continue;
            }
            BcQuad quad = armFace(side, face, center, radius, i);
            quads.add(quad);
            quads.add(insideCopy(quad));
            i++;
        }
    }

    /** The arm's centre point ({@code 0.5 + side * 0.375} per axis), as {@code [cx, cy, cz]}. */
    private static float[] armCenter(Direction side) {
        return new float[] {
            0.5f + side.getStepX() * ARM_OFFSET,
            0.5f + side.getStepY() * ARM_OFFSET,
            0.5f + side.getStepZ() * ARM_OFFSET };
    }

    /** The arm's per-axis half extents (0.125 along the arm axis, 0.25 across), as {@code [rx, ry, rz]}. */
    private static float[] armRadius(Direction side) {
        return new float[] {
            side.getAxis() == Axis.X ? ARM_RADIUS_AXIS : ARM_RADIUS_CROSS,
            side.getAxis() == Axis.Y ? ARM_RADIUS_AXIS : ARM_RADIUS_CROSS,
            side.getAxis() == Axis.Z ? ARM_RADIUS_AXIS : ARM_RADIUS_CROSS };
    }

    /** The {@code i}-th side face of the {@code side} arm: edge-band UVs rotated by the legacy {@code uvsRot} table. */
    private static BcQuad armFace(Direction side, Direction face, float[] center, float[] radius, int i) {
        float[] uv = ARM_UVS[i];
        BcQuad quad = createFace(face, center[0], center[1], center[2], radius[0], radius[1], radius[2],
            uv[0], uv[1], uv[2], uv[3]);
        return rotateTextureUp(quad, UVS_ROT[side.ordinal()][i]);
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

        // p0..p3 = centerOfFace + addOrNegate(faceRadius, u, v) for (f,f) (f,t) (t,t) (t,f). Legacy addOrNegate's z
        // term is z * (zisv ? (v ? -1 : 1) : (u ? 1 : -1)), i.e. p0.z flips on zisv, p1.z is always -frz, p2.z flips
        // on !zisv, and p3.z is ALWAYS +frz (both ternary branches give +1 for p3's u=true, v=false). Collapsing p3.z
        // to -frz in the non-zisv branch (an earlier port typo) made p3 == p0 on every X-axis face, degenerating the
        // WEST and EAST sides to half-triangles.
        float p0x = fcx + -frx, p0y = fcy + fry, p0z = fcz + (zisv ? frz : -frz);
        float p1x = fcx + -frx, p1y = fcy + -fry, p1z = fcz - frz;
        float p2x = fcx + frx, p2y = fcy + -fry, p2z = fcz + (zisv ? -frz : frz);
        float p3x = fcx + frx, p3y = fcy + fry, p3z = fcz + frz;

        // Colours stay white — the entity pipelines apply the face diffuse from the normal (see class javadoc).
        int argb = 0xFFFFFFFF;
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

    /**
     * Translates one skin quad {@code 0.01} into the pipe (legacy {@code PipeBaseModelGenStandard#faceOffset}:
     * {@code Vec3.atLowerCornerOf(face.getOpposite().getNormal()).scale(colourOffset)}).
     */
    private static BcQuad insetForSkin(BcQuad quad) {
        Direction face = quad.face();
        float dx = face.getOpposite().getStepX() * SKIN_INSET;
        float dy = face.getOpposite().getStepY() * SKIN_INSET;
        float dz = face.getOpposite().getStepZ() * SKIN_INSET;
        BcQuad out = quad;
        for (int i = 0; i < 4; i++) {
            BcVertex vertex = out.vertex(i);
            org.joml.Vector3f p = vertex.position();
            out = out.withVertex(i, vertex.withPosition(p.x() + dx, p.y() + dy, p.z() + dz));
        }
        return out;
    }

    /** The skin's second copy (legacy {@code createDoubleFace}/{@code dupInverted}): reversed winding + opposite
     * normal, flat colour — no {@code dupDarker} darkening on the dye skin. */
    private static BcQuad reversedCopy(BcQuad quad) {
        return new BcQuad(quad.face().getOpposite(), false, quad.tintIndex(),
            quad.v3(), quad.v2(), quad.v1(), quad.v0());
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
        return reversedCopy(quad).multiplyColor(INSIDE_DARKEN, INSIDE_DARKEN, INSIDE_DARKEN, 1.0f);
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
