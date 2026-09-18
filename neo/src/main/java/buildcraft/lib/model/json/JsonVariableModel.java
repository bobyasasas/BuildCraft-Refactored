/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.model.json;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import net.minecraft.resources.Identifier;
import buildcraft.lib.expression.FunctionContext;
import buildcraft.lib.expression.InternalCompiler;
import buildcraft.lib.expression.api.IConstantNode;
import buildcraft.lib.expression.api.IExpressionNode;
import buildcraft.lib.expression.api.InvalidExpressionException;
import buildcraft.lib.expression.node.value.NodeUpdatable;

/**
 * Minimal port of the legacy {@code buildcraft.lib.client.model.json.JsonVariableModel} (M4.4): loads a
 * {@code .jsonbc} variable model and bakes its cutout elements into {@link JsonQuad}s against a caller-provided
 * {@link FunctionContext} (the caller owns the animatable variables &mdash; e.g. the engine renderer's
 * {@code progress}/{@code stage}/{@code direction}).
 *
 * <p>Ported capability set (what the engine models use, and what M4.7's {@code heat_exchange_static.jsonbc} needs):
 * <ul>
 * <li>{@code textures} &mdash; id strings, {@code #reference} chains (max {@link #MAX_TEXTURE_LOOKUPS} hops) and the
 * object form with a {@code uv} sub-rect (see {@link JsonTexture}); merged along the {@code parent} chain
 * (opt-out via {@code textures_reset});</li>
 * <li>{@code parent} &mdash; loaded as {@code <parent>.jsonbc}, with the legacy merge flags
 * {@code textures_reset}/{@code cutout_replace}/{@code rules_replace};</li>
 * <li>{@code variables} &mdash; plain constant/expression values compiled into the model's context
 * (legacy {@code JsonVariableObject#putVariables}: lowercased names, local duplicates are an error, overriding an
 * inherited variable keeps the inherited value); the legacy stateful/getter form is rejected;</li>
 * <li>{@code elements} / cutout cuboids (see {@link VariablePartCuboid}); the legacy {@code cutout},
 * {@code translucent}, {@code face}, {@code led}, {@code texture_expand} and {@code container} constructs are
 * rejected &mdash; extend the parser if a future model needs them;</li>
 * <li>{@code rules} &mdash; {@code builtin:rotate_facing} only (see {@link JsonModelRule}).</li>
 * </ul>
 *
 * <p>Unlike the legacy class this knows nothing about {@code ResourceManager}s or sprite atlases: json loading is
 * delegated to a {@link JsonSource} (the client passes a resource-manager-backed source; tests and datagen pass
 * classpath/memory sources) and textures stay as ids &mdash; the renderer resolves them against the block atlas.
 */
public final class JsonVariableModel {

    public static final int MAX_TEXTURE_LOOKUPS = 10;

    /** The texture map of this model (its own entries plus the parent chain's, unless {@code textures_reset}). */
    public final Map<String, JsonTexture> textures = new HashMap<>();
    public final boolean ambientOcclusion;

    private final Map<String, NodeUpdatable> variableNodes = new LinkedHashMap<>();
    /** Every variable name this model (or its parent chain) defines, so child models can inherit by name. */
    private final Set<String> definedVariables = new HashSet<>();
    /** The parent chain's variable names: our own {@code variables} must not redefine them (legacy drop). */
    private final Set<String> inheritedVariables = new HashSet<>();
    private final List<VariablePartCuboid> cutoutElements = new ArrayList<>();
    private final List<JsonModelRule> rules = new ArrayList<>();

    /** Loads one jsonbc file (Gson-parsed) by id; implemented by the caller (resource manager, classpath, memory). */
    @FunctionalInterface
    public interface JsonSource {
        JsonObject load(Identifier from) throws IOException;
    }

    /** Resolves one texture lookup string to a (chained) {@link JsonTexture} &mdash; the bake-time counterpart of
     * the legacy {@code ITextureGetter}. */
    @FunctionalInterface
    public interface ITextureGetter {
        JsonTexture get(String location);
    }

    /** One evaluated cuboid face: the resolved texture id, its mapped uv rect, quarter-turn texture rotation and the
     * invert/both-sides flags (legacy {@code VariablePartCuboidBase.VariableFaceData}, minus the sprite). */
    public static final class EvaluatedFace {
        public String texture = "";
        public JsonUvRect uvs = new JsonUvRect();
        public int rotations = 0;
        public boolean invertNormal = false;
        public boolean bothSides = false;
    }

    /** Parses {@code <id>} (with its {@code parent} chain) out of the given source, compiling expressions into
     * {@code fnCtx}. The context should be owned by this call (pass a child of the renderer's context, like the
     * legacy {@code ModelHolderVariable} callers did). */
    public static JsonVariableModel deserialize(Identifier id, FunctionContext fnCtx, JsonSource source)
        throws JsonParseException {
        Deque<Identifier> loading = new ArrayDeque<>();
        return deserialize(id, fnCtx, source, loading);
    }

