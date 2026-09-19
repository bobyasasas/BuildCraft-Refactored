/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.transport.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import buildcraft.core.client.render.BcBoxes;
import buildcraft.lib.client.render.BcPipeGeometry;
import buildcraft.lib.client.render.BcQuad;
import buildcraft.transport.BuildCraftTransport;
import buildcraft.transport.blockentity.PipeHolderBlockEntity;
import buildcraft.transport.client.PipeItemFlowClient;
import buildcraft.transport.client.PipeItemFlowClient.ItemVisual;
import buildcraft.transport.pipe.BcPipeFamilies;
import buildcraft.transport.pipe.BcPipeFamilies.Family;
import buildcraft.transport.pipe.BcPipeFamilies.FlowKind;

/**
 * The M4.6 pipe block entity renderer &mdash; the whole pipe visual is dynamic custom geometry (the block renders as
 * {@code INVISIBLE}; there is no static model, and the chunk-baked optimisation is explicitly post-M4.6). Baseline
 * counterpart: {@code RenderPipeHolder} + the {@code PipeBaseModelGenStandard}/{@code PipeFlowRenderer*} model/render
 * family.
 *
 * <ul>
 * <li><b>Body + connections</b> ({@link BcPipeGeometry}): centre cube + one arm per connected face, textured with the
 * family's world sprite ({@code buildcrafttransport:pipes/<stem>}, resolved by {@code BcPipeFamilies#textureStem}).
 * Connections come from the server through the pipe BE's update tag, so client and server agree.</li>
 * <li><b>Dyed skin</b>: a pipe with a dye colour ({@code PipeHolderBlockEntity#getColour}) draws one extra
 * translucent layer over the body &mdash; the baseline {@code PIPE_COLOUR} skin ({@code BCTransportSprites#
 * PIPE_COLOUR} = {@code pipes/overlay_stained} tinted with the dye's {@code ColourUtil#LIGHT_HEX}), inset
 * {@code 0.01} into the surface and double-sided ({@code PipeBaseModelGenStandard#generateTranslucent} +
 * {@code QUADS_COLOURED}, {@code PipeBaseTranslucentKey#shouldRender}: colourless pipes draw nothing here).</li>
 * <li><b>Plugs</b>: a 2-pixel plate per plugged face ({@code PipeHolderBlock#PLUG_BOXES} geometry), textured with the
 * baseline {@code plug.png}/{@code power_adapter.png}. They are drawn in the same static cutout pass as the body
 * &mdash; with a fully BER-rendered pipe there is no separate static model layer, which is the "静态层" role.</li>
 * <li><b>Fluid flow</b>: a liquid column of the buffered fluid, radius {@code sqrt(fill fraction) * 0.24} (baseline
 * {@code PipeFlowRendererFluids}), textured with the fluid's still sprite and tint, translucent layer.</li>
 * <li><b>Power flow</b>: an inner core of radius {@code 0.248 * fraction} with the animated {@code power_flow}/
 * {@code rf_flow} sprite, full-bright with a gentle pulse (baseline {@code PipeFlowRendererPower}).</li>
 * <li><b>Travelling items</b>: the {@code PipeItemFlowClient} mirror (fed by the legacy
 * {@code MessageMultiPipeItem} wire format) drives per-item mini item-model renders interpolated along the pipe.</li>
 * </ul>
 */
public class PipeHolderBlockRenderer implements BlockEntityRenderer<PipeHolderBlockEntity, PipeHolderBlockRenderer.State> {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** One-shot extract diagnostics (the kinesis {@code loggedGate} pattern): first sight per position. */
    private static final Set<BlockPos> FIRST_EXTRACT = ConcurrentHashMap.newKeySet();

    /** Render type for the opaque pipe body/plugs/power core (cutout + cull, like baked block quads). */
    private static final RenderType CUTOUT = Sheets.cutoutBlockSheet();
    /** Render type for the fluid column (baseline pipes render their contents translucent). */
    private static final RenderType TRANSLUCENT = Sheets.translucentBlockSheet();

