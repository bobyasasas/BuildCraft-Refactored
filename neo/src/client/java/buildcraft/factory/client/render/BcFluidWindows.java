/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory.client.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import buildcraft.core.client.render.BcBoxes;
import buildcraft.lib.client.render.BcQuad;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * M4.7: the fluid window helper shared by the factory machine renderers &mdash; builds the six faces of one liquid
 * box (block-relative coordinates) textured with the fluid's still sprite and tinted with its in-world tint, exactly
 * the pipeline the pipe renderer's fluid column uses ({@code PipeHolderBlockRenderer}, via the vanilla fluid-state
 * model set). The sprite rides along in the returned {@link Window} because UVs can only be mapped at submit time,
 * once the atlas sprite is resolved.
 */
public final class BcFluidWindows {

    /** One built window: the fluid's atlas sprite plus its tinted (unlit, un-UV-mapped) faces. */
    public record Window(TextureAtlasSprite sprite, List<BcQuad> quads) {
    }

    /**
     * Builds the liquid box {@code [x0,y0,z0] .. [x1,y1,z1]} for the given fluid, or null for an empty tank. The
     * block state/level/position triple feeds the tint source (water tints per biome; most fluids come back untinted,
     * which keeps the vertex colour white).
     */
    public static @Nullable Window window(FluidStack fluid, float x0, float y0, float z0, float x1, float y1,
        float z1, BlockState state, @Nullable ClientLevel level, BlockPos pos) {
        if (fluid.isEmpty()) {
            return null;
        }
        var fluidModel = Minecraft.getInstance()
            .getModelManager()
            .getFluidStateModelSet()
            .get(fluid.getFluid().defaultFluidState());
        TextureAtlasSprite sprite = fluidModel.stillMaterial().sprite();
        List<BcQuad> quads = BcBoxes.box(x0, y0, z0, x1, y1, z1);
        int tint = fluidModel.tintSource() != null && level != null
            ? fluidModel.tintSource().colorInWorld(state, level, pos)
            : -1;
        if (tint != -1) {
            quads = new ArrayList<>(quads.stream().map(quad -> quad.multiplyColor(tint)).toList());
        }
        return new Window(sprite, quads);
    }

    /** The liquid column of one tank window: the box between {@code yBottom} and {@code yTop}, filled from the bottom
     * up to the fill fraction (the legacy {@code FluidRenderer} behaviour). */
    public static @Nullable Window column(FluidStack fluid, float fillFraction, float x0, float yBottom, float z0,
        float x1, float yTop, float z1, BlockState state, @Nullable ClientLevel level, BlockPos pos) {
        return window(fluid, x0, yBottom, z0, x1, yBottom + (yTop - yBottom) * fillFraction, z1, state, level, pos);
    }

    private BcFluidWindows() {
    }
}
