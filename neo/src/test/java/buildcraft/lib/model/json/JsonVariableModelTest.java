/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.model.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import buildcraft.lib.expression.FunctionContext;
import buildcraft.lib.expression.node.value.NodeVariableDouble;
import buildcraft.lib.expression.node.value.NodeVariableLong;
import buildcraft.lib.expression.node.value.NodeVariableObject;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

/**
 * M4.4 unit tests for the minimal jsonbc parser: every fixture is an inline string loaded through an in-memory
 * {@link JsonVariableModel.JsonSource}, so the tests run without a resource manager. Covers the bake pipeline
 * (cuboids, normalised positions, variable-driven geometry, texture chains, the rotate_facing rule,
 * light/colour/shade, both_sides), the parent-chain merge with the legacy variable-inheritance semantics, and the
 * fail-closed error paths.
 */
public class JsonVariableModelTest {

    private static final Identifier MODEL_ID = Identifier.parse("test:models/tile/model");

    /** Parses one inline jsonbc (with no parents) against a fresh default context. */
    private static JsonVariableModel parse(String json) {
        Map<Identifier, JsonObject> sources = new HashMap<>();
        sources.put(MODEL_ID, JsonParser.parseString(json).getAsJsonObject());
        return JsonVariableModel.deserialize(MODEL_ID, new FunctionContext("test"), sources::get);
    }

    /** The project pins JUnit 4.12 (no assertThrows): expects the action to throw the given
     * {@link JsonParseException} subtype ({@link JsonSyntaxException} included). */
    private static void assertFails(Class<? extends JsonParseException> type, Runnable action) {
        try {
            action.run();
        } catch (JsonParseException e) {
            assertTrue("Expected " + type.getSimpleName() + " but got " + e.getClass().getSimpleName(),//
                type.isInstance(e));
            return;
        }
        fail("Expected " + type.getSimpleName() + " but nothing was thrown");
    }

    /** A minimal one-cuboid model with all six faces on the {@code #tex} texture. */
    private static final String SIMPLE = """
            {
                "textures": { "#tex": "test:block/tex" },
                "elements": [
                    {
                        "from": [ 0, 4, 0 ],
                        "to": [ 16, 16, 16 ],
                        "faces": {
                            "down":  { "uv": [ 0, 0, 16, 16 ], "texture": "'#tex'" },
                            "up":    { "uv": [ 0, 0, 16, 16 ], "texture": "'#tex'" },
                            "north": { "uv": [ 0, 0, 16, 4 ], "texture": "'#tex'" },
                            "south": { "uv": [ 0, 0, 16, 4 ], "texture": "'#tex'" },
                            "west":  { "uv": [ 0, 0, 16, 4 ], "texture": "'#tex'" },
                            "east":  { "uv": [ 0, 0, 16, 4 ], "texture": "'#tex'" }
                        }
                    }
                ]
            }
            """;

    @Test
    public void simpleModelBakesSixQuads() {
        JsonVariableModel model = parse(SIMPLE);
        JsonTexture tex = new JsonTexture("test:block/tex");
        List<JsonQuad> quads = model.bakeCutout(lookup -> tex);
        assertEquals(6, quads.size());
        // every face is present exactly once, with the resolved (un-referenced) texture id
        for (Direction face : Direction.values()) {
            JsonQuad quad = quads.stream().filter(q -> q.face == face).findFirst().orElse(null);
            assertNotNull("missing face " + face, quad);
            assertEquals("test:block/tex", quad.texture);
        }
    }

    @Test
    public void cuboidPositionsAreNormalised() {
        JsonVariableModel model = parse(SIMPLE);
        JsonTexture tex = new JsonTexture("test:block/tex");
        List<JsonQuad> quads = model.bakeCutout(lookup -> tex);
        // the up face of the from [0,4,0] to [16,16,16] cuboid sits at y = 16/16 = 1
        JsonQuad up = quads.stream().filter(q -> q.face == Direction.UP).findFirst().orElseThrow();
        for (JsonQuad.Vertex v : up.vertices) {
            assertEquals(1.0f, v.y, 1e-6f);
        }
        // the up-face uv covers the full [0,1] rect
        float minU = Float.MAX_VALUE, maxU = -Float.MAX_VALUE;
        for (JsonQuad.Vertex v : up.vertices) {
            minU = Math.min(minU, v.u);
            maxU = Math.max(maxU, v.u);
        }
        assertEquals(0.0f, minU, 1e-6f);
        assertEquals(1.0f, maxU, 1e-6f);
    }