    private static JsonVariableModel deserialize(Identifier id, FunctionContext fnCtx, JsonSource source,
        Deque<Identifier> loading) {
        if (loading.contains(id)) {
            throw new JsonParseException("Circular jsonbc parent chain: " + loading + " -> " + id);
        }
        JsonObject obj;
        try {
            loading.addLast(id);
            obj = source.load(id);
            if (obj == null) {
                throw new IOException("No jsonbc source contained '" + id + "'");
            }
            // constructed INSIDE the try: the parent chain loads while `id` is still on the deque, which is what
            // makes the circular-chain check above fire
            return new JsonVariableModel(id, obj, fnCtx, source, loading);
        } catch (IOException e) {
            throw new JsonParseException("Didn't find the jsonbc model '" + id + "'!", e);
        } finally {
            loading.removeLast();
        }
    }

    private JsonVariableModel(Identifier id, JsonObject obj, FunctionContext fnCtx, JsonSource source,
        Deque<Identifier> loading) {
        boolean ambf = false;

        if (obj.has("parent")) {
            String parentName = obj.get("parent").getAsString() + ".jsonbc";
            int sep = parentName.indexOf(':');
            Identifier parentId = Identifier.fromNamespaceAndPath(parentName.substring(0, sep),
                parentName.substring(sep + 1));
            JsonVariableModel parent = deserialize(parentId, fnCtx, source, loading);
            ambf = parent.ambientOcclusion;
            if (!getBoolean(obj, "textures_reset", false)) {
                this.textures.putAll(parent.textures);
            }
            // The parent's compiled variables drive the inherited elements, so they must keep being refreshed by
            // every bake of THIS model: inherit the updatable nodes (parents first) and their names (a child's
            // 'variables' entry for an inherited name is dropped, legacy "inherited variables keep their value").
            this.inheritedVariables.addAll(parent.definedVariables);
            this.variableNodes.putAll(parent.variableNodes);
            if (!getBoolean(obj, "cutout_replace", false)) {
                this.cutoutElements.addAll(parent.cutoutElements);
            }
            if (!getBoolean(obj, "rules_replace", false)) {
                this.rules.addAll(parent.rules);
            }
        }

        this.ambientOcclusion = getBoolean(obj, "ambientocclusion", ambf);
        deserializeTextures(obj.get("textures"));
        if (obj.has("variables")) {
            // No context wrapping: the variables land directly in the caller's context so the whole parent chain
            // (and the caller) sees the same nodes, exactly like the caller-supplied progress/stage/direction.
            putVariables(getObject(obj, "variables"), fnCtx);
        }

        if (obj.has("translucent") || obj.has("cutout")) {
            throw new JsonSyntaxException(
                "The minimal jsonbc parser only supports the 'elements' member (got 'cutout'/'translucent') in " + id);
        }
        if (obj.has("elements")) {
            JsonElement elem = obj.get("elements");
            if (!elem.isJsonArray()) {
                throw new JsonSyntaxException("Expected an array 'elements', got '" + elem + "'");
            }
            for (JsonElement part : elem.getAsJsonArray()) {
                this.cutoutElements.add(deserializeElement(part, fnCtx));
            }
        }

        if (obj.has("rules")) {
            JsonElement elem = obj.get("rules");
            if (!elem.isJsonArray()) {
                throw new JsonSyntaxException("Expected an array 'rules', got '" + elem + "'");
            }
            for (JsonElement rule : elem.getAsJsonArray()) {
                this.rules.add(JsonModelRule.deserialize(rule, fnCtx));
            }
        }
    }

    private static VariablePartCuboid deserializeElement(JsonElement json, FunctionContext fnCtx) {
        if (!json.isJsonObject()) {
            throw new JsonSyntaxException("Expected an object, got " + json);
        }
        JsonObject obj = json.getAsJsonObject();
        String type = "cuboid";
        if (obj.has("type")) {
            JsonElement jType = obj.get("type");
            if (!jType.isJsonPrimitive()) {
                throw new JsonSyntaxException("Expected a string, got " + jType);
            }
            type = jType.getAsString();
        }
        if (!"cuboid".equals(type)) {
            throw new JsonSyntaxException("Unknown element type '" + type
                + "' -- the minimal jsonbc parser only supports 'cuboid'");
        }
        return new VariablePartCuboid(obj, fnCtx);
    }

