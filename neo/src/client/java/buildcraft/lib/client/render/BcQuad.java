/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import org.joml.Vector3fc;

/**
 * Immutable four-vertex quad — the 26.1.2 counterpart of legacy
 * {@code buildcraft.lib.client.model.MutableQuad}, reduced to what the M2.7+ renderers actually need. Where legacy
 * mutated one shared quad object in place, this is a value object: every transform returns a new quad, so instances
 * can safely be cached as fields of a {@code BlockEntityRenderState} (extract once per frame, submit possibly several
 * times) — see the package javadoc for the porting rationale.
 *
 * <p>Correspondence to legacy {@code MutableQuad}:
 * <ul>
 * <li>{@code vertex_0..vertex_3} → {@link #v0()}..{@link #v3()} ({@link BcVertex} records)</li>
 * <li>{@code tintIndex}/{@code face}/{@code shade} → {@link #tintIndex()}, {@link #face()}, {@link #shade()}</li>
 * <li>{@code sprite} → dropped; UVs stay raw and are mapped at emit/transform time via
 * {@link BcVertex#mapUv(TextureAtlasSprite)}</li>
 * <li>{@code colouri/multColourd/multColouri} → {@link #withColor(int)}, {@link #multiplyColor(int, int, int, int)},
 * {@link #multiplyColor(float, float, float, float)}</li>
 * <li>{@code rotateTextureUp}, per-vertex normals, {@code fromBakedBlock/toBakedBlock} → dropped (no consumer in the
 * 26.1.2 pipeline; re-add on demand)</li>
 * <li>{@code render(PoseStack.Pose, VertexConsumer)} → {@link #emit(PoseStack.Pose, VertexConsumer)}</li>
 * </ul>
 */
public record BcQuad(Direction face, boolean shade, int tintIndex, BcVertex v0, BcVertex v1, BcVertex v2, BcVertex v3) {

    /** Builds a quad covering one face of the unit cube, analogous to the legacy quad factories. UVs are assigned
     * sequentially (0,0)→(1,0)→(1,1)→(0,1) in vertex order — callers that need vanilla's exact per-face UV rotation
     * should re-set them with {@link #withVertexUv}. Vertices wind counter-clockwise seen from outside the face, so
     * the quad survives culling. */
    public static BcQuad unit(Direction face, int tintIndex, boolean shade) {
        BcVertex[] corners = unitCorners(face);
        BcQuad quad = new BcQuad(face, shade, tintIndex,
                corners[0], corners[1], corners[2], corners[3]);
        float[][] uvs = { { 0, 0 }, { 1, 0 }, { 1, 1 }, { 0, 1 } };
        BcVertex[] out = new BcVertex[4];
        for (int i = 0; i < 4; i++) {
            out[i] = quad.vertex(i).withUv(uvs[i][0], uvs[i][1]);
        }
        return new BcQuad(face, shade, tintIndex, out[0], out[1], out[2], out[3]);
    }

    private static BcVertex[] unitCorners(Direction face) {
        // Vanilla-style unit-cube face tables (counter-clockwise from outside),
        // same winding the block model baker uses for each Direction.
        return switch (face) {
            case DOWN -> new BcVertex[] { at(0, 0, 1), at(1, 0, 1), at(1, 0, 0), at(0, 0, 0) };
            case UP -> new BcVertex[] { at(0, 1, 0), at(1, 1, 0), at(1, 1, 1), at(0, 1, 1) };
            case NORTH -> new BcVertex[] { at(1, 1, 0), at(0, 1, 0), at(0, 0, 0), at(1, 0, 0) };
            case SOUTH -> new BcVertex[] { at(0, 1, 1), at(1, 1, 1), at(1, 0, 1), at(0, 0, 1) };
            case WEST -> new BcVertex[] { at(0, 1, 0), at(0, 1, 1), at(0, 0, 1), at(0, 0, 0) };
            case EAST -> new BcVertex[] { at(1, 1, 1), at(1, 1, 0), at(1, 0, 0), at(1, 0, 1) };
        };
    }

    private static BcVertex at(float x, float y, float z) {
        return new BcVertex(new org.joml.Vector3f(x, y, z), 0, 0, 0xFFFFFFFF, 15728880);
    }

    public BcVertex vertex(int index) {
        return switch (index) {
            case 0 -> this.v0;
            case 1 -> this.v1;
            case 2 -> this.v2;
            case 3 -> this.v3;
            default -> throw new IndexOutOfBoundsException("quad vertex " + index);
        };
    }

    public BcQuad withFace(Direction face) {
        return new BcQuad(face, this.shade, this.tintIndex, this.v0, this.v1, this.v2, this.v3);
    }

    public BcQuad withShade(boolean shade) {
        return new BcQuad(this.face, shade, this.tintIndex, this.v0, this.v1, this.v2, this.v3);
    }

