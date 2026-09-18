/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.client.render.laser;

/**
 * The shared laser type tables (M4.5 port of legacy {@code buildcraft.core.client.BuildCraftLaserManager} plus the
 * quarry's own {@code FRAME}/{@code FRAME_BOTTOM}/{@code DRILL} types from legacy
 * {@code buildcraft.builders.client.render.RenderQuarry}). Strip layouts are the legacy ones, sprite for sprite; the
 * sprites themselves already live under {@code assets/buildcraftcore/textures/lasers/} and
 * {@code assets/buildcraftbuilders/textures/block/} and are stitched into the block atlas through the
 * {@code lasers/} directory source in {@code assets/minecraft/atlases/blocks.json}.
 */
public final class BcLaserTypes {

    private static final String CORE = "buildcraftcore";
    private static final String BUILDERS = "buildcraftbuilders";

    public static final BcLaserType MARKER_VOLUME_CONNECTED;
    public static final BcLaserType MARKER_VOLUME_POSSIBLE;
    public static final BcLaserType MARKER_VOLUME_SIGNAL;

    public static final BcLaserType MARKER_PATH_CONNECTED;
    public static final BcLaserType MARKER_PATH_POSSIBLE;

    public static final BcLaserType MARKER_DEFAULT_POSSIBLE;

    public static final BcLaserType STRIPES_READ;
    public static final BcLaserType STRIPES_WRITE;
    public static final BcLaserType STRIPES_WRITE_DIRECTION;

    /** The quarry power lasers, red to blue with rising power (legacy {@code POWER_LOW..POWER_FULL}). */
    public static final BcLaserType POWER_LOW;
    public static final BcLaserType POWER_MED;
    public static final BcLaserType POWER_HIGH;
    public static final BcLaserType POWER_FULL;
    public static final BcLaserType[] POWERS;

    /** Quarry frame rail strips (legacy {@code RenderQuarry.FRAME}). */
    public static final BcLaserType FRAME;
    /** Same strip with a shaped end cap (legacy {@code RenderQuarry.FRAME_BOTTOM}). */
    public static final BcLaserType FRAME_BOTTOM;
    /** The drill head column (legacy {@code RenderQuarry.DRILL}, sprite {@code block/quarry/drill}). */
    public static final BcLaserType DRILL;

    static {
        {
            BcLaserType connected = markerVolumeConnected("lasers/marker_volume_connected");
            MARKER_VOLUME_CONNECTED = connected;
            MARKER_VOLUME_SIGNAL = connected.withSprite(sprite(CORE, "lasers/marker_volume_signal"));
        }
        MARKER_VOLUME_POSSIBLE = markerVolumePossible("lasers/marker_volume_possible");
        MARKER_PATH_CONNECTED = markerPathConnected("lasers/marker_path_connected");
        MARKER_PATH_POSSIBLE = markerVolumePossible("lasers/marker_path_possible");
        MARKER_DEFAULT_POSSIBLE = markerVolumePossible("lasers/marker_default_possible");

        STRIPES_READ = MARKER_VOLUME_CONNECTED.withSprite(sprite(CORE, "lasers/stripes_read"));
        STRIPES_WRITE = MARKER_VOLUME_CONNECTED.withSprite(sprite(CORE, "lasers/stripes_write"));
        STRIPES_WRITE_DIRECTION = MARKER_PATH_CONNECTED.withSprite(sprite(CORE, "lasers/stripes_write_direction"));

        BcLaserType possible = MARKER_VOLUME_POSSIBLE;
        POWER_LOW = possible.withSprite(sprite(CORE, "lasers/power_low"));
        POWER_MED = possible.withSprite(sprite(CORE, "lasers/power_med"));
        POWER_HIGH = possible.withSprite(sprite(CORE, "lasers/power_high"));
        POWER_FULL = possible.withSprite(sprite(CORE, "lasers/power_full"));
        POWERS = new BcLaserType[] { POWER_LOW, POWER_MED, POWER_HIGH, POWER_FULL };

        {
            BcLaserRow capStart = BcLaserRow.of(BUILDERS, "block/frame/default", 0, 0, 0, 0);
            BcLaserRow[] middle = { BcLaserRow.of(BUILDERS, "block/frame/default", 0, 4, 16, 12) };
            BcLaserRow end = BcLaserRow.of(BUILDERS, "block/frame/default", 0, 4, 16, 12);
            BcLaserRow capEnd = BcLaserRow.of(BUILDERS, "block/frame/default", 0, 0, 0, 0);
            FRAME = new BcLaserType(capStart, null, middle, end, capEnd);
        }
        {
            BcLaserRow capStart = BcLaserRow.of(BUILDERS, "block/frame/default", 0, 0, 0, 0);
            BcLaserRow[] middle = { BcLaserRow.of(BUILDERS, "block/frame/default", 0, 4, 16, 12) };
            BcLaserRow end = BcLaserRow.of(BUILDERS, "block/frame/default", 0, 4, 16, 12);
            BcLaserRow capEnd = BcLaserRow.of(BUILDERS, "block/frame/default", 4, 4, 12, 12);
            FRAME_BOTTOM = new BcLaserType(capStart, null, middle, end, capEnd);
        }
        {
            BcLaserRow capStart = BcLaserRow.of(BUILDERS, "block/quarry/drill", 6, 0, 10, 4);
            BcLaserRow[] middle = { BcLaserRow.of(BUILDERS, "block/quarry/drill", 0, 0, 16, 4) };
            BcLaserRow capEnd = BcLaserRow.of(BUILDERS, "block/quarry/drill", 6, 0, 10, 4);
            DRILL = new BcLaserType(capStart, null, middle, null, capEnd);
        }
    }

