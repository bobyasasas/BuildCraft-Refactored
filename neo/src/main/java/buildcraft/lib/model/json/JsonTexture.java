/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.model.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import buildcraft.lib.expression.DefaultContexts;
import buildcraft.lib.expression.GenericExpressionCompiler;
import buildcraft.lib.expression.api.InvalidExpressionException;

/**
 * Minimal port of the legacy {@code buildcraft.lib.client.model.json.JsonTexture} (M4.4): one entry of a jsonbc
 * {@code textures} map &mdash; a texture id string (possibly a {@code #reference} into the same map) plus an optional
 * sub-rectangle of that texture authored in 0..16 units ({@code {"location": ..., "uv": [minU, minV, maxU, maxV]}}).
 */
public final class JsonTexture {

    /** The texture id or {@code #reference}; resolved to a real id by {@link JsonVariableModel#lookupTexture}. */
    public final String location;
    /** The sub-rectangle this entry maps onto the texture it references (full texture by default). */
    public final JsonUvRect faceData;

    public JsonTexture(String location) {
        this(location, new JsonUvRect());
    }

    public JsonTexture(String location, JsonUvRect faceData) {
        this.location = location;
        this.faceData = faceData;
    }

    /** The object form: {@code {"location": "<id>", "uv": [minU, minV, maxU, maxV]}} where every uv element is either
     * a number or a (constant) double expression, in 0..16 texture-pixel units and clamped to the texture. */
    public JsonTexture(JsonObject obj) {
        try {
            JsonElement locationElem = obj.get("location");
            if (locationElem == null || !locationElem.isJsonPrimitive()) {
                throw new JsonSyntaxException("Expected a string 'location' in " + obj);
            }
            this.location = locationElem.getAsString();
            JsonElement uvElem = obj.get("uv");
            if (uvElem == null || !uvElem.isJsonArray()) {
                throw new JsonSyntaxException("Expected an array 'uv' in " + obj);
            }
            JsonArray uvs = uvElem.getAsJsonArray();
            if (uvs.size() != 4) {
                throw new JsonSyntaxException("Must have 4 elements (uMin, vMin, uMax, vMax), got " + uvs);
            }
            double[] arr = new double[4];
            for (int i = 0; i < 4; i++) {
                JsonElement elem = uvs.get(i);
                if (elem.isJsonPrimitive() && elem.getAsJsonPrimitive().isNumber()) {
                    arr[i] = elem.getAsDouble();
                } else if (elem.isJsonPrimitive() && elem.getAsJsonPrimitive().isString()) {
                    try {
                        arr[i] = GenericExpressionCompiler.compileExpressionDouble(//
                            elem.getAsString(), DefaultContexts.createWithAll()).evaluate();
                    } catch (InvalidExpressionException e) {
                        throw new JsonSyntaxException("in " + elem, e);
                    }
                } else {
                    throw new JsonSyntaxException("Expected a number or a double expression, got " + elem);
                }
            }
            this.faceData = JsonUvRect.from16Clamped(arr[0], arr[1], arr[2], arr[3]);
        } catch (JsonSyntaxException jse) {
            throw new JsonSyntaxException("in " + obj, jse);
        }
    }

    public JsonTexture andSub(JsonTexture sub) {
        return new JsonTexture(this.location, this.faceData.andSub(sub.faceData));
    }

    public JsonTexture inParent(JsonTexture parent) {
        return parent.andSub(this);
    }

    @Override
    public String toString() {
        return "location = " + this.location + ", uvs = " + this.faceData;
    }
}
