/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.junit.Test;

/**
 * M4.15: covers the legacy-lang-key bridge {@link BcLangKeys} (one mechanism + one table). The lookup tests pin the
 * table contract (exact frozen keys, pipe family fallback with longest-prefix, unmapped ids stay {@code null}); the
 * registration test drives every item/block helper overload through both the mapped and the unmapped branch.
 *
 * <p>Plain JUnit only: constructing any real {@code DeferredHolder} eagerly binds against
 * {@code BuiltInRegistries.REGISTRY}, which requires a bootstrapped game, so the test registers through
 * {@link RecordingItems}/{@link RecordingBlocks}: minimal {@link DeferredRegister} subclasses that override the
 * {@code register(name, factory)} funnel every convenience method dispatches to, recording the path instead of
 * creating a holder. This exercises exactly the branch logic of {@link BcLangKeys} (which plain call was reached with
 * which path) without touching registry internals.
 */
public class BcLangKeysTest {

    @Test
    public void exactIdsResolveToTheirFrozenLegacyKeys() {
        assertEquals("tile.engineIron.name", BcLangKeys.descriptionIdFor("buildcraftcore", "engine_iron"));
        assertEquals("item.wrenchItem.name", BcLangKeys.descriptionIdFor("buildcraftcore", "wrench"));
        assertEquals("item.mapLocation.name", BcLangKeys.descriptionIdFor("buildcraftcore", "map_location"));
        // The heat-loop entries: every heat level and bucket variant lands on the family key.
        assertEquals("fluid.oil", BcLangKeys.descriptionIdFor("buildcraftenergy", "fluid_block_oil_heat_2"));
        assertEquals("item.bucketFuel.name",
                BcLangKeys.descriptionIdFor("buildcraftenergy", "fuel_mixed_light_heat_0_bucket"));
        assertEquals("item.redstone_diamond_chipset.name",
                BcLangKeys.descriptionIdFor("buildcraftsilicon", "chipset_diamond"));
        assertEquals("buildcraft.boardRobotMiner",
                BcLangKeys.descriptionIdFor("buildcraftrobotics", "board_robot_miner"));
    }

    @Test
    public void unmappedIdsStayNullForAdjudication() {
        // No defensible frozen key: these keep the vanilla default and are listed in the M4.15 report.
        assertNull(BcLangKeys.descriptionIdFor("buildcraftcore", "energy_meter"));
        assertNull(BcLangKeys.descriptionIdFor("buildcraftcore", "fragile_fluid_shard"));
        assertNull(BcLangKeys.descriptionIdFor("buildcraftcore", "decorated_destroy"));
        assertNull(BcLangKeys.descriptionIdFor("buildcraftsilicon", "plug_gate_iron_and"));
        // A transport id that is not a pipe family (the pipe_holder block has no item key).
        assertNull(BcLangKeys.descriptionIdFor("buildcrafttransport", "pipe_holder"));
    }

    @Test
    public void pipeColourVariantsResolveToTheirFamilyKey() {
        assertEquals("item.PipeItemsWood.name",
                BcLangKeys.descriptionIdFor("buildcrafttransport", "pipe_items_wood_colorless"));
        assertEquals("item.PipeItemsGold.name",
                BcLangKeys.descriptionIdFor("buildcrafttransport", "pipe_items_gold_blue"));
        assertEquals("item.PipeRedstoneFluxDiamond.name",
                BcLangKeys.descriptionIdFor("buildcrafttransport", "pipe_rf_diamond_white"));
    }

