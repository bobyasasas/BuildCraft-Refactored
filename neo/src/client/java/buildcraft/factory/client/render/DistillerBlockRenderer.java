/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory.client.render;

import java.util.ArrayList;
import java.util.List;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import buildcraft.core.client.render.BcBoxes;
import buildcraft.factory.blockentity.BcMachineFluids;
import buildcraft.factory.blockentity.DistillerBlockEntity;
import buildcraft.lib.client.render.BcQuad;

/**
 * M4.7: the distiller block entity renderer &mdash; fills the three tank windows of the static block model with the
 * tanks' contents (the model itself keeps rendering; only the windows are custom geometry). The window boxes are the
 * legacy {@code RenderDistiller} geometry: {@code tankIn} {@code [0,0,4]..[8,16,12]}, {@code gasOut}
 * {@code [8,8,0]..[16,16,16]} and {@code liquidOut} {@code [8,0,0]..[16,8,16]}, each shrunk by 1/64 of a block so
 * the liquid sits just inside the model's frames, each filled bottom-up by its tank's fill fraction.
 *
 * <p>v1 slice note: the legacy machine has no facing property and its windows are on the north/east faces (the
 * baseline default orientation) &mdash; this renderer draws them unrotated, exactly like the baseline block model
 * does.
 */
public class DistillerBlockRenderer implements BlockEntityRenderer<DistillerBlockEntity, DistillerBlockRenderer.State> {

    /** Render type for the liquid windows (the pipe renderer draws its fluids translucent). */
    private static final RenderType TRANSLUCENT = Sheets.translucentBlockSheet();

    /** The legacy {@code RenderDistiller} window bounds, shrunk by 1/64 (its {@code sr} inset). */
    private static final float SR = 1.0f / 64.0f;
    private static final float TANK_IN_X0 = 0.0f + SR, TANK_IN_Y0 = 0.0f + SR, TANK_IN_Z0 = 4.0f / 16.0f + SR;
    private static final float TANK_IN_X1 = 8.0f / 16.0f - SR, TANK_IN_Y1 = 1.0f - SR, TANK_IN_Z1 = 12.0f / 16.0f - SR;
    private static final float GAS_OUT_X0 = 8.0f / 16.0f + SR, GAS_OUT_Y0 = 8.0f / 16.0f + SR, GAS_OUT_Z0 = 0.0f + SR;
    private static final float GAS_OUT_X1 = 1.0f - SR, GAS_OUT_Y1 = 1.0f - SR, GAS_OUT_Z1 = 1.0f - SR;
    private static final float LIQ_OUT_X0 = 8.0f / 16.0f + SR, LIQ_OUT_Y0 = 0.0f + SR, LIQ_OUT_Z0 = 0.0f + SR;
    private static final float LIQ_OUT_X1 = 1.0f - SR, LIQ_OUT_Y1 = 8.0f / 16.0f - SR, LIQ_OUT_Z1 = 1.0f - SR;

    public DistillerBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
        DistillerBlockEntity distiller, State state, float partialTicks, Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(distiller, state, partialTicks, cameraPosition, breakProgress);
        state.windows.clear();
        ClientLevel clientLevel = distiller.getLevel() instanceof ClientLevel cl ? cl : null;
        if (clientLevel == null) {
            return;
        }
        BlockPos pos = distiller.getBlockPos();
        addWindow(state, BcFluidWindows.column(distiller.getTankIn().getFluidStack(),
            BcMachineFluids.fillFraction(distiller.getTankIn().getAmountAsLong(0), DistillerBlockEntity.TANK_CAPACITY),
            TANK_IN_X0, TANK_IN_Y0, TANK_IN_Z0, TANK_IN_X1, TANK_IN_Y1, TANK_IN_Z1,
            distiller.getBlockState(), clientLevel, pos));
        addWindow(state, BcFluidWindows.column(distiller.getTankGasOut().getFluidStack(),
            BcMachineFluids.fillFraction(distiller.getTankGasOut().getAmountAsLong(0),
                DistillerBlockEntity.TANK_CAPACITY),
            GAS_OUT_X0, GAS_OUT_Y0, GAS_OUT_Z0, GAS_OUT_X1, GAS_OUT_Y1, GAS_OUT_Z1,
            distiller.getBlockState(), clientLevel, pos));
        addWindow(state, BcFluidWindows.column(distiller.getTankLiquidOut().getFluidStack(),
            BcMachineFluids.fillFraction(distiller.getTankLiquidOut().getAmountAsLong(0),
                DistillerBlockEntity.TANK_CAPACITY),
            LIQ_OUT_X0, LIQ_OUT_Y0, LIQ_OUT_Z0, LIQ_OUT_X1, LIQ_OUT_Y1, LIQ_OUT_Z1,
            distiller.getBlockState(), clientLevel, pos));
        for (BcFluidWindows.Window window : state.windows) {
            BcBoxes.lightAll(window.quads(), state);
        }
    }

    private static void addWindow(State state, BcFluidWindows.Window window) {
        if (window != null) {
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

    /** M4.7 render state of one distiller: up to three fluid windows (input, gas out, liquid out). */
    public static class State extends BlockEntityRenderState {
        public final List<BcFluidWindows.Window> windows = new ArrayList<>();
    }
}
