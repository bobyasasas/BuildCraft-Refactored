/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.client.render;

import java.util.ArrayList;
import java.util.List;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import buildcraft.core.blockentity.EngineVisual;
import buildcraft.lib.client.render.BcQuad;

/**
 * M4.4: the jsonbc-driven engine block entity renderer, shared by all six engines (stone, wood, creative, iron, rf and
 * the MJ dynamo). One-to-one port of the legacy {@code RenderEngine_BC8} render model: the whole engine (static base +
 * piston trunk + chamber) comes out of the {@code .jsonbc} variable model (see {@link BcEngineModels}), animated by the
 * BE's {@link EngineVisual} state &mdash; the block models are particle-only (the legacy {@code builtin/entity}
 * counterpart), so this renderer draws everything.
 *
 * <p>Pipeline (verified against the 26.1.2 client sources, see {@code neo/docs/render-pipeline-26.1.2.md}):
 * <ul>
 * <li><b>extract</b> ({@link #extractRenderState}): refreshes the model variables from {@link EngineVisual} and bakes
 * the frame's quads against the ambient light read from the open cell above the engine (see the comment in
 * {@link #extractRenderState} for why the block's own cell cannot be used).</li>
 * <li><b>submit</b> ({@link #submit}): emits them through {@link SubmitNodeCollector#submitCustomGeometry} into the
 * cutout block sheet. The pose stack origin is the block's (0,0,0) corner ({@code LevelRenderer#submitBlockEntities}
 * translates by {@code blockPos - cameraPos}); no rotation is applied because the jsonbc
 * {@code builtin:rotate_facing} rule already rotates the quads onto the output facing.</li>
 * </ul>
 *
 * <p>Piston animation: driven by {@link EngineVisual#getProgressClient} (the legacy client-side progress counter the
 * block entities advance themselves), with the trunk texture and its emitted light following
 * {@link EngineVisual#getPowerStage()}.
 *
 * @param <T> the engine block entity type (the slice {@code EngineBlockEntity} family or the stone engine)
 */
public class EngineBlockRenderer<T extends BlockEntity & EngineVisual>
    implements BlockEntityRenderer<T, EngineBlockRenderer.State> {

    /** Render type for block-atlas-textured custom geometry (cutout + cull, like baked block quads). */
    private static final RenderType RENDER_TYPE = Sheets.cutoutBlockSheet();

    private final SpriteGetter sprites;
    /** The jsonbc model id this renderer instance draws (one renderer per engine block entity type). */
    private final Identifier modelId;

    public EngineBlockRenderer(BlockEntityRendererProvider.Context context, Identifier modelId) {
        this.sprites = context.sprites();
        this.modelId = modelId;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
        T engine, State state, float partialTicks, Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(engine, state, partialTicks, cameraPosition, breakProgress);
        state.quads.clear();
        // The engine block is a full cube, and full cubes store no light inside their own cell (the light engine
        // keeps opaque-block cells at 0) — the baseline engine registered with {@code noOcclusion()}
        // ({@code BCCoreBlocks#registerEngine}, 8.0.x), which let the legacy BER's {@code lightc} parameter see the
        // surrounding light. Reading {@code state.lightCoords} (light at the block's own position, filled by
        // {@link BlockEntityRenderState#extractBase}) therefore gave every engine the dark lightmap texel and
        // rendered them near-black (M4.18b). The open cell above the engine sees the same ambient light the
        // baseline's non-occluded cell did, so bake against that instead.
        state.lightCoords = engine.getLevel() != null
            ? LevelRenderer.getLightCoords(engine.getLevel(), state.blockPos.above())
            : 15728880;
        state.quads.addAll(BcEngineModels.bake(this.modelId, engine.getProgressClient(partialTicks),//
            engine.getPowerStage(), engine.getOutputFacing(), state.lightCoords));
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
        CameraRenderState cameraRenderState) {
        if (state.quads.isEmpty()) {
            return;
        }
        submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
            for (BcEngineModels.TexturedQuad textured : state.quads) {
                TextureAtlasSprite sprite = this.sprites.get(textured.sprite());
                BcQuad quad = textured.quad().mapUv(sprite);
                quad.emit(pose, buffer);
            }
        });
    }

    /** M4.4 render state of one engine: the extract-baked quads of the current frame (empty only if the model failed
     * to load &mdash; the engine jsonbc always bakes at least the base cuboid). */
    public static class State extends BlockEntityRenderState {
        public final List<BcEngineModels.TexturedQuad> quads = new ArrayList<>();
    }
}