    @Test
    public void animatedPositionAndUvFollowVariable() {
        // the engine pattern: a variable-driven 'to.y' expression over the caller-owned progress variable
        String json = """
                {
                    "elements": [
                        {
                            "from": [ 3, 4, 3 ],
                            "to": [ 13, "4 + progress_size", 13 ],
                            "faces": {
                                "north": { "uv": [ 3, "progress_size", 13, 0 ], "texture": "'#tex'" }
                            }
                        }
                    ]
                }
                """;
        FunctionContext ctx = new FunctionContext("test");
        // the variable is referenced by name from the jsonbc, so it must be defined under that name
        NodeVariableDouble progress = ctx.putVariableDouble("progress_size");
        Map<Identifier, JsonObject> sources = new HashMap<>();
        sources.put(MODEL_ID, JsonParser.parseString(json).getAsJsonObject());
        JsonVariableModel model = JsonVariableModel.deserialize(MODEL_ID, ctx, sources::get);

        // progress_size = 0.25 -> to.y = (4 + 0.25) / 16, i.e. the cuboid grows with the variable
        progress.set(0.25);
        JsonTexture tex = new JsonTexture("test:block/tex");
        List<JsonQuad> quads = model.bakeCutout(lookup -> tex);
        assertEquals(1, quads.size());
        JsonQuad north = quads.get(0);
        assertEquals((4 + 0.25) / 16.0, maxY(north), 1e-4);
        // the uv's v extent spans [0, progress_size]/16 (authored as [3, progress_size, 13, 0])
        assertEquals(0.25 / 16.0, maxV(north), 1e-4);
        // and the NEXT bake follows the new value (the cuboid positions re-evaluate per bake)
        progress.set(0.75);
        quads = model.bakeCutout(lookup -> tex);
        assertEquals((4 + 0.75) / 16.0, maxY(quads.get(0)), 1e-4);
        assertEquals(0.75 / 16.0, maxV(quads.get(0)), 1e-4);
    }

    private static float maxY(JsonQuad quad) {
        float max = -Float.MAX_VALUE;
        for (JsonQuad.Vertex v : quad.vertices) {
            max = Math.max(max, v.y);
        }
        return max;
    }

    private static float maxV(JsonQuad quad) {
        float max = -Float.MAX_VALUE;
        for (JsonQuad.Vertex v : quad.vertices) {
            max = Math.max(max, v.v);
        }
        return max;
    }

    @Test
    public void textureChainsCarryParentUv() {
        // '#a' -> '#b' -> the real texture; the object-form '#b' rect maps the face uv into itself
        String json = """
                {
                    "textures": {
                        "#tex": "test:block/tex",
                        "#b": { "location": "#tex", "uv": [ 8, 0, 16, 8 ] },
                        "#a": "#b"
                    },
                    "elements": [
                        {
                            "from": [ 0, 0, 0 ],
                            "to": [ 16, 16, 16 ],
                            "faces": {
                                "up": { "uv": [ 0, 0, 16, 16 ], "texture": "'#a'" }
                            }
                        }
                    ]
                }
                """;
        JsonVariableModel model = parse(json);
        JsonTexture tex = model.lookupTexture("#a");
        assertEquals("test:block/tex", tex.location);
        // the chained rect: the face's full [0,1] uv mapped into the [8,0,16,8]/16 sub-rect
        assertEquals(0.5f, tex.faceData.minU, 1e-6f);
        assertEquals(0.0f, tex.faceData.minV, 1e-6f);
        assertEquals(1.0f, tex.faceData.maxU, 1e-6f);
        assertEquals(0.5f, tex.faceData.maxV, 1e-6f);
    }

