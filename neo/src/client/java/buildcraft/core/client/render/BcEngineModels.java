/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.client.render;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.joml.Vector3f;
import buildcraft.core.blockentity.EnumPowerStage;
import buildcraft.lib.client.render.BcQuad;
import buildcraft.lib.client.render.BcVertex;
import buildcraft.lib.expression.DefaultContexts;
import buildcraft.lib.expression.FunctionContext;
import buildcraft.lib.expression.api.NodeType;
import buildcraft.lib.expression.api.NodeTypes;
import buildcraft.lib.expression.node.value.NodeVariableDouble;
import buildcraft.lib.expression.node.value.NodeVariableObject;
import buildcraft.lib.model.json.JsonModelExpressionTypes;
import buildcraft.lib.model.json.JsonQuad;
import buildcraft.lib.model.json.JsonVariableModel;

/**
 * M4.4: loads the engine {@code .jsonbc} models and bakes one frame of them into {@link BcQuad}s &mdash; the client
 * counterpart of the legacy {@code BCCoreModels}/{@code BCEnergyModels} static holders. The engine jsonbc files are
 * animated through three model variables, so this class owns the single {@link FunctionContext} they are compiled
 * against and refreshes it before every bake:
 * <ul>
 * <li>{@code progress} &mdash; the piston position 0..1 (legacy {@code getProgressClient});</li>
 * <li>{@code stage} &mdash; the power stage (drives {@code trunk_tex} and the trunk's {@code stage_light});</li>
 * <li>{@code direction} &mdash; the output facing (the {@code builtin:rotate_facing} rule rotates the baked quads, so
 * the renderer itself needs no pose rotation).</li>
 * </ul>
 *
 * <p>The expression types mirror the legacy {@code ExpressionCompat}: {@code Facing.*} (see
 * {@link JsonModelExpressionTypes}, registered globally by the parser) and the engine power stage constants
 * ({@code stage == overheat}), which resolve because the power-stage {@link NodeType} is itself a parent
 * {@link FunctionContext} of the model context. The {@code (string)} cast function is what makes
 * {@code '#trunk_' + stage} compile.
 *
 * <p>Models are parsed lazily through the resource manager (the {@link JsonVariableModel.JsonSource}) and cached for
 * the session; only quad <em>positions</em>, texture ids and expression results are cached &mdash; sprites are
 * resolved against the block atlas at submit time (see {@link EngineBlockRenderer}).
 */
public final class BcEngineModels {

    /** One baked quad plus the atlas sprite id its texture string resolved to (kept paired for the submit pass). */
    public record TexturedQuad(SpriteId sprite, BcQuad quad) {
    }

    /** The engine power stage expression type (legacy {@code ExpressionCompat.ENUM_POWER_STAGE}). */
    private static final NodeType<EnumPowerStage> POWER_STAGE_TYPE;
    /** The model context: variables shared by every engine jsonbc file. */
    private static final FunctionContext MODEL_CTX;
    private static final NodeVariableDouble PROGRESS;
    private static final NodeVariableObject<EnumPowerStage> STAGE;
    private static final NodeVariableObject<Direction> DIRECTION;

    private static final Map<Identifier, JsonVariableModel> MODELS = new HashMap<>();

