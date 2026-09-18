/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.datagen;

import buildcraft.builders.BcBuildersBlocks;
import buildcraft.builders.BcBuildersItems;
import buildcraft.core.BcBlocks;
import buildcraft.core.BcItems;
import buildcraft.energy.BcEnergyBlocks;
import buildcraft.energy.BcEnergyItems;
import buildcraft.factory.BcFactoryBlocks;
import buildcraft.factory.BcFactoryItems;
import buildcraft.lib.BcLibItems;
import buildcraft.robotics.BcRoboticsBlocks;
import buildcraft.robotics.BcRoboticsItems;
import buildcraft.silicon.BcSiliconBlocks;
import buildcraft.silicon.BcSiliconItems;
import buildcraft.transport.BcTransportBlocks;
import buildcraft.transport.BcTransportItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.data.DataGenerator;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * M3.4: BuildCraft datagen entry (legacy {@code common/buildcraft/datagen/BCDataGenerators} successor). Each of the
 * eight mods' constructors calls {@link #register}; {@code GatherDataEvent} then fires once per mod and the handler
 * generates ONLY that mod's namespace slice, so the runData output lands as one per-mod tree
 * ({@code <output>/<modid>/assets/<modid>/...}) that diffs 1:1 against {@code neo/src/main/resources/assets/<modid>}.
 *
 * <p>Scope (M3.4 ruling): the asset face that ships with the jar — item model definitions ({@code items/}), item
 * models ({@code models/item/}), blockstates + block models ({@code blockstates/}, {@code models/block/}) and the
 * frozen en_us lang files. Data-side content (recipes/tags) is covered by the deterministic converter
 * ({@code neo/tools/convert_recipes_m210.py}) plus the runtime registry parity gate, not by datagen.
 *
 * <p>Enumeration is programmatic end to end: the providers walk each mod's {@link DeferredRegister} entries (the same
 * collections the {@code buildcraftcore:registry_parity} GameTest asserts against the 1.20.1 baseline), so a registry
 * change without an asset rule fails the run instead of drifting silently.
 */
public final class BcDatagen {

    private BcDatagen() {
    }

    /** Registers the {@code GatherDataEvent} handler on one mod's event bus (called from every mod constructor). */
    public static void register(IEventBus modEventBus) {
        // The bus refuses listeners on the abstract GatherDataEvent; a datagen run fires exactly one of the two
        // subclasses (serverData -> Server, clientData -> Client) and both share the same handler body.
        modEventBus.addListener(GatherDataEvent.Server.class, BcDatagen::onGatherData);
        modEventBus.addListener(GatherDataEvent.Client.class, BcDatagen::onGatherData);
    }

    /** Fires once per mod in the datagen run; generates only {@code event}'s own mod namespace. */
    private static void onGatherData(GatherDataEvent event) {
        String modid = event.getModContainer().getModId();
        List<DeferredHolder<Item, ? extends Item>> items = new ArrayList<>();
        itemsFor(modid).forEach(items::add);
        List<DeferredHolder<Block, ? extends Block>> blocks = new ArrayList<>();
        blocksFor(modid).forEach(blocks::add);

        DataGenerator.PackGenerator pack = event.getGenerator().getVanillaPack(true);
        pack.addProvider(output -> new BcItemModelsProvider(modid, output, items));
        pack.addProvider(output -> new BcBlockStateProvider(modid, output, blocks));
        pack.addProvider(output -> new BcLangProvider(modid, output));
        // M4.4: the tile jsonbc models re-emit byte-exact so the S1 drift gate covers them too
        pack.addProvider(output -> new BcTileModelsProvider(modid, output));

        if ("buildcraftcore".equals(modid)) {
            // frozen-table guard: the drift gate depends on the embedded table being complete (857 keys, M3.5)
            if (BcLangData.KEY_COUNT != 857) {
                throw new IllegalStateException("BcDatagen: frozen lang table drifted, expected 857 keys, got "
                        + BcLangData.KEY_COUNT);
            }
        }
    }

    /** All registered items of one BuildCraft mod, in registration order. */
    private static Iterable<DeferredHolder<Item, ? extends Item>> itemsFor(String modid) {
        return switch (modid) {
            case "buildcraftlib" -> BcLibItems.ITEMS.getEntries();
            case "buildcraftcore" -> BcItems.ITEMS.getEntries();
            case "buildcraftbuilders" -> BcBuildersItems.ITEMS.getEntries();
            case "buildcraftenergy" -> BcEnergyItems.ITEMS.getEntries();
            case "buildcraftfactory" -> BcFactoryItems.ITEMS.getEntries();
            case "buildcraftsilicon" -> BcSiliconItems.ITEMS.getEntries();
            case "buildcrafttransport" -> BcTransportItems.ITEMS.getEntries();
            case "buildcraftrobotics" -> BcRoboticsItems.ITEMS.getEntries();
            default -> throw new IllegalStateException("BcDatagen: unexpected mod id '" + modid + "'");
        };
    }

    /** All registered blocks of one BuildCraft mod, in registration order (buildcraftlib has none, items only). */
    private static Iterable<DeferredHolder<Block, ? extends Block>> blocksFor(String modid) {
        return switch (modid) {
            case "buildcraftcore" -> BcBlocks.BLOCKS.getEntries();
            case "buildcraftbuilders" -> BcBuildersBlocks.BLOCKS.getEntries();
            case "buildcraftenergy" -> BcEnergyBlocks.BLOCKS.getEntries();
            case "buildcraftfactory" -> BcFactoryBlocks.BLOCKS.getEntries();
            case "buildcraftsilicon" -> BcSiliconBlocks.BLOCKS.getEntries();
            case "buildcrafttransport" -> BcTransportBlocks.BLOCKS.getEntries();
            case "buildcraftrobotics" -> BcRoboticsBlocks.BLOCKS.getEntries();
            case "buildcraftlib" -> List.of();
            default -> throw new IllegalStateException("BcDatagen: unexpected mod id '" + modid + "'");
        };
    }
}
