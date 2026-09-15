/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.test.lib.expression;

import org.junit.Assert;
import org.junit.Test;

import buildcraft.lib.expression.DefaultContexts;
import buildcraft.lib.expression.GenericExpressionCompiler;
import buildcraft.lib.expression.api.IExpressionNode.INodeBoolean;
import buildcraft.lib.expression.api.IExpressionNode.INodeDouble;
import buildcraft.lib.expression.api.IExpressionNode.INodeLong;
import buildcraft.lib.expression.api.IExpressionNode.INodeObject;
import buildcraft.lib.expression.api.InvalidExpressionException;

/**
 * Characterization tests (M0.5 baseline) for the arithmetic, bit-level and comparison behaviour of the expression
 * compiler on the 1.20.1 baseline. Every "surprising" expectation below pins down the CURRENT behaviour on purpose -
 * it is a regression guard for the NeoForge migration, NOT a statement about what the behaviour ought to be.
 */
public class ExpressionOpsCharacterizationTester {

    private static INodeLong compileLong(String expr) throws InvalidExpressionException {
        return GenericExpressionCompiler.compileExpressionLong(expr, DefaultContexts.createWithAll());
    }

    private static INodeDouble compileDouble(String expr) throws InvalidExpressionException {
        return GenericExpressionCompiler.compileExpressionDouble(expr, DefaultContexts.createWithAll());
    }

    private static INodeBoolean compileBoolean(String expr) throws InvalidExpressionException {
        return GenericExpressionCompiler.compileExpressionBoolean(expr, DefaultContexts.createWithAll());
    }

    private static INodeObject<String> compileString(String expr) throws InvalidExpressionException {
        return GenericExpressionCompiler.compileExpressionString(expr, DefaultContexts.createWithAll());
    }

    @Test
    public void integerLiteralsAreLongs() throws InvalidExpressionException {
        // All plain integer literals are compiled as longs, so "7 / 2" is integer division
        Assert.assertEquals(3, compileLong("7 / 2").evaluate());
        Assert.assertEquals(0xff, compileLong("0xff").evaluate());
        Assert.assertEquals(0x1_0, compileLong("0x1_0").evaluate());
    }

    @Test
    public void mixedLongDoubleArithmeticPromotesToDouble() throws InvalidExpressionException {
        Assert.assertEquals(1.5, compileDouble("1 + 0.5").evaluate(), 0);
        Assert.assertEquals(3.5, compileDouble("7 / 2.0").evaluate(), 0);
        Assert.assertEquals(0.25, compileDouble("1.0 / 4").evaluate(), 0);
    }

    @Test
    public void moduloBindsLooserThanMultiply_quirk() throws InvalidExpressionException {
        // Characterization baseline, not a correctness claim: the precedence table puts "%" *between* "+-" and "*/",
        // so "a * b % c * d" groups as "(a * b) % (c * d)" = 6 % 20 = 6 (plain Java would give ((2*3)%4)*5 = 10).
        Assert.assertEquals(6, compileLong("2 * 3 % 4 * 5").evaluate());
    }

    @Test
    public void xorBindsTighterThanMultiply_quirk() throws InvalidExpressionException {
        // Characterization baseline, not a correctness claim: "^" has the highest binary precedence, so "2 * 3 ^ 2"
        // is 2 * (3 ^ 2) = 2 * 1 = 2.
        Assert.assertEquals(2, compileLong("2 * 3 ^ 2").evaluate());
    }

    @Test
    public void caretIsBitwiseXorNotPower_quirk() throws InvalidExpressionException {
        // Characterization baseline, not a correctness claim: "^" is XOR on longs (power is the "pow" function), and
        // it is left-associative: (2 ^ 3) ^ 2 = 1 ^ 2 = 3.
        Assert.assertEquals(6, compileLong("5 ^ 3").evaluate());
        Assert.assertEquals(3, compileLong("2 ^ 3 ^ 2").evaluate());
    }

