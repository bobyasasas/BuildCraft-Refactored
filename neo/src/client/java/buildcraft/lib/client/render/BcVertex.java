/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ARGB;
import org.joml.Vector3f;

/**
 * One vertex of a {@link BcQuad}: position, texture coordinates (raw, typically 0..1), colour (ARGB, exactly the
 * format {@link VertexConsumer#setColor(int)} wants) and packed light coords (block | sky &lt;&lt; 16, exactly the
 * format {@link VertexConsumer#setLight(int)} wants).
 *
 * <p>Record counterpart of the legacy {@code buildcraft.lib.client.model.MutableVertex} fields
 * {@code position_x/y/z}, {@code tex_u/v}, {@code colour_r/g/b/a} and {@code light_block/sky}. Deliberately NOT
 * ported: per-vertex normals (a quad's normal comes from its face) and per-vertex overlay coords (26.1.2 custom
 * geometry has no use for them yet — add if a M2.7+ renderer needs breaking overlays).
 *
 * <p>Like legacy {@code MutableVertex} the position vector is a shared mutable {@link Vector3f}; the {@code with*}
 * methods return new records with fresh vectors, so treating records as immutable value types stays safe as long as
 * callers don't hand out their position vectors.
 */
public record BcVertex(Vector3f position, float u, float v, int color, int light) {

    public BcVertex withPosition(float x, float y, float z) {
        return new BcVertex(new Vector3f(x, y, z), this.u, this.v, this.color, this.light);
    }

    public BcVertex withUv(float u, float v) {
        return new BcVertex(this.position, u, v, this.color, this.light);
    }

    public BcVertex withColor(int argb) {
        return new BcVertex(this.position, this.u, this.v, argb, this.light);
    }

    public BcVertex withLight(int packedLight) {
        return new BcVertex(this.position, this.u, this.v, this.color, packedLight);
    }

    /** Maps the raw UVs into the given atlas sprite's coordinates — the emit-time counterpart of what legacy
     * {@code MutableQuad#toBakedBlock()} baked in via the quad's {@code sprite}. */
    public BcVertex mapUv(TextureAtlasSprite sprite) {
        return new BcVertex(this.position, sprite.getU(this.u), sprite.getV(this.v), this.color, this.light);
    }

    /** Transforms position and (via {@link BcQuad}) normal by the pose, like legacy
     * {@code MutableVertex#render(PoseStack.Pose, VertexConsumer)} but returning a new immutable vertex instead of
     * writing straight into a buffer. */
    public BcVertex transform(PoseStack.Pose pose) {
        Vector3f transformed = pose.pose().transformPosition(this.position, new Vector3f());
        return new BcVertex(transformed, this.u, this.v, this.color, this.light);
    }

    /** Multiplies this vertex's colour by the given per-channel factors (0..1), legacy
     * {@code MutableVertex}-style colour scaling done through {@link ARGB} so the result stays buffer-ready. */
    public BcVertex multiplyColor(float r, float g, float b, float a) {
        int argb = ARGB.color(
                (int) (ARGB.alpha(this.color) * a),
                (int) (ARGB.red(this.color) * r),
                (int) (ARGB.green(this.color) * g),
                (int) (ARGB.blue(this.color) * b));
        return this.withColor(argb);
    }
}