    @Test
    public void parentChainMergesTexturesElementsRulesAndVariables() {
        String parent = """
                {
                    "textures": { "#base": "test:block/base" },
                    "variables": { "progress_size": "progress * 15.99" },
                    "rules": [
                        { "when": "direction != Facing.UP", "type": "builtin:rotate_facing",
                          "from": "Facing.UP", "to": "direction" }
                    ],
                    "elements": [
                        { "from": [ 0, 0, 0 ], "to": [ 16, 4, 16 ],
                          "faces": { "up": { "uv": [ 0, 0, 16, 16 ], "texture": "'#base'" } } }
                    ]
                }
                """;
        String child = """
                {
                    "parent": "test:models/tile/parent",
                    "textures": { "#child": "test:block/child" },
                    "elements": [
                        { "from": [ 4, 4, 4 ], "to": [ 12, 16, 12 ],
                          "faces": { "up": { "uv": [ 0, 0, 8, 8 ], "texture": "'#child'" } } }
                    ]
                }
                """;
        // ENUM_FACING registers/holds the Facing expression type that putVariableObject(Direction) needs
        FunctionContext ctx = new FunctionContext("test", JsonModelExpressionTypes.ENUM_FACING);
        NodeVariableDouble progress = ctx.putVariableDouble("progress");
        NodeVariableObject<Direction> direction = ctx.putVariableObject("direction", Direction.class);
        Map<Identifier, JsonObject> sources = new HashMap<>();
        sources.put(Identifier.parse("test:models/tile/parent.jsonbc"), JsonParser.parseString(parent).getAsJsonObject());
        sources.put(MODEL_ID, JsonParser.parseString(child).getAsJsonObject());
        JsonVariableModel model = JsonVariableModel.deserialize(MODEL_ID, ctx, sources::get);

        assertEquals(2, model.textures.size());
        progress.set(0.5);
        direction.set(Direction.UP);
        List<JsonQuad> quads = model.bakeCutout(model::lookupTexture);
        // the parent's element plus the child's; the rule's `when` is false (UP) so nothing rotates
        assertEquals(2, quads.size());
        for (JsonQuad quad : quads) {
            assertEquals(Direction.UP, quad.face);
        }
    }

    @Test
    public void rotateFacingRuleRotatesQuadsOntoDirection() {
        String json = """
                {
                    "rules": [
                        { "when": "direction != Facing.UP", "type": "builtin:rotate_facing",
                          "from": "Facing.UP", "to": "direction" }
                    ],
                    "elements": [
                        { "from": [ 4, 4, 4 ], "to": [ 12, 16, 12 ],
                          "faces": {
                              "up": { "uv": [ 0, 0, 8, 8 ], "texture": "'#tex'" },
                              "north": { "uv": [ 8, 0, 16, 12 ], "texture": "'#tex'" }
                          } }
                    ]
                }
                """;
        FunctionContext ctx = new FunctionContext("test", JsonModelExpressionTypes.ENUM_FACING);
        NodeVariableObject<Direction> direction = ctx.putVariableObject("direction", Direction.class);
        Map<Identifier, JsonObject> sources = new HashMap<>();
        sources.put(MODEL_ID, JsonParser.parseString(json).getAsJsonObject());
        JsonVariableModel model = JsonVariableModel.deserialize(MODEL_ID, ctx, sources::get);
        direction.set(Direction.EAST);
        JsonTexture tex = new JsonTexture("test:block/tex");
        List<JsonQuad> quads = model.bakeCutout(lookup -> tex);
        // the old up face (the trunk top at y=1) now points east at x = 1
        JsonQuad top = quads.stream().filter(q -> q.face == Direction.EAST).findFirst().orElseThrow();
        for (JsonQuad.Vertex v : top.vertices) {
            assertEquals(1.0f, v.x, 1e-6f);
        }
        // and the old north face rotated onto the new vertical ring
        assertEquals(2, quads.size());
    }