    /** Plug body texture (baseline {@code buildcrafttransport:textures/pipes/plug.png}). */
    private static final SpriteId PLUG = new SpriteId(
        TextureAtlas.LOCATION_BLOCKS, Identifier.fromNamespaceAndPath(BuildCraftTransport.MOD_ID, "pipes/plug"));
    /** Power adaptor plug texture (baseline {@code power_adapter.png}). */
    private static final SpriteId POWER_ADAPTER = new SpriteId(
        TextureAtlas.LOCATION_BLOCKS, Identifier.fromNamespaceAndPath(BuildCraftTransport.MOD_ID, "pipes/power_adapter"));
    /** The animated power-flow core (baseline {@code BCTransportSprites#POWER_FLOW}). */
    private static final SpriteId POWER_FLOW = new SpriteId(
        TextureAtlas.LOCATION_BLOCKS, Identifier.fromNamespaceAndPath(BuildCraftTransport.MOD_ID, "pipes/power_flow"));
    private static final SpriteId RF_FLOW = new SpriteId(
        TextureAtlas.LOCATION_BLOCKS, Identifier.fromNamespaceAndPath(BuildCraftTransport.MOD_ID, "pipes/rf_flow"));
    /** The dyed skin overlay (baseline {@code BCTransportSprites#PIPE_COLOUR}). */
    private static final SpriteId PIPE_COLOUR = new SpriteId(
        TextureAtlas.LOCATION_BLOCKS, Identifier.fromNamespaceAndPath(BuildCraftTransport.MOD_ID,
            "pipes/overlay_stained"));

    /**
     * The baseline dye table (legacy {@code ColourUtil#LIGHT_HEX}, stored there indexed {@code 15 - DyeColor#getId()};
     * re-indexed here by {@code DyeColor#ordinal()}), applied as the skin's ARGB with full alpha (legacy
     * {@code PipeBaseModelGenStandard#getPipeModelColour}).
     */
    private static final int[] DYE_ARGB = {
        0xe4e4e4, // white
        0xEA7835, // orange
        0xD943C6, // magenta
        0x66AAFF, // light_blue
        0xFFD91C, // yellow
        0x39D52E, // lime
        0xD97199, // pink
        0x7A7A7A, // gray
        0xa0a7a7, // light_gray
        0x299799, // cyan
        0x7e34bf, // purple
        0x253193, // blue
        0x89502D, // brown
        0x007F0E, // green
        0xBE2B27, // red
        0x181414, // black
    };

    /** Full fluid column radius at {@code perc = 1} (baseline {@code PipeFlowRendererFluids}). */
    private static final float FLUID_RADIUS = 0.24f;
    /** Full power core radius (baseline {@code PipeFlowRendererPower}: {@code 0.248 * fraction}). */
    private static final float POWER_RADIUS = 0.248f;
    /** Packed full-bright light for the power core. */
    private static final int FULL_BRIGHT = 15728880;
    /** Travelling items render at 1/4 block. */
    private static final float ITEM_SCALE = 0.25f;

    private final SpriteGetter sprites;
    private final ItemModelResolver itemModels;

    public PipeHolderBlockRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
        this.itemModels = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
        PipeHolderBlockEntity pipe, State state, float partialTicks, Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(pipe, state, partialTicks, cameraPosition, breakProgress);
        state.bodyQuads.clear();
        state.plugQuads.clear();
        state.fluidQuads.clear();
        state.powerQuads.clear();
        state.skinQuads.clear();
        state.items.clear();
        state.bodySprite = null;
        state.fluidSprite = null;
        state.powerSprite = null;
        state.skinSprite = null;

        Family family = pipe.getFamily();
        if (FIRST_EXTRACT.add(pipe.getBlockPos())) {
            LOGGER.info("[M46-render] first extract at {}: family={} connections={}", pipe.getBlockPos(),
                family == null ? "null" : family.idPrefix(), pipe.connections);
            if (family == null) {
                LOGGER.error("[M46-render] pipe at {} has NO family client-side - it will draw nothing",
                    pipe.getBlockPos());
            }
        }
        if (family == null) {
            return;
        }
        Level level = pipe.getLevel();
        BlockPos pos = pipe.getBlockPos();

        // body: centre + arms, one sprite for the whole pipe (legacy world look, texture index 0)
        state.bodySprite = this.sprites.get(new SpriteId(TextureAtlas.LOCATION_BLOCKS,
            Identifier.fromNamespaceAndPath(BuildCraftTransport.MOD_ID,
                "pipes/" + BcPipeFamilies.textureStem(family, pipe.getColour()))));
        List<BcQuad> body = BcPipeGeometry.pipeBody(pipe.connections);
        BcBoxes.lightAll(body, state);
        state.bodyQuads.addAll(body);

        // dyed skin (colour != null only, baseline PipeBaseTranslucentKey#shouldRender): the overlay_stained
        // sprite tinted with the dye colour, inset 0.01 into the body surface, translucent layer
        DyeColor colour = pipe.getColour();
        if (colour != null) {
            state.skinSprite = this.sprites.get(PIPE_COLOUR);
            int argb = 0xFF000000 | DYE_ARGB[colour.ordinal()];
            List<BcQuad> skin = new ArrayList<>(BcPipeGeometry.colouredSkin(pipe.connections));
            skin.replaceAll(quad -> quad.withColor(argb));
            BcBoxes.lightAll(skin, state);
            state.skinQuads.addAll(skin);
        }

