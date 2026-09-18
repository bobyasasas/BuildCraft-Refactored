/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.client.render.laser;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * One baked laser face: four vertices (positions in block-relative space, UVs already mapped into the atlas sprite),
 * one face normal, one packed light value per vertex and one ARGB colour (the per-face diffuse light, legacy
 * {@code MutableQuad#diffuseLight}).
 *
 * <p>The laser faces can point in any direction (the tube is rotated arbitrarily), so this cannot reuse
 * {@code BcQuad} &mdash; its normal comes from a {@code Direction}. The vertex emission order mirrors
 * {@code BcQuad#emit} (position, colour, UV, overlay, light, normal) so both play well with the same custom-geometry
 * render types.
 */
public final class BcLaserQuad {

    private final float[] x = new float[4];
    private final float[] y = new float[4];
    private final float[] z = new float[4];
    private final float[] u = new float[4];
    private final float[] v = new float[4];
    private final int[] light = new int[4];
    private float nx, ny, nz;
    private int argb = 0xFFFFFFFF;

    /** Fills the quad from one face's accumulated vertex ring (legacy {@code LaserContext#vertex(0..3)}). */
    static BcLaserQuad of(float[] x, float[] y, float[] z, float[] u, float[] v, int[] light, float nx, float ny,
        float nz, int argb) {
        BcLaserQuad quad = new BcLaserQuad();
        System.arraycopy(x, 0, quad.x, 0, 4);
        System.arraycopy(y, 0, quad.y, 0, 4);
        System.arraycopy(z, 0, quad.z, 0, 4);
        System.arraycopy(u, 0, quad.u, 0, 4);
        System.arraycopy(v, 0, quad.v, 0, 4);
        System.arraycopy(light, 0, quad.light, 0, 4);
        quad.nx = nx;
        quad.ny = ny;
        quad.nz = nz;
        quad.argb = argb;
        return quad;
    }

    /** The same face seen from the back: reversed vertex winding, flipped normal, matching per-face diffuse colour. */
    BcLaserQuad reversed(int backArgb) {
        BcLaserQuad back = new BcLaserQuad();
        for (int i = 0; i < 4; i++) {
            int src = 3 - i;
            back.x[i] = this.x[src];
            back.y[i] = this.y[src];
            back.z[i] = this.z[src];
            back.u[i] = this.u[src];
            back.v[i] = this.v[src];
            back.light[i] = this.light[src];
        }
        back.nx = -this.nx;
        back.ny = -this.ny;
        back.nz = -this.nz;
        back.argb = backArgb;
        return back;
    }

    /** Emits the quad into the given vertex consumer, transforming position/normal by the pose (the same contract as
     * {@code BcQuad#emit}; the overlay element is pinned to {@link OverlayTexture#NO_OVERLAY} because the entity
     * vertex formats of the custom-geometry render types carry UV1). */
    public void emit(PoseStack.Pose pose, VertexConsumer consumer) {
        org.joml.Vector3fc outNormal = pose.transformNormal(new org.joml.Vector3f(this.nx, this.ny, this.nz),
            new org.joml.Vector3f());
        for (int i = 0; i < 4; i++) {
            org.joml.Vector3f pos = pose.pose()
                .transformPosition(new org.joml.Vector3f(this.x[i], this.y[i], this.z[i]));
            consumer.addVertex(pos.x(), pos.y(), pos.z())
                    .setColor(this.argb)
                    .setUv(this.u[i], this.v[i])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(this.light[i])
                    .setNormal(outNormal.x(), outNormal.y(), outNormal.z());
        }
    }

    public float normalX() {
        return this.nx;
    }

    public float normalY() {
        return this.ny;
    }

    public float normalZ() {
        return this.nz;
    }
}