    @Test
    public void lightColourShadeLandOnQuads() {
        String json = """
                {
                    "elements": [
                        {
                            "from": [ 4, 4, 4 ],
                            "to": [ 12, 12, 12 ],
                            "shade": false,
                            "light": "stage_light",
                            "colour": "4278190335",
                            "both_sides": "true",
                            "faces": {
                                "up": { "uv": [ 0, 0, 8, 8 ], "texture": "'#tex'" }
                            }
                        }
                    ]
                }
                """;
        FunctionContext ctx = new FunctionContext("test");
        NodeVariableLong stageLight = ctx.putVariableLong("stage_light");
        stageLight.set(10);
        Map<Identifier, JsonObject> sources = new HashMap<>();
        sources.put(MODEL_ID, JsonParser.parseString(json).getAsJsonObject());
        JsonVariableModel model = JsonVariableModel.deserialize(MODEL_ID, ctx, sources::get);
        JsonTexture tex = new JsonTexture("test:block/tex");
        List<JsonQuad> quads = model.bakeCutout(lookup -> tex);
        // both_sides: the inverted copy first, then the original
        assertEquals(2, quads.size());
        assertEquals(Direction.DOWN, quads.get(0).face);
        assertEquals(Direction.UP, quads.get(1).face);
        for (JsonQuad quad : quads) {
            assertEquals(10, quad.blockLight);
            assertEquals(0xFF0000FF, quad.colorArgb);
            assertEquals(false, quad.shade);
        }
    }

    @Test
    public void inheritedVariablesAreNotRedefined() {
        String parent = """
                { "variables": { "inherited": "7" } }
                """;
        String child = """
                {
                    "parent": "test:models/tile/parent",
                    "variables": { "inherited": "9" },
                    "elements": []
                }
                """;
        Map<Identifier, JsonObject> sources = new HashMap<>();
        sources.put(Identifier.parse("test:models/tile/parent.jsonbc"), JsonParser.parseString(parent).getAsJsonObject());
        sources.put(MODEL_ID, JsonParser.parseString(child).getAsJsonObject());
        FunctionContext ctx = new FunctionContext("test");
        // the legacy semantics: the redefinition is dropped and the inherited variable keeps its value
        JsonVariableModel model = JsonVariableModel.deserialize(MODEL_ID, ctx, sources::get);
        assertNotNull(model);
        assertEquals(7L, ((buildcraft.lib.expression.api.IExpressionNode.INodeLong) ctx.getVariable("inherited"))
                .evaluate());
    }

    /** The engine's case: the parent defines an animated variable whose nodes the inherited elements reference —
     * the child's bake must keep refreshing them (otherwise the piston freezes at the load-time value). */
    @Test
    public void inheritedVariablesKeepRefreshingInChildBakes() {
        String parent = """
                {
                    "variables": { "progress_size": "progress * 16" },
                    "elements": [
                        { "from": [ 0, 0, 0 ], "to": [ 8, "progress_size", 8 ],
                          "faces": { "up": { "uv": [ 0, 0, 8, 8 ], "texture": "'#tex'" } } }
                    ]
                }
                """;
        String child = """
                {
                    "parent": "test:models/tile/parent",
                    "textures": { "#tex": "test:block/tex" }
                }
                """;
        FunctionContext ctx = new FunctionContext("test");
        NodeVariableDouble progress = ctx.putVariableDouble("progress");
        Map<Identifier, JsonObject> sources = new HashMap<>();
        sources.put(Identifier.parse("test:models/tile/parent.jsonbc"), JsonParser.parseString(parent).getAsJsonObject());
        sources.put(MODEL_ID, JsonParser.parseString(child).getAsJsonObject());
        JsonVariableModel model = JsonVariableModel.deserialize(MODEL_ID, ctx, sources::get);

        progress.set(0.5);
        assertEquals(0.5f, maxY(model.bakeCutout(model::lookupTexture).get(0)), 1e-4);
        // the second bake re-evaluates the inherited variable through the child model
        progress.set(0.25);
        assertEquals(0.25f, maxY(model.bakeCutout(model::lookupTexture).get(0)), 1e-4);
    }

    // ------------------------------------------------------------------ fail-closed error paths

    @Test
    public void unresolvedTextureReferenceFailsTheBake() {
        String json = """
                {
                    "elements": [
                        { "from": [ 0, 0, 0 ], "to": [ 16, 16, 16 ],
                          "faces": { "up": { "uv": [ 0, 0, 16, 16 ], "texture": "'#missing'" } } }
                    ]
                }
                """;
        JsonVariableModel model = parse(json);
        assertFails(JsonSyntaxException.class, () -> model.bakeCutout(model::lookupTexture));
    }