    /** Legacy {@code MARKER_VOLUME_CONNECTED} strip layout on the given sprite. */
    private static BcLaserType markerVolumeConnected(String spritePath) {
        BcLaserRow capStart = BcLaserRow.of(CORE, spritePath, 0, 0, 2, 2);
        BcLaserRow start = BcLaserRow.of(CORE, spritePath, 0, 0, 16, 2);
        BcLaserRow[] middle = {
                BcLaserRow.of(CORE, spritePath, 0, 2, 16, 4), BcLaserRow.of(CORE, spritePath, 0, 4, 16, 6),
                BcLaserRow.of(CORE, spritePath, 0, 6, 16, 8), BcLaserRow.of(CORE, spritePath, 0, 8, 16, 10),
                BcLaserRow.of(CORE, spritePath, 0, 10, 16, 12), BcLaserRow.of(CORE, spritePath, 0, 12, 16, 14) };
        BcLaserRow end = BcLaserRow.of(CORE, spritePath, 0, 14, 16, 16);
        BcLaserRow capEnd = BcLaserRow.of(CORE, spritePath, 14, 14, 16, 16);
        return new BcLaserType(capStart, start, middle, end, capEnd);
    }

    /** Legacy {@code MARKER_VOLUME_POSSIBLE} strip layout on the given sprite. */
    private static BcLaserType markerVolumePossible(String spritePath) {
        BcLaserRow capStart = BcLaserRow.of(CORE, spritePath, 0, 0, 1, 1);
        BcLaserRow start = BcLaserRow.of(CORE, spritePath, 0, 0, 16, 1);
        BcLaserRow[] middle = {
                BcLaserRow.of(CORE, spritePath, 0, 1, 16, 2), BcLaserRow.of(CORE, spritePath, 0, 2, 16, 3),
                BcLaserRow.of(CORE, spritePath, 0, 3, 16, 4), BcLaserRow.of(CORE, spritePath, 0, 4, 16, 5),
                BcLaserRow.of(CORE, spritePath, 0, 5, 16, 6), BcLaserRow.of(CORE, spritePath, 0, 6, 16, 7),
                BcLaserRow.of(CORE, spritePath, 0, 7, 16, 8), BcLaserRow.of(CORE, spritePath, 0, 8, 16, 9),
                BcLaserRow.of(CORE, spritePath, 0, 9, 16, 10), BcLaserRow.of(CORE, spritePath, 0, 10, 16, 11),
                BcLaserRow.of(CORE, spritePath, 0, 11, 16, 12), BcLaserRow.of(CORE, spritePath, 0, 12, 16, 13),
                BcLaserRow.of(CORE, spritePath, 0, 13, 16, 14), BcLaserRow.of(CORE, spritePath, 0, 14, 16, 15) };
        BcLaserRow end = BcLaserRow.of(CORE, spritePath, 0, 15, 16, 16);
        BcLaserRow capEnd = BcLaserRow.of(CORE, spritePath, 15, 15, 16, 16);
        return new BcLaserType(capStart, start, middle, end, capEnd);
    }

    /** Legacy {@code MARKER_PATH_CONNECTED} strip layout on the given sprite (the two middle strips are
     * side-specific: top/bottom vs left/right). */
    private static BcLaserType markerPathConnected(String spritePath) {
        BcLaserRow capStart = BcLaserRow.of(CORE, spritePath, 0, 0, 3, 3);
        BcLaserRow start = BcLaserRow.of(CORE, spritePath, 0, 0, 16, 3);
        BcLaserRow[] middle = {
                BcLaserRow.of(CORE, spritePath, 0, 4, 16, 7, BcLaserSide.TOP, BcLaserSide.BOTTOM),
                BcLaserRow.of(CORE, spritePath, 0, 8, 16, 11, BcLaserSide.LEFT, BcLaserSide.RIGHT) };
        BcLaserRow end = BcLaserRow.of(CORE, spritePath, 0, 12, 16, 15);
        BcLaserRow capEnd = BcLaserRow.of(CORE, spritePath, 13, 12, 16, 15);
        return new BcLaserType(capStart, start, middle, end, capEnd);
    }

    private static net.minecraft.client.resources.model.sprite.SpriteId sprite(String namespace, String path) {
        return new net.minecraft.client.resources.model.sprite.SpriteId(
            net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS,
            net.minecraft.resources.Identifier.fromNamespaceAndPath(namespace, path));
    }

    private BcLaserTypes() {
    }
}
