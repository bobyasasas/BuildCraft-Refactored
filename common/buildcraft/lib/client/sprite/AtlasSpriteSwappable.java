/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.sprite;

import buildcraft.lib.BCLibConfig;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

/** Provides the basic implementation for */
@Deprecated(forRemoval = true)
public abstract class AtlasSpriteSwappable extends TextureAtlasSprite {
    private TextureAtlasSprite current;
    private boolean needsSwapping = true;

    public AtlasSpriteSwappable(ResourceLocation atlasLocation, SpriteContents contents, int atlasWidth, int atlasHeight, int spriteX, int spriteY) {
        super(atlasLocation, contents, atlasWidth, atlasHeight, spriteX, spriteY);
        if (!BCLibConfig.useSwappableSprites) {
            throw new IllegalStateException(
                    "The user has disabled swappable sprites but some code still called it's constructor anyway!\n"
                            + "(Note that this is a *mod* bug, not a user configuration issue - there are legitimate reasons\n"
                            + "to disabled swappable sprites normally, like for optifine compat)");
        }
    }
//    @Override

//    @Override
//        // MAPPING: func_194340_a: Profiler.startSection
//            TextureUtil.uploadTextureMipmap(current.getFrameTextureData(0), current.getIconWidth(),


//    /** Actually loads the given location. Note that subclasses should override this, and possibly call
//     * {@link #loadSprite(IResourceManager, String, ResourceLocation, boolean)} to load all of the possible variants. */
//    @Override
//    public boolean load(ResourceManager manager, ResourceLocation location,


//    public static TextureAtlasSprite loadSprite(ResourceManager manager, String name, ResourceLocation location,
//        // Load the initial variant
//            // Copied almost directly from TextureMap.
//                // Do the same as forge - track the missing texture for later rather than printing out the error.

//    @Override

//    @Override

    // Overrides

//    @Override

//    @Override

//    @Override

//    @Override
//        // NO-OP

    @Override
    public String toString() {
        return getClass().getSimpleName() + super.toString();
    }
}