        // plugs: one 2-pixel plate per plugged face (the static layer role on a BER-only pipe)
        for (Direction face : Direction.values()) {
            byte plug = pipe.getPlug(face);
            if (plug == PipeHolderBlockEntity.PLUG_NONE) {
                continue;
            }
            List<BcQuad> plate = switch (face) {
                case DOWN -> BcBoxes.box(4 / 16f, 0.0f, 4 / 16f, 12 / 16f, 2 / 16f, 12 / 16f);
                case UP -> BcBoxes.box(4 / 16f, 14 / 16f, 4 / 16f, 12 / 16f, 1.0f, 12 / 16f);
                case NORTH -> BcBoxes.box(4 / 16f, 4 / 16f, 0.0f, 12 / 16f, 12 / 16f, 2 / 16f);
                case SOUTH -> BcBoxes.box(4 / 16f, 4 / 16f, 14 / 16f, 12 / 16f, 12 / 16f, 1.0f);
                case WEST -> BcBoxes.box(0.0f, 4 / 16f, 4 / 16f, 2 / 16f, 12 / 16f, 12 / 16f);
                case EAST -> BcBoxes.box(14 / 16f, 4 / 16f, 4 / 16f, 1.0f, 12 / 16f, 12 / 16f);
            };
            BcBoxes.lightAll(plate, state);
            state.plugQuads.add(new PlugVisual(plate, this.sprites.get(
                plug == PipeHolderBlockEntity.PLUG_POWER_ADAPTOR ? POWER_ADAPTER : PLUG)));
        }

        // fluid column (fluid pipe families): radius grows with the fill level
        if (family.flow == FlowKind.FLUIDS && !pipe.getFluid().isEmpty()) {
            FluidModel fluidModel = Minecraft.getInstance()
                .getModelManager()
                .getFluidStateModelSet()
                .get(pipe.getFluid().getFluid().defaultFluidState());
            state.fluidSprite = fluidModel.stillMaterial().sprite();
            float fraction = pipe.getFluid().getAmount() / (float) PipeHolderBlockEntity.FLUID_CAPACITY;
            float radius = (float) Math.sqrt(fraction) * FLUID_RADIUS;
            List<BcQuad> fluid = BcBoxes.box(0.5f - radius, 0.5f - radius, 0.5f - radius,
                0.5f + radius, 0.5f + radius, 0.5f + radius);
            int tint = fluidModel.tintSource() != null && level instanceof ClientLevel clientLevel
                ? fluidModel.tintSource().colorInWorld(pipe.getBlockState(), clientLevel, pos)
                : -1;
            if (tint != -1) {
                fluid = fluid.stream().map(quad -> quad.multiplyColor(tint)).toList();
            }
            fluid = new ArrayList<>(fluid);
            BcBoxes.lightAll(fluid, state);
            state.fluidQuads.addAll(fluid);
        }

        // power core (power/rf families): pulsing full-bright inner box, radius by stored fraction
        if (family.flow == FlowKind.POWER && pipe.getPowerStored() > 0 && level != null) {
            state.powerSprite = this.sprites.get(family.stem.startsWith("rf_") ? RF_FLOW : POWER_FLOW);
            float fraction = Math.min(1.0f, pipe.getPowerStored() / 1000f);
            float pulse = 0.8f + 0.2f * (float) Math.sin(level.getGameTime() * 0.3 + pos.hashCode() * 0.7);
            float radius = POWER_RADIUS * fraction * pulse;
            List<BcQuad> core = BcBoxes.box(0.5f - radius, 0.5f - radius, 0.5f - radius,
                0.5f + radius, 0.5f + radius, 0.5f + radius);
            for (int i = 0; i < core.size(); i++) {
                core.set(i, core.get(i).withLight(FULL_BRIGHT));
            }
            state.powerQuads.addAll(core);
        }

