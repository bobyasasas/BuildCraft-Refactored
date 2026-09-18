/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory.client.render;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.joml.Vector3f;
import buildcraft.lib.client.render.BcQuad;
import buildcraft.lib.client.render.BcVertex;
import buildcraft.lib.expression.DefaultContexts;
import buildcraft.lib.expression.FunctionContext;
import buildcraft.lib.expression.api.NodeType;
import buildcraft.lib.expression.api.NodeTypes;
import buildcraft.lib.expression.node.value.NodeVariableBoolean;
import buildcraft.lib.expression.node.value.NodeVariableObject;
import buildcraft.lib.model.json.JsonModelExpressionTypes;
import buildcraft.lib.model.json.JsonQuad;
import buildcraft.lib.model.json.JsonVariableModel;

/**
 * M4.7: loads the heat exchange {@code heat_exchange_static.jsonbc} and bakes it into {@link BcQuad}s &mdash; the
 * factory counterpart of {@code BcEngineModels}, with the legacy {@code TileHeatExchange} section model's variables:
 * {@code part} ({@code START}/{@code MIDDLE}/{@code END} &mdash; the multi-block tower sections; this milestone's
 * single-block slice always bakes {@code MIDDLE}), {@code connected_left}/{@code connected_right} (the pipe caps; the
 * v1 exchanger connects to nothing, so both stay false and the caps draw), and {@code direction} (the tower's facing;
 * the MIDDLE geometry is facing-symmetric so the renderer pins the baseline default {@code west}, making the jsonbc's
 * rotate rule a no-op).
 *
 * <p>Models are parsed lazily through the resource manager (the {@link JsonVariableModel.JsonSource}) and cached for
 * the session; sprites are resolved against the block atlas at submit time (see
 * {@link HeatExchangeBlockRenderer}).
 */
public final class BcHeatExchangeModels {

    /** One baked quad plus the atlas sprite id its texture string resolved to (kept paired for the submit pass). */
    public record TexturedQuad(SpriteId sprite, BcQuad quad) {
    }

    /** The legacy {@code TileHeatExchange} section kinds (the jsonbc {@code part} variable's constants). */
    public enum Part {
        START, MIDDLE, END
    }

    /** The heat exchange section expression type (the {@code part == MIDDLE} comparisons resolve through it). */
    private static final NodeType<Part> PART_TYPE;
    /** The model context: variables shared by the heat exchange jsonbc file. */
    private static final FunctionContext MODEL_CTX;
    private static final NodeVariableObject<Part> PART;
    private static final NodeVariableBoolean CONNECTED_LEFT;
    private static final NodeVariableBoolean CONNECTED_RIGHT;
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
        PART_TYPE = new NodeType<>("HeatExchangePart", Part.MIDDLE);
        NodeTypes.addType(PART_TYPE);
        for (Part part : Part.values()) {
            PART_TYPE.putConstant("" + part, part);
        }
        MODEL_CTX = new FunctionContext("BuildCraft factory heat exchange",//
            JsonModelExpressionTypes.ENUM_FACING, PART_TYPE,//
            DefaultContexts.createWithAll());
        PART = MODEL_CTX.putVariableObject("part", Part.class);
        CONNECTED_LEFT = MODEL_CTX.putVariableBoolean("connected_left");
        CONNECTED_RIGHT = MODEL_CTX.putVariableBoolean("connected_right");
        DIRECTION = MODEL_CTX.putVariableObject("direction", Direction.class);
    }

    /**
     * Bakes one frame of the given model, resolving the texture strings into block-atlas sprite ids and applying the
     * world light (the v1 slice always bakes the MIDDLE section, unconnected, facing west &mdash; see class javadoc).
     */
    public static List<TexturedQuad> bake(Identifier modelId, int packedWorldLight) {
        JsonVariableModel model = MODELS.computeIfAbsent(jsonbcResource(modelId), BcHeatExchangeModels::deserialize);
        PART.set(Part.MIDDLE);
        CONNECTED_LEFT.set(false);
        CONNECTED_RIGHT.set(false);
        DIRECTION.set(Direction.WEST);
        List<JsonQuad> quads = model.bakeCutout(model::lookupTexture);
        List<TexturedQuad> out = new ArrayList<>(quads.size());
        for (JsonQuad quad : quads) {
            out.add(new TexturedQuad(spriteId(quad.texture), convert(quad, packedWorldLight)));
        }
        return out;
    }

    /** The renderer registration hands the legacy jsonbc stem ({@code ns:models/tile/heat_exchange_static}); the
     * resource manager needs the full {@code .jsonbc} filename (normalised the {@code BcEngineModels} way so the
     * model cache and the cycle-detection deque stay on one identity). */
    private static Identifier jsonbcResource(Identifier stem) {
        return stem.getPath().endsWith(".jsonbc") ? stem : stem.withSuffix(".jsonbc");
    }

    private static JsonVariableModel deserialize(Identifier modelId) {
        try {
            return JsonVariableModel.deserialize(modelId, new FunctionContext(modelId.toString(), MODEL_CTX), SOURCE);
        } catch (JsonParseException e) {
            throw new IllegalStateException("Failed to load the heat exchange jsonbc model '" + modelId + "'", e);
        }
    }

    private static SpriteId spriteId(String texture) {
        return new SpriteId(TextureAtlas.LOCATION_BLOCKS, Identifier.parse(texture));
    }

    /** Converts one baked {@link JsonQuad} into an emit-ready {@link BcQuad}: world light merged with the quad's own
     * {@code light} value (packed as block&nbsp;|&nbsp;sky&nbsp;&lt;&lt;&nbsp;20, see {@code LightTexture#pack}) and
     * the vanilla per-face diffuse shade folded into the vertex colour (the {@code BcEngineModels} conversion, copied
     * for the factory slice). */
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
        BcQuad bcQuad = new BcQuad(quad.face, quad.shade, -1, vertices[0], vertices[1], vertices[2], vertices[3]);
        if (quad.shade) {
            float shade = switch (quad.face) {
                case DOWN -> 0.5f;
                case UP -> 1.0f;
                case NORTH, SOUTH -> 0.8f;
                case WEST, EAST -> 0.6f;
            };
            bcQuad = bcQuad.multiplyColor(shade, shade, shade, 1.0f);
        }
        return bcQuad;
    }

    private BcHeatExchangeModels() {
    }
}
