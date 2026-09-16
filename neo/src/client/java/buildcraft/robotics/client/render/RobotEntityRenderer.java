/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.robotics.client.render;

import java.util.ArrayList;
import java.util.List;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import buildcraft.core.client.render.BcBoxes;
import buildcraft.lib.client.render.BcQuad;
import buildcraft.robotics.entity.EntityRobot;

/**
 * M2.13 minimal robot entity renderer (the first entity renderer of the neo tree). A simple box assembly drawn from
 * the vanilla block atlas &mdash; a 0.5&sup3; body (the entity's bounding box) on the entity's feet position plus a
 * top antenna &mdash; deliberately NOT the legacy
 * {@code buildcraftrobotics:entities/robot_base} layered model ({@code EntityRobotEnergyLayer}/gear meshes); the
 * legacy model migrates with the full client port. 1.20.1 counterpart:
 * {@code buildcraft.robotics.client.render.RobotRenderer} (a {@code LivingEntityRenderer} over the board-driven
 * model); the 26.1.2 shape is the two-phase extract &rarr; submit pipeline (see
 * {@code neo/docs/render-pipeline-26.1.2.md} &mdash; the same M2.7b pattern the BE renderers use, with
 * {@link EntityRenderState} instead of the BE state base).
 *
 * <p>The body tint mirrors the synced task state (legacy paints the robot's "eye" by board and plays the laser
 * glow): grey while idle/done, warm amber while searching/moving, hot orange while breaking. The tint source is the
 * robot's {@code EntityDataAccessor} sync (task state byte), so no extra client channel is needed.
 */
public class RobotEntityRenderer extends EntityRenderer<EntityRobot, RobotEntityRenderer.State> {

    /** Body texture, straight from the vanilla block atlas (iron = the classic robot shell colour). */
    private static final SpriteId IRON_BLOCK = new SpriteId(
        TextureAtlas.LOCATION_BLOCKS, Identifier.withDefaultNamespace("block/iron_block"));
    /** Antenna texture (dark iron). */
    private static final SpriteId IRON_DARK = new SpriteId(
        TextureAtlas.LOCATION_BLOCKS, Identifier.withDefaultNamespace("block/iron_bars"));

    /** Render type for block-atlas-textured custom geometry (cutout + cull, like baked block quads). */
    private static final RenderType RENDER_TYPE = Sheets.cutoutBlockSheet();

    /** Body box: the 0.5&sup3; bounding box, centred on the entity position (entity space origin = feet). */
    private static final List<BcQuad> BODY = BcBoxes.box(-0.25f, 0.0f, -0.25f, 0.25f, 0.5f, 0.25f);
    /** The top antenna. */
    private static final List<BcQuad> ANTENNA = BcBoxes.box(-0.0625f, 0.5f, -0.0625f, 0.0625f, 0.6875f, 0.0625f);

    /** Idle/done body tint (dark iron). */
    private static final int TINT_IDLE = 0xFFB4B4B4;
    /** Search/move body tint (warm amber, the legacy "working" colour). */
    private static final int TINT_WORKING = 0xFFFFC860;
    /** Breaking body tint (hot orange). */
    private static final int TINT_BREAKING = 0xFFFF9848;

    private final SpriteGetter sprites;

    public RobotEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.sprites = context.getSprites();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityRobot robot, State state, float partialTicks) {
        super.extractRenderState(robot, state, partialTicks);
        state.taskState = robot.getTaskState();
        int tint = switch (state.taskState) {
            case SEARCH, MOVE -> TINT_WORKING;
            case BREAK -> TINT_BREAKING;
            default -> TINT_IDLE;
        };
        state.bodyQuads.clear();
        for (BcQuad quad : BODY) {
            state.bodyQuads.add(quad.withColor(tint).withLight(state.lightCoords));
        }
        state.antennaQuads.clear();
        for (BcQuad quad : ANTENNA) {
            state.antennaQuads.add(quad.withLight(state.lightCoords));
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
        CameraRenderState cameraRenderState) {
        // Entity-relative coordinates: the dispatcher poses us at the entity's position (feet centre), so the
        // 0.5-sized box geometry hangs symmetrically around the origin like the bounding box.
        TextureAtlasSprite bodySprite = this.sprites.get(IRON_BLOCK);
        submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
            for (BcQuad quad : state.bodyQuads) {
                quad.mapUv(bodySprite).emit(pose, buffer);
            }
        });
        TextureAtlasSprite antennaSprite = this.sprites.get(IRON_DARK);
        submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
            for (BcQuad quad : state.antennaQuads) {
                quad.mapUv(antennaSprite).emit(pose, buffer);
            }
        });
        super.submit(state, poseStack, submitNodeCollector, cameraRenderState);
    }

    /** The robot's render state: base entity data plus the synced task state driving the body tint. */
    public static class State extends EntityRenderState {

        /** The synced task state (grey/amber/orange tint source); IDLE until the first extract. */
        public EntityRobot.RobotTaskState taskState = EntityRobot.RobotTaskState.IDLE;
        /** Rebuilt every extract from the static body box with the tint + light baked in. */
        public final List<BcQuad> bodyQuads = new ArrayList<>();
        /** Rebuilt every extract from the static antenna box with the light baked in. */
        public final List<BcQuad> antennaQuads = new ArrayList<>();
    }
}
