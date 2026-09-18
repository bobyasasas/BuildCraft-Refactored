/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.client.render.laser;

import java.util.List;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

/**
 * Bakes a {@link BcLaserData} into {@link BcLaserQuad}s (M4.5 port of the legacy laser pipeline
 * {@code LaserContext} + {@code CompiledLaserType} + {@code CompiledLaserRow} + {@code LaserRenderer_BC8#makeLaser},
 * minus the GL display-list caching: on 26.1.2 the quads ride the block entity render state instead).
 *
 * <p>Geometry is the legacy maths verbatim: the laser runs along the local +X axis with a square cross-section of side
 * {@code row height * scale}, the world transform is {@code translate(start) * scale * rotY(angleZ) * rotZ(angleY)},
 * the middle segments repeat the variation strips until the whole length is covered and the remaining slack is
 * distributed over the start/end taper rows.
 */
public final class BcLaserBaker {

    /** Legacy {@code MutableQuad#diffuseLight}: BuildCraft's fixed directional light factor for one face normal. */
    public static float diffuseLight(float x, float y, float z) {
        boolean up = y >= 0;
        float xx = x * x;
        float yy = y * y;
        float zz = z * z;
        float t = xx + yy + zz;
        float light = (xx * 0.6f + zz * 0.8f) / t;
        float yyt = yy / t;
        if (!up) {
            yyt *= 0.5f;
        }
        light += yyt;
        return light;
    }

    /**
     * Bakes one laser. Positions are emitted relative to {@code origin} (the block entity position &mdash; the BER
     * submit pose is already translated to that corner, the same convention the M2.12 quarry renderer uses), while
     * light sampling keeps using the absolute world coordinates.
     *
     * @param sprites resolves each row's sprite id into its block-atlas sprite
     * @param out the baked quads are appended here (coordinates relative to {@code origin})
     */
    public static void bake(BcLaserData data, BlockPos origin, @Nullable Level level, SpriteGetter sprites,
        List<BcLaserQuad> out) {
        Vec3 delta = data.start().subtract(data.end());
        double dx = delta.x;
        double dy = delta.y;
        double dz = delta.z;
        double realLength = delta.length();
        if (realLength < 1.0e-6) {
            return;
        }
        double length = realLength / data.scale();

        final double angleZ = Math.PI - Math.atan2(dz, dx);
        final double angleY;
        if (dx == 0 && dz == 0) {
            angleY = dy < 0 ? Math.PI / 2 : -Math.PI / 2;
        } else {
            double horizontal = Math.sqrt(realLength * realLength - dy * dy);
            angleY = -Math.atan2(dy, horizontal);
        }

        // matrix = translate * scale * rotY(angleZ) * rotZ(angleY), the exact legacy LaserContext order
        Vector3f startLocal = new Vector3f(
            (float) (data.start().x - origin.getX()),
            (float) (data.start().y - origin.getY()),
            (float) (data.start().z - origin.getZ()));
        Matrix4f matrix = new Matrix4f();
        Matrix4f holding = new Matrix4f();
        holding.translation(startLocal);
        matrix.mul(holding);
        holding.scaling((float) data.scale(), (float) data.scale(), (float) data.scale());
        matrix.mul(holding);
        holding.rotationY((float) angleZ);
        matrix.mul(holding);
        holding.rotationZ((float) angleY);
        matrix.mul(holding);

        Context ctx = new Context(data, origin, level, sprites, matrix, length, out);

        // segment layout, legacy CompiledLaserType constructor + bakeFor
        PerSide perSide = new PerSide(data.type().variations());
        BcLaserRow start = data.type().start();
        BcLaserRow end = data.type().end();
        double startWidth = start == null ? 0 : start.width();
        double endWidth = end == null ? 0 : end.width();

        bakeCap(ctx, data.type().capStart(), true);
        bakeCap(ctx, data.type().capEnd(), false);

        double lengthForMiddle = Math.max(0, length - startWidth - endWidth);
        int numMiddle = Mth.floor(lengthForMiddle / perSide.middleWidth);
        double leftOver = lengthForMiddle - perSide.middleWidth * numMiddle;
        if (leftOver > 0) {
            numMiddle++;
        }
        double lengthEnds = length - perSide.middleWidth * numMiddle;
        final double startLength;
        final double endLength;
        if (startWidth > 0 && endWidth > 0) {
            double ratioStartEnd = startWidth / endWidth;
            startLength = (lengthEnds / 2) * ratioStartEnd;
            endLength = (lengthEnds / 2) / ratioStartEnd;
        } else if (startWidth <= 0) {
            startLength = 0;
            endLength = lengthEnds;
        } else {
            startLength = lengthEnds;
            endLength = 0;
        }
        if (startLength > 0 && start != null) {
            bakeTaper(ctx, start, startLength, true);
        }
        if (endLength > 0 && end != null) {
            bakeTaper(ctx, end, endLength, false);
        }
        if (numMiddle > 0) {
            for (BcLaserSide side : BcLaserSide.VALUES) {
                bakeMiddle(ctx, perSide.rows(side), side, startLength, numMiddle);
            }
        }
    }