    @Test
    public void circularParentChainFails() {
        Map<Identifier, JsonObject> sources = new HashMap<>();
        sources.put(Identifier.parse("test:models/tile/a.jsonbc"), JsonParser.parseString(
                "{\"parent\": \"test:models/tile/b\"}").getAsJsonObject());
        sources.put(Identifier.parse("test:models/tile/b.jsonbc"), JsonParser.parseString(
                "{\"parent\": \"test:models/tile/a\"}").getAsJsonObject());
        assertFails(JsonParseException.class, () -> JsonVariableModel.deserialize(
                Identifier.parse("test:models/tile/a.jsonbc"), new FunctionContext("test"), sources::get));
    }

    @Test
    public void missingParentFails() {
        Map<Identifier, JsonObject> sources = new HashMap<>();
        sources.put(MODEL_ID, JsonParser.parseString("{\"parent\": \"test:models/tile/gone\"}").getAsJsonObject());
        assertFails(JsonParseException.class, () -> JsonVariableModel.deserialize(MODEL_ID,
                new FunctionContext("test"), sources::get));
    }

    @Test
    public void unknownElementTypeFails() {
        Map<Identifier, JsonObject> sources = new HashMap<>();
        sources.put(MODEL_ID, JsonParser.parseString("""
                { "elements": [ { "type": "box", "from": [0,0,0], "to": [16,16,16], "faces": {} } ] }
                """).getAsJsonObject());
        assertFails(JsonSyntaxException.class, () -> JsonVariableModel.deserialize(MODEL_ID,
                new FunctionContext("test"), sources::get));
    }

    @Test
    public void cutoutAndTranslucentMembersFail() {
        Map<Identifier, JsonObject> sources = new HashMap<>();
        FunctionContext ctx = new FunctionContext("test");
        sources.put(MODEL_ID, JsonParser.parseString("{\"cutout\": []}").getAsJsonObject());
        assertFails(JsonSyntaxException.class, () -> JsonVariableModel.deserialize(MODEL_ID, ctx, sources::get));
        sources.put(MODEL_ID, JsonParser.parseString("{\"translucent\": []}").getAsJsonObject());
        assertFails(JsonSyntaxException.class, () -> JsonVariableModel.deserialize(MODEL_ID, ctx, sources::get));
    }

    @Test
    public void statefulVariableFails() {
        Map<Identifier, JsonObject> sources = new HashMap<>();
        sources.put(MODEL_ID, JsonParser.parseString(
                "{ \"variables\": { \"stateful\": { \"type\": \"something\" } } }").getAsJsonObject());
        assertFails(JsonSyntaxException.class, () -> JsonVariableModel.deserialize(MODEL_ID,
                new FunctionContext("test"), sources::get));
    }

    @Test
    public void duplicateLocalVariableFails() {
        Map<Identifier, JsonObject> sources = new HashMap<>();
        sources.put(MODEL_ID, JsonParser.parseString(
                "{ \"variables\": { \"dup\": \"1\", \"DUP\": \"2\" } }").getAsJsonObject());
        assertFails(JsonSyntaxException.class, () -> JsonVariableModel.deserialize(MODEL_ID,
                new FunctionContext("test"), sources::get));
    }

    @Test
    public void unknownRuleTypeFails() {
        Map<Identifier, JsonObject> sources = new HashMap<>();
        sources.put(MODEL_ID, JsonParser.parseString(
                "{ \"rules\": [ { \"when\": \"true\", \"type\": \"builtin:shrink\", \"factor\": \"0.5\" } ] }")
                .getAsJsonObject());
        assertFails(JsonSyntaxException.class, () -> JsonVariableModel.deserialize(MODEL_ID,
                new FunctionContext("test"), sources::get));
    }

    @Test
    public void unknownRuleNamespaceFails() {
        Map<Identifier, JsonObject> sources = new HashMap<>();
        sources.put(MODEL_ID, JsonParser.parseString(
                "{ \"rules\": [ { \"when\": \"true\", \"type\": \"mod:custom\" } ] }").getAsJsonObject());
        assertFails(JsonSyntaxException.class, () -> JsonVariableModel.deserialize(MODEL_ID,
                new FunctionContext("test"), sources::get));
    }
}