    @Test
    public void bitwiseAndOrOnLongs() throws InvalidExpressionException {
        Assert.assertEquals(1, compileLong("3 & 5").evaluate());
        Assert.assertEquals(3, compileLong("1 | 2").evaluate());
    }

    @Test
    public void unaryOperators() throws InvalidExpressionException {
        Assert.assertEquals(-6, compileLong("~5").evaluate());
        // "--2" tokenizes as two unary negations, so it is +2
        Assert.assertEquals(2, compileLong("--2").evaluate());
        Assert.assertEquals(2, compileLong("- - 2").evaluate());
    }

    @Test
    public void shiftCountIsReducedModulo64_quirk() throws InvalidExpressionException {
        // Characterization baseline, not a correctness claim: Java long shifts mask the right operand by 63, so
        // "1 << 70" is "1 << 6" = 64.
        Assert.assertEquals(64, compileLong("1 << 70").evaluate());
    }

    @Test
    public void unsignedRightShiftCannotBeWritten_quirk() {
        // Characterization baseline, not a correctness claim: ">>>" is listed in the compiler's operator table but
        // the tokenizer only recognizes 2-char operators, so ">>>" always fails to compile.
        try {
            compileLong("8 >>> 1");
            Assert.fail("Expected InvalidExpressionException");
        } catch (InvalidExpressionException e) {
            // expected
        }
    }

    @Test
    public void longDivisionByZeroThrowsArithmeticException() {
        try {
            compileLong("1 / 0").evaluate();
            Assert.fail("Expected ArithmeticException");
        } catch (ArithmeticException e) {
            // expected
        } catch (InvalidExpressionException e) {
            Assert.fail("Compilation should succeed; failure must come from evaluation");
        }
    }

    @Test
    public void doubleDivisionByZeroIsInfinity() throws InvalidExpressionException {
        Assert.assertEquals(Double.POSITIVE_INFINITY, compileDouble("1.0 / 0").evaluate(), 0);
    }

    @Test
    public void remainderUsesJavaSemantics() throws InvalidExpressionException {
        Assert.assertEquals(2, compileLong("5 % 3").evaluate());
        Assert.assertEquals(-1, compileLong("-7 % 3").evaluate());
    }

    @Test
    public void stringConcatenationAcrossTypesViaAndOperator() throws InvalidExpressionException {
        Assert.assertEquals("a10.5true", compileString("'a' & 1 & 0.5 & true").evaluate());
        Assert.assertEquals("a1", compileString("'a' + 1").evaluate());
    }

    @Test
    public void crossTypeEqualityCastsLeftSideToString_quirk() throws InvalidExpressionException {
        // Characterization baseline, not a correctness claim: "long == string" compiles by casting the long to a
        // string ("1" == "a" is false) instead of failing.
        Assert.assertFalse(compileBoolean("1 == 'a'").evaluate());
        Assert.assertTrue(compileBoolean("'a' == 'a'").evaluate());
        Assert.assertTrue(compileBoolean("'abc' < 'abd'").evaluate());
    }

    @Test
    public void booleanOperators() throws InvalidExpressionException {
        Assert.assertFalse(compileBoolean("true & false").evaluate());
        Assert.assertFalse(compileBoolean("true ^ true").evaluate());
        Assert.assertTrue(compileBoolean("true | false").evaluate());
    }

    @Test
    public void comparisons() throws InvalidExpressionException {
        Assert.assertTrue(compileBoolean("3 > 2").evaluate());
        Assert.assertTrue(compileBoolean("2 >= 2").evaluate());
        Assert.assertTrue(compileBoolean("2 <= 1 || 2 != 1").evaluate());
    }

    @Test
    public void ternaryCastsBranchesToCommonType() throws InvalidExpressionException {
        // long branch is cast to double when the other branch is a double
        Assert.assertEquals(0.4, compileDouble("false ? 1 : 0.4").evaluate(), 0);
    }
}