    // ------------------------------------------------------------------
    // Strip bakers (legacy CompiledLaserRow vertex tables, verbatim)
    // ------------------------------------------------------------------

    private static void bakeCap(Context ctx, BcLaserRow row, boolean startCap) {
        double h = row.height() / 2;
        if (startCap) {
            ctx.setFaceNormal(-1, 0, 0);
            ctx.addPoint(row, 0, h, h, row.texU(1), row.texV(1));
            ctx.addPoint(row, 0, h, -h, row.texU(1), row.texV(0));
            ctx.addPoint(row, 0, -h, -h, row.texU(0), row.texV(0));
            ctx.addPoint(row, 0, -h, h, row.texU(0), row.texV(1));
        } else {
            ctx.setFaceNormal(1, 0, 0);
            ctx.addPoint(row, ctx.length, -h, h, row.texU(0), row.texV(1));
            ctx.addPoint(row, ctx.length, -h, -h, row.texU(0), row.texV(0));
            ctx.addPoint(row, ctx.length, h, -h, row.texU(1), row.texV(0));
            ctx.addPoint(row, ctx.length, h, h, row.texU(1), row.texV(1));
        }
    }

    private static void bakeTaper(Context ctx, BcLaserRow row, double stripLength, boolean atStart) {
        double h = row.height() / 2;
        double l = stripLength;
        if (atStart) {
            double i = 1 - (stripLength / row.width());
            ctx.setFaceNormal(0, 1, 0);
            ctx.addPoint(row, 0, h, -h, row.texU(i), row.texV(0));
            ctx.addPoint(row, 0, h, h, row.texU(i), row.texV(1));
            ctx.addPoint(row, l, h, h, row.texU(1), row.texV(1));
            ctx.addPoint(row, l, h, -h, row.texU(1), row.texV(0));
            ctx.setFaceNormal(0, -1, 0);
            ctx.addPoint(row, l, -h, -h, row.texU(1), row.texV(0));
            ctx.addPoint(row, l, -h, h, row.texU(1), row.texV(1));
            ctx.addPoint(row, 0, -h, h, row.texU(i), row.texV(1));
            ctx.addPoint(row, 0, -h, -h, row.texU(i), row.texV(0));
            ctx.setFaceNormal(0, 0, -1);
            ctx.addPoint(row, 0, -h, -h, row.texU(i), row.texV(0));
            ctx.addPoint(row, 0, h, -h, row.texU(i), row.texV(1));
            ctx.addPoint(row, l, h, -h, row.texU(1), row.texV(1));
            ctx.addPoint(row, l, -h, -h, row.texU(1), row.texV(0));
            ctx.setFaceNormal(0, 0, 1);
            ctx.addPoint(row, l, -h, h, row.texU(1), row.texV(0));
            ctx.addPoint(row, l, h, h, row.texU(1), row.texV(1));
            ctx.addPoint(row, 0, h, h, row.texU(i), row.texV(1));
            ctx.addPoint(row, 0, -h, h, row.texU(i), row.texV(0));
        } else {
            double ls = ctx.length - stripLength;
            double lb = ctx.length;
            double i = stripLength / row.width();
            ctx.setFaceNormal(0, 1, 0);
            ctx.addPoint(row, ls, h, -h, row.texU(0), row.texV(0));
            ctx.addPoint(row, ls, h, h, row.texU(0), row.texV(1));
            ctx.addPoint(row, lb, h, h, row.texU(i), row.texV(1));
            ctx.addPoint(row, lb, h, -h, row.texU(i), row.texV(0));
            ctx.setFaceNormal(0, -1, 0);
            ctx.addPoint(row, lb, -h, -h, row.texU(i), row.texV(0));
            ctx.addPoint(row, lb, -h, h, row.texU(i), row.texV(1));
            ctx.addPoint(row, ls, -h, h, row.texU(0), row.texV(1));
            ctx.addPoint(row, ls, -h, -h, row.texU(0), row.texV(0));
            ctx.setFaceNormal(0, 0, -1);
            ctx.addPoint(row, ls, -h, -h, row.texU(0), row.texV(0));
            ctx.addPoint(row, ls, h, -h, row.texU(0), row.texV(1));
            ctx.addPoint(row, lb, h, -h, row.texU(i), row.texV(1));
            ctx.addPoint(row, lb, -h, -h, row.texU(i), row.texV(0));
            ctx.setFaceNormal(0, 0, 1);
            ctx.addPoint(row, lb, -h, h, row.texU(i), row.texV(0));
            ctx.addPoint(row, lb, h, h, row.texU(i), row.texV(1));
            ctx.addPoint(row, ls, h, h, row.texU(0), row.texV(1));
            ctx.addPoint(row, ls, -h, h, row.texU(0), row.texV(0));
        }
    }

