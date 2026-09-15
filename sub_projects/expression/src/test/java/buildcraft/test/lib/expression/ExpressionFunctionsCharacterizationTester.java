/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.test.lib.expression;

import org.junit.Assert;
import org.junit.Test;

import buildcraft.lib.expression.Argument;
import buildcraft.lib.expression.DefaultContexts;
import buildcraft.lib.expression.FunctionContext;
import buildcraft.lib.expression.GenericExpressionCompiler;
import buildcraft.lib.expression.NodeStack;
import buildcraft.lib.expression.VecDouble;
import buildcraft.lib.expression.VecLong;
import buildcraft.lib.expression.api.IExpressionNode.INodeDouble;
import buildcraft.lib.expression.api.IExpressionNode.INodeLong;
import buildcraft.lib.expression.api.IExpressionNode.INodeObject;
import buildcraft.lib.expression.api.INodeFunc.INodeFuncLong;
import buildcraft.lib.expression.api.InvalidExpressionException;
import buildcraft.lib.expression.node.value.NodeVariableDouble;

/**
 * Characterization tests (M0.5 baseline) for the default function contexts, string/vector helpers and variables of the
 * expression library on the 1.20.1 baseline. Surprising expectations pin down CURRENT behaviour on purpose - they are
 * regression guards for the NeoForge migration, NOT statements about what the behaviour ought to be.
 */
public class ExpressionFunctionsCharacterizationTester {

    private static INodeLong compileLong(String expr) throws InvalidExpressionException {
        return GenericExpressionCompiler.compileExpressionLong(expr, DefaultContexts.createWithAll());
    }

    private static INodeDouble compileDouble(String expr) throws InvalidExpressionException {
        return GenericExpressionCompiler.compileExpressionDouble(expr, DefaultContexts.createWithAll());
    }

    private static INodeObject<String> compileString(String expr) throws InvalidExpressionException {
        return GenericExpressionCompiler.compileExpressionString(expr, DefaultContexts.createWithAll());
    }

    @Test
    public void mathConstantsAreCaseInsensitive() throws InvalidExpressionException {
        Assert.assertEquals(Math.PI, compileDouble("PI").evaluate(), 0);
        Assert.assertEquals(Math.PI, compileDouble("Pi").evaluate(), 0);
        Assert.assertEquals(Math.PI, compileDouble("pi").evaluate(), 0);
        Assert.assertEquals(Math.E, compileDouble("e").evaluate(), 0);
    }

    @Test
    public void roundingFunctions() throws InvalidExpressionException {
        Assert.assertEquals(3, compileLong("round(2.5)").evaluate());
        // Math.round(-2.5) = -2 (rounds towards positive infinity on .5)
        Assert.assertEquals(-2, compileLong("round(-2.5)").evaluate());
        Assert.assertEquals(-2, compileLong("floor(-1.5)").evaluate());
        Assert.assertEquals(1, compileLong("ceil(0.4)").evaluate());
    }

    @Test
    public void signFunction() throws InvalidExpressionException {
        Assert.assertEquals(-1, compileLong("sign(-3)").evaluate());
        Assert.assertEquals(0, compileLong("sign(0)").evaluate());
        Assert.assertEquals(1, compileLong("sign(7)").evaluate());
    }

    @Test
    public void clampFunction() throws InvalidExpressionException {
        Assert.assertEquals(3, compileLong("clamp(5, 0, 3)").evaluate());
        Assert.assertEquals(2.5, compileDouble("clamp(2.5, 0, 3)").evaluate(), 0);
    }

    @Test
    public void minMaxFunctions() throws InvalidExpressionException {
        Assert.assertEquals(2, compileLong("min(3, 2)").evaluate());
        // the long argument is cast to double for the dd->d overload
        Assert.assertEquals(2.5, compileDouble("max(2.5, 2)").evaluate(), 0);
    }

    @Test
    public void powerAndRootFunctions() throws InvalidExpressionException {
        Assert.assertEquals(1024.0, compileDouble("pow(2, 10)").evaluate(), 0);
        Assert.assertEquals(3.0, compileDouble("cbrt(27)").evaluate(), 0);
        // function names are matched case-insensitively
        Assert.assertEquals(2.0, compileDouble("SQRT(4)").evaluate(), 0);
        Assert.assertEquals(2.0, compileDouble("sqrt(4)").evaluate(), 0);
    }

    @Test
    public void logFunctions() throws InvalidExpressionException {
        Assert.assertEquals(3.0, compileDouble("log10(1000)").evaluate(), 0);
        Assert.assertEquals(1.0, compileDouble("log(e)").evaluate(), 0);
    }

    @Test
    public void angleConversionFunctions() throws InvalidExpressionException {
        Assert.assertEquals(Math.PI, compileDouble("radians(180)").evaluate(), 0);
        Assert.assertEquals(180.0, compileDouble("degrees(3.141592653589793)").evaluate(), 0);
        Assert.assertEquals(0.0, compileDouble("sin(0)").evaluate(), 0);
    }

    @Test
    public void stringFunctions() throws InvalidExpressionException {
        Assert.assertEquals(3, compileLong("'abc'.length()").evaluate());
        Assert.assertEquals("ABC", compileString("'ABC'.toUpperCase()").evaluate());
        Assert.assertEquals("abc", compileString("'ABC'.toLowerCase()").evaluate());
        Assert.assertEquals("b", compileString("char_at('abc', 1)").evaluate());
    }

