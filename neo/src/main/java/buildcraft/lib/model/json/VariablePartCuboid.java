/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.model.json;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.Direction;
import buildcraft.lib.expression.FunctionContext;
import buildcraft.lib.expression.api.IExpressionNode.INodeBoolean;
import buildcraft.lib.expression.api.IExpressionNode.INodeDouble;
import buildcraft.lib.expression.api.IExpressionNode.INodeLong;
import buildcraft.lib.expression.node.value.NodeConstantBoolean;
import buildcraft.lib.expression.node.value.NodeConstantLong;

/**
 * Minimal port of the legacy {@code buildcraft.lib.client.model.json.VariablePartCuboid} + its
 * {@code VariablePartCuboidBase} (M4.4; the minimal parser supports cuboid elements only): a jsonbc {@code elements}
 * entry with the animatable {@code from}/{@code to} positions, {@code visible}, {@code shade}, {@code light} and
 * {@code colour} expressions plus 1&ndash;6 {@code faces}. Baked quads inherit the element-level {@code invert} /
 * {@code both_sides} defaults into each face, exactly like the legacy constructor mutated the face json.
 */
public final class VariablePartCuboid {

    private final INodeDouble[] from;
    private final INodeDouble[] to;
    private final INodeBoolean visible;
    private final INodeBoolean shade;
    private final INodeLong light;
    private final INodeLong colour;
    private final Map<Direction, JsonVariableFaceUV> faces = new HashMap<>();

    public VariablePartCuboid(JsonObject obj, FunctionContext fnCtx) {
        this.from = JsonVariableFaceUV.readVariablePosition(obj, "from", fnCtx);
        this.to = JsonVariableFaceUV.readVariablePosition(obj, "to", fnCtx);
        this.shade = obj.has("shade") ? JsonVariableFaceUV.readVariableBoolean(obj, "shade", fnCtx)
            : NodeConstantBoolean.TRUE;
        this.visible = obj.has("visible") ? JsonVariableFaceUV.readVariableBoolean(obj, "visible", fnCtx)
            : NodeConstantBoolean.TRUE;
        this.light = obj.has("light") ? JsonVariableFaceUV.readVariableLong(obj, "light", fnCtx)
            : NodeConstantLong.ZERO;
        this.colour = obj.has("colour") ? JsonVariableFaceUV.readVariableLong(obj, "colour", fnCtx)
            : new NodeConstantLong(-1);

        if (!obj.has("faces")) {
            throw new JsonSyntaxException("Expected between 1 and 6 faces, got nothing");
        }
        JsonElement elem = obj.get("faces");
        if (!elem.isJsonObject()) {
            throw new JsonSyntaxException("Expected between 1 and 6 faces, got '" + elem + "'");
        }
        JsonObject jFaces = elem.getAsJsonObject();
        boolean hasInvertDefault = obj.has("invert");
        String invertDefault = hasInvertDefault ? obj.get("invert").getAsString() : null;
        boolean hasBothSidesDefault = obj.has("both_sides");
        String bothSidesDefault = hasBothSidesDefault ? obj.get("both_sides").getAsString() : null;
        for (Direction face : Direction.values()) {
            if (!jFaces.has(face.getName())) {
                continue;
            }
            JsonElement jFace = jFaces.get(face.getName());
            if (!jFace.isJsonObject()) {
                throw new JsonSyntaxException("Expected an object, but got " + jFace);
            }
            JsonObject jFaceObj = jFace.getAsJsonObject();
            // The legacy constructor *mutated* the parsed json to inject the cuboid-level defaults; here the
            // fallbacks are applied at read time instead (same semantics, no json mutation).
            if (invertDefault != null && !jFaceObj.has("invert")) {
                jFaceObj.addProperty("invert", invertDefault);
            }
            if (bothSidesDefault != null && !jFaceObj.has("both_sides")) {
                jFaceObj.addProperty("both_sides", bothSidesDefault);
            }
            this.faces.put(face, new JsonVariableFaceUV(jFaceObj, fnCtx));
        }
        if (this.faces.isEmpty()) {
            throw new JsonSyntaxException("Expected between 1 and 6 faces, got an empty object " + jFaces);
        }
    }

    /** Legacy {@code VariablePartCuboidBase#addQuads}: bakes the six (or fewer) faces of this cuboid against the
     * current variable values, applying texture rotation, light, colour, shade and inverted/both-sides handling. */
    public void addQuads(List<JsonQuad> addTo, JsonVariableModel.ITextureGetter spriteLookup) {
        if (!this.visible.evaluate()) {
            return;
        }
        float[] f = JsonVariableFaceUV.bakePosition(this.from);
        float[] t = JsonVariableFaceUV.bakePosition(this.to);
        boolean s = this.shade.evaluate();
        int l = (int) (this.light.evaluate() & 15);
        int rgba = (int) this.colour.evaluate();
        for (Direction face : Direction.values()) {
            JsonVariableFaceUV var = this.faces.get(face);
            if (var == null || !var.isVisible()) {
                continue;
            }
            JsonVariableModel.EvaluatedFace data = var.evaluate(spriteLookup);
            float[] radius = { (t[0] - f[0]) * 0.5f, (t[1] - f[1]) * 0.5f, (t[2] - f[2]) * 0.5f };
            float[] centre = { f[0] + radius[0], f[1] + radius[1], f[2] + radius[2] };
            JsonQuad quad = JsonModelGeometry.createFace(face, centre, radius, data.uvs);
            quad.rotateTextureUp(data.rotations);
            quad.blockLight = l;
            quad.colorArgb = rgba;
            quad.texture = data.texture;
            quad.shade = s;
            if (data.bothSides) {
                addTo.add(quad.copyAndInvertNormal());
            } else if (data.invertNormal) {
                quad = quad.copyAndInvertNormal();
            }
            addTo.add(quad);
        }
    }
}
