/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.model.json;

import java.util.List;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.Direction;
import buildcraft.lib.expression.FunctionContext;
import buildcraft.lib.expression.GenericExpressionCompiler;
import buildcraft.lib.expression.api.IExpressionNode.INodeBoolean;
import buildcraft.lib.expression.api.IExpressionNode.INodeDouble;
import buildcraft.lib.expression.api.IExpressionNode.INodeObject;
import buildcraft.lib.expression.api.InvalidExpressionException;
import buildcraft.lib.expression.node.value.NodeConstantDouble;

/**
 * Minimal port of the legacy {@code buildcraft.lib.client.model.json.JsonModelRule} (M4.4): a jsonbc {@code rules}
 * entry applied to the baked quads when its {@code when} expression evaluates true. Of the legacy builtin rule types
 * only {@code builtin:rotate_facing} is ported (the only one the engine and {@code heat_exchange_static} models use);
 * every other type fails the parse on purpose.
 */
public abstract class JsonModelRule {

    public final INodeBoolean when;

    protected JsonModelRule(INodeBoolean when) {
        this.when = when;
    }

    public static JsonModelRule deserialize(JsonElement json, FunctionContext fnCtx) {
        if (!json.isJsonObject()) {
            throw new JsonSyntaxException("Expected an object, got " + json);
        }
        JsonObject obj = json.getAsJsonObject();
        String when = getRequiredString(obj, "when");
        INodeBoolean nodeWhen = JsonVariableFaceUV.convertStringToBooleanNode(when, fnCtx);

        String type = getRequiredString(obj, "type");
        if (type.startsWith("builtin:")) {
            String builtin = type.substring("builtin:".length());
            if ("rotate_facing".equals(builtin)) {
                // The facing constants ("Facing.UP") resolve through the Facing type's own context, like the legacy
                // `new FunctionContext(fnCtx, ExpressionCompat.ENUM_FACING)`.
                FunctionContext facingCtx = new FunctionContext(fnCtx, JsonModelExpressionTypes.ENUM_FACING);
                String from = getRequiredString(obj, "from");
                INodeObject<Direction> nodeFrom = convertStringToObjectNode(from, facingCtx);
                String to = getRequiredString(obj, "to");
                INodeObject<Direction> nodeTo = convertStringToObjectNode(to, facingCtx);

                INodeDouble[] origin;
                if (obj.has("origin")) {
                    origin = JsonVariableFaceUV.readVariablePosition(obj, "origin", facingCtx);
                } else {
                    origin = RuleRotateFacing.DEFAULT_ORIGIN;
                }
                return new RuleRotateFacing(nodeWhen, nodeFrom, nodeTo, origin);
            }
            throw new JsonSyntaxException(//
                "Unknown built in rule type '" + builtin + "' (the minimal jsonbc parser only supports rotate_facing)");
        }
        throw new JsonSyntaxException("Unknown rule type '" + type + "'");
    }

    private static String getRequiredString(JsonObject obj, String member) {
        JsonElement elem = obj.get(member);
        if (elem == null || !elem.isJsonPrimitive()) {
            throw new JsonSyntaxException("Expected a string '" + member + "' in " + obj);
        }
        return elem.getAsString();
    }

    private static INodeObject<Direction> convertStringToObjectNode(String expression, FunctionContext context) {
        try {
            return GenericExpressionCompiler.compileExpressionObject(Direction.class, expression, context);
        } catch (InvalidExpressionException e) {
            throw new JsonSyntaxException("Invalid expression " + expression, e);
        }
    }

    public abstract void apply(List<JsonQuad> quads);

    /** Rotates every quad from one facing to another around the given origin (legacy {@code RuleRotateFacing}). */
    public static final class RuleRotateFacing extends JsonModelRule {

        private static final NodeConstantDouble CONST_ORIGIN = new NodeConstantDouble(8);
        public static final INodeDouble[] DEFAULT_ORIGIN = { CONST_ORIGIN, CONST_ORIGIN, CONST_ORIGIN };

        public final INodeObject<Direction> from, to;
        public final INodeDouble[] origin;

        public RuleRotateFacing(INodeBoolean when, INodeObject<Direction> from, INodeObject<Direction> to,
                                INodeDouble[] origin) {
            super(when);
            this.from = from;
            this.to = to;
            this.origin = origin;
        }

        @Override
        public void apply(List<JsonQuad> quads) {
            Direction faceFrom = this.from.evaluate();
            Direction faceTo = this.to.evaluate();
            if (faceFrom == faceTo) {
                // don't bother rotating: there is nothing to rotate!
                return;
            }
            float ox = (float) this.origin[0].evaluate() / 16f;
            float oy = (float) this.origin[1].evaluate() / 16f;
            float oz = (float) this.origin[2].evaluate() / 16f;
            for (JsonQuad q : quads) {
                q.rotate(faceFrom, faceTo, ox, oy, oz);
            }
        }
    }
}
