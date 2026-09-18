/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.model.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import buildcraft.lib.expression.FunctionContext;
import buildcraft.lib.expression.GenericExpressionCompiler;
import buildcraft.lib.expression.api.IExpressionNode.INodeBoolean;
import buildcraft.lib.expression.api.IExpressionNode.INodeDouble;
import buildcraft.lib.expression.api.IExpressionNode.INodeLong;
import buildcraft.lib.expression.api.IExpressionNode.INodeObject;
import buildcraft.lib.expression.api.InvalidExpressionException;
import buildcraft.lib.expression.node.value.NodeConstantBoolean;
import buildcraft.lib.expression.node.value.NodeConstantLong;

/**
 * Minimal port of the legacy {@code buildcraft.lib.client.model.json.JsonVariableFaceUV} (M4.4): one face of a jsonbc
 * cuboid element &mdash; {@code uv} (four numbers or double expressions, 0..16 texture-pixel units),
 * {@code texture} (a string expression; a leading {@code #} means "reference the textures map"), {@code rotation}
 * (quarter turns), {@code visible}, {@code invert} and {@code both_sides}.
 */
public final class JsonVariableFaceUV {

    private final INodeDouble[] uv;
    private final INodeLong textureRotation;
    private final INodeBoolean visible;
    private final INodeBoolean invert;
    private final INodeBoolean bothSides;
    private final INodeObject<String> texture;

    public JsonVariableFaceUV(JsonObject json, FunctionContext fnCtx) {
        this.uv = readVariableUV(json, "uv", fnCtx);
        this.visible = json.has("visible") ? readVariableBoolean(json, "visible", fnCtx) : NodeConstantBoolean.TRUE;
        this.invert = json.has("invert") ? readVariableBoolean(json, "invert", fnCtx) : NodeConstantBoolean.FALSE;
        this.bothSides = json.has("both_sides") ? readVariableBoolean(json, "both_sides", fnCtx)
            : NodeConstantBoolean.FALSE;
        this.texture = readVariableString(json, "texture", fnCtx);
        this.textureRotation = json.has("rotation") ? readVariableLong(json, "rotation", fnCtx)
            : NodeConstantLong.ZERO;
    }

    private static INodeObject<String> readVariableString(JsonObject json, String member, FunctionContext fnCtx) {
        if (!json.has(member)) {
            throw new JsonSyntaxException("Required member " + member + " in '" + json + "'");
        }
        JsonElement elem = json.get(member);
        if (!elem.isJsonPrimitive()) {
            throw new JsonSyntaxException("Expected a string, but got '" + json + "'");
        }
        String asString = elem.getAsString();
        if (asString.startsWith("#")) {
            // Its a simple texture definition
            asString = "'" + asString + "'";
        }
        return convertStringToStringNode(asString, fnCtx);
    }

    private static INodeDouble[] readVariableUV(JsonObject obj, String member, FunctionContext fnCtx) {
        JsonElement elem = obj.get(member);
        if (elem == null || !elem.isJsonArray()) {
            throw new JsonSyntaxException("Expected an array '" + member + "' in " + obj);
        }
        JsonArray array = elem.getAsJsonArray();
        INodeDouble[] to = new INodeDouble[4];
        if (array.size() != 4) {
            throw new JsonSyntaxException("Expected exactly 4 doubles, but got " + array);
        }
        for (int i = 0; i < 4; i++) {
            to[i] = convertStringToDoubleNode(array.get(i).getAsString(), fnCtx);
        }
        return to;
    }

    private static INodeDouble convertStringToDoubleNode(String expression, FunctionContext context) {
        try {
            return GenericExpressionCompiler.compileExpressionDouble(expression, context);
        } catch (InvalidExpressionException e) {
            throw new JsonSyntaxException("Invalid expression " + expression, e);
        }
    }

    private static INodeObject<String> convertStringToStringNode(String expression, FunctionContext context) {
        try {
            return GenericExpressionCompiler.compileExpressionString(expression, context);
        } catch (InvalidExpressionException e) {
            throw new JsonSyntaxException("Invalid expression " + expression, e);
        }
    }