    public BcQuad withTint(int tintIndex) {
        return new BcQuad(this.face, this.shade, tintIndex, this.v0, this.v1, this.v2, this.v3);
    }

    public BcQuad withVertex(int index, BcVertex vertex) {
        return switch (index) {
            case 0 -> new BcQuad(this.face, this.shade, this.tintIndex, vertex, this.v1, this.v2, this.v3);
            case 1 -> new BcQuad(this.face, this.shade, this.tintIndex, this.v0, vertex, this.v2, this.v3);
            case 2 -> new BcQuad(this.face, this.shade, this.tintIndex, this.v0, this.v1, vertex, this.v3);
            case 3 -> new BcQuad(this.face, this.shade, this.tintIndex, this.v0, this.v1, this.v2, vertex);
            default -> throw new IndexOutOfBoundsException("quad vertex " + index);
        };
    }

    public BcQuad withVertexUv(int index, float u, float v) {
        return this.withVertex(index, this.vertex(index).withUv(u, v));
    }

    /** Sets one ARGB colour on all four vertices (legacy {@code MutableQuad#colouri(int)}). */
    public BcQuad withColor(int argb) {
        return new BcQuad(this.face, this.shade, this.tintIndex,
                this.v0.withColor(argb), this.v1.withColor(argb), this.v2.withColor(argb), this.v3.withColor(argb));
    }

    /** Multiplies all four vertex colours by per-channel factors 0..1 (legacy {@code multColourd}). */
    public BcQuad multiplyColor(float r, float g, float b, float a) {
        return new BcQuad(this.face, this.shade, this.tintIndex,
                this.v0.multiplyColor(r, g, b, a),
                this.v1.multiplyColor(r, g, b, a),
                this.v2.multiplyColor(r, g, b, a),
                this.v3.multiplyColor(r, g, b, a));
    }

    /** Multiplies all four vertex colours by an ARGB colour (legacy {@code multColouri(int)}), using
     * {@link ARGB#multiply}. */
    public BcQuad multiplyColor(int argb) {
        return new BcQuad(this.face, this.shade, this.tintIndex,
                this.v0.withColor(ARGB.multiply(this.v0.color(), argb)),
                this.v1.withColor(ARGB.multiply(this.v1.color(), argb)),
                this.v2.withColor(ARGB.multiply(this.v2.color(), argb)),
                this.v3.withColor(ARGB.multiply(this.v3.color(), argb)));
    }

    /** Sets one packed light coordinate on all four vertices (block | sky &lt;&lt; 16). */
    public BcQuad withLight(int packedLight) {
        return new BcQuad(this.face, this.shade, this.tintIndex,
                this.v0.withLight(packedLight), this.v1.withLight(packedLight),
                this.v2.withLight(packedLight), this.v3.withLight(packedLight));
    }

    /** Maps every vertex's raw UVs into the sprite's atlas coordinates. */
    public BcQuad mapUv(TextureAtlasSprite sprite) {
        return new BcQuad(this.face, this.shade, this.tintIndex,
                this.v0.mapUv(sprite), this.v1.mapUv(sprite), this.v2.mapUv(sprite), this.v3.mapUv(sprite));
    }

    /** Returns a copy with positions (and UV/colour/light data) transformed by the pose; the face normal is rotated
     * by the pose's normal matrix at emit time, so this only touches positions. */
    public BcQuad transform(PoseStack.Pose pose) {
        return new BcQuad(this.face, this.shade, this.tintIndex,
                this.v0.transform(pose), this.v1.transform(pose), this.v2.transform(pose), this.v3.transform(pose));
    }

    /** The quad's outward normal (the face's unit vector), already a {@link Vector3fc} for the vertex consumer. */
    public Vector3fc normal() {
        return this.face.getUnitVec3f();
    }

    /** Emits the quad into the given vertex consumer — the 26.1.2 counterpart of legacy
     * {@code MutableQuad#render(PoseStack.Pose, VertexConsumer)}. Positions are transformed by the pose on the fly;
     * the normal by the pose's normal matrix. UVs must already be mapped (see {@link #mapUv}). */
    public void emit(PoseStack.Pose pose, VertexConsumer consumer) {
        Vector3fc outNormal = pose.transformNormal(this.normal(), new org.joml.Vector3f());
        for (int i = 0; i < 4; i++) {
            BcVertex vertex = this.vertex(i).transform(pose);
            consumer.addVertex(vertex.position().x(), vertex.position().y(), vertex.position().z())
                    .setColor(vertex.color())
                    .setUv(vertex.u(), vertex.v())
                    .setLight(vertex.light())
                    .setNormal(outNormal.x(), outNormal.y(), outNormal.z());
        }
    }
}
