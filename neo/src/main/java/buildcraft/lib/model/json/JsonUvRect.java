/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.model.json;

/**
 * Minimal port of the legacy {@code buildcraft.lib.client.model.ModelUtil.UvFaceData} for the jsonbc parser (M4.4):
 * the current {@code minU}/{@code maxU}/{@code minV}/{@code maxV} of one texture face, normalised to 0..1 (jsonbc
 * authors them in 0..16 texture-pixel units and the parser divides by 16, exactly like the legacy
 * {@code UvFaceData.from16} callers did).
 */
public final class JsonUvRect {

    public float minU, maxU, minV, maxV;

    public JsonUvRect() {
        this(0, 0, 1, 1);
    }

    public JsonUvRect(double minU, double minV, double maxU, double maxV) {
        this.minU = (float) minU;
        this.minV = (float) minV;
        this.maxU = (float) maxU;
        this.maxV = (float) maxV;
    }

    public JsonUvRect(JsonUvRect from) {
        this(from.minU, from.minV, from.maxU, from.maxV);
    }

    /** Maps this (a sub-rect) into the given parent rect, legacy {@code UvFaceData#andSub}. */
    public JsonUvRect andSub(JsonUvRect sub) {
        float sizeU = this.maxU - this.minU;
        float sizeV = this.maxV - this.minV;
        return new JsonUvRect(//
            this.minU + sub.minU * sizeU, //
            this.minV + sub.minV * sizeV, //
            this.minU + sub.maxU * sizeU, //
            this.minV + sub.maxV * sizeV);
    }

    /** Maps this face's authored 0..1 rect into the parent texture's rect, legacy {@code UvFaceData#inParent}. */
    public JsonUvRect inParent(JsonUvRect parent) {
        return parent.andSub(this);
    }

    /** Converts 0..16 texture-pixel units into a clamped 0..1 rect (legacy {@code JsonTexture}'s object-form clamp). */
    public static JsonUvRect from16Clamped(double minU, double minV, double maxU, double maxV) {
        return new JsonUvRect(//
            clamp01(minU / 16.0), //
            clamp01(minV / 16.0), //
            clamp01(maxU / 16.0), //
            clamp01(maxV / 16.0));
    }

    private static double clamp01(double value) {
        return value < 0 ? 0 : value > 1 ? 1 : value;
    }

    @Override
    public String toString() {
        return "[ " + this.minU * 16 + ", " + this.minV * 16 + ", " + this.maxU * 16 + ", " + this.maxV * 16 + " ]";
    }
}
