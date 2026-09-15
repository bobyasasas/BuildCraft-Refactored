/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy.client.sprite;

import buildcraft.lib.client.sprite.AtlasSpriteSwappable;
import buildcraft.lib.fluid.BCFluid;
import net.minecraft.resources.ResourceLocation;

@Deprecated(forRemoval = true)
public class AtlasSpriteFluid extends AtlasSpriteSwappable {
    final ResourceLocation fromName;
    final BCFluid fluid;
    final int colourLight, colourDark;

    public AtlasSpriteFluid(String baseName, ResourceLocation fromName, BCFluid fluid) {
        super(null, null, 0, 0, 0, 0);
        throw new RuntimeException("");
    }

//    @Override

//            // frameData[0] is mipmap 0

    private void recolourPixel(int[] pixels, int i) {
        int rgba = pixels[i];
        int r = recolourSubPixel(rgba, 0);
        int g = recolourSubPixel(rgba, 8);
        int b = recolourSubPixel(rgba, 16);
        int a = 0xFF;// recolourSubPixel(rgba, 24);
        pixels[i] = (a << 24) | (b << 16) | (g << 8) | r;
    }

    private int recolourSubPixel(int rgba, int offset) {
        int data = (rgba >>> offset) & 0xFF;
        int dark = (colourDark >>> offset) & 0xFF;
        int light = (colourLight >>> offset) & 0xFF;
        return (dark * (256 - data) + light * data) / 256;
    }
}