    static INodeBoolean convertStringToBooleanNode(String expression, FunctionContext context) {
        try {
            return GenericExpressionCompiler.compileExpressionBoolean(expression, context);
        } catch (InvalidExpressionException e) {
            throw new JsonSyntaxException("Invalid expression " + expression, e);
        }
    }

    static INodeLong convertStringToLongNode(String expression, FunctionContext context) {
        try {
            return GenericExpressionCompiler.compileExpressionLong(expression, context);
        } catch (InvalidExpressionException e) {
            throw new JsonSyntaxException("Invalid expression " + expression, e);
        }
    }

    static INodeBoolean readVariableBoolean(JsonObject obj, String member, FunctionContext context) {
        if (!obj.has(member)) {
            throw new JsonSyntaxException("Required '" + member + "' in '" + obj + "'");
        }
        JsonElement elem = obj.get(member);
        if (elem.isJsonPrimitive()) {
            return convertStringToBooleanNode(elem.getAsString(), context);
        }
        throw new JsonSyntaxException("Expected a string, got " + elem);
    }

    static INodeLong readVariableLong(JsonObject obj, String member, FunctionContext context) {
        if (!obj.has(member)) {
            throw new JsonSyntaxException("Required '" + member + "' in '" + obj + "'");
        }
        JsonElement elem = obj.get(member);
        if (elem.isJsonPrimitive()) {
            return convertStringToLongNode(elem.getAsString(), context);
        }
        throw new JsonSyntaxException("Expected a string, got " + elem);
    }

    /** Reads a 3-element {@code [x, y, z]} member of numbers or double expressions (legacy
     * {@code JsonVariableModelPart.readVariablePosition}). */
    static INodeDouble[] readVariablePosition(JsonObject obj, String member, FunctionContext fnCtx) {
        JsonElement elem = obj.get(member);
        if (elem == null || !elem.isJsonArray()) {
            throw new JsonSyntaxException("Expected an array '" + member + "' in " + obj);
        }
        JsonArray array = elem.getAsJsonArray();
        INodeDouble[] to = new INodeDouble[3];
        if (array.size() != 3) {
            throw new JsonSyntaxException("Expected exactly 3 floats, but got " + array);
        }
        for (int i = 0; i < 3; i++) {
            to[i] = convertStringToDoubleNode(array.get(i).getAsString(), fnCtx);
        }
        return to;
    }

    /** Legacy {@code JsonVariableModelPart.bakePosition}: evaluates the position expression triple into block space
     * (0..1). */
    static float[] bakePosition(INodeDouble[] in) {
        float x = (float) in[0].evaluate() / 16f;
        float y = (float) in[1].evaluate() / 16f;
        float z = (float) in[2].evaluate() / 16f;
        return new float[] { x, y, z };
    }

    /** Evaluates this face against the current variable values (legacy {@code #evaluate}): the authored uv rect is
     * normalised to 0..1 and then mapped into the referenced texture's own rect. */
    public JsonVariableModel.EvaluatedFace evaluate(JsonVariableModel.ITextureGetter spriteLookup) {
        JsonVariableModel.EvaluatedFace data = new JsonVariableModel.EvaluatedFace();
        JsonTexture face = spriteLookup.get(this.texture.evaluate());
        data.texture = face.location;
        data.rotations = (int) this.textureRotation.evaluate();
        float minU = (float) (this.uv[0].evaluate() / 16.0);
        float minV = (float) (this.uv[1].evaluate() / 16.0);
        float maxU = (float) (this.uv[2].evaluate() / 16.0);
        float maxV = (float) (this.uv[3].evaluate() / 16.0);
        data.uvs = new JsonUvRect(minU, minV, maxU, maxV).inParent(face.faceData);
        data.invertNormal = this.invert.evaluate();
        data.bothSides = this.bothSides.evaluate();
        return data;
    }

    public boolean isVisible() {
        return this.visible.evaluate();
    }
}
