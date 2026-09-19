/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders.client.render;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import org.jspecify.annotations.Nullable;
import buildcraft.builders.blockentity.ConstructionMarkerBlockEntity;
import buildcraft.builders.marker.MarkerPair;
import buildcraft.core.client.render.BcBoxes;
import buildcraft.lib.client.render.BcQuad;
import org.slf4j.Logger;

/**
 * M4.17 construction marker block entity renderer: draws the pair's box frame (the legacy
 * {@code RenderMarker}/volume-marker box outline in miniature). All geometry rides the same {@link BcBoxes} +
 * {@code submitCustomGeometry} path as the {@link FillerBlockRenderer}, tinted construction-marker blue.
 *
 * <p>Everything drawn here comes through the BE's update tag (the Beacon-pattern sync of
 * {@link ConstructionMarkerBlockEntity}): a frame appearing on the client is itself the proof that the pairing state
 * ({@code bc_peer}/{@code bc_box}) reached the client.
 */
public class ConstructionMarkerRenderer
    implements BlockEntityRenderer<ConstructionMarkerBlockEntity, ConstructionMarkerRenderer.State> {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Plain white block sprite, tinted per part (one atlas sprite keeps the submit group single). */
    private static final SpriteId WHITE_CONCRETE = new SpriteId(
        TextureAtlas.LOCATION_BLOCKS, Identifier.withDefaultNamespace("block/white_concrete"));

    /** Render type for block-atlas-textured custom geometry (cutout + cull, like baked block quads). */
    private static final RenderType RENDER_TYPE = Sheets.cutoutBlockSheet();

    /** Frame edge cross-section, in block units (1.5/16 - a hair slimmer than the filler's frame). */
    private static final float FRAME_T = 1.5f / 16.0f;
    /** Outward inflation so the frame does not z-fight with the neighbour block faces it sits on. */
    private static final float FRAME_EPSILON = 1.0f / 64.0f;
    /** Light construction-blue tint of the pair box frame. */
    private static final int FRAME_TINT = 0xFF7EC0F0;

    /** Positions whose pair already got a "marker synced" log line (the M417 sync evidence). */
    private final Set<BlockPos> loggedSync = new HashSet<>();
    /** Block atlas sprite accessor, resolved once per renderer instance (the M2.7b BER pattern). */
    private final SpriteGetter sprites;

    public ConstructionMarkerRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
        ConstructionMarkerBlockEntity marker, State state, float partialTicks, Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(marker, state, partialTicks, cameraPosition, breakProgress);
        state.geometry.clear();
        state.hasPair = marker.getPair() != null;
        if (!state.hasPair) {
            return;
        }
        BlockPos pos = marker.getBlockPos();
        if (this.loggedSync.add(pos)) {
            LOGGER.info("[M417] marker at {} synced: peer={}, box={}", pos, marker.getPeerPos(),
                marker.getPair() == null ? "none" : marker.getPair().sizeSummary());
        }
        MarkerPair pair = marker.getPair();
        // block-relative frame box, inflated a hair outward (see FRAME_EPSILON)
        float x0 = pair.min().getX() - pos.getX() - FRAME_EPSILON;
        float y0 = pair.min().getY() - pos.getY() - FRAME_EPSILON;
        float z0 = pair.min().getZ() - pos.getZ() - FRAME_EPSILON;
        float x1 = pair.max().getX() - pos.getX() + 1 + FRAME_EPSILON;
        float y1 = pair.max().getY() - pos.getY() + 1 + FRAME_EPSILON;
        float z1 = pair.max().getZ() - pos.getZ() + 1 + FRAME_EPSILON;
        List<BcQuad> frame = BcBoxes.frame(x0, y0, z0, x1, y1, z1, FRAME_T);
        BcBoxes.lightAll(frame, state);
        for (int i = 0; i < frame.size(); i++) {
            frame.set(i, frame.get(i).multiplyColor(FRAME_TINT));
        }
        state.geometry.addAll(frame);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
        CameraRenderState cameraRenderState) {
        // Block-relative coordinates: LevelRenderer#submitBlockEntities already translated the pose to the block's
        // (0,0,0) corner, no per-position transform needed here.
        if (state.geometry.isEmpty()) {
            return;
        }
        TextureAtlasSprite sprite = this.sprites.get(WHITE_CONCRETE);
        submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
            for (BcQuad quad : state.geometry) {
                quad.mapUv(sprite).emit(pose, buffer);
            }
        });
    }

    /** M4.17 render state of one construction marker: the extracted pair mirror plus the built frame geometry. */
    public static class State extends BlockEntityRenderState {
        /** True while the marker has a live pair (the frame keeps rendering while the pair lives). */
        public boolean hasPair;
        /** The box frame quads, block-relative and light-baked at extract time. */
        public final List<BcQuad> geometry = new ArrayList<>();
    }
}
