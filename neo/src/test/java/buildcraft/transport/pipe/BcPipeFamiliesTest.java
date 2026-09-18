/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.transport.pipe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.List;
import net.minecraft.world.item.DyeColor;
import org.junit.Test;
import buildcraft.transport.pipe.BcPipeFamilies.Family;
import buildcraft.transport.pipe.BcPipeFamilies.FlowKind;
import buildcraft.transport.pipe.BcPipeFamilies.TextureKind;

/**
 * M4.6 guard test for the pipe family table: the registry prefix order stays legacy ({@code BCTransportPipes}) and
 * every world texture the renderer can ever resolve exists as a real resource (fail-closed — a renamed or missing
 * baseline png must fail the build, not render the missing-sprite checkerboard).
 */
public class BcPipeFamiliesTest {

    @Test
    public void tableHoldsAll46FamiliesInLegacyOrder() {
        List<Family> families = BcPipeFamilies.FAMILIES;
        assertEquals(46, families.size());
        // legacy registration order: structure; 16 item pipes; 11 fluid pipes; 9 power pipes; 9 rf pipes
        assertEquals("structure_cobblestone", families.get(0).stem);
        assertEquals("items_wood", families.get(1).stem);
        assertEquals("items_stripes", families.get(16).stem);
        assertEquals("fluids_wood", families.get(17).stem);
        assertEquals("fluids_diamond_wood", families.get(27).stem);
        assertEquals("power_wood", families.get(28).stem);
        assertEquals("power_diamond_wood", families.get(36).stem);
        assertEquals("rf_wood", families.get(37).stem);
        assertEquals("rf_diamond_wood", families.get(45).stem);

        // flow kinds: rf pipes ride the POWER flow kind (same visuals), structure pipes are non-connecting
        assertEquals(FlowKind.STRUCTURE, families.get(0).flow);
        assertEquals(FlowKind.ITEMS, families.get(1).flow);
        assertEquals(FlowKind.FLUIDS, families.get(17).flow);
        assertEquals(FlowKind.POWER, families.get(28).flow);
        assertEquals(FlowKind.POWER, families.get(37).flow);

        // the wooden behaviours (legacy canConnect refusing wood-to-wood) are the wooden stems
        for (Family family : families) {
            assertEquals(family.stem, family.stem.contains("_wood"), family.wooden);
        }
    }

    @Test
    public void textureKindsMatchLegacySuffixTables() {
        // CLEAR_FILLED (wooden behaviours + iron + emzuli), LIMITER (power/rf iron+diamond), COLOURED (lapis/daizuli)
        assertEquals(TextureKind.CLEAR_FILLED, BcPipeFamilies.byStem("items_wood").textures);
        assertEquals(TextureKind.CLEAR_FILLED, BcPipeFamilies.byStem("items_iron").textures);
        assertEquals(TextureKind.CLEAR_FILLED, BcPipeFamilies.byStem("items_emzuli").textures);
        assertEquals(TextureKind.LIMITER, BcPipeFamilies.byStem("power_iron").textures);
        assertEquals(TextureKind.LIMITER, BcPipeFamilies.byStem("rf_diamond").textures);
        assertEquals(TextureKind.COLOURED, BcPipeFamilies.byStem("items_lapis").textures);
        assertEquals(TextureKind.COLOURED, BcPipeFamilies.byStem("items_daizuli").textures);
        assertEquals(TextureKind.PLAIN, BcPipeFamilies.byStem("items_stone").textures);
        assertEquals("items_lapis_base", BcPipeFamilies.byStem("items_lapis").colorlessTexture);
        assertEquals("items_daizuli_filled", BcPipeFamilies.byStem("items_daizuli").colorlessTexture);
    }

    /** Every {@code textureStem} the renderer can produce must exist as a real pipe texture resource. */
    @Test
    public void everyWorldTextureExists() throws Exception {
        DyeColor[] colours = DyeColor.values();
        for (Family family : BcPipeFamilies.FAMILIES) {
            if (family.textures == TextureKind.COLOURED) {
                assertTextureExists(BcPipeFamilies.textureStem(family, null));
                for (DyeColor colour : colours) {
                    assertTextureExists(BcPipeFamilies.textureStem(family, colour));
                }
            } else {
                assertTextureExists(BcPipeFamilies.textureStem(family, null));
                assertTextureExists(BcPipeFamilies.textureStem(family, DyeColor.RED)); // non-coloured families ignore it
            }
        }
        // the plug sprites and the flow sprites the renderer resolves directly
        assertTextureExists("plug");
        assertTextureExists("power_adapter");
        assertTextureExists("power_flow");
        assertTextureExists("rf_flow");
        assertTextureExists("gate_material_iron");
    }

    private static void assertTextureExists(String stem) throws Exception {
        String path = "/assets/buildcrafttransport/textures/pipes/" + stem + ".png";
        try (InputStream stream = BcPipeFamiliesTest.class.getResourceAsStream(path)) {
            assertNotNull("missing pipe texture resource " + path, stream);
            assertTrue("empty pipe texture resource " + path, stream.read() >= 0);
        }
    }

    @Test
    public void parseSplitsRegistryPaths() {
        DyeColor[] colourOut = new DyeColor[1];
        assertEquals("items_wood", BcPipeFamilies.parse("pipe_items_wood_colorless", colourOut).stem);
        assertNull(colourOut[0]);
        assertEquals("items_daizuli", BcPipeFamilies.parse("pipe_items_daizuli_light_blue", colourOut).stem);
        assertEquals(DyeColor.LIGHT_BLUE, colourOut[0]); // longest-suffix match, not "..._blue"
        assertEquals("items_wood", BcPipeFamilies.parse("pipe_items_wood_white", colourOut).stem);
        assertEquals(DyeColor.WHITE, colourOut[0]);
        assertEquals("structure_cobblestone", BcPipeFamilies.parse("pipe_structure_cobblestone", colourOut).stem);
        assertNull(colourOut[0]);
        assertNull(BcPipeFamilies.parse("pipe_not_a_family", colourOut));
        assertNull(BcPipeFamilies.parse("filtered_buffer", colourOut));
    }
}
