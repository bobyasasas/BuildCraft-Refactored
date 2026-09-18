/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * M4.2: builders for the baseline-static block/item model shapes, shared by {@link BcBlockStateProvider} and
 * {@link BcItemModelsProvider}.
 *
 * <p>Every method returns a plain {@link JsonObject}; serialisation (2-space indent, "parent" first, natural key
 * order, trailing newline) is the unchanged {@link BcDatagenJson#save} path, so the drift gate keeps proving the
 * shipped assets byte for byte. The two shapes the 1.20.1 baseline generated tree uses for its static blocks are:
 * <ol>
 * <li>a vanilla parent ({@code cube_all}/{@code cube}/{@code orientable}/{@code item/generated}/...) plus a texture
 * map — {@link #parented};</li>
 * <li>a parent-less elements model ({@code tank}, {@code chute}, {@code distiller}, the silicon tables, the robot
 * item base) — {@link #elements}.</li>
 * </ol>
 * Numeric conventions mirror the baseline files: {@code from}/{@code to} and face {@code rotation} are integers,
 * {@code uv} coordinates and display transform scales/translations are doubles (gson keeps the decimal point).
 */
final class BcModelJson {

    private BcModelJson() {
    }

    /** Ordered texture map: {@code tex("down", "ns:block/x", "up", "ns:block/y")}. */
    static Map<String, String> tex(String... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("BcModelJson.tex: odd number of arguments");
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }

    /** {@code {"parent": <parent>, "textures": {...}}} — the vanilla-parented static model shape; a null parent is
     * omitted (the parent-less fluid block models carry only their particle texture). */
    static JsonObject parented(String parent, Map<String, String> textures) {
        JsonObject model = new JsonObject();
        if (parent != null) {
            model.addProperty("parent", parent);
        }
        if (textures != null && !textures.isEmpty()) {
            model.add("textures", textures(textures));
        }
        return model;
    }

    /** {@code cube_all} with a single {@code all} texture. */
    static JsonObject cubeAll(String all) {
        return parented("minecraft:block/cube_all", tex("all", all));
    }

    /** Parent-less elements model with textures. */
    static JsonObject elements(Map<String, String> textures, Element... elements) {
        return elements(null, textures, elements);
    }

    /** Elements model: optional {@code parent} (the silicon tables parent {@code minecraft:block/block}), the
     * {@code elements} array and an optional texture map (a null/empty map is omitted — the robot item base carries
     * none). Key order is fixed at serialisation. */
    static JsonObject elements(String parent, Map<String, String> textures, Element... elements) {
        JsonObject model = new JsonObject();
        if (parent != null) {
            model.addProperty("parent", parent);
        }
        JsonArray array = new JsonArray();
        for (Element element : elements) {
            array.add(element.toJson());
        }
        model.add("elements", array);
        if (textures != null && !textures.isEmpty()) {
            model.add("textures", textures(textures));
        }
        return model;
    }

    /** Attaches/merges a display block (e.g. the {@code default_cube} hand transform, the robot item base). */
    static JsonObject withDisplay(JsonObject model, Map<String, Transform> display) {
        JsonObject displayObj = new JsonObject();
        for (Map.Entry<String, Transform> entry : display.entrySet()) {
            displayObj.add(entry.getKey(), entry.getValue().toJson());
        }
        model.add("display", displayObj);
        return model;
    }

    private static JsonObject textures(Map<String, String> textures) {
        JsonObject obj = new JsonObject();
        for (Map.Entry<String, String> entry : textures.entrySet()) {
            obj.addProperty(entry.getKey(), entry.getValue());
        }
        return obj;
    }

    /** One box element ({@code from}/{@code to} corners plus per-direction faces, in baseline face order). */
    static final class Element {
        private final double[] from;
        private final double[] to;
        private final Map<String, Face> faces = new LinkedHashMap<>();

        private Element(double[] from, double[] to) {
            this.from = from;
            this.to = to;
        }

        static Element of(int fx, int fy, int fz, int tx, int ty, int tz) {
            return of((double) fx, fy, fz, tx, ty, tz);
        }

        /** Fractional corners for the M4.3 frozen-variable item geometries (the engine's moving cuboid bakes at
         * y 7.198/11.198, the plugs at x 4.01). */
        static Element of(double fx, double fy, double fz, double tx, double ty, double tz) {
            return new Element(new double[]{fx, fy, fz}, new double[]{tx, ty, tz});
        }

        Element face(String dir, String texture) {
            faces.put(dir, new Face(texture, null, null, null));
            return this;
        }

        Element face(String dir, String texture, String cullface) {
            faces.put(dir, new Face(texture, null, cullface, null));
            return this;
        }

        Element face(String dir, String texture, String cullface, Integer rotation, double... uv) {
            faces.put(dir, new Face(texture, uv, cullface, rotation));
            return this;
        }

        JsonObject toJson() {
            JsonObject obj = new JsonObject();
            JsonObject facesObj = new JsonObject();
            for (Map.Entry<String, Face> entry : faces.entrySet()) {
                facesObj.add(entry.getKey(), entry.getValue().toJson());
            }
            obj.add("faces", facesObj);
            obj.add("from", coords(from));
            obj.add("to", coords(to));
            return obj;
        }
    }

    /** One face of an element: texture reference plus optional cullface, UV rectangle and 90-step rotation. */
    private record Face(String texture, double[] uv, String cullface, Integer rotation) {
        private JsonObject toJson() {
            JsonObject obj = new JsonObject();
            if (cullface != null) {
                obj.addProperty("cullface", cullface);
            }
            if (rotation != null) {
                obj.addProperty("rotation", rotation);
            }
            obj.addProperty("texture", texture);
            if (uv != null) {
                JsonArray uvArr = new JsonArray();
                for (double v : uv) {
                    uvArr.add(v);
                }
                obj.add("uv", uvArr);
            }
            return obj;
        }
    }

    /** One named display transform; any of rotation/scale/translation may be null. */
    record Transform(int[] rotation, double[] scale, double[] translation) {
        JsonObject toJson() {
            JsonObject obj = new JsonObject();
            if (rotation != null) {
                JsonArray arr = new JsonArray();
                for (int v : rotation) {
                    arr.add(v);
                }
                obj.add("rotation", arr);
            }
            if (scale != null) {
                JsonArray arr = new JsonArray();
                for (double v : scale) {
                    arr.add(v);
                }
                obj.add("scale", arr);
            }
            if (translation != null) {
                JsonArray arr = new JsonArray();
                for (double v : translation) {
                    arr.add(v);
                }
                obj.add("translation", arr);
            }
            return obj;
        }
    }

    private static JsonArray coords(double[] values) {
        JsonArray arr = new JsonArray();
        for (double v : values) {
            // whole values emit as integers (the established shipped convention, e.g. the robot base "from": [4, 4, 4]);
            // fractional values keep their decimals (gson writes 7.198, 4.01, ...)
            if (v == Math.floor(v) && !Double.isInfinite(v)) {
                arr.add((long) v);
            } else {
                arr.add(v);
            }
        }
        return arr;
    }
}