    @Test
    public void substringBoundsAreUnusual_quirk() throws InvalidExpressionException {
        // Characterization baseline, not a correctness claim: "substring" returns "" whenever start >= end,
        // start < 0, OR end >= length (the last one differs from Java's String.substring, which would clamp).
        Assert.assertEquals("cd", compileString("substring('abcdef', 2, 4)").evaluate());
        Assert.assertEquals("", compileString("substring('abcdef', 4, 2)").evaluate());
        Assert.assertEquals("", compileString("substring('abcdef', -1, 3)").evaluate());
        Assert.assertEquals("", compileString("substring('abcdef', 2, 9)").evaluate());
    }

    @Test
    public void vecLongBasics() throws InvalidExpressionException {
        Assert.assertEquals("{ 3, 4, 0, 0 }", compileString("vec(3, 4)").evaluate());
        Assert.assertEquals("{ 4, 6, 0, 0 }", compileString("vec(3, 4) + vec(1, 2)").evaluate());
        Assert.assertEquals(11, compileLong("vec(3, 4).dot2(vec(1, 2))").evaluate());
        Assert.assertEquals(5.0, compileDouble("vec(3, 4).length()").evaluate(), 0);
        Assert.assertEquals(5.0, compileDouble("vec(3, 4).distanceTo(vec(3, 9))").evaluate(), 0);
    }

    @Test
    public void vecCrossProductIsNotACrossProduct_quirk() throws InvalidExpressionException {
        // Characterization baseline, not a correctness claim: VecLong.crossProduct computes y as "c * w.b - a * w.c"
        // (note w.b, not w.a) and hardcodes the 4th component to 1:
        // (1,0,0) x (0,1,0) = { 0, 0, 1, 1 } and (1,2,3,4) x (5,6,7,8) = { -4, 11, -4, 1 }.
        Assert.assertEquals("{ 0, 0, 1, 1 }", compileString("vec(1,0,0).cross(vec(0,1,0))").evaluate());
        VecLong c = new VecLong(1, 2, 3, 4).crossProduct(new VecLong(5, 6, 7, 8));
        Assert.assertEquals(-4, c.a);
        Assert.assertEquals(11, c.b);
        Assert.assertEquals(-4, c.c);
        Assert.assertEquals(1, c.d);
    }

    @Test
    public void vecDivisionDividesByEveryComponentIncludingZero_quirk() {
        // Characterization baseline, not a correctness claim: vec division is component-wise over all 4 components,
        // so the implicit 0 components throw ArithmeticException (0 / 0) even when the written coordinates divide fine.
        try {
            compileString("vec(4, 8) / vec(2, 2)").evaluate();
            Assert.fail("Expected ArithmeticException");
        } catch (InvalidExpressionException e) {
            Assert.fail("Compilation should succeed; failure must come from evaluation");
        } catch (ArithmeticException e) {
            // expected
        }
    }

    @Test
    public void vecDoubleLength() throws InvalidExpressionException {
        Assert.assertEquals(5.0, compileDouble("vec(3.0, 4.0).length()").evaluate(), 0);
        // VecDouble has no toString override; only check the public helpers we can assert stably
        VecDouble zero = VecDouble.ZERO.normalize();
        Assert.assertEquals(0.0, zero.length(), 0);
    }

    @Test
    public void compiledFunctionCanBeReusedWithDifferentArguments() throws InvalidExpressionException {
        INodeFuncLong func = GenericExpressionCompiler.compileFunctionLong("input * 2 + 1",
            DefaultContexts.createWithAll(), Argument.argLong("input"));
        NodeStack stack = new NodeStack();
        buildcraft.lib.expression.node.value.NodeVariableLong input = stack
            .push(new buildcraft.lib.expression.node.value.NodeVariableLong("input"));
        INodeLong node = func.getNode(stack);
        input.value = 5;
        Assert.assertEquals(11, node.evaluate());
        input.value = 30;
        Assert.assertEquals(61, node.evaluate());
    }

    @Test
    public void variableNamesAreCaseInsensitive() throws InvalidExpressionException {
        FunctionContext ctx = new FunctionContext(DefaultContexts.createWithAll());
        NodeVariableDouble var = ctx.putVariableDouble("Some_Var");
        var.value = 42;
        INodeDouble viaLower = GenericExpressionCompiler.compileExpressionDouble("some_var", ctx);
        INodeDouble viaUpper = GenericExpressionCompiler.compileExpressionDouble("SOME_VAR", ctx);
        Assert.assertEquals(42.0, viaLower.evaluate(), 0);
        Assert.assertEquals(42.0, viaUpper.evaluate(), 0);
    }

    @Test
    public void variableCannotClashWithTypeName() {
        FunctionContext ctx = new FunctionContext();
        try {
            ctx.putVariableDouble("int");
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void stringEqualityUsesValueNotIdentity() throws InvalidExpressionException {
        FunctionContext ctx = new FunctionContext();
        buildcraft.lib.expression.node.value.NodeVariableObject<String> variant = ctx.putVariableString("variant");
        INodeLong sel = GenericExpressionCompiler.compileExpressionLong(
            "variant == 'wood' ? 0 : variant == 'steel' ? 1 : 2", ctx);
        variant.value = "wood";
        Assert.assertEquals(0, sel.evaluate());
        variant.value = "steel";
        Assert.assertEquals(1, sel.evaluate());
        variant.value = "other";
        Assert.assertEquals(2, sel.evaluate());
    }
}
