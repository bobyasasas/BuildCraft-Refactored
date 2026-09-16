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
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import org.jspecify.annotations.Nullable;
import buildcraft.core.BcBlocks;
import buildcraft.core.block.KinesisPipeBlock;
import buildcraft.core.block.StoneEngineBlock;
import buildcraft.core.blockentity.KinesisPipeBlockEntity;
import buildcraft.lib.client.render.BcQuad;

/**
 * M2.7b wooden kinesis pipe block entity renderer: the whole pipe body is dynamic custom geometry (there is no static
 * block model left — {@code assets/buildcraftcore/models/block/pipe_kinesis_wood.json} now carries only the particle
 * texture, the vanilla chest pattern for BER-drawn blocks), replacing the fixed thin-column placeholder model of
 * M2.2c with the classic auto-connecting pipe look. 1.20.1 counterpart: the pipe's dynamic
 * {@code PipeModelRenderer}/state-based connected texture model.
 *
 * <p><b>Connections (extract):</b> six-direction boolean set, re-scanned every frame from the client block states
 * (cheap, six lookups, no block entity access): a neighbour connects when it is another kinesis pipe, or a stone
 * engine whose {@code FACING} (output face, {@code StoneEngineBlockEntity#getOutputFacing}) points at this pipe, or
 * the slice's {@code energy_meter} sink — the same three cases the server side {@code KinesisPipeBlockEntity} power
 * simulation accepts.
 *
 * <p><b>Geometry (submit):</b> a centre box (the 6..10 pixel column the block's collision shape
 * {@link KinesisPipeBlock} already declares) plus one arm box per connected direction reaching from the centre to the
 * block face; every part is emitted through {@link SubmitNodeCollector#submitCustomGeometry} with
 * {@link BcBoxes}-built {@link BcQuad}s textured from the vanilla planks sprites (oak arms, darker-tinted centre),
 * light-baked from {@code state.lightCoords}. Not connected on a side, the centre box ends flat — the standalone
 * pipe look.
 */
public class KinesisPipeBlockRenderer implements BlockEntityRenderer<KinesisPipeBlockEntity, KinesisPipeBlockRenderer.State> {

    /** Wooden pipe texture, straight from the vanilla block atlas (oak planks = the classic wooden pipe colour). */
    private static final SpriteId OAK_PLANKS = new SpriteId(
        TextureAtlas.LOCATION_BLOCKS, Identifier.withDefaultNamespace("block/oak_planks"));

    /** Render type for block-atlas-textured custom geometry (cutout + cull, like baked block quads). */
    private static final RenderType RENDER_TYPE = Sheets.cutoutBlockSheet();

    /** Centre box bounds, in block units (6..10 pixels, matching {@link KinesisPipeBlock}'s collision shape). */
    private static final float CENTER_MIN = 6.0f / 16.0f;
    private static final float CENTER_MAX = 10.0f / 16.0f;
    /** Grey multiplier tinting the centre box darker than the arms (distinguishes hub from connection arms). */
    private static final int CENTER_TINT = 0xFFB4B4B4;

    private final SpriteGetter sprites;

    public KinesisPipeBlockRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
        KinesisPipeBlockEntity pipe, State state, float partialTicks, Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(pipe, state, partialTicks, cameraPosition, breakProgress);
        state.connections.clear();
        Level level = pipe.getLevel();
        if (level != null) {
            BlockPos pos = pipe.getBlockPos();
            for (Direction direction : Direction.values()) {
                if (isConnected(level, pos, direction)) {
                    state.connections.add(direction);
                }
            }
        }

        state.geometry.clear();
        // Centre box (hub) ...
        List<BcQuad> centre = BcBoxes.box(CENTER_MIN, CENTER_MIN, CENTER_MIN, CENTER_MAX, CENTER_MAX, CENTER_MAX);
        BcBoxes.lightAll(centre, state);
        for (BcQuad quad : centre) {
            state.geometry.add(quad.multiplyColor(CENTER_TINT));
        }
        // ... plus one arm per connected direction.
        for (Direction direction : state.connections) {
            List<BcQuad> arm = armBox(direction);
            BcBoxes.lightAll(arm, state);
            state.geometry.addAll(arm);
        }
    }

    /** Client-side connection scan: pipe-to-pipe, engine-output-to-pipe and pipe-to-meter all connect. Block-state
     * checks only — the engine's output face is its {@code FACING} block property
     * ({@code StoneEngineBlockEntity#getOutputFacing} reads exactly that), so no neighbour block entity access. */
    private static boolean isConnected(Level level, BlockPos pos, Direction direction) {
        BlockState neighbour = level.getBlockState(pos.relative(direction));
        if (neighbour.getBlock() == BcBlocks.PIPE_KINESIS_WOOD.value()) {
            return true;
        }
        if (neighbour.getBlock() == BcBlocks.ENERGY_METER.value()) {
            return true;
        }
        return neighbour.getBlock() == BcBlocks.ENGINE_STONE.value()
            && neighbour.getValue(StoneEngineBlock.FACING) == direction.getOpposite();
    }

    /** The connection arm for one direction: centre cross-section, from the centre box out to the block face. */
    private static List<BcQuad> armBox(Direction direction) {
        return switch (direction) {
            case DOWN -> BcBoxes.box(CENTER_MIN, 0.0f, CENTER_MIN, CENTER_MAX, CENTER_MIN, CENTER_MAX);
            case UP -> BcBoxes.box(CENTER_MIN, CENTER_MAX, CENTER_MIN, CENTER_MAX, 1.0f, CENTER_MAX);
            case NORTH -> BcBoxes.box(CENTER_MIN, CENTER_MIN, 0.0f, CENTER_MAX, CENTER_MAX, CENTER_MIN);
            case SOUTH -> BcBoxes.box(CENTER_MIN, CENTER_MIN, CENTER_MAX, CENTER_MAX, CENTER_MAX, 1.0f);
            case WEST -> BcBoxes.box(0.0f, CENTER_MIN, CENTER_MIN, CENTER_MIN, CENTER_MAX, CENTER_MAX);
            case EAST -> BcBoxes.box(CENTER_MAX, CENTER_MIN, CENTER_MIN, 1.0f, CENTER_MAX, CENTER_MAX);
        };
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
        CameraRenderState cameraRenderState) {
        if (state.geometry.isEmpty()) {
            return;
        }
        TextureAtlasSprite sprite = this.sprites.get(OAK_PLANKS);
        // Block-relative coordinates: LevelRenderer#submitBlockEntities already translated the pose to the block's
        // (0,0,0) corner, no per-position transform needed here.
        submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
            for (BcQuad quad : state.geometry) {
                quad.mapUv(sprite).emit(pose, buffer);
            }
        });
    }

    /** M2.7b render state of one pipe: the six-direction connection set plus the extract-built geometry. */
    public static class State extends BlockEntityRenderState {
        /** Connected directions, re-scanned from client block states every extract. */
        public final EnumSet<Direction> connections = EnumSet.noneOf(Direction.class);
        /** Centre + arm quads, block-relative and light-baked at extract time. */
        public final List<BcQuad> geometry = new ArrayList<>();
    }
}
