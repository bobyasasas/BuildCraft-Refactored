/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.model.json;

import net.minecraft.core.Direction;

/**
 * Minimal port of the legacy {@code buildcraft.lib.client.model.MutableQuad} for the jsonbc parser (M4.4): one
 * mutable baked quad in block space (positions 0..1), carrying everything a {@code BlockEntityRenderer} needs to emit
 * it &mdash; face (the outward normal, unlike legacy it is kept up to date by {@link #rotate} so custom-geometry
 * emitters survive backface culling), per-face shade flag, block light 0..15, ARGB colour, the resolved texture id and
 * four vertices (position + raw 0..1 UV).
 *
 * <p>Deliberately not carried over from legacy {@code MutableQuad}: tint index (no engine/tile jsonbc consumer),
 * sprite references (the renderer resolves {@link #texture} against the block atlas at emit time), per-vertex normals
 * (the quad's normal is {@link #face}) and overlay coordinates.
 */
public final class JsonQuad {

    /** One vertex: block-space position (0..1) and raw texture coordinates (0..1). */
    public static final class Vertex {
        public float x, y, z;
        public float u, v;

        public Vertex(float x, float y, float z, float u, float v) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.u = u;
            this.v = v;
        }

        public Vertex(Vertex from) {
            this(from.x, from.y, from.z, from.u, from.v);
        }
    }

    public final Vertex[] vertices = {//
        new Vertex(0, 0, 0, 0, 0), new Vertex(0, 0, 0, 0, 0),//
        new Vertex(0, 0, 0, 0, 0), new Vertex(0, 0, 0, 0, 0) };

    /** The outward normal of this quad. Kept in sync by {@link #rotate} (legacy left it stale and used per-vertex
     * normals instead &mdash; see the class javadoc). */
    public Direction face;
    /** Per-face diffuse shading flag (legacy {@code setShade}); applied as a colour multiplier by the renderer. */
    public boolean shade = true;
    /** Block light of this quad, 0..15 (jsonbc {@code "light"}); 0 = inherit the world light. */
    public int blockLight = 0;
    /** Quad colour as ARGB (jsonbc {@code "colour"}; white by default). */
    public int colorArgb = 0xFFFF_FFFF;
    /** The resolved texture id (e.g. {@code buildcraftlib:block/engine/trunk_blue}). */
    public String texture = "";

    public JsonQuad(Direction face) {
        this.face = face;
    }

    public JsonQuad(JsonQuad from) {
        this.face = from.face;
        this.shade = from.shade;
        this.blockLight = from.blockLight;
        this.colorArgb = from.colorArgb;
        this.texture = from.texture;
        for (int i = 0; i < 4; i++) {
            this.vertices[i] = new Vertex(from.vertices[i]);
        }
    }

    /** Legacy {@code MutableQuad#rotateTextureUp}: cycles the four UVs around the vertices {@code times} quarter
     * turns (clockwise when viewed from outside the face). */
    public JsonQuad rotateTextureUp(int times) {
        switch (times & 3) {
            case 0: {
                return this;
            }
            case 1: {
                Vertex t = new Vertex(this.vertices[0]);
                this.setVertex(0, this.vertices[1]);
                this.setVertex(1, this.vertices[2]);
                this.setVertex(2, this.vertices[3]);
                this.setVertex(3, t);
                return this;
            }
            case 2: {
                Vertex t0 = new Vertex(this.vertices[0]);
                Vertex t1 = new Vertex(this.vertices[1]);
                this.setVertex(0, this.vertices[2]);
                this.setVertex(1, this.vertices[3]);
                this.setVertex(2, t0);
                this.setVertex(3, t1);
                return this;
            }
            case 3: {
                Vertex t = new Vertex(this.vertices[3]);
                this.setVertex(3, this.vertices[2]);
                this.setVertex(2, this.vertices[1]);
                this.setVertex(1, this.vertices[0]);
                this.setVertex(0, t);
                return this;
            }
            default: {
                throw new IllegalStateException("'times & 3' was not 0, 1, 2 or 3!");
            }
        }
    }

    /** Copies this quad with its vertices reversed (0&larr;3, 1&larr;2, 2&larr;1, 3&larr;0 &mdash; legacy
     * {@code copyAndInvertNormal}'s winding reversal) and the face flipped to the opposite direction (legacy inverted
     * the per-vertex normal; here the normal is {@link #face}, so flipping the face is the equivalent). */
    public JsonQuad copyAndInvertNormal() {
        JsonQuad copy = new JsonQuad(this);
        copy.face = this.face.getOpposite();
        Vertex v0 = new Vertex(this.vertices[0]);
        Vertex v1 = new Vertex(this.vertices[1]);
        copy.setVertex(0, this.vertices[3]);
        copy.setVertex(1, this.vertices[2]);
        copy.setVertex(2, v1);
        copy.setVertex(3, v0);
        return copy;
    }

    /** Legacy {@code MutableQuad#rotate(Direction, Direction, float, float, float)}: rotates this quad from one
     * facing to another around the given origin (block space). Touches positions and the face only &mdash; UVs ride
     * with their vertices, exactly like legacy. */
    public JsonQuad rotate(Direction from, Direction to, float ox, float oy, float oz) {
        if (from == to) {
            // don't bother rotating: there is nothing to rotate!
            return this;
        }
        this.translate(-ox, -oy, -oz);
        // @formatter:off
        switch (from.getAxis()) {
            case X: {
                int mult = from.getStepX();
                switch (to.getAxis()) {
                    case X: rotateY_180(); break;
                    case Y: rotateZ_90(mult * to.getStepY()); break;
                    case Z: rotateY_90(mult * to.getStepZ()); break;
                }
                break;
            }
            case Y: {
                int mult = from.getStepY();
                switch (to.getAxis()) {
                    case X: rotateZ_90(-mult * to.getStepX()); break;
                    case Y: rotateZ_180(); break;
                    case Z: rotateX_90(mult * to.getStepZ()); break;
                }
                break;
            }
            case Z: {
                int mult = -from.getStepZ();
                switch (to.getAxis()) {
                    case X: rotateY_90(mult * to.getStepX()); break;
                    case Y: rotateX_90(mult * to.getStepY()); break;
                    case Z: rotateY_180(); break;
                }
                break;
            }
        }
        // @formatter:on
        this.translate(ox, oy, oz);
        this.face = JsonModelGeometry.rotateDirection(from, to, this.face);
        return this;
    }

    private void translate(float dx, float dy, float dz) {
        for (Vertex vertex : this.vertices) {
            vertex.x += dx;
            vertex.y += dy;
            vertex.z += dz;
        }
    }

    private void rotateX_90(float scale) {
        float ym = scale;
        float zm = -ym;
        for (Vertex vertex : this.vertices) {
            float t = vertex.y * ym;
            vertex.y = vertex.z * zm;
            vertex.z = t;
        }
    }

    private void rotateY_90(float scale) {
        float xm = scale;
        float zm = -xm;
        for (Vertex vertex : this.vertices) {
            float t = vertex.x * xm;
            vertex.x = vertex.z * zm;
            vertex.z = t;
        }
    }

    private void rotateZ_90(float scale) {
        float xm = scale;
        float ym = -xm;
        for (Vertex vertex : this.vertices) {
            float t = vertex.x * xm;
            vertex.x = vertex.y * ym;
            vertex.y = t;
        }
    }

    private void rotateX_180() {
        for (Vertex vertex : this.vertices) {
            vertex.y = -vertex.y;
            vertex.z = -vertex.z;
        }
    }

    private void rotateY_180() {
        for (Vertex vertex : this.vertices) {
            vertex.x = -vertex.x;
            vertex.z = -vertex.z;
        }
    }

    private void rotateZ_180() {
        for (Vertex vertex : this.vertices) {
            vertex.x = -vertex.x;
            vertex.y = -vertex.y;
        }
    }

    private void setVertex(int index, Vertex from) {
        this.vertices[index].x = from.x;
        this.vertices[index].y = from.y;
        this.vertices[index].z = from.z;
        this.vertices[index].u = from.u;
        this.vertices[index].v = from.v;
    }
}