    /** Loads one jsonbc (Gson-parsed) out of the resource manager &mdash; the parent chain uses it too. */
    private static final JsonVariableModel.JsonSource SOURCE = id -> {
        Resource resource = Minecraft.getInstance().getResourceManager().getResourceOrThrow(id);
        try (Reader reader = resource.openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    };

    static {
        POWER_STAGE_TYPE = new NodeType<>("EnginePowerStage", EnumPowerStage.BLUE);
        NodeTypes.addType(POWER_STAGE_TYPE);
        POWER_STAGE_TYPE.put_t_o("(string)", String.class, EnumPowerStage::getSerializedName);
        for (EnumPowerStage stage : EnumPowerStage.values()) {
            POWER_STAGE_TYPE.putConstant("" + stage, stage);
        }
        MODEL_CTX = new FunctionContext("BuildCraft engines",//
            JsonModelExpressionTypes.ENUM_FACING, POWER_STAGE_TYPE,//
            DefaultContexts.createWithAll());
        PROGRESS = MODEL_CTX.putVariableDouble("progress");
        STAGE = MODEL_CTX.putVariableObject("stage", EnumPowerStage.class);
        DIRECTION = MODEL_CTX.putVariableObject("direction", Direction.class);
    }

    /**
     * Bakes one frame of the given engine model against the given render state, resolving the texture strings into
     * block-atlas sprite ids and applying the world light (each quad's jsonbc {@code light} value takes the block
     * light maximum, so the trunk visibly glows in the dark).
     */
    public static List<TexturedQuad> bake(Identifier modelId, float progress, EnumPowerStage stage,
        Direction direction, int packedWorldLight) {
        JsonVariableModel model = MODELS.computeIfAbsent(jsonbcResource(modelId), BcEngineModels::deserialize);
        PROGRESS.set(progress);
        STAGE.set(stage);
        DIRECTION.set(direction);
        List<JsonQuad> quads = model.bakeCutout(model::lookupTexture);
        List<TexturedQuad> out = new ArrayList<>(quads.size());
        for (JsonQuad quad : quads) {
            out.add(new TexturedQuad(spriteId(quad.texture), convert(quad, packedWorldLight)));
        }
        return out;
    }

    /** The renderer registrations hand the legacy jsonbc stem ({@code ns:models/tile/engine_stone}); the resource
     * manager needs the full {@code .jsonbc} filename (the parent chain resolves through that same full-filename
     * space, so normalising here keeps the model cache and the cycle-detection deque on one identity). */
    private static Identifier jsonbcResource(Identifier stem) {
        return stem.getPath().endsWith(".jsonbc") ? stem : stem.withSuffix(".jsonbc");
    }

    private static JsonVariableModel deserialize(Identifier modelId) {
        try {
            return JsonVariableModel.deserialize(modelId, new FunctionContext(modelId.toString(), MODEL_CTX), SOURCE);
        } catch (JsonParseException e) {
            throw new IllegalStateException("Failed to load the engine jsonbc model '" + modelId + "'", e);
        }
    }

    private static SpriteId spriteId(String texture) {
        return new SpriteId(TextureAtlas.LOCATION_BLOCKS, Identifier.parse(texture));
    }

    /** Converts one baked {@link JsonQuad} into an emit-ready {@link BcQuad}: world light merged with the quad's own
     * {@code light} value (packed as block&nbsp;|&nbsp;sky&nbsp;&lt;&lt;&nbsp;20, exactly
     * {@code LightCoordsUtil#pack}). The quad colour passes through untouched: the jsonbc {@code colour} expression is
     * the only vertex-colour source, matching the baseline pipeline where
     * {@code VariablePartCuboidBase#addQuads} emitted {@code RenderUtil.swapARGBforABGR(colour.evaluate())} verbatim.
     *
     * <p>Deliberately NOT ported here: the legacy per-face diffuse bake ({@code MutableQuad#multShade}, the
     * DOWN&nbsp;0.5&nbsp;/&nbsp;UP&nbsp;1.0&nbsp;/&nbsp;NS&nbsp;0.8&nbsp;/&nbsp;EW&nbsp;0.6 table that
     * {@code RenderEngine_BC8} applied once for 1.20.1's chunk {@code RenderType.cutout()}, whose terrain shader does
     * no directional lighting). The 26.1.2 render type this renderer submits through
     * ({@code Sheets.cutoutBlockSheet()} &rarr; entity cutout-cull) runs the entity vertex shader, which already folds
     * a normal-based directional diffuse ({@code minecraft_mix_light}, light.glsl) into the vertex colour &mdash;
     * baking the legacy table here as well shaded every horizontal face twice (east/west 0.6&times;0.498&nbsp;&asymp;&nbsp;0.30
     * of baseline, north/south 0.8&times;0.742, bottom 0.5&times;0.4&nbsp;=&nbsp;0.20), which rendered the engines
     * nearly black (M4.18b before-evidence: stone front face mean RGB&nbsp;8&ndash;15/255 vs texture&nbsp;76).
     * The engine jsonbc files all leave {@code shade} at its {@code true} default and no quad here carries a
     * non-white {@code colour}, so the entity diffuse is the single (vanilla-consistent) shade source. */
    private static BcQuad convert(JsonQuad quad, int packedWorldLight) {
        int worldBlock = (packedWorldLight >> 4) & 0xF;
        int worldSky = (packedWorldLight >> 20) & 0xF;
        int blockLight = Math.max(worldBlock, quad.blockLight);
        int light = blockLight << 4 | worldSky << 20;

        BcVertex[] vertices = new BcVertex[4];
        for (int i = 0; i < 4; i++) {
            JsonQuad.Vertex v = quad.vertices[i];
            vertices[i] = new BcVertex(new Vector3f(v.x, v.y, v.z), v.u, v.v, quad.colorArgb, light);
        }
        return new BcQuad(quad.face, quad.shade, -1, vertices[0], vertices[1], vertices[2], vertices[3]);
    }

    private BcEngineModels() {
    }
}
