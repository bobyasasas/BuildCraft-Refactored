/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.client.render;

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
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.logging.LogUtils;
import org.jspecify.annotations.Nullable;
import buildcraft.core.block.StoneEngineBlock;
import buildcraft.core.blockentity.StoneEngineBlockEntity;
import buildcraft.lib.client.render.BcQuad;
import org.slf4j.Logger;

/**
 * M2.7b stone engine block entity renderer — the first real {@link BlockEntityRenderer} of the port and the reference
 * implementation for the remaining 17 (M2.8+). 1.20.1 counterpart: {@code RenderEngine} drawing its piston through the
 * model cache; here the static base stays the block model ({@code assets/buildcraftcore/models/block/engine_stone.json},
 * orientable) and only the animated part is custom geometry.
 *
 * <p>Two-stage pipeline mapping (verified against the 26.1.2 client sources, see
 * {@code neo/docs/render-pipeline-26.1.2.md}):
 * <ul>
 * <li><b>extract</b> ({@link #extractRenderState}): reads the client-side {@link StoneEngineBlockEntity} fields that
 * {@code StoneEngineBlockEntity}'s update-tag sync keeps fresh, turns them into
 * {@link State#burning}/{@link State#burnProgress}, and fills {@link State#piston} with the light-baked piston quads
 * (position depends on the burn progress).</li>
 * <li><b>submit</b> ({@link #submit}): emits {@link State#piston} through
 * {@link SubmitNodeCollector#submitCustomGeometry}. The pose stack origin is the block's (0,0,0) corner
 * ({@code LevelRenderer#submitBlockEntities} translates by {@code blockPos - cameraPos}), the quads are authored in
 * block-relative coordinates and rotated onto the facing like the blockstate model rotations
 * (north 0°, east 90°, south 180°, west 270°).</li>
 * </ul>
 *
 * <p>Piston animation: {@code extension = min(2·sin(π·progress), 1)} — ramps out within the first quarter of the fuel
 * item's burn, holds extended, retracts in the last quarter (a slice stand-in for the legacy engine's pulsing piston;
 * the real animation timing migrates with the engine module in M2.4/M2.9).
 */
public class StoneEngineBlockRenderer implements BlockEntityRenderer<StoneEngineBlockEntity, StoneEngineBlockRenderer.State> {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Iron piston head, textured from the vanilla block atlas. */
    private static final SpriteId PISTON_SPRITE = new SpriteId(
        TextureAtlas.LOCATION_BLOCKS,
        Identifier.withDefaultNamespace("block/iron_block"));

    /** Render type for block-atlas-textured custom geometry (cutout + cull, like baked block quads). */
    private static final RenderType RENDER_TYPE = Sheets.cutoutBlockSheet();
    /** Piston head cross-section, in block units (0.32 of the block, centred on the block axis). */
    private static final float PISTON_HALF = 0.16f;
    /** Piston rest depth inside the block (how much of the trunk is hidden inside the base model). */
    private static final float PISTON_REST = 0.22f;
    /** Piston stroke: how far the head travels out of the front face at full extension. */
    private static final float PISTON_STROKE = 0.35f;

    private final SpriteGetter sprites;
    /** Positions whose current burn session already got a "burning synced" log line (M2.7b sync evidence). */
    private final Set<BlockPos> loggedBurning = new HashSet<>();

    public StoneEngineBlockRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
        StoneEngineBlockEntity engine, State state, float partialTicks, Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(engine, state, partialTicks, cameraPosition, breakProgress);
        int burnRemain = engine.getBurnRemain();
        int burnTotal = engine.getBurnTotal();
        // Guard against the client's not-yet-synced state (burnTotal arrives with the same update as burnRemain).
        state.burning = burnRemain > 0 && burnTotal > 0;
        state.burnProgress = state.burning ? Mth.clamp(1.0f - (float) burnRemain / burnTotal, 0.0f, 1.0f) : 0.0f;
        state.facing = engine.getBlockState().getValue(StoneEngineBlock.FACING);

        // M2.7b sync evidence: log once per burn session when the client-side BE first reports a burn —
        // reaching this line proves the server's update-tag packet arrived and was applied through loadAdditional.
        if (state.burning) {
            if (this.loggedBurning.add(engine.getBlockPos())) {
                LOGGER.info("Stone engine at {} burning synced: burnRemain={}, burnTotal={}",
                    engine.getBlockPos(), burnRemain, burnTotal);
            }
        } else {
            this.loggedBurning.remove(engine.getBlockPos());
        }

        state.piston.clear();
        if (state.burning) {
            // min(2·sin(π·progress), 1): out by the first quarter of the burn, holds, retracts at the end.
            float wave = Mth.sin(state.burnProgress * Mth.PI);
            float extension = Math.min(wave * 2.0f, 1.0f);
            float travel = extension * PISTON_STROKE;
            // Authored pointing north (the blockstate's unrotated facing), like the base model's front texture:
            // cross-section centred on the block axis, trunk from inside the block out through the front (z = 0) face.
            float x0 = 0.5f - PISTON_HALF;
            float x1 = 0.5f + PISTON_HALF;
            List<BcQuad> piston = BcBoxes.box(
                x0, x0, 0.0f - travel, x1, x1, PISTON_REST - travel);
            BcBoxes.lightAll(piston, state);
            state.piston.addAll(piston);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
        CameraRenderState cameraRenderState) {
        if (state.piston.isEmpty()) {
            return;
        }
        TextureAtlasSprite sprite = this.sprites.get(PISTON_SPRITE);
        poseStack.pushPose();
        // Same y-rotation table the engine blockstate uses for its facing variants (north 0, east 90, south 180,
        // west 270 degrees). The blockstate applies that as a +y rotation in its own convention, which for geometry
        // rotated through the pose stack is (180 - toYRot) around +Y about the block centre — verified per facing:
        // rotating -Z by it lands on the facing's normal.
        poseStack.rotateAround(Axis.YP.rotationDegrees(180.0f - state.facing.toYRot()), 0.5f, 0.0f, 0.5f);
        submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
            for (BcQuad quad : state.piston) {
                quad.mapUv(sprite).emit(pose, buffer);
            }
        });
        poseStack.popPose();
    }

    /** M2.7b render state of one engine: the synced burn data (see {@link StoneEngineBlockEntity}'s client sync
     * javadoc), the facing copied from the block state, and the extract-built piston quads. */
    public static class State extends BlockEntityRenderState {
        /** True while a fuel item burns (client copy of the server's burn state). */
        public boolean burning;
        /** 0..1 progress through the current fuel item's burn (0 = just ignited). */
        public float burnProgress;
        /** Output face (block state, not block entity data — the base model rotates the same way). */
        public Direction facing = Direction.NORTH;
        /** Piston head quads, block-relative and light-baked at extract time; empty when not burning. */
        public final List<BcQuad> piston = new ArrayList<>();
    }
}
