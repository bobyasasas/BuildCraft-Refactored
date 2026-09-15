/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.test.builders.snapshot;

import java.util.BitSet;
import java.util.function.BiFunction;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import buildcraft.api.filler.FillerManager;
import buildcraft.api.filler.IFilledTemplate;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.builders.BCBuildersStatements;
import buildcraft.builders.registry.FillerRegistry;
import buildcraft.builders.snapshot.Snapshot;
import buildcraft.builders.snapshot.Template;
import buildcraft.builders.snapshot.pattern.parameter.PatternParameterAxis;
import buildcraft.builders.snapshot.pattern.parameter.PatternParameterCenter;
import buildcraft.builders.snapshot.pattern.parameter.PatternParameterHollow;
import buildcraft.builders.snapshot.pattern.parameter.PatternParameterRotation;
import buildcraft.builders.snapshot.pattern.parameter.PatternParameterXZDir;
import buildcraft.builders.snapshot.pattern.parameter.PatternParameterYDir;
import buildcraft.test.VanillaSetupBaseTester;
import net.minecraft.core.BlockPos;

/**
 * Characterization tests (M0.5 baseline) for the filler placement patterns on the 1.20.1 baseline. The exact filled
 * cell counts and per-Y-layer distributions below were recorded from the current implementation on purpose - they are
 * regression guards for the NeoForge migration, NOT statements about what the shapes ought to look like.
 *
 * Unlike {@link buildcraft.test.core.builders.patterns.ShapePatternsTester} (which only checks that the patterns do not
 * throw and that sphere halves match the full sphere), these tests pin the CONCRETE output of each pattern.
 */
public class FillerPatternCharacterizationTester extends VanillaSetupBaseTester {

    @BeforeClass
    public static void setupRegistry() {
        FillerManager.registry = FillerRegistry.INSTANCE;
    }

    /** Runs the pattern over a fresh template of the given size and returns (total, per-Y-layer counts). */
    private static int[] fill(Template template, BiFunction<IFilledTemplate, IStatementParameter[], Boolean> pattern,
        IStatementParameter[] params) {
        IFilledTemplate filled = template.getFilledTemplate();
        Assert.assertTrue(pattern.apply(filled, params));
        int[] perY = new int[template.size.getY()];
        for (int y = 0; y < template.size.getY(); y++) {
            for (int z = 0; z < template.size.getZ(); z++) {
                for (int x = 0; x < template.size.getX(); x++) {
                    if (filled.get(x, y, z)) {
                        perY[y]++;
                    }
                }
            }
        }
        return perY;
    }

    private static int total(int[] perY) {
        int sum = 0;
        for (int v : perY) {
            sum += v;
        }
        return sum;
    }

    private static Template emptyTemplate(BlockPos size) {
        Template template = new Template();
        template.size = size;
        template.offset = BlockPos.ZERO;
        template.data = new BitSet(Snapshot.getDataSize(size));
        return template;
    }

    private static void assertCounts(int[] actual, String label, int... expectedPerY) {
        Assert.assertEquals(label + " layer count", expectedPerY.length, actual.length);
        Assert.assertEquals(label + " total", total(expectedPerY), total(actual));
        Assert.assertArrayEquals(label, expectedPerY, actual);
    }

    @Test
    public void fillPatternFillsEverything() {
        assertCounts(fill(emptyTemplate(new BlockPos(3, 3, 3)), BCBuildersStatements.PATTERN_FILL::fillTemplate,
            new IStatementParameter[0]), "fill 3x3x3", 9, 9, 9);
        assertCounts(fill(emptyTemplate(new BlockPos(2, 2, 2)), BCBuildersStatements.PATTERN_FILL::fillTemplate,
            new IStatementParameter[0]), "fill 2x2x2", 4, 4);
    }