    private static void bakeMiddle(Context ctx, BcLaserRow[] rows, BcLaserSide side, double startX, int count) {
        double segmentWidth = rows[0].width();
        double h = rows[0].height() / 2;
        double xMin = startX;
        double xMax = startX + segmentWidth;
        for (int i = 0; i < count; i++) {
            BcLaserRow row = rows[i % rows.length];
            double ls = xMin;
            double lb = xMax;
            switch (side) {
                case TOP -> {
                    ctx.setFaceNormal(0, 1, 0);
                    ctx.addPoint(row, ls, h, -h, row.texU(0), row.texV(0));
                    ctx.addPoint(row, ls, h, h, row.texU(0), row.texV(1));
                    ctx.addPoint(row, lb, h, h, row.texU(1), row.texV(1));
                    ctx.addPoint(row, lb, h, -h, row.texU(1), row.texV(0));
                }
                case BOTTOM -> {
                    ctx.setFaceNormal(0, -1, 0);
                    ctx.addPoint(row, lb, -h, -h, row.texU(1), row.texV(0));
                    ctx.addPoint(row, lb, -h, h, row.texU(1), row.texV(1));
                    ctx.addPoint(row, ls, -h, h, row.texU(0), row.texV(1));
                    ctx.addPoint(row, ls, -h, -h, row.texU(0), row.texV(0));
                }
                case LEFT -> {
                    ctx.setFaceNormal(0, 0, -1);
                    ctx.addPoint(row, ls, -h, -h, row.texU(0), row.texV(0));
                    ctx.addPoint(row, ls, h, -h, row.texU(0), row.texV(1));
                    ctx.addPoint(row, lb, h, -h, row.texU(1), row.texV(1));
                    ctx.addPoint(row, lb, -h, -h, row.texU(1), row.texV(0));
                }
                case RIGHT -> {
                    ctx.setFaceNormal(0, 0, 1);
                    ctx.addPoint(row, lb, -h, h, row.texU(1), row.texV(0));
                    ctx.addPoint(row, lb, h, h, row.texU(1), row.texV(1));
                    ctx.addPoint(row, ls, h, h, row.texU(0), row.texV(1));
                    ctx.addPoint(row, ls, -h, h, row.texU(0), row.texV(0));
                }
            }
            xMin += segmentWidth;
            xMax += segmentWidth;
        }
    }

    /** The per-side variation strips of one laser type (legacy {@code CompiledLaserType}'s row filter). */
    private static final class PerSide {
        private final BcLaserRow[][] bySide = new BcLaserRow[BcLaserSide.VALUES.length][];
        private final double middleWidth;

