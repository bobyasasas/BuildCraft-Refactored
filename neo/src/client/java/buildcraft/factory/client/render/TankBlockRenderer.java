/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory.client.render;

import java.util.ArrayList;
import java.util.List;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import buildcraft.core.client.render.BcBoxes;
import buildcraft.factory.blockentity.BcMachineFluids;
import buildcraft.factory.blockentity.TankBlockEntity;
import buildcraft.lib.client.render.BcQuad;

/**
 * M4.7: the tank block entity renderer &mdash; draws the liquid column inside the glass block model (the model keeps
 * rendering; only the contents are custom geometry). One-to-one port of the legacy {@code RenderTank} fluid box:
 * the liquid fills the inner volume {@code [0.13, 0.01, 0.13] .. [0.86, 0.99, 0.86]} from the bottom up to the tank's
 * fill fraction, so a third-full tank shows a third-height column (the screenshot contract of the M4.7 evidence run).
 *
 * <p>v1 slice note: the legacy renderer culls the faces two stacked (same-fluid) tanks share; here the inset boxes of
 * neighbouring blocks never coincide, so all six faces are always drawn instead (no z-fighting either way; the
 * stacked-tank fluid balancing itself is a later milestone's work, see {@code TankBlockEntity}).
 */
public class TankBlockRenderer implements BlockEntityRenderer<TankBlockEntity, TankBlockRenderer.State> {

    /** Render type for the liquid column (the pipe renderer draws its fluids translucent). */
    private static final RenderType TRANSLUCENT = Sheets.translucentBlockSheet();

    /** The legacy {@code RenderTank} inner liquid bounds (block-relative). */
    private static final float MIN_X = 0.13f, MIN_Y = 0.01f, MIN_Z = 0.13f;
    private static final float MAX_X = 0.86f, MAX_Y = 0.99f, MAX_Z = 0.86f;

    public TankBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
        TankBlockEntity tank, State state, float partialTicks, Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(tank, state, partialTicks, cameraPosition, breakProgress);
        state.windows.clear();
        ClientLevel clientLevel = tank.getLevel() instanceof ClientLevel cl ? cl : null;
        if (clientLevel == null) {
            return;
        }
        BlockPos pos = tank.getBlockPos();
        BcFluidWindows.Window window = BcFluidWindows.column(tank.getTank().getFluidStack(),
            BcMachineFluids.fillFraction(tank.getTank().getAmountAsLong(0), TankBlockEntity.CAPACITY),
            MIN_X, MIN_Y, MIN_Z, MAX_X, MAX_Y, MAX_Z, tank.getBlockState(), clientLevel, pos);
        if (window != null) {
            BcBoxes.lightAll(window.quads(), state);
            state.windows.add(window);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
        CameraRenderState cameraRenderState) {
        if (state.windows.isEmpty()) {
            return;
        }
        submitNodeCollector.submitCustomGeometry(poseStack, TRANSLUCENT, (pose, buffer) -> {
            for (BcFluidWindows.Window window : state.windows) {
                for (BcQuad quad : window.quads()) {
                    quad.mapUv(window.sprite()).emit(pose, buffer);
                }
            }
        });
    }

    /** M4.7 render state of one tank: at most one fluid window (empty when the tank is empty). */
    public static class State extends BlockEntityRenderState {
        public final List<BcFluidWindows.Window> windows = new ArrayList<>();
    }
}