    @Test
    public void boxPatternLeavesOnlyTheInteriorEmpty() {
        assertCounts(fill(emptyTemplate(new BlockPos(3, 3, 3)), BCBuildersStatements.PATTERN_BOX::fillTemplate,
            new IStatementParameter[0]), "box 3x3x3", 9, 8, 9);
        assertCounts(fill(emptyTemplate(new BlockPos(5, 5, 5)), BCBuildersStatements.PATTERN_BOX::fillTemplate,
            new IStatementParameter[0]), "box 5x5x5", 25, 16, 16, 16, 25);
        // a 2x2x2 box has no interior, so it is filled completely
        assertCounts(fill(emptyTemplate(new BlockPos(2, 2, 2)), BCBuildersStatements.PATTERN_BOX::fillTemplate,
            new IStatementParameter[0]), "box 2x2x2", 4, 4);
    }

    @Test
    public void framePatternIsOnlyTheCubeEdges() {
        assertCounts(fill(emptyTemplate(new BlockPos(3, 3, 3)), BCBuildersStatements.PATTERN_FRAME::fillTemplate,
            new IStatementParameter[0]), "frame 3x3x3", 8, 4, 8);
        assertCounts(fill(emptyTemplate(new BlockPos(5, 5, 5)), BCBuildersStatements.PATTERN_FRAME::fillTemplate,
            new IStatementParameter[0]), "frame 5x5x5", 16, 4, 4, 4, 16);
    }

    @Test
    public void pyramidPatternGrowsFromTheBottomLayer() {
        assertCounts(fill(emptyTemplate(new BlockPos(3, 3, 3)), BCBuildersStatements.PATTERN_PYRAMID::fillTemplate,
            new IStatementParameter[] { PatternParameterYDir.UP, PatternParameterCenter.CENTER }),//
            "pyramid 3x3x3", 9, 1, 0);
        assertCounts(fill(emptyTemplate(new BlockPos(5, 5, 5)), BCBuildersStatements.PATTERN_PYRAMID::fillTemplate,
            new IStatementParameter[] { PatternParameterYDir.UP, PatternParameterCenter.CENTER }),//
            "pyramid 5x5x5", 25, 9, 1, 0, 0);
    }

    @Test
    public void stairsPatternDescendsLinearly() {
        assertCounts(fill(emptyTemplate(new BlockPos(3, 3, 3)), BCBuildersStatements.PATTERN_STAIRS::fillTemplate,
            new IStatementParameter[] { PatternParameterYDir.UP, PatternParameterXZDir.EAST }),//
            "stairs 3x3x3", 9, 6, 3);
        assertCounts(fill(emptyTemplate(new BlockPos(5, 5, 5)), BCBuildersStatements.PATTERN_STAIRS::fillTemplate,
            new IStatementParameter[] { PatternParameterYDir.UP, PatternParameterXZDir.EAST }),//
            "stairs 5x5x5", 25, 20, 15, 10, 5);
    }

    @Test
    public void spherePatternCountsOnOddCubes() {
        assertCounts(fill(emptyTemplate(new BlockPos(3, 3, 3)), BCBuildersStatements.PATTERN_SPHERE::fillTemplate,
            new IStatementParameter[] { PatternParameterHollow.FILLED_INNER }),//
            "sphere filled 3x3x3", 5, 9, 5);
        assertCounts(fill(emptyTemplate(new BlockPos(5, 5, 5)), BCBuildersStatements.PATTERN_SPHERE::fillTemplate,
            new IStatementParameter[] { PatternParameterHollow.FILLED_INNER }),//
            "sphere filled 5x5x5", 9, 21, 21, 21, 9);
        assertCounts(fill(emptyTemplate(new BlockPos(3, 3, 3)), BCBuildersStatements.PATTERN_SPHERE::fillTemplate,
            new IStatementParameter[] { PatternParameterHollow.HOLLOW }),//
            "sphere hollow 3x3x3", 5, 8, 5);
        assertCounts(fill(emptyTemplate(new BlockPos(5, 5, 5)), BCBuildersStatements.PATTERN_SPHERE::fillTemplate,
            new IStatementParameter[] { PatternParameterHollow.HOLLOW }),//
            "sphere hollow 5x5x5", 9, 12, 12, 12, 9);
    }

