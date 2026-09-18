/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.client.render.laser;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import buildcraft.lib.client.render.BcQuad;

/**
 * One horizontal strip of the laser sprite sheet (M4.5 port of legacy
 * {@code buildcraft.lib.client.render.laser.LaserData_BC8.LaserRow}).
 *
 * <p>UVs live in the sprite's own 16&times;16 coordinate space (0..16, exactly the legacy constructor domain); the
 * baker divides them down to the 0..1 space {@link BcQuad} and {@code TextureAtlasSprite#getU/getV} expect. Sprites are
 * referenced by {@link SpriteId} (resolved through the block atlas on every bake) instead of the legacy
 * {@code SpriteHolderRegistry.SpriteHolder}, which has no 26.1.2 counterpart.
 *
 * @param uMin minimum U in the 0..16 sprite space
 * @param vMin minimum V in the 0..16 sprite space
 * @param uMax maximum U in the 0..16 sprite space
 * @param vMax maximum V in the 0..16 sprite space
 * @param validSides the tube sides this strip is drawn on (all of them when the legacy varargs were left empty)
 */
public record BcLaserRow(SpriteId sprite, double uMin, double vMin, double uMax, double vMax,
    BcLaserSide[] validSides) {

    public BcLaserRow(SpriteId sprite, double uMin, double vMin, double uMax, double vMax) {
        this(sprite, uMin, vMin, uMax, vMax, BcLaserSide.VALUES);
    }

    public BcLaserRow {
        validSides = validSides == null || validSides.length == 0 ? BcLaserSide.VALUES : validSides;
    }

    /** Convenience constructor matching the legacy {@code getHolder("buildcraftcore:lasers/...")} call sites: the
     * sprite id is resolved inside the {@code LOCATION_BLOCKS} atlas. */
    public static BcLaserRow of(String namespace, String spritePath, double uMin, double vMin, double uMax,
        double vMax, BcLaserSide... sides) {
        return new BcLaserRow(new SpriteId(TextureAtlas.LOCATION_BLOCKS,
            Identifier.fromNamespaceAndPath(namespace, spritePath)), uMin, vMin, uMax, vMax, sides);
    }

    /** Legacy {@code LaserRow(LaserRow from, ISprite sprite)}: same strip, different sprite (the
     * {@code new LaserType(MARKER_VOLUME_CONNECTED, STRIPES_WRITE)} colour-swap pattern). */
    public BcLaserRow withSprite(SpriteId replacement) {
        return new BcLaserRow(replacement, this.uMin, this.vMin, this.uMax, this.vMax, this.validSides);
    }

    /** Strip width in sprite pixels (the segment length one full UV sweep covers). */
    public double width() {
        return this.uMax - this.uMin;
    }

    /** Strip height in sprite pixels; half of it is the tube's half cross-section. */
    public double height() {
        return this.vMax - this.vMin;
    }

    /** Legacy {@code CompiledLaserRow#texU}: interpolated strip U, normalised to 0..1 for the atlas sprite. */
    public double texU(double between) {
        double u = between == 0 ? this.uMin : between == 1 ? this.uMax : this.uMin * (1 - between) + this.uMax * between;
        return u / 16.0;
    }

    /** Legacy {@code CompiledLaserRow#texV}: interpolated strip V, normalised to 0..1 for the atlas sprite. */
    public double texV(double between) {
        double v = between == 0 ? this.vMin : between == 1 ? this.vMax : this.vMin * (1 - between) + this.vMax * between;
        return v / 16.0;
    }
}
