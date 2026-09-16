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
import buildcraft.builders.blockentity.FillerBlockEntity;
import buildcraft.core.client.render.BcBoxes;
import buildcraft.lib.client.render.BcQuad;
import org.slf4j.Logger;

/**
 * M2.12 filler block entity renderer: outlines the filler's work area and highlights the cell currently being placed
 * into, mirroring the legacy {@code RenderFiller} + {@code TemplateBuilder} area/glow rendering in miniature (legacy
 * renders the full pattern blueprint; the slice draws the box frame plus the active cell). All geometry is drawn as
 * {@link BcBoxes} boxes through {@code submitCustomGeometry}, exactly like the M2.7b/M2.11 engine/pipe/gate slice.
 *
 * <p>Everything the renderer draws rides the BE's update tag (the Beacon-pattern sync of
 * {@link FillerBlockEntity}): box corners, current cell and the finished flag are plain {@code saveAdditional} keys,
 * so a frame appearing on the client is itself the proof that the update-tag channel carried the state.
 */
public class FillerBlockRenderer implements BlockEntityRenderer<FillerBlockEntity, FillerBlockRenderer.State> {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Plain white block sprite, tinted per part (one atlas sprite keeps the submit group single). */
    private static final SpriteId WHITE_CONCRETE = new SpriteId(
        TextureAtlas.LOCATION_BLOCKS, Identifier.withDefaultNamespace("block/white_concrete"));

    /** Render type for block-atlas-textured custom geometry (cutout + cull, like baked block quads). */
    private static final RenderType RENDER_TYPE = Sheets.cutoutBlockSheet();

    /** Frame edge cross-section, in block units (2/16 - chunky like the legacy filler area borders). */
    private static final float FRAME_T = 2.0f / 16.0f;
    /** Outward inflation so the frame does not z-fight with the neighbour block faces it sits on. */
    private static final float FRAME_EPSILON = 1.0f / 64.0f;
    /** Light grey tint of the work-area frame. */
    private static final int FRAME_TINT = 0xFFD8D8D8;
    /** Warm amber tint of the active-cell marker (the M2.11 gate glow family colour). */
    private static final int CELL_TINT = 0xFFFFC860;
    /** Packed full-bright light for the active-cell marker. */
    private static final int FULL_BRIGHT = 15728880;

    /** Positions whose work area already got a "filler synced" log line (M2.12 sync evidence). */
    private final Set<BlockPos> loggedSync = new HashSet<>();
    /** Block atlas sprite accessor, resolved once per renderer instance (same pattern as the M2.7b BERs). */
    private final SpriteGetter sprites;

    public FillerBlockRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
        FillerBlockEntity filler, State state, float partialTicks, Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(filler, state, partialTicks, cameraPosition, breakProgress);
        state.geometry.clear();
        BlockPos pos = filler.getBlockPos();
        state.hasArea = filler.getAreaMin() != null && filler.getAreaMax() != null;
        state.areaMin = filler.getAreaMin();
        state.areaMax = filler.getAreaMax();
        state.currentCell = filler.getCurrentCell();
        state.finished = filler.isFinished();
        if (!state.hasArea) {
            return;
        }
        // M2.12 sync evidence: the first extract seeing an area proves the server's update-tag packet arrived and was
        // applied through loadAdditional (the box only exists client-side after that packet).
        if (this.loggedSync.add(pos)) {
            LOGGER.info("Filler block entity at {} synced: area={}..{}, pattern={}, finished={}",
                pos, state.areaMin, state.areaMax, filler.getPattern(), state.finished);
        }
        Level level = filler.getLevel();
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
        // the active placement cell: a small full-bright amber cube inside the targeted cell
        if (state.currentCell != null) {
            float cx = state.currentCell.getX() - pos.getX();
            float cy = state.currentCell.getY() - pos.getY();
            float cz = state.currentCell.getZ() - pos.getZ();
            List<BcQuad> cell = BcBoxes.box(cx + 0.3f, cy + 0.3f, cz + 0.3f, cx + 0.7f, cy + 0.7f, cz + 0.7f);
            for (int i = 0; i < cell.size(); i++) {
                cell.set(i, cell.get(i).multiplyColor(CELL_TINT).withLight(FULL_BRIGHT));
            }
            state.geometry.addAll(cell);
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

    /** M2.12 render state of one filler: the extracted box/cell/finished mirror plus the built frame geometry. */
    public static class State extends BlockEntityRenderState {
        /** True while the filler has a work area (legacy: markers placed). */
        public boolean hasArea;
        /** Work area corners (absolute coords), null when unset. */
        @Nullable
        public BlockPos areaMin;
        @Nullable
        public BlockPos areaMax;
        /** The cell currently being filled, or null between placements. */
        @Nullable
        public BlockPos currentCell;
        /** True once the area matches the pattern (the frame keeps rendering as the "job done" border). */
        public boolean finished;
        /** Frame + active-cell quads, block-relative and light-baked (or full-bright) at extract time. */
        public final List<BcQuad> geometry = new ArrayList<>();
    }
}
