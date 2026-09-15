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
import buildcraft.lib.expression.api.IExpressionNode.INodeLong;
import buildcraft.lib.expression.api.InvalidExpressionException;

/**
 * Characterization tests (M0.5 baseline) for the error paths of the expression compiler on the 1.20.1 baseline. These
 * pin down the CURRENT failure modes on purpose - they are regression guards for the NeoForge migration, NOT
 * statements about what the behaviour ought to be.
 */
public class ExpressionErrorsCharacterizationTester {

    private static INodeLong compileLong(String expr) throws InvalidExpressionException {
        return GenericExpressionCompiler.compileExpressionLong(expr, DefaultContexts.createWithAll());
    }

    private static InvalidExpressionException compileFailure(String expr) {
        try {
            compileLong(expr);
        } catch (InvalidExpressionException e) {
            return e;
        }
        throw new AssertionError("Expected InvalidExpressionException while compiling \"" + expr + "\"");
    }

    @Test
    public void trailingOperatorFails() {
        Assert.assertNotNull(compileFailure("1 +"));
    }

    @Test
    public void unclosedParenthesisFails() {
        Assert.assertNotNull(compileFailure("(1"));
    }

    @Test
    public void strayClosingParenthesisFails() {
        Assert.assertNotNull(compileFailure("1)"));
    }

    @Test
    public void unclosedFunctionCallFails() {
        Assert.assertNotNull(compileFailure("min(1, 2"));
    }

    @Test
    public void unknownVariableFailsWithListedVariables() {
        InvalidExpressionException e = compileFailure("no_such_var");
        // The error message lists the valid variables of the context
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("no_such_var"));
    }

    @Test
    public void unknownFunctionFails() {
        Assert.assertNotNull(compileFailure("foo(1)"));
    }

    @Test
    public void wrongArityFails() {
        // "sin" exists with exactly 1 argument: 0 and 2 arguments are both "no viable function"
        Assert.assertNotNull(compileFailure("sin(1, 2)"));
        Assert.assertNotNull(compileFailure("abs()"));
    }

    @Test
    public void doubleXorIsUnsupported() {
        // "^" is only registered on longs
        Assert.assertNotNull(compileFailure("5.0 ^ 2.0"));
    }

    @Test
    public void booleanPlusLongFailsWithMergedToken_quirk() {
        // Characterization baseline, not a correctness claim: the tokenizer's word-merging step glues "true" and "1"
        // into the single unknown word "true1", so the reported expression is about 'true1' rather than "+ types".
        InvalidExpressionException e = compileFailure("true + 1");
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("true1"));
    }

    @Test
    public void stringResultIsNotALong() {
        InvalidExpressionException e = compileFailure("'text'");
        Assert.assertTrue(e.getMessage(), e.getMessage().contains("Not a long"));
    }
}
