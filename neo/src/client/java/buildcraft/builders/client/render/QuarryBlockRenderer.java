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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import org.jspecify.annotations.Nullable;
import buildcraft.builders.blockentity.QuarryBlockEntity;
import buildcraft.core.client.render.BcBoxes;
import buildcraft.lib.client.render.BcQuad;
import org.slf4j.Logger;

/**
 * M2.12 quarry block entity renderer: outlines the mining area and highlights the cell currently being drilled,
 * mirroring the legacy {@code RenderQuarry}/{@code AdvDebuggerQuarry} box rendering in miniature (the legacy drill
 * head, laser animations and frame-laying visuals have not migrated). All geometry is drawn as {@link BcBoxes} boxes
 * through {@code submitCustomGeometry}, exactly like the M2.7b/M2.11 engine/pipe/gate slice.
 *
 * <p>Everything the renderer draws rides the BE's update tag (the Beacon-pattern sync of
 * {@link QuarryBlockEntity}): area corners, current target cell and the finished flag are plain
 * {@code saveAdditional} keys, so the frame appearing on the client is itself the proof that the update-tag channel
 * carried the state.
 */
public class QuarryBlockRenderer implements BlockEntityRenderer<QuarryBlockEntity, QuarryBlockRenderer.State> {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Plain white block sprite, tinted per part (one atlas sprite keeps the submit group single). */
    private static final SpriteId WHITE_CONCRETE = new SpriteId(
        TextureAtlas.LOCATION_BLOCKS, Identifier.withDefaultNamespace("block/white_concrete"));

    /** Render type for block-atlas-textured custom geometry (cutout + cull, like baked block quads). */
    private static final RenderType RENDER_TYPE = Sheets.cutoutBlockSheet();

    /** Frame edge cross-section, in block units (2/16 - chunky like the legacy quarry frame pipes). */
    private static final float FRAME_T = 2.0f / 16.0f;
    /** Outward inflation so the frame does not z-fight with the neighbour block faces it sits on. */
    private static final float FRAME_EPSILON = 1.0f / 64.0f;
    /** Dark steel tint of the mining-area frame (the legacy quarry frame colour family). */
    private static final int FRAME_TINT = 0xFF505058;
    /** Warm amber tint of the active-target marker (the M2.11 gate glow family colour). */
    private static final int TARGET_TINT = 0xFFFFC860;
    /** Target-cage edge cross-section, in block units (same family as the area frame so the cage reads at
     * evidence-screenshot distance). */
    private static final float TARGET_CAGE_T = 2.5f / 16.0f;
    /** Packed full-bright light for the active-target marker. */
    private static final int FULL_BRIGHT = 15728880;

    /** Positions whose area already got a "quarry synced" log line (M2.12 sync evidence). */
    private final Set<BlockPos> loggedSync = new HashSet<>();
    /** Positions whose current drill target already got a "target synced" log line (M2.12 sync evidence). */
    private final Set<BlockPos> loggedTargets = new HashSet<>();
    /** Block atlas sprite accessor, resolved once per renderer instance (same pattern as the M2.7b BERs). */
    private final SpriteGetter sprites;

    public QuarryBlockRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
        QuarryBlockEntity quarry, State state, float partialTicks, Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(quarry, state, partialTicks, cameraPosition, breakProgress);
        state.geometry.clear();
        BlockPos pos = quarry.getBlockPos();
        state.hasArea = quarry.getAreaMin() != null && quarry.getAreaMax() != null;
        state.areaMin = quarry.getAreaMin();
        state.areaMax = quarry.getAreaMax();
        state.currentTarget = quarry.getCurrentTarget();
        state.finished = quarry.isFinished();
        if (!state.hasArea) {
            return;
        }
        // M2.12 sync evidence: the first extract seeing an area proves the server's update-tag packet arrived and was
        // applied through loadAdditional (the area only exists client-side after that packet).
        if (this.loggedSync.add(pos)) {
            LOGGER.info("Quarry block entity at {} synced: area={}..{}, finished={}",
                pos, state.areaMin, state.areaMax, state.finished);
        }
        if (state.currentTarget != null && this.loggedTargets.add(pos)) {
            LOGGER.info("Quarry block entity at {} drill target synced: {}", pos, state.currentTarget);
        }
        Level level = quarry.getLevel();
        if (level == null) {
            return;
        }
        // block-relative frame box, inflated a hair outward (see FRAME_EPSILON)
        float x0 = state.areaMin.getX() - pos.getX() - FRAME_EPSILON;
        float y0 = state.areaMin.getY() - pos.getY() - FRAME_EPSILON;
        float z0 = state.areaMin.getZ() - pos.getZ() - FRAME_EPSILON;
        float x1 = state.areaMax.getX() - pos.getX() + 1 + FRAME_EPSILON;
        float y1 = state.areaMax.getY() - pos.getY() + 1 + FRAME_EPSILON;
        float z1 = state.areaMax.getZ() - pos.getZ() + 1 + FRAME_EPSILON;
        List<BcQuad> frame = BcBoxes.frame(x0, y0, z0, x1, y1, z1, FRAME_T);
        BcBoxes.lightAll(frame, state);
        for (int i = 0; i < frame.size(); i++) {
            frame.set(i, frame.get(i).multiplyColor(FRAME_TINT));
        }
        state.geometry.addAll(frame);
        // the active drill target: a full-bright amber cage wrapped around the whole target cell (the legacy
        // laser wrapped the drill block too — an inner cube would be buried inside the not-yet-broken stone)
        if (state.currentTarget != null) {
            float cx = state.currentTarget.getX() - pos.getX();
            float cy = state.currentTarget.getY() - pos.getY();
            float cz = state.currentTarget.getZ() - pos.getZ();
            List<BcQuad> cage = BcBoxes.frame(
                cx - 0.05f, cy - 0.05f, cz - 0.05f, cx + 1.05f, cy + 1.05f, cz + 1.05f, TARGET_CAGE_T);
            for (int i = 0; i < cage.size(); i++) {
                cage.set(i, cage.get(i).multiplyColor(TARGET_TINT).withLight(FULL_BRIGHT));
            }
            state.geometry.addAll(cage);
        }
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

    /** M2.12 render state of one quarry: the extracted area/target/finished mirror plus the built frame geometry. */
    public static class State extends BlockEntityRenderState {
        /** True while the quarry has a mining area (legacy: markers placed). */
        public boolean hasArea;
        /** Mining area corners (absolute coords), null when unset. */
        @Nullable
        public BlockPos areaMin;
        @Nullable
        public BlockPos areaMax;
        /** The cell currently being drilled, or null between tasks (legacy {@code TileQuarry.drillPos} stand-in). */
        @Nullable
        public BlockPos currentTarget;
        /** True once the area is fully mined. */
        public boolean finished;
        /** Frame + active-target quads, block-relative and light-baked (or full-bright) at extract time. */
        public final List<BcQuad> geometry = new ArrayList<>();
    }
}
