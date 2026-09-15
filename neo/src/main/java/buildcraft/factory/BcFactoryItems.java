/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central item registration for buildcraftfactory (task M2.4a skeleton, registry parity since M2.4b). Every item id
 * the 1.20.1 registry baseline attributes to {@code buildcraftfactory} registers here: block items for exactly the
 * ids the baseline pairs with an item, plus pure placeholder items. Note the baseline deliberately has no item for some
 * blocks (buildcraftfactory:tube, buildcraftfactory:water_gel), so none is invented here.
 */
public final class BcFactoryItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BuildCraftFactory.MOD_ID);
    /** Item form of the {@code buildcraftfactory:autoworkbench_item} placeholder block (legacy {@code BlockItem} of {@code BlockAutoWorkbenchItems}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> AUTOWORKBENCH_ITEM = ITEMS.registerSimpleBlockItem(BcFactoryBlocks.AUTOWORKBENCH_ITEM);

    /** Item form of the {@code buildcraftfactory:chute} placeholder block (legacy {@code BlockItem} of {@code BlockChute}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> CHUTE = ITEMS.registerSimpleBlockItem(BcFactoryBlocks.CHUTE);

    /** Item form of the {@code buildcraftfactory:distiller} placeholder block (legacy {@code BlockItem} of {@code BlockDistiller_BC8}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> DISTILLER = ITEMS.registerSimpleBlockItem(BcFactoryBlocks.DISTILLER);

    /** Item form of the {@code buildcraftfactory:flood_gate} placeholder block (legacy {@code BlockItem} of {@code BlockFloodGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> FLOOD_GATE = ITEMS.registerSimpleBlockItem(BcFactoryBlocks.FLOOD_GATE);

    /** Placeholder for {@code buildcraftfactory:gel} (legacy {@code ItemBC_Neptune}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> GEL = ITEMS.registerSimpleItem("gel");

    /** Item form of the {@code buildcraftfactory:heat_exchange} placeholder block (legacy {@code BlockItem} of {@code BlockHeatExchange}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> HEAT_EXCHANGE = ITEMS.registerSimpleBlockItem(BcFactoryBlocks.HEAT_EXCHANGE);

    /** Item form of the {@code buildcraftfactory:mining_well} placeholder block (legacy {@code BlockItem} of {@code BlockMiningWell}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> MINING_WELL = ITEMS.registerSimpleBlockItem(BcFactoryBlocks.MINING_WELL);

    /** Placeholder for {@code buildcraftfactory:plastic_sheet} (legacy {@code ItemBC_Neptune}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLASTIC_SHEET = ITEMS.registerSimpleItem("plastic_sheet");

    /** Item form of the {@code buildcraftfactory:pump} placeholder block (legacy {@code BlockItem} of {@code BlockPump}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> PUMP = ITEMS.registerSimpleBlockItem(BcFactoryBlocks.PUMP);

    /** Item form of the {@code buildcraftfactory:tank} placeholder block (legacy {@code BlockItem} of {@code BlockTank}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> TANK = ITEMS.registerSimpleBlockItem(BcFactoryBlocks.TANK);

    /** Placeholder for {@code buildcraftfactory:water_gel_spawn} (legacy {@code ItemWaterGel}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> WATER_GEL_SPAWN = ITEMS.registerSimpleItem("water_gel_spawn");


    private BcFactoryItems() {
    }
}
