/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.client.render;

import java.util.ArrayList;
import java.util.EnumSet;
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
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import org.jspecify.annotations.Nullable;
import buildcraft.core.blockentity.MarkerBlockEntity;
import buildcraft.core.blockentity.MarkerVolumeBlockEntity;
import buildcraft.lib.client.render.laser.BcLaserBaker;
import buildcraft.lib.client.render.laser.BcLaserBoxRenderer;
import buildcraft.lib.client.render.laser.BcLaserData;
import buildcraft.lib.client.render.laser.BcLaserQuad;
import buildcraft.lib.client.render.laser.BcLaserTypes;

/**
 * The volume/signal marker renderer (M4.5 port of legacy
 * {@code buildcraft.core.client.render.RenderMarkerVolume} plus the connection box half of legacy
 * {@code VolumeConnection#renderInWorld}):
 * <ul>
 * <li>connected markers draw the {@code MARKER_VOLUME_CONNECTED} laser lines around their group's box through
 * {@link BcLaserBoxRenderer} &mdash; the lines run through the block centres and an axis only gets lines once the box
 * spans more than one block along it (the legacy {@code makeLaserBox} rules). The group's lowest member renders the
 * box, so shared lines are drawn exactly once;</li>
 * <li>while redstone drives the "signals" state, every not-yet-connected axis emits a long
 * {@code MARKER_VOLUME_SIGNAL} laser (legacy {@code RenderMarkerVolume}'s six lines, scale 1/16.2).</li>
 * </ul>
 *
 * <p>Everything is baked in {@code extractRenderState} from the marker BE's update-tag-synced state and submitted as
 * custom geometry on the cutout block sheet, exactly like the M2.12 quarry renderer.
 */
public class MarkerVolumeBlockRenderer
    implements BlockEntityRenderer<MarkerVolumeBlockEntity, MarkerVolumeBlockRenderer.State> {

    /** Legacy {@code RenderMarkerVolume#SCALE}: signal lines are slightly thinner than connection lines. */
    private static final double SIGNAL_SCALE = 1 / 16.2;
    private static final Vec3 VEC_HALF = new Vec3(0.5, 0.5, 0.5);

    private static final RenderType RENDER_TYPE = Sheets.cutoutBlockSheet();

    private final SpriteGetter sprites;

    public MarkerVolumeBlockRenderer(BlockEntityRendererProvider.Context context) {
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
        // legacy: BCCoreConfig.markerMaxDistance * 2
        return MarkerBlockEntity.MAX_MARKER_DISTANCE * 2;
    }

    @Override
    public AABB getRenderBoundingBox(MarkerVolumeBlockEntity marker) {
        if (marker.isConnected()) {
            AABB box = boxOf(marker.getConnectedPositions());
            if (box != null) {
                // grow by a block so the laser cross-sections stick out of the bare box
                return box.inflate(1);
            }
        }
        if (marker.isShowingSignals()) {
            // the signal lasers reach MAX_MARKER_DISTANCE blocks out
            return new AABB(marker.getBlockPos()).inflate(MarkerBlockEntity.MAX_MARKER_DISTANCE + 1);
        }
        return BlockEntityRenderer.super.getRenderBoundingBox(marker);
    }

    @Override
    public void extractRenderState(
        MarkerVolumeBlockEntity marker, State state, float partialTicks, Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(marker, state, partialTicks, cameraPosition, breakProgress);
        state.lasers.clear();
        Level level = marker.getLevel();
        if (level == null) {
            return;
        }
        List<BlockPos> members = marker.getConnectedPositions();
        BlockPos origin = marker.getBlockPos();

        if (marker.isConnected() && isBoxAnchor(origin, members)) {
            BlockPos min = memberCorner(members, false);
            BlockPos max = memberCorner(members, true);
            for (BcLaserData data : BcLaserBoxRenderer.makeLaserBox(min, max, BcLaserTypes.MARKER_VOLUME_CONNECTED,
                true)) {
                BcLaserBaker.bake(data, origin, level, this.sprites, state.lasers);
            }
        }

        if (marker.isShowingSignals()) {
            EnumSet<Axis> taken = connectedAxes(members);
            Vec3 start = VEC_HALF.add(Vec3.atLowerCornerOf(origin));
            for (Direction face : Direction.values()) {
                if (taken.contains(face.getAxis())) {
                    continue;
                }
                Vec3 end = start.add(new Vec3(
                    face.getStepX() * (double) MarkerBlockEntity.MAX_MARKER_DISTANCE,
                    face.getStepY() * (double) MarkerBlockEntity.MAX_MARKER_DISTANCE,
                    face.getStepZ() * (double) MarkerBlockEntity.MAX_MARKER_DISTANCE));
                BcLaserData data = new BcLaserData(BcLaserTypes.MARKER_VOLUME_SIGNAL,
                    offsetOutward(start, face), offsetOutward(end, face.getOpposite()), SIGNAL_SCALE);
                BcLaserBaker.bake(data, origin, level, this.sprites, state.lasers);
            }
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

    /** Only the group's lowest member draws the shared box lines (the legacy global cache drew each connection once). */
    private static boolean isBoxAnchor(BlockPos self, List<BlockPos> members) {
        for (BlockPos member : members) {
            if (member.compareTo(self) < 0) {
                return false;
            }
        }
        return true;
    }

    private static BlockPos memberCorner(List<BlockPos> members, boolean max) {
        int x = max ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int y = max ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int z = max ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (BlockPos p : members) {
            x = max ? Math.max(x, p.getX()) : Math.min(x, p.getX());
            y = max ? Math.max(y, p.getY()) : Math.min(y, p.getY());
            z = max ? Math.max(z, p.getZ()) : Math.min(z, p.getZ());
        }
        return new BlockPos(x, y, z);
    }

    private static EnumSet<Axis> connectedAxes(List<BlockPos> members) {
        EnumSet<Axis> taken = EnumSet.noneOf(Axis.class);
        for (BlockPos a : members) {
            for (BlockPos b : members) {
                Direction offset = MarkerBlockEntity.directFacingOffset(a, b);
                if (offset != null) {
                    taken.add(offset.getAxis());
                }
            }
        }
        return taken;
    }

    /** Legacy {@code RenderMarkerVolume#offset}: push the end point 1/16 out along the laser direction. */
    private static Vec3 offsetOutward(Vec3 vec, Direction face) {
        return vec.add(face.getStepX() / 16.0, face.getStepY() / 16.0, face.getStepZ() / 16.0);
    }

    @Nullable
    private static AABB boxOf(List<BlockPos> members) {
        if (members.isEmpty()) {
            return null;
        }
        BlockPos min = memberCorner(members, false);
        BlockPos max = memberCorner(members, true);
        return new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1);
    }

    /** Render state of one marker: the baked laser quads (block-relative), rebuilt every frame from the BE mirror. */
    public static class State extends BlockEntityRenderState {
        public final List<BcLaserQuad> lasers = new ArrayList<>();
    }
}