    @Test
    public void sphereFilledOuterEqualsBoxShellOnOddCubes_quirk() {
        // Characterization baseline, not a correctness claim: on a 5x5x5 "filled outer" fills every cell that the
        assertCounts(fill(emptyTemplate(new BlockPos(5, 5, 5)), BCBuildersStatements.PATTERN_SPHERE::fillTemplate,
            new IStatementParameter[] { PatternParameterHollow.FILLED_OUTER }),//
            "sphere filled-outer 5x5x5", 25, 16, 16, 16, 25);
    }

    @Test
    public void shape2dPatternsExtrudeOverEveryYLayer() {
        assertCounts(fill(emptyTemplate(new BlockPos(5, 5, 5)), BCBuildersStatements.PATTERN_CIRCLE::fillTemplate,
            new IStatementParameter[] { PatternParameterAxis.Y, PatternParameterHollow.HOLLOW,
                PatternParameterRotation.NONE }),//
            "2d circle 5x5x5", 12, 12, 12, 12, 12);
        assertCounts(fill(emptyTemplate(new BlockPos(5, 5, 5)), BCBuildersStatements.PATTERN_CIRCLE::fillTemplate,
            new IStatementParameter[] { PatternParameterAxis.Y, PatternParameterHollow.FILLED_INNER,
                PatternParameterRotation.NONE }),//
            "2d circle filled 5x5x5", 21, 21, 21, 21, 21);
        assertCounts(fill(emptyTemplate(new BlockPos(5, 5, 5)), BCBuildersStatements.PATTERN_SQUARE::fillTemplate,
            new IStatementParameter[] { PatternParameterAxis.Y, PatternParameterHollow.HOLLOW,
                PatternParameterRotation.NONE }),//
            "2d square 5x5x5", 16, 16, 16, 16, 16);
        // a filled 2d square covers the whole volume
        assertCounts(fill(emptyTemplate(new BlockPos(5, 5, 5)), BCBuildersStatements.PATTERN_SQUARE::fillTemplate,
            new IStatementParameter[] { PatternParameterAxis.Y, PatternParameterHollow.FILLED_INNER,
                PatternParameterRotation.NONE }),//
            "2d square filled 5x5x5", 25, 25, 25, 25, 25);
    }

    @Test
    public void nonePatternReturnsFalseAndChangesNothing() {
        Template template = emptyTemplate(new BlockPos(3, 3, 3));
        IFilledTemplate filled = template.getFilledTemplate();
        Assert.assertFalse(BCBuildersStatements.PATTERN_NONE.fillTemplate(filled, new IStatementParameter[0]));
        Assert.assertTrue(template.data.isEmpty());
    }

    @Test
    public void clearPatternReturnsTrueButChangesNothing_quirk() {
        // Characterization baseline, not a correctness claim: PatternClear.fillTemplate just returns true without
        // touching the template at all
        Template template = emptyTemplate(new BlockPos(3, 3, 3));
        IFilledTemplate filled = template.getFilledTemplate();
        Assert.assertTrue(BCBuildersStatements.PATTERN_CLEAR.fillTemplate(filled, new IStatementParameter[0]));
        Assert.assertTrue(template.data.isEmpty());
    }

    @Test
    public void boxPatternSetsExactlyTheSixPlanesOnAsymmetricBoxes() {
        Template template = emptyTemplate(new BlockPos(4, 2, 3));
        IFilledTemplate filled = template.getFilledTemplate();
        Assert.assertTrue(BCBuildersStatements.PATTERN_BOX.fillTemplate(filled, new IStatementParameter[0]));
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < 3; z++) {
                    boolean onShell = x == 0 || x == 3 || y == 0 || y == 1 || z == 0 || z == 2;
                    Assert.assertEquals("cell " + x + "," + y + "," + z, onShell, filled.get(x, y, z));
                }
            }
        }
    }
}
