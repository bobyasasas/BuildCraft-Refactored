/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.model.json;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import buildcraft.core.blockentity.EnumPowerStage;
import buildcraft.lib.expression.FunctionContext;
import buildcraft.lib.expression.GenericExpressionCompiler;
import buildcraft.lib.expression.InternalCompiler;
import buildcraft.lib.expression.api.InvalidExpressionException;
import buildcraft.lib.expression.api.NodeType;
import buildcraft.lib.expression.api.NodeTypes;
import buildcraft.lib.expression.node.value.NodeVariableDouble;
import buildcraft.lib.expression.node.value.NodeVariableObject;
import net.minecraft.core.Direction;

/**
 * M4.4: compiles the real engine jsonbc variable expressions ({@code engine_base.jsonbc} verbatim) against the same
 * context shape the BER builds (progress/stage/direction over the Facing + power-stage expression types) and asserts
 * the evaluated values for every power stage and both piston phases. This is the expression-layer twin of
 * {@link JsonVariableModelTest} &mdash; it pins the strings that ship inside the jsonbc files.
 */
public class EngineJsonbcExpressionsTest {

    private static FunctionContext ctx;
    private static NodeVariableDouble progress;
    private static NodeVariableObject<EnumPowerStage> stage;
    private static NodeVariableObject<Direction> direction;

    /** The exact {@code variables} member of {@code engine_base.jsonbc} / {@code mj_dynamo.jsonbc}. */
    private static final JsonObject ENGINE_VARIABLES = JsonParser.parseString("""
            {
                "progress_size": "(progress > 0.5 ? ((1 - progress) * (8 * 2 - 0.01)) : (progress * (8 * 2 - 0.01)))",
                "trunk_tex": "'#trunk_' + stage",
                "stage_light":"(stage == overheat || stage == red) ? 10 : stage == yellow ?  7 : stage == green ?  4 : 0"
            }
            """).getAsJsonObject();

    @BeforeClass
    public static void buildContext() throws InvalidExpressionException {
        // mirrors BcEngineModels' static block (the client holder is not on the test classpath)
        NodeType<EnumPowerStage> powerStageType = new NodeType<>("EnginePowerStage", EnumPowerStage.BLUE);
        NodeTypes.addType(powerStageType);
        powerStageType.put_t_o("(string)", String.class, EnumPowerStage::getSerializedName);
        for (EnumPowerStage s : EnumPowerStage.values()) {
            powerStageType.putConstant("" + s, s);
        }
        ctx = new FunctionContext("test engines",//
            JsonModelExpressionTypes.ENUM_FACING, powerStageType,//
            buildcraft.lib.expression.DefaultContexts.createWithAll());
        progress = ctx.putVariableDouble("progress");
        stage = ctx.putVariableObject("stage", EnumPowerStage.class);
        direction = ctx.putVariableObject("direction", Direction.class);
        // compile each variable exactly like JsonVariableModel#putVariables does
        for (String name : ENGINE_VARIABLES.keySet()) {
            ctx.putVariable(name.toLowerCase(), InternalCompiler.compileExpression(
                    ENGINE_VARIABLES.get(name).getAsString(), new FunctionContext("Value Object", ctx)));
        }
    }

    /** The engine's piston slice: grows on the way out, mirrors on the way back (piecewise peak 7.995 at 0.5). */
    @Test
    public void progressSizeIsSymmetricAroundHalf() throws InvalidExpressionException {
        progress.set(0.2);
        double out = ((buildcraft.lib.expression.api.IExpressionNode.INodeDouble) ctx.getVariable("progress_size"))
                .evaluate();
        assertEquals(3.198, out, 1e-9);
        progress.set(0.8);
        double back = ((buildcraft.lib.expression.api.IExpressionNode.INodeDouble) ctx.getVariable("progress_size"))
                .evaluate();
        assertEquals(3.198, back, 1e-9);
        // the two branches meet at progress 0.5: 0.5 * 15.99 = 7.995 on either side
        progress.set(0.5);
        double peak = ((buildcraft.lib.expression.api.IExpressionNode.INodeDouble) ctx.getVariable("progress_size"))
                .evaluate();
        assertEquals(7.995, peak, 1e-9);
    }

    /** {@code trunk_tex} concatenates the stage texture reference through the power-stage {@code (string)} cast. */
    @Test
    public void trunkTexFollowsStage() throws InvalidExpressionException {
        stage.set(EnumPowerStage.RED);
        assertEquals("#trunk_red", ((buildcraft.lib.expression.api.IExpressionNode.INodeObject<String>) ctx
                .getVariable("trunk_tex")).evaluate());
        stage.set(EnumPowerStage.OVERHEAT);
        assertEquals("#trunk_overheat", ((buildcraft.lib.expression.api.IExpressionNode.INodeObject<String>) ctx
                .getVariable("trunk_tex")).evaluate());
        stage.set(EnumPowerStage.BLUE);
        assertEquals("#trunk_blue", ((buildcraft.lib.expression.api.IExpressionNode.INodeObject<String>) ctx
                .getVariable("trunk_tex")).evaluate());
    }

    /** The trunk glow table: overheat/red = 10, yellow = 7, green = 4, blue/black = 0. */
    @Test
    public void stageLightTable() throws InvalidExpressionException {
        buildcraft.lib.expression.api.IExpressionNode.INodeLong node =
                (buildcraft.lib.expression.api.IExpressionNode.INodeLong) ctx.getVariable("stage_light");
        stage.set(EnumPowerStage.BLUE);
        assertEquals(0, node.evaluate());
        stage.set(EnumPowerStage.GREEN);
        assertEquals(4, node.evaluate());
        stage.set(EnumPowerStage.YELLOW);
        assertEquals(7, node.evaluate());
        stage.set(EnumPowerStage.RED);
        assertEquals(10, node.evaluate());
        stage.set(EnumPowerStage.OVERHEAT);
        assertEquals(10, node.evaluate());
        // the creative engine runs BLACK (which no condition matches), so its trunk never glows
        stage.set(EnumPowerStage.BLACK);
        assertEquals(0, node.evaluate());
    }

    /** The jsonbc rule's {@code when}: false for UP (no rotation), true for every other direction. */
    @Test
    public void ruleWhenIsFalseOnlyForUp() throws InvalidExpressionException {
        // compiled exactly like JsonModelRule#deserialize compiles the `when` member
        buildcraft.lib.expression.api.IExpressionNode.INodeBoolean when = GenericExpressionCompiler
                .compileExpressionBoolean("direction != Facing.UP", new FunctionContext("when", ctx));
        direction.set(Direction.UP);
        assertEquals(false, when.evaluate());
        for (Direction d : new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST }) {
            direction.set(d);
            assertEquals(true, when.evaluate());
        }
    }
}
