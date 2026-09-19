/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.transport.client.render;

import java.util.EnumSet;
import net.minecraft.core.Direction;
import buildcraft.lib.client.render.BcPipeGeometry;
import buildcraft.lib.client.render.BcQuad;
import buildcraft.lib.client.render.BcVertex;

/**
 * Regression smoke check for {@link BcPipeGeometry} (the M4.18 "管道侧面渲染有问题" fix). The baseline
 * {@code ModelUtil#addOrNegate} gives p3.z {@code +frz} unconditionally; an earlier port typo collapsed p3 onto p0 on
 * every X-axis face, degenerating the WEST and EAST pipe sides to half-triangles. This check walks every quad the
 * geometry emits and asserts each one is a proper planar, outward-wound quad:
 * <ol>
 * <li>all four corners are pairwise distinct (the regression — p0 == p3 — fails here);</li>
 * <li>the diagonals cross at a non-zero area (no sliver triangles);</li>
 * <li>the winding's cross-product normal agrees with the quad's face, so nothing disappears to backface culling.</li>
 * </ol>
 *
 * <p>Everything touched here ({@link Direction}, JOML, plain arithmetic) is bootstrap-free, so {@link #main} runs on a
 * bare JVM against the compiled client classes — no FML, no atlas. It is dev evidence only and never runs in normal
 * play.
 */
public final class BcPipeGeometrySmoke {

    /** Runs the checks and returns {@code true} when every quad passed. Never throws. */
    public static boolean check() {
        return checkConnections(EnumSet.noneOf(Direction.class))//
            & checkConnections(EnumSet.of(Direction.NORTH))//
            & checkConnections(EnumSet.of(Direction.NORTH, Direction.SOUTH))//
            & checkConnections(EnumSet.of(Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH,
                Direction.EAST, Direction.WEST));
    }

    private static boolean checkConnections(EnumSet<Direction> connections) {
        for (BcQuad quad : BcPipeGeometry.pipeBody(connections)) {
            if (!checkQuad(quad, quad.face())) {
                return false;
            }
        }
        // the M4.18c dye skin: same planarity/winding rules apply to the inset faces and their reversed twins
        for (BcQuad quad : BcPipeGeometry.colouredSkin(connections)) {
            if (!checkQuad(quad, quad.face())) {
                return false;
            }
        }
        return true;
    }

    private static boolean checkQuad(BcQuad quad, Direction face) {
        BcVertex v0 = quad.v0(), v1 = quad.v1(), v2 = quad.v2(), v3 = quad.v3();
        // 1. pairwise distinct corners (the p0 == p3 regression).
        for (BcVertex a : new BcVertex[] { v0, v1, v2 }) {
            for (BcVertex b : new BcVertex[] { v1, v2, v3 }) {
                if (a != b && a.position().equals(b.position())) {
                    return false;
                }
            }
        }
        // 2. non-zero area: the diagonal cross product must not vanish.
        float[] d1 = sub(v2.position(), v0.position());
        float[] d2 = sub(v3.position(), v1.position());
        float[] cross = { d1[1] * d2[2] - d1[2] * d2[1], d1[2] * d2[0] - d1[0] * d2[2],
                d1[0] * d2[1] - d1[1] * d2[0] };
        double area2 = Math.sqrt(cross[0] * (double) cross[0] + cross[1] * (double) cross[1]
            + cross[2] * (double) cross[2]);
        if (area2 < 1e-4) {
            return false;
        }
        // 3. outward winding: (v1 - v0) x (v3 - v0) must point along the face normal (CCW seen from outside).
        float[] e1 = sub(v1.position(), v0.position());
        float[] e2 = sub(v3.position(), v0.position());
        float[] wn = { e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2], e1[0] * e2[1] - e1[1] * e2[0] };
        return wn[0] * face.getStepX() + wn[1] * face.getStepY() + wn[2] * face.getStepZ() > 0.0f;
    }

    private static float[] sub(org.joml.Vector3fc a, org.joml.Vector3fc b) {
        return new float[] { a.x() - b.x(), a.y() - b.y(), a.z() - b.z() };
    }

    public static void main(String[] args) {
        boolean pass = check();
        System.out.println("[M418] BcPipeGeometrySmoke: " + (pass ? "PASS" : "FAIL"));
        if (!pass) {
            System.exit(1);
        }
    }

    private BcPipeGeometrySmoke() {
    }
}