    @Test
    public void pipeLongestFamilyPrefixWinsOverShorterCollisions() {
        // *_diamond_wood_* must resolve to the Wooden Diamond family, never to plain Diamond.
        assertEquals("item.PipePowerWoodenDiamond.name",
                BcLangKeys.descriptionIdFor("buildcrafttransport", "pipe_power_diamond_wood_colorless"));
        assertEquals("item.PipeItemsWoodenDiamond.name",
                BcLangKeys.descriptionIdFor("buildcrafttransport", "pipe_items_diamond_wood_lime"));
        // The bare family path is never a registered id, and the prefix rule must not resolve it either.
        assertNull(BcLangKeys.descriptionIdFor("buildcrafttransport", "pipe_items_wood"));
    }

    @Test
    public void registrationHelpersDispatchEveryOverloadThroughBothBranches() {
        RecordingItems items = new RecordingItems();
        RecordingBlocks blocks = new RecordingBlocks();

        // item(items, path): mapped branch pins the legacy key; unmapped branch falls through to the plain call.
        assertNull(BcLangKeys.item(items, "wrench"));
        assertNull(BcLangKeys.item(items, "energy_meter"));
        // simpleItem(items, path, UnaryOperator)
        assertNull(BcLangKeys.simpleItem(items, "gear_wood", properties -> properties));
        assertNull(BcLangKeys.simpleItem(items, "fragile_fluid_shard", properties -> properties));
        // item(items, path, factory)
        assertNull(BcLangKeys.item(items, "list", Item::new));
        assertNull(BcLangKeys.item(items, "decorated_destroy", Item::new));
        // item(items, path, factory, UnaryOperator)
        assertNull(BcLangKeys.item(items, "map_location", Item::new, properties -> properties));
        assertNull(BcLangKeys.item(items, "decorated_leather", Item::new, properties -> properties));

        // simpleBlock(blocks, path, UnaryOperator)
        assertNull(BcLangKeys.simpleBlock(blocks, "engine_stone", properties -> properties));
        assertNull(BcLangKeys.simpleBlock(blocks, "energy_meter", properties -> properties));
        // block(blocks, path, factory)
        assertNull(BcLangKeys.block(blocks, "marker", Block::new));
        assertNull(BcLangKeys.block(blocks, "decorated_destroy", Block::new));
        // block(blocks, path, factory, UnaryOperator)
        assertNull(BcLangKeys.block(blocks, "spring_water", Block::new, properties -> properties));
        assertNull(BcLangKeys.block(blocks, "decorated_leather", Block::new, properties -> properties));
        // block(blocks, path, factory, Supplier)
        assertNull(BcLangKeys.block(blocks, "spring_oil", Block::new, BlockBehaviour.Properties::of));
        assertNull(BcLangKeys.block(blocks, "decorated_paper", Block::new, BlockBehaviour.Properties::of));

        // Every helper reached the plain DeferredRegister funnel exactly once, with the id's own path either way.
        assertEquals(List.of("wrench", "energy_meter", "gear_wood", "fragile_fluid_shard", "list",
                "decorated_destroy", "map_location", "decorated_leather"), items.paths);
        assertEquals(List.of("engine_stone", "energy_meter", "marker", "decorated_destroy", "spring_water",
                "decorated_leather", "spring_oil", "decorated_paper"), blocks.paths);
    }

    /** {@link DeferredRegister.Items} whose register funnel only records the path (no holder, no registry access). */
    private static final class RecordingItems extends DeferredRegister.Items {
        final List<String> paths = new ArrayList<>();

        RecordingItems() {
            super("buildcraftcore");
        }

        @Override
        public <I extends Item> DeferredItem<I> register(String name, Function<Identifier, ? extends I> func) {
            paths.add(name);
            return null;
        }
    }

    /** {@link DeferredRegister.Blocks} whose register funnel only records the path (no holder, no registry access). */
    private static final class RecordingBlocks extends DeferredRegister.Blocks {
        final List<String> paths = new ArrayList<>();

        RecordingBlocks() {
            super("buildcraftcore");
        }

        @Override
        public <B extends Block> DeferredBlock<B> register(String name, Function<Identifier, ? extends B> func) {
            paths.add(name);
            return null;
        }
    }
}