    private void deserializeTextures(JsonElement elem) {
        if (elem == null) {
            return;
        }
        if (!elem.isJsonObject()) {
            throw new JsonSyntaxException("Expected to find an object for 'textures', but found " + elem);
        }
        JsonObject obj = elem.getAsJsonObject();
        for (Entry<String, JsonElement> entry : obj.entrySet()) {
            String name = entry.getKey();
            JsonElement tex = entry.getValue();
            JsonTexture texture;
            if (tex.isJsonPrimitive() && tex.getAsJsonPrimitive().isString()) {
                texture = new JsonTexture(tex.getAsString());
            } else if (tex.isJsonObject()) {
                texture = new JsonTexture(tex.getAsJsonObject());
            } else {
                throw new JsonSyntaxException("Expected a string or an object, but got " + tex);
            }
            this.textures.put(name, texture);
        }
    }

    /** Minimal port of the legacy {@code JsonVariableObject#putVariables}: plain constant/expression values only. */
    private void putVariables(JsonObject values, FunctionContext fnCtx) {
        for (Entry<String, JsonElement> entry : values.entrySet()) {
            String name = entry.getKey().toLowerCase(Locale.ROOT);
            if (this.inheritedVariables.contains(name)) {
                // The legacy behaviour: the inherited variable keeps its node, the redefinition is dropped
                continue;
            }
            if (fnCtx.hasLocalVariable(name)) {
                throw new JsonSyntaxException("Duplicate local variable '" + name + "'");
            } else if (fnCtx.getVariable(name) != null) {
                // Allow overriding of higher up variables (caller-supplied ones like progress/stage/direction)
                continue;
            }
            JsonElement value = entry.getValue();
            if (value.isJsonObject()) {
                throw new JsonSyntaxException(
                    "Stateful variables ({type/getter}) are not supported by the minimal jsonbc parser: '" + name + "'");
            }
            if (!value.isJsonPrimitive()) {
                throw new JsonSyntaxException("Expected a primitive, got " + value + " for the variable '" + name + "'");
            }
            String expression = value.getAsString();
            IExpressionNode node;
            try {
                node = InternalCompiler.compileExpression(expression, new FunctionContext("Value Object", fnCtx));
            } catch (InvalidExpressionException e) {
                throw new JsonSyntaxException("Failed to compile variable " + name, e);
            }
            this.definedVariables.add(name);
            if (node instanceof IConstantNode) {
                // No point in adding it to variables
                fnCtx.putVariable(name, node);
                continue;
            }
            NodeUpdatable nodeUpdatable = new NodeUpdatable(name, node);
            this.variableNodes.put(name, nodeUpdatable);
            fnCtx.putVariable(name, nodeUpdatable.variable);
        }
    }

    /** Legacy {@code lookupTexture}: follows {@code #} references through the textures map (up to
     * {@link #MAX_TEXTURE_LOOKUPS} hops), keeping the referenced texture's uv rect along the way. */
    public JsonTexture lookupTexture(String lookup) {
        int attempts = 0;
        JsonTexture texture = new JsonTexture(lookup);
        while (texture.location.startsWith("#") && attempts < MAX_TEXTURE_LOOKUPS) {
            JsonTexture tex = this.textures.get(texture.location);
            if (tex == null) {
                break;
            }
            texture = texture.inParent(tex);
            attempts++;
        }
        if (texture.location.startsWith("#")) {
            throw new JsonSyntaxException("Unknown texture reference '" + texture.location + "' (looked up '"
                + lookup + "' in " + this.textures.keySet() + ")");
        }
        return texture;
    }

    /** Refreshes the model's variables (in definition order, parents first) and bakes the cutout elements plus any
     * matching rules (legacy {@code getCutoutQuads} = {@code bakePart(cutoutElements, this::lookupTexture)}). */
    public List<JsonQuad> bakeCutout(JsonVariableModel.ITextureGetter spriteLookup) {
        for (NodeUpdatable node : this.variableNodes.values()) {
            node.refresh();
        }
        List<JsonQuad> list = new ArrayList<>();
        for (VariablePartCuboid part : this.cutoutElements) {
            part.addQuads(list, spriteLookup);
        }
        for (JsonModelRule rule : this.rules) {
            if (rule.when.evaluate()) {
                rule.apply(list);
            }
        }
        return list;
    }

    private static JsonObject getObject(JsonObject obj, String member) {
        JsonElement elem = obj.get(member);
        if (elem == null || !elem.isJsonObject()) {
            throw new JsonSyntaxException("Expected an object '" + member + "' in " + obj);
        }
        return elem.getAsJsonObject();
    }

    private static boolean getBoolean(JsonObject obj, String member, boolean fallback) {
        JsonElement elem = obj.get(member);
        if (elem == null) {
            return fallback;
        }
        if (!elem.isJsonPrimitive() || !elem.getAsJsonPrimitive().isBoolean()) {
            throw new JsonSyntaxException("Expected a boolean '" + member + "' in " + obj);
        }
        return elem.getAsBoolean();
    }
}
