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
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import org.jspecify.annotations.Nullable;
import buildcraft.builders.blockentity.QuarryBlockEntity;
import buildcraft.lib.client.render.laser.BcLaserBaker;
import buildcraft.lib.client.render.laser.BcLaserBoxRenderer;
import buildcraft.lib.client.render.laser.BcLaserData;
import buildcraft.lib.client.render.laser.BcLaserQuad;
import buildcraft.lib.client.render.laser.BcLaserTypes;
import org.slf4j.Logger;

/**
 * M4.5 quarry block entity renderer, the laser-visual port of legacy {@code RenderQuarry}: the mining area gets the
 * blue {@code STRIPES_WRITE} border box (legacy
 * {@code LaserBoxRenderer.renderLaserBoxStatic(tile.frameBox, STRIPES_WRITE, pose, true)}), the active drill target
 * gets the red {@code POWER_LOW} beam from the quarry's centre (legacy "don't render a laser before we have any
 * power", keyed here on the synced energy buffer) and the {@code DRILL} column hovering above the target block.
 *
 * <p>Not ported from legacy {@code RenderQuarry}: the {@code FRAME}/<code>FRAME_BOTTOM</code> hanger rails and the
 * power-driven drill bobbing ({@code yOffset}) both need the legacy per-tick {@code drillPos}/{@code clientPower}
 * interpolation state, which the M4.5 sync slice does not carry — the drill column sits at the fixed rest offset
 * instead. Geometry is baked block-relative by {@link BcLaserBaker} and submitted through
 * {@code submitCustomGeometry}, like every other BER in this port.
 *
 * <p>Everything the renderer draws rides the BE's update tag (the Beacon-pattern sync of
 * {@link QuarryBlockEntity}): area corners, current target cell and the finished flag are plain
 * {@code saveAdditional} keys, so the lasers appearing on the client is itself the proof that the update-tag channel
 * carried the state.
 */
public class QuarryBlockRenderer implements BlockEntityRenderer<QuarryBlockEntity, QuarryBlockRenderer.State> {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Legacy {@code RenderQuarry#getViewDistance()}: the frame box can be far outside the normal 64-block cull. */
    private static final int VIEW_DISTANCE = 512;

    /** Legacy laser scale for the power beam, drill column and frame rails ({@code 1 / 16D}). */
    private static final double LASER_SCALE = 1 / 16.0;

    /** The drill column's rest offset above the target block, legacy {@code yOffset = 1 + 4 / 16D}. */
    private static final double DRILL_REST_OFFSET = 1 + 4 / 16.0;

    /** Render type for block-atlas-textured custom geometry (cutout + cull, like baked block quads). */
    private static final RenderType RENDER_TYPE = Sheets.cutoutBlockSheet();

    /** Positions whose area already got a "quarry synced" log line (M2.12 sync evidence, kept for M4.5). */
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
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return VIEW_DISTANCE;
    }

    @Override
    public AABB getRenderBoundingBox(QuarryBlockEntity quarry) {
        BlockPos min = quarry.getAreaMin();
        BlockPos max = quarry.getAreaMax();
        BlockPos target = quarry.getCurrentTarget();
        if (min == null || max == null) {
            if (target == null) {
                return BlockEntityRenderer.super.getRenderBoundingBox(quarry);
            }
            min = target;
            max = target;
        }
        if (target != null) {
            min = new BlockPos(
                Math.min(min.getX(), target.getX()), Math.min(min.getY(), target.getY()),
                Math.min(min.getZ(), target.getZ()));
            max = new BlockPos(
                Math.max(max.getX(), target.getX()), Math.max(max.getY(), target.getY()),
                Math.max(max.getZ(), target.getZ()));
        }
        return new AABB(
            Vec3.atLowerCornerOf(min), Vec3.atLowerCornerOf(max).add(1, 1, 1)).inflate(1);
    }

    @Override
    public void extractRenderState(
        QuarryBlockEntity quarry, State state, float partialTicks, Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(quarry, state, partialTicks, cameraPosition, breakProgress);
        state.lasers.clear();
        BlockPos pos = quarry.getBlockPos();
        state.hasArea = quarry.getAreaMin() != null && quarry.getAreaMax() != null;
        state.areaMin = quarry.getAreaMin();
        state.areaMax = quarry.getAreaMax();
        state.currentTarget = quarry.getCurrentTarget();
        state.finished = quarry.isFinished();
        state.hasPower = quarry.getEnergyStored() > 0;
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
        // The STRIPES_WRITE border around the whole mining area (legacy frame box, centre-aligned so the lasers sit
        // on the box's edges).
        BcLaserBoxRenderer.makeLaserBox(state.areaMin, state.areaMax, BcLaserTypes.STRIPES_WRITE, true)
            .forEach(laser -> BcLaserBaker.bake(laser, pos, level, this.sprites, state.lasers));
        if (state.currentTarget == null) {
            return;
        }
        // The POWER_LOW beam from the quarry's centre to the target's centre — legacy skips it entirely before any
        // power arrived, which here is "the synced energy buffer is still empty".
        if (state.hasPower) {
            BcLaserBaker.bake(new BcLaserData(
                BcLaserTypes.POWER_LOW, Vec3.atLowerCornerOf(pos).add(0.5, 0.5, 0.5),
                Vec3.atLowerCornerOf(state.currentTarget).add(0.5, 0.5, 0.5), LASER_SCALE),
                pos, level, this.sprites, state.lasers);
        }
        // The DRILL column above the target block (legacy rest offset; see class javadoc for the dropped bobbing).
        double tx = state.currentTarget.getX() + 0.5 - pos.getX();
        double ty = state.currentTarget.getY() - pos.getY();
        double tz = state.currentTarget.getZ() + 0.5 - pos.getZ();
        BcLaserBaker.bake(new BcLaserData(
            BcLaserTypes.DRILL, new Vec3(tx, ty + 1 + DRILL_REST_OFFSET, tz),
            new Vec3(tx, ty + DRILL_REST_OFFSET, tz), LASER_SCALE, true),
            pos, level, this.sprites, state.lasers);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
        CameraRenderState cameraRenderState) {
        // Block-relative coordinates: LevelRenderer#submitBlockEntities already translated the pose to the block's
        // (0,0,0) corner, no per-position transform needed here.
        if (state.lasers.isEmpty()) {
            return;
        }
        submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
            for (BcLaserQuad quad : state.lasers) {
                quad.emit(pose, buffer);
            }
        });
    }

    /** M4.5 render state of one quarry: the extracted area/target/power mirror plus the baked laser quads. */
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
        /** True while the synced energy buffer holds any power (legacy {@code clientPower != 0}). */
        public boolean hasPower;
        /** Baked laser quads (block-relative) for the area border, power beam and drill column. */
        public final List<BcLaserQuad> lasers = new ArrayList<>();
    }
}
