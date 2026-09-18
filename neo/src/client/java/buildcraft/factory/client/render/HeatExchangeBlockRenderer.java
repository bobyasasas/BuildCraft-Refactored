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
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import buildcraft.core.client.render.BcBoxes;
import buildcraft.factory.BuildCraftFactory;
import buildcraft.factory.blockentity.BcMachineFluids;
import buildcraft.factory.blockentity.HeatExchangeBlockEntity;
import buildcraft.lib.client.render.BcQuad;

/**
 * M4.7: the heat exchange block entity renderer &mdash; the whole visible machine (the MIDDLE pipe section of the
 * legacy tower) comes out of the {@code heat_exchange_static.jsonbc} model (see {@link BcHeatExchangeModels}), plus
 * the two liquid windows of the legacy {@code RenderHeatExchange}: the input tank in the bottom plate
 * {@code [2,0,2]..[14,2,14]} and the output window on the start-section side plate {@code [0,4,4]..[2,12,12]}, each
 * shrunk by 1/64 and filled bottom-up by their tank's fill fraction.
 */
public class HeatExchangeBlockRenderer
    implements BlockEntityRenderer<HeatExchangeBlockEntity, HeatExchangeBlockRenderer.State> {

    /** Render type for the jsonbc geometry (block-atlas cutout, like the engines' baked quads). */
    private static final RenderType CUTOUT = Sheets.cutoutBlockSheet();
    /** Render type for the liquid windows. */
    private static final RenderType TRANSLUCENT = Sheets.translucentBlockSheet();

    /** The legacy jsonbc stem ({@code BcEngineModels}-style, the {@code .jsonbc} suffix is normalised at load). */
    private static final Identifier MODEL_ID = Identifier
        .parse(BuildCraftFactory.MOD_ID + ":models/tile/heat_exchange_static");

    /** The block-atlas sprite lookup (the baked quads carry sprite ids, resolved here at submit time). */
    private final SpriteGetter sprites;

    /** The legacy {@code RenderHeatExchange} window bounds, shrunk by 1/64 (its {@code sr} inset). */
    private static final float SR = 1.0f / 64.0f;
    private static final float TANK_BOTTOM_X0 = 2.0f / 16.0f + SR, TANK_BOTTOM_Y0 = 0.0f + SR,
        TANK_BOTTOM_Z0 = 2.0f / 16.0f + SR;
    private static final float TANK_BOTTOM_X1 = 14.0f / 16.0f - SR, TANK_BOTTOM_Y1 = 2.0f / 16.0f - SR,
        TANK_BOTTOM_Z1 = 14.0f / 16.0f - SR;
    private static final float PLATE_X0 = 0.0f + SR, PLATE_Y0 = 4.0f / 16.0f + SR, PLATE_Z0 = 4.0f / 16.0f + SR;
    private static final float PLATE_X1 = 2.0f / 16.0f - SR, PLATE_Y1 = 12.0f / 16.0f - SR,
        PLATE_Z1 = 12.0f / 16.0f - SR;

    public HeatExchangeBlockRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
        HeatExchangeBlockEntity exchanger, State state, float partialTicks, Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(exchanger, state, partialTicks, cameraPosition, breakProgress);
        state.modelQuads.clear();
        state.windows.clear();
        state.modelQuads.addAll(BcHeatExchangeModels.bake(MODEL_ID, state.lightCoords));
        ClientLevel clientLevel = exchanger.getLevel() instanceof ClientLevel cl ? cl : null;
        if (clientLevel == null) {
            return;
        }
        BlockPos pos = exchanger.getBlockPos();
        addWindow(state, BcFluidWindows.column(exchanger.getTankIn().getFluidStack(),
            BcMachineFluids.fillFraction(exchanger.getTankIn().getAmountAsLong(0),
                HeatExchangeBlockEntity.TANK_CAPACITY),
            TANK_BOTTOM_X0, TANK_BOTTOM_Y0, TANK_BOTTOM_Z0, TANK_BOTTOM_X1, TANK_BOTTOM_Y1, TANK_BOTTOM_Z1,
            exchanger.getBlockState(), clientLevel, pos));
        addWindow(state, BcFluidWindows.column(exchanger.getTankOut().getFluidStack(),
            BcMachineFluids.fillFraction(exchanger.getTankOut().getAmountAsLong(0),
                HeatExchangeBlockEntity.TANK_CAPACITY),
            PLATE_X0, PLATE_Y0, PLATE_Z0, PLATE_X1, PLATE_Y1, PLATE_Z1,
            exchanger.getBlockState(), clientLevel, pos));
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
        if (!state.modelQuads.isEmpty()) {
            submitNodeCollector.submitCustomGeometry(poseStack, CUTOUT, (pose, buffer) -> {
                for (BcHeatExchangeModels.TexturedQuad textured : state.modelQuads) {
                    textured.quad().mapUv(this.sprites.get(textured.sprite())).emit(pose, buffer);
                }
            });
        }
        if (!state.windows.isEmpty()) {
            submitNodeCollector.submitCustomGeometry(poseStack, TRANSLUCENT, (pose, buffer) -> {
                for (BcFluidWindows.Window window : state.windows) {
                    for (BcQuad quad : window.quads()) {
                        quad.mapUv(window.sprite()).emit(pose, buffer);
                    }
                }
            });
        }
    }

    /** M4.7 render state of one heat exchanger: the baked jsonbc quads plus up to two fluid windows. */
    public static class State extends BlockEntityRenderState {
        public final List<BcHeatExchangeModels.TexturedQuad> modelQuads = new ArrayList<>();
        public final List<BcFluidWindows.Window> windows = new ArrayList<>();
    }
}
