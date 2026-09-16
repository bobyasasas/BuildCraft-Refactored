/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.client.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import buildcraft.lib.client.render.BcQuad;
import buildcraft.lib.client.render.BcVertex;

/**
 * Dynamic box geometry helpers for the M2.7b block entity renderers (and later M2.8+ ones): builds the six faces of an
 * arbitrary axis-aligned box as {@link BcQuad}s, using the same per-face winding tables as {@link BcQuad#unit} (just
 * with the unit cube's 0/1 corners re-mapped onto the given bounds), so every face survives backface culling and the
 * quad order matches the block-model baker's.
 *
 * <p>UVs are the full sprite (0,0)-&gt;(1,1) per face in the same order as {@link BcQuad#unit} assigns them; light is
 * filled in from the render state at extract time via {@link #lightAll(List, BlockEntityRenderState)}, colours stay
 * white unless the caller tints.
 */
public final class BcBoxes {

    /** Builds the six faces of the box {@code [x0,y0,z0] .. [x1,y1,z1]} (block-relative coordinates), no tint,
     * shaded. */
    public static List<BcQuad> box(float x0, float y0, float z0, float x1, float y1, float z1) {
        List<BcQuad> quads = new ArrayList<>(6);
        for (Direction face : Direction.values()) {
            BcVertex[] corners = corners(face, x0, y0, z0, x1, y1, z1);
            BcQuad quad = new BcQuad(face, true, -1, corners[0], corners[1], corners[2], corners[3]);
            float[][] uvs = { { 0, 0 }, { 1, 0 }, { 1, 1 }, { 0, 1 } };
            for (int i = 0; i < 4; i++) {
                quad = quad.withVertexUv(i, uvs[i][0], uvs[i][1]);
            }
            quads.add(quad);
        }
        return quads;
    }

    /** Builds the twelve 1-pixel-thick edges of the box {@code [x0,y0,z0] .. [x1,y1,z1]} as slim solid boxes
     * (M2.12 filler/quarry work-area outlines): each edge is a full {@link #box} of cross-section {@code t} running
     * along one axis, so the whole frame survives backface culling and light baking like any other {@link BcQuad}
     * geometry. Coordinates are block-relative; callers usually inflate the box a hair outward to avoid z-fighting
     * with the neighbour block faces the frame sits on. */
    public static List<BcQuad> frame(float x0, float y0, float z0, float x1, float y1, float z1, float t) {
        List<BcQuad> out = new ArrayList<>(72); // 12 edges x 6 faces
        // four edges along X (bottom/front, bottom/back, top/front, top/back)
        out.addAll(box(x0, y0, z0, x1, y0 + t, z0 + t));
        out.addAll(box(x0, y0, z1 - t, x1, y0 + t, z1));
        out.addAll(box(x0, y1 - t, z0, x1, y1, z0 + t));
        out.addAll(box(x0, y1 - t, z1 - t, x1, y1, z1));
        // four edges along Y (the vertical corners)
        out.addAll(box(x0, y0, z0, x0 + t, y1, z0 + t));
        out.addAll(box(x1 - t, y0, z0, x1, y1, z0 + t));
        out.addAll(box(x0, y0, z1 - t, x0 + t, y1, z1));
        out.addAll(box(x1 - t, y0, z1 - t, x1, y1, z1));
        // four edges along Z (bottom/left, bottom/right, top/left, top/right)
        out.addAll(box(x0, y0, z0, x0 + t, y0 + t, z1));
        out.addAll(box(x1 - t, y0, z0, x1, y0 + t, z1));
        out.addAll(box(x0, y1 - t, z0, x0 + t, y1, z1));
        out.addAll(box(x1 - t, y1 - t, z0, x1, y1, z1));
        return out;
    }

    /** Copies the given box (fresh vertex positions, shared UV/colour/light) &mdash; used to translate animated boxes
     * without rebuilding them. */
    public static List<BcQuad> translate(List<BcQuad> quads, float dx, float dy, float dz) {
        List<BcQuad> out = new ArrayList<>(quads.size());
        for (BcQuad quad : quads) {
            BcQuad moved = quad;
            for (int i = 0; i < 4; i++) {
                BcVertex v = quad.vertex(i);
                Vector3f p = v.position();
                moved = moved.withVertex(i, v.withPosition(p.x() + dx, p.y() + dy, p.z() + dz));
            }
            out.add(moved);
        }
        return out;
    }

    /** Sets the render state's packed light coords on every vertex of every quad (call at extract time). */
    public static void lightAll(List<BcQuad> quads, BlockEntityRenderState state) {
        for (int i = 0; i < quads.size(); i++) {
            quads.set(i, quads.get(i).withLight(state.lightCoords));
        }
    }

    /** The unit-cube face corner table of {@link BcQuad} re-mapped onto {@code [x0..x1] × [y0..y1] × [z0..z1]}:
     * each 0 coordinate becomes the box minimum and each 1 the box maximum, keeping the counter-clockwise-from-outside
     * winding (so scaled boxes still survive culling). */
    private static BcVertex[] corners(Direction face, float x0, float y0, float z0, float x1, float y1, float z1) {
        // Unit corner coordinates from BcQuad.unitCorners, each with a per-axis min/max replacement.
        return switch (face) {
            case DOWN -> new BcVertex[] { at(x0, y0, z1), at(x1, y0, z1), at(x1, y0, z0), at(x0, y0, z0) };
            case UP -> new BcVertex[] { at(x0, y1, z0), at(x1, y1, z0), at(x1, y1, z1), at(x0, y1, z1) };
            case NORTH -> new BcVertex[] { at(x1, y1, z0), at(x0, y1, z0), at(x0, y0, z0), at(x1, y0, z0) };
            case SOUTH -> new BcVertex[] { at(x0, y1, z1), at(x1, y1, z1), at(x1, y0, z1), at(x0, y0, z1) };
            case WEST -> new BcVertex[] { at(x0, y1, z0), at(x0, y1, z1), at(x0, y0, z1), at(x0, y0, z0) };
            case EAST -> new BcVertex[] { at(x1, y1, z1), at(x1, y1, z0), at(x1, y0, z0), at(x1, y0, z1) };
        };
    }

    private static BcVertex at(float x, float y, float z) {
        return new BcVertex(new Vector3f(x, y, z), 0, 0, 0xFFFFFFFF, 15728880);
    }

    private BcBoxes() {
    }
}
