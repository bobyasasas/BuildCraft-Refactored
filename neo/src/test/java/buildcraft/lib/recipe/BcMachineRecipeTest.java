/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.recipe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import net.minecraft.world.item.crafting.PlacementInfo;

/**
 * M2.10: the machine-recipe base contract the later machine milestones (M2.11-M2.13) rely on, plus the value
 * semantics of the two data-carrier records behind the migrated recipes.
 *
 * <p>Scope note: these tests run in a plain JVM — deliberately no vanilla registry access, which the FML-less unit
 * test JVM cannot bootstrap. The registry-bound halves (the codec holders of both records, and the
 * {@code Recipe}-implementing concrete recipe types, whose class init builds vanilla codecs) are exercised for real
 * by the GameTests and the M3.3 registry-dump run, which load all 1561 migrated recipes in a live server.
 */
public class BcMachineRecipeTest {

    /** Minimal concrete instance of the behaviour base (a real recipe type additionally implements Recipe). */
    private static final class TestRecipe extends BcMachineRecipe {
    }

    @Test
    public void machineRecipeBaseIsInert() {
        TestRecipe recipe = new TestRecipe();

        assertFalse("matches must never fire (data carrier, not a crafting recipe)", recipe.matches(null, null));
        assertTrue("isSpecial keeps the recipe out of the recipe book", recipe.isSpecial());
        assertFalse("showNotification off (never shown in the crafting book)", recipe.showNotification());
        assertEquals("group empty (no recipe book tab)", "", recipe.group());
        assertSame("not placeable into the crafting grid UI", PlacementInfo.NOT_PLACEABLE, recipe.placementInfo());
    }

    /** The sized-ingredient record behaves as a validated plain value type (M2.10 codec port). */
    @Test
    public void ingredientStackValueSemantics() {
        BcIngredientStack stack = new BcIngredientStack(null, 3);
        assertEquals(3, stack.count());
        assertNull(stack.ingredient());
        assertEquals(new BcIngredientStack(null, 3), stack);
        assertEquals("value semantics include the identity hash contract", stack.hashCode(), new BcIngredientStack(
            null, 3).hashCode());
        assertNotEquals(new BcIngredientStack(null, 2), stack);
        assertNotEquals(new Object(), stack);
        assertNotNull("toString stays available for diagnostics", stack.toString());
        // the compact constructor mirrors the codec's count in [1, 99] bound
        assertRejectedCount(0);
        assertRejectedCount(100);
        assertAcceptedCount(1);
        assertAcceptedCount(99);
    }

    private static void assertRejectedCount(int count) {
        try {
            new BcIngredientStack(null, count);
            fail("count " + count + " must be rejected (codec bound is count in [1, 99])");
        } catch (IllegalArgumentException expected) {
            // bound mirrored from Codecs.CODEC's intRange(1, 99)
        }
    }

    private static void assertAcceptedCount(int count) {
        assertEquals(count, new BcIngredientStack(null, count).count());
    }

    /** The fluid-amount record behaves as a validated plain value type (M2.10 codec port). */
    @Test
    public void fluidAmountValueSemantics() {
        BcFluidAmount amount = new BcFluidAmount(null, 1000);
        assertEquals(1000, amount.amount());
        assertNull(amount.fluid());
        assertEquals(new BcFluidAmount(null, 1000), amount);
        assertEquals("value semantics include the identity hash contract", amount.hashCode(), new BcFluidAmount(null,
            1000).hashCode());
        assertNotEquals(new BcFluidAmount(null, 999), amount);
        assertNotEquals(new Object(), amount);
        assertNotNull("toString stays available for diagnostics", amount.toString());
        // the compact constructor mirrors the codec's amount >= 0 bound
        try {
            new BcFluidAmount(null, -1);
            fail("amount -1 must be rejected (codec bound is amount >= 0)");
        } catch (IllegalArgumentException expected) {
            // bound mirrored from Codecs.MAP_CODEC's intRange(0, MAX_VALUE)
        }
        assertEquals(0, new BcFluidAmount(null, 0).amount());
    }
}
