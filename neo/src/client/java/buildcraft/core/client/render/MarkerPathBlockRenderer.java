/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.client.render;

import java.util.ArrayList;
import java.util.List;
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
import org.jspecify.annotations.Nullable;
import buildcraft.core.blockentity.MarkerBlockEntity;
import buildcraft.core.blockentity.MarkerPathBlockEntity;
import buildcraft.lib.client.render.laser.BcLaserBaker;
import buildcraft.lib.client.render.laser.BcLaserData;
import buildcraft.lib.client.render.laser.BcLaserQuad;
import buildcraft.lib.client.render.laser.BcLaserTypes;

/**
 * The path marker renderer (M4.5 port of the connection-line half of legacy
 * {@code buildcraft.core.marker.PathConnection#renderInWorld}): one {@code MARKER_PATH_CONNECTED} laser per chain hop,
 * both ends pulled 0.125 out of the marker blocks, scale 1/16.05. The chain's lowest member draws the whole chain so
 * shared lines are emitted once.
 */
public class MarkerPathBlockRenderer
    implements BlockEntityRenderer<MarkerPathBlockEntity, MarkerPathBlockRenderer.State> {

    private static final double RENDER_SCALE = 1 / 16.05;
    private static final Vec3 VEC_HALF = new Vec3(0.5, 0.5, 0.5);

    private static final RenderType RENDER_TYPE = Sheets.cutoutBlockSheet();

    private final SpriteGetter sprites;

    public MarkerPathBlockRenderer(BlockEntityRendererProvider.Context context) {
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
        return MarkerBlockEntity.MAX_MARKER_DISTANCE * 2;
    }

    @Override
    public AABB getRenderBoundingBox(MarkerPathBlockEntity marker) {
        List<BlockPos> members = marker.getConnectedPositions();
        if (marker.isConnected() && !members.isEmpty()) {
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            for (BlockPos p : members) {
                minX = Math.min(minX, p.getX());
                minY = Math.min(minY, p.getY());
                minZ = Math.min(minZ, p.getZ());
                maxX = Math.max(maxX, p.getX());
                maxY = Math.max(maxY, p.getY());
                maxZ = Math.max(maxZ, p.getZ());
            }
            return new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1).inflate(1);
        }
        return BlockEntityRenderer.super.getRenderBoundingBox(marker);
    }

    @Override
    public void extractRenderState(
        MarkerPathBlockEntity marker, State state, float partialTicks, Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(marker, state, partialTicks, cameraPosition, breakProgress);
        state.lasers.clear();
        Level level = marker.getLevel();
        if (level == null || !marker.isConnected()) {
            return;
        }
        List<BlockPos> chain = marker.getConnectedPositions();
        BlockPos origin = marker.getBlockPos();
        if (!isChainAnchor(origin, chain)) {
            return;
        }
        for (int i = 1; i < chain.size(); i++) {
            Vec3 from = VEC_HALF.add(Vec3.atLowerCornerOf(chain.get(i - 1)));
            Vec3 to = VEC_HALF.add(Vec3.atLowerCornerOf(chain.get(i)));
            Vec3 dir = to.subtract(from).normalize();
            Vec3 one = from.add(dir.scale(0.125));
            Vec3 two = to.subtract(dir.scale(0.125));
            BcLaserBaker.bake(new BcLaserData(BcLaserTypes.MARKER_PATH_CONNECTED, one, two, RENDER_SCALE),
                origin, level, this.sprites, state.lasers);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
        CameraRenderState cameraRenderState) {
        if (state.lasers.isEmpty()) {
            return;
        }
        submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
            for (BcLaserQuad quad : state.lasers) {
                quad.emit(pose, buffer);
            }
        });
    }

    /** Only the chain's lowest member draws the shared lines. */
    private static boolean isChainAnchor(BlockPos self, List<BlockPos> members) {
        for (BlockPos member : members) {
            if (member.compareTo(self) < 0) {
                return false;
            }
        }
        return true;
    }

    /** Render state of one marker: the baked laser quads (block-relative), rebuilt every frame from the BE mirror. */
    public static class State extends BlockEntityRenderState {
        public final List<BcLaserQuad> lasers = new ArrayList<>();
    }
}
