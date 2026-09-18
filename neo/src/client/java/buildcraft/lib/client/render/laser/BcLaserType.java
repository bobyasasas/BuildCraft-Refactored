/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.client.render.laser;

import org.jspecify.annotations.Nullable;

/**
 * The five sprite strips that make up one kind of laser (M4.5 port of legacy
 * {@code buildcraft.lib.client.render.laser.LaserData_BC8.LaserType}): end caps plus the optional start/end taper rows
 * and the repeating middle variations. Layout, in local laser space (+X is forward):
 *
 * <pre>
 * [capStart][start ... variations ... end][capEnd]
 * </pre>
 */
public record BcLaserType(BcLaserRow capStart, @Nullable BcLaserRow start, BcLaserRow[] variations,
    @Nullable BcLaserRow end, BcLaserRow capEnd) {

    /** Legacy {@code LaserType(LaserType from, SpriteHolder replacementSprite)}: re-skins every strip of an existing
     * type (how {@code STRIPES_WRITE} reuses the marker-volume layout with a different sprite). */
    public BcLaserType withSprite(net.minecraft.client.resources.model.sprite.SpriteId replacement) {
        BcLaserRow[] newVariations = new BcLaserRow[this.variations.length];
        for (int i = 0; i < this.variations.length; i++) {
            newVariations[i] = this.variations[i].withSprite(replacement);
        }
        return new BcLaserType(
            this.capStart.withSprite(replacement),
            this.start == null ? null : this.start.withSprite(replacement),
            newVariations,
            this.end == null ? null : this.end.withSprite(replacement),
            this.capEnd.withSprite(replacement));
    }
}