        PerSide(BcLaserRow[] variations) {
            double width = 0;
            for (BcLaserSide side : BcLaserSide.VALUES) {
                java.util.List<BcLaserRow> validRows = new java.util.ArrayList<>();
                for (BcLaserRow row : variations) {
                    for (BcLaserSide inner : row.validSides()) {
                        if (inner == side) {
                            validRows.add(row);
                            break;
                        }
                    }
                }
                if (validRows.isEmpty()) {
                    throw new IllegalArgumentException("No laser variation strip covers side " + side);
                }
                this.bySide[side.ordinal()] = validRows.toArray(new BcLaserRow[0]);
                if (side == BcLaserSide.BOTTOM) {
                    width = this.bySide[side.ordinal()][0].width();
                }
            }
            this.middleWidth = width;
        }

        BcLaserRow[] rows(BcLaserSide side) {
            return this.bySide[side.ordinal()];
        }
    }

    // ------------------------------------------------------------------
    // Bake context (legacy LaserContext)
    // ------------------------------------------------------------------

    private static final class Context {
        private final BcLaserData data;
        private final BlockPos origin;
        @Nullable
        private final Level level;
        private final SpriteGetter sprites;
        private final Matrix4f matrix;
        private final double length;
        private final List<BcLaserQuad> out;
        private final boolean drawBothSides;

        private final Vector4f normal = new Vector4f();
        private final Vector4f point = new Vector4f();
        private float diffuse = 1;

        // the 4-vertex ring of the face currently being assembled (legacy LaserContext fields)
        private final float[] vx = new float[4];
        private final float[] vy = new float[4];
        private final float[] vz = new float[4];
        private final float[] vu = new float[4];
        private final float[] vv = new float[4];
        private final int[] vlight = new int[4];
        private float fnx = 0, fny = 1, fnz = 0;
        private int index = 0;

        Context(BcLaserData data, BlockPos origin, @Nullable Level level, SpriteGetter sprites, Matrix4f matrix,
            double length, List<BcLaserQuad> out) {
            this.data = data;
            this.origin = origin;
            this.level = level;
            this.sprites = sprites;
            this.matrix = matrix;
            this.length = length;
            this.out = out;
            this.drawBothSides = data.doubleFace();
        }

        void setFaceNormal(double nx, double ny, double nz) {
            this.normal.set((float) nx, (float) ny, (float) nz, 0);
            this.matrix.transform(this.normal);
            this.fnx = this.normal.x();
            this.fny = this.normal.y();
            this.fnz = this.normal.z();
            this.diffuse = diffuseLight(this.fnx, this.fny, this.fnz);
        }

        void addPoint(BcLaserRow row, double x, double y, double z, double u, double v) {
            this.point.set((float) x, (float) y, (float) z, 1);
            this.matrix.transform(this.point);
            TextureAtlasSprite sprite = this.sprites.get(row.sprite());
            this.vx[this.index] = this.point.x();
            this.vy[this.index] = this.point.y();
            this.vz[this.index] = this.point.z();
            this.vu[this.index] = sprite.getU((float) u);
            this.vv[this.index] = sprite.getV((float) v);
            this.vlight[this.index] = computeLightmap(
                this.point.x() + this.origin.getX(),
                this.point.y() + this.origin.getY(),
                this.point.z() + this.origin.getZ());
            this.index++;
            if (this.index == 4) {
                this.index = 0;
                int frontArgb = shadeArgb(this.diffuse);
                this.out.add(BcLaserQuad.of(this.vx, this.vy, this.vz, this.vu, this.vv, this.vlight,
                    this.fnx, this.fny, this.fnz, frontArgb));
                if (this.drawBothSides) {
                    float backDiffuse = diffuseLight(-this.fnx, -this.fny, -this.fnz);
                    BcLaserQuad front = this.out.get(this.out.size() - 1);
                    this.out.add(front.reversed(shadeArgb(backDiffuse)));
                }
            }
        }

        private int shadeArgb(float diffuse) {
            int channel = Math.round(Mth.clamp(diffuse, 0, 1) * 255);
            return 0xFF000000 | (channel << 16) | (channel << 8) | channel;
        }

        private int computeLightmap(double x, double y, double z) {
            if (this.level == null) {
                return 0;
            }
            int packed = LevelRenderer.getLightCoords(this.level, BlockPos.containing(x, y, z));
            int minBlockLight = this.data.minBlockLight();
            if (minBlockLight > 0) {
                int block = packed & 0xF;
                if (block < minBlockLight) {
                    packed = (packed & ~0xF) | minBlockLight;
                }
            }
            return packed;
        }
    }

    private BcLaserBaker() {
    }
}