        // travelling items: interpolate the client mirror along the current half-leg
        if (level != null) {
            for (ItemVisual visual : PipeItemFlowClient.get(pos)) {
                float progress = (level.getGameTime() - visual.receivedTick + partialTicks) / PipeItemFlowClient.LEG_TICKS;
                progress = Math.min(1.0f, Math.max(0.0f, progress));
                float from = visual.toCenter ? 1.0f : 0.5f;
                float to = visual.toCenter ? 0.5f : 1.0f;
                float f = from + (to - from) * progress;
                float x = 0.5f + visual.side.getStepX() * (f - 0.5f);
                float y = 0.5f + visual.side.getStepY() * (f - 0.5f);
                float z = 0.5f + visual.side.getStepZ() * (f - 0.5f);
                ItemStackRenderState renderState = new ItemStackRenderState();
                this.itemModels.updateForTopItem(renderState, visual.stack, ItemDisplayContext.GROUND,
                    level, null, visual.stack.getItem().hashCode());
                state.items.add(new RenderableItem(renderState, x, y, z));
            }
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
        CameraRenderState cameraRenderState) {
        // Block-relative coordinates: LevelRenderer#submitBlockEntities already translated the pose to the block's
        // (0,0,0) corner, no per-position transform needed here.
        if (state.bodySprite != null && !state.bodyQuads.isEmpty()) {
            TextureAtlasSprite sprite = state.bodySprite;
            submitNodeCollector.submitCustomGeometry(poseStack, CUTOUT, (pose, buffer) -> {
                for (BcQuad quad : state.bodyQuads) {
                    quad.mapUv(sprite).emit(pose, buffer);
                }
            });
        }
        if (state.skinSprite != null && !state.skinQuads.isEmpty()) {
            TextureAtlasSprite skinSprite = state.skinSprite;
            submitNodeCollector.submitCustomGeometry(poseStack, TRANSLUCENT, (pose, buffer) -> {
                for (BcQuad quad : state.skinQuads) {
                    quad.mapUv(skinSprite).emit(pose, buffer);
                }
            });
        }
        for (PlugVisual plug : state.plugQuads) {
            submitNodeCollector.submitCustomGeometry(poseStack, CUTOUT, (pose, buffer) -> {
                for (BcQuad quad : plug.quads()) {
                    quad.mapUv(plug.sprite()).emit(pose, buffer);
                }
            });
        }
        if (state.fluidSprite != null && !state.fluidQuads.isEmpty()) {
            TextureAtlasSprite fluidSprite = state.fluidSprite;
            submitNodeCollector.submitCustomGeometry(poseStack, TRANSLUCENT, (pose, buffer) -> {
                for (BcQuad quad : state.fluidQuads) {
                    quad.mapUv(fluidSprite).emit(pose, buffer);
                }
            });
        }
        if (state.powerSprite != null && !state.powerQuads.isEmpty()) {
            TextureAtlasSprite powerSprite = state.powerSprite;
            submitNodeCollector.submitCustomGeometry(poseStack, CUTOUT, (pose, buffer) -> {
                for (BcQuad quad : state.powerQuads) {
                    quad.mapUv(powerSprite).emit(pose, buffer);
                }
            });
        }
        for (RenderableItem item : state.items) {
            poseStack.pushPose();
            poseStack.translate(item.x() - 0.5f * ITEM_SCALE, item.y() - 0.5f * ITEM_SCALE, item.z() - 0.5f * ITEM_SCALE);
            poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
            item.renderState().submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, -1);
            poseStack.popPose();
        }
    }

    /** One plug on one face: its plate quads plus the sprite to map them onto. */
    private record PlugVisual(List<BcQuad> quads, TextureAtlasSprite sprite) {
    }

    /** One travelling item resolved into an item-model render state, positioned block-relative. */
    private record RenderableItem(ItemStackRenderState renderState, float x, float y, float z) {
    }

    /** M4.6 render state of one pipe: body/plug/fluid/power geometry plus the travelling item visuals. */
    public static class State extends BlockEntityRenderState {
        /** Centre + arm quads, light-baked at extract time (see {@link BcPipeGeometry}). */
        public final List<BcQuad> bodyQuads = new ArrayList<>();
        /** The pipe body sprite ({@code buildcrafttransport:pipes/<stem>}). */
        @Nullable
        public TextureAtlasSprite bodySprite;
        /** The dyed skin quads (empty on colourless pipes; see the class javadoc). */
        public final List<BcQuad> skinQuads = new ArrayList<>();
        /** The skin overlay sprite ({@code buildcrafttransport:pipes/overlay_stained}). */
        @Nullable
        public TextureAtlasSprite skinSprite;
        /** One entry per plugged face. */
        public final List<PlugVisual> plugQuads = new ArrayList<>();
        /** The fluid column quads (empty on non-fluid or empty pipes). */
        public final List<BcQuad> fluidQuads = new ArrayList<>();
        @Nullable
        public TextureAtlasSprite fluidSprite;
        /** The power-flow core quads (empty on non-power or unpowered pipes). */
        public final List<BcQuad> powerQuads = new ArrayList<>();
        @Nullable
        public TextureAtlasSprite powerSprite;
        /** Travelling items (the client mirror), resolved for rendering. */
        public final List<RenderableItem> items = new ArrayList<>();
    }
}
