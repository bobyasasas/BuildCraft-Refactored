/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.silicon;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import buildcraft.lib.datacomponent.gate.EnumGateLogic;
import buildcraft.lib.datacomponent.gate.EnumGateMaterial;
import buildcraft.lib.datacomponent.gate.EnumGateModifier;
import buildcraft.lib.datacomponent.gate.GateVariantData;
import buildcraft.silicon.item.ItemPluggableGate;

/**
 * Central item registration for buildcraftsilicon (task M2.4a skeleton, registry parity since M2.4b). Every item id
 * the 1.20.1 registry baseline attributes to {@code buildcraftsilicon} registers here: block items for exactly the
 * ids the baseline pairs with an item, plus pure placeholder items. Note the baseline deliberately has no item for some
 * blocks (buildcraftfactory:tube, buildcraftfactory:water_gel), so none is invented here.
 */
public final class BcSiliconItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BuildCraftSilicon.MOD_ID);
    /** Item form of the {@code buildcraftsilicon:advanced_crafting_table} placeholder block (legacy {@code BlockItem} of {@code BlockLaserTable}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> ADVANCED_CRAFTING_TABLE = ITEMS.registerSimpleBlockItem(BcSiliconBlocks.ADVANCED_CRAFTING_TABLE);

    /** Item form of the {@code buildcraftsilicon:assembly_table} placeholder block (legacy {@code BlockItem} of {@code BlockLaserTable}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> ASSEMBLY_TABLE = ITEMS.registerSimpleBlockItem(BcSiliconBlocks.ASSEMBLY_TABLE);

    /** Item form of the {@code buildcraftsilicon:charging_table} placeholder block (legacy {@code BlockItem} of {@code BlockLaserTable}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> CHARGING_TABLE = ITEMS.registerSimpleBlockItem(BcSiliconBlocks.CHARGING_TABLE);

    /** Placeholder for {@code buildcraftsilicon:chipset_diamond} (legacy {@code ItemRedstoneChipset}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> CHIPSET_DIAMOND = ITEMS.registerSimpleItem("chipset_diamond");

    /** Placeholder for {@code buildcraftsilicon:chipset_gold} (legacy {@code ItemRedstoneChipset}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> CHIPSET_GOLD = ITEMS.registerSimpleItem("chipset_gold");

    /** Placeholder for {@code buildcraftsilicon:chipset_iron} (legacy {@code ItemRedstoneChipset}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> CHIPSET_IRON = ITEMS.registerSimpleItem("chipset_iron");

    /** Placeholder for {@code buildcraftsilicon:chipset_quartz} (legacy {@code ItemRedstoneChipset}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> CHIPSET_QUARTZ = ITEMS.registerSimpleItem("chipset_quartz");

    /** Placeholder for {@code buildcraftsilicon:chipset_redstone} (legacy {@code ItemRedstoneChipset}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> CHIPSET_REDSTONE = ITEMS.registerSimpleItem("chipset_redstone");

    /** Placeholder for {@code buildcraftsilicon:gate_copier} (legacy {@code ItemGateCopier}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> GATE_COPIER = ITEMS.registerSimpleItem("gate_copier");

    /** Item form of the {@code buildcraftsilicon:integration_table} placeholder block (legacy {@code BlockItem} of {@code BlockLaserTable}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> INTEGRATION_TABLE = ITEMS.registerSimpleBlockItem(BcSiliconBlocks.INTEGRATION_TABLE);

    /** Item form of the {@code buildcraftsilicon:laser} placeholder block (legacy {@code BlockItem} of {@code BlockLaser}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> LASER = ITEMS.registerSimpleBlockItem(BcSiliconBlocks.LASER);

    /** Placeholder for {@code buildcraftsilicon:plug_facade} (legacy {@code ItemPlugFacade}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_FACADE = ITEMS.registerSimpleItem("plug_facade");

    /** Placeholder for {@code buildcraftsilicon:plug_gate} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE = ITEMS.registerSimpleItem("plug_gate");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_gold_and_diamond} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_GOLD_AND_DIAMOND = ITEMS.registerSimpleItem("plug_gate_gold_and_diamond");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_gold_and_lapis} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_GOLD_AND_LAPIS = ITEMS.registerSimpleItem("plug_gate_gold_and_lapis");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_gold_and_no_modifier} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_GOLD_AND_NO_MODIFIER = ITEMS.registerSimpleItem("plug_gate_gold_and_no_modifier");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_gold_and_quartz} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_GOLD_AND_QUARTZ = ITEMS.registerSimpleItem("plug_gate_gold_and_quartz");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_gold_or_diamond} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_GOLD_OR_DIAMOND = ITEMS.registerSimpleItem("plug_gate_gold_or_diamond");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_gold_or_lapis} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_GOLD_OR_LAPIS = ITEMS.registerSimpleItem("plug_gate_gold_or_lapis");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_gold_or_no_modifier} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_GOLD_OR_NO_MODIFIER = ITEMS.registerSimpleItem("plug_gate_gold_or_no_modifier");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_gold_or_quartz} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_GOLD_OR_QUARTZ = ITEMS.registerSimpleItem("plug_gate_gold_or_quartz");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_iron_and_diamond} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_IRON_AND_DIAMOND = ITEMS.registerSimpleItem("plug_gate_iron_and_diamond");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_iron_and_lapis} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_IRON_AND_LAPIS = ITEMS.registerSimpleItem("plug_gate_iron_and_lapis");

    /** The M2.11 gate slice variant (legacy {@code GateVariant}: AND x IRON x no_modifier, 2 slots). */
    public static final GateVariantData IRON_GATE_VARIANT = new GateVariantData(EnumGateLogic.AND,
            EnumGateMaterial.IRON, EnumGateModifier.NO_MODIFIER);

    /** Item form of the {@code buildcraftsilicon:iron_and_no_modifier} gate variant (legacy {@code ItemPluggableGate});
     * the M2.11 gate slice: attaches a real 2-slot gate to a slice pipe face (see {@link ItemPluggableGate}).
     * registerItem (not the raw supplier register) so the 26.1.2 {@code Item.Properties} item id gets injected. */
    public static final DeferredItem<ItemPluggableGate> PLUG_GATE_IRON_AND_NO_MODIFIER = ITEMS.registerItem(
            "plug_gate_iron_and_no_modifier",
            properties -> new ItemPluggableGate(properties, IRON_GATE_VARIANT),
            () -> new Item.Properties());

    /** Placeholder for {@code buildcraftsilicon:plug_gate_iron_and_quartz} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_IRON_AND_QUARTZ = ITEMS.registerSimpleItem("plug_gate_iron_and_quartz");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_iron_or_diamond} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_IRON_OR_DIAMOND = ITEMS.registerSimpleItem("plug_gate_iron_or_diamond");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_iron_or_lapis} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_IRON_OR_LAPIS = ITEMS.registerSimpleItem("plug_gate_iron_or_lapis");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_iron_or_no_modifier} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_IRON_OR_NO_MODIFIER = ITEMS.registerSimpleItem("plug_gate_iron_or_no_modifier");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_iron_or_quartz} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_IRON_OR_QUARTZ = ITEMS.registerSimpleItem("plug_gate_iron_or_quartz");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_nether_brick_and_diamond} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_NETHER_BRICK_AND_DIAMOND = ITEMS.registerSimpleItem("plug_gate_nether_brick_and_diamond");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_nether_brick_and_lapis} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_NETHER_BRICK_AND_LAPIS = ITEMS.registerSimpleItem("plug_gate_nether_brick_and_lapis");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_nether_brick_and_no_modifier} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_NETHER_BRICK_AND_NO_MODIFIER = ITEMS.registerSimpleItem("plug_gate_nether_brick_and_no_modifier");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_nether_brick_and_quartz} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_NETHER_BRICK_AND_QUARTZ = ITEMS.registerSimpleItem("plug_gate_nether_brick_and_quartz");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_nether_brick_or_diamond} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_NETHER_BRICK_OR_DIAMOND = ITEMS.registerSimpleItem("plug_gate_nether_brick_or_diamond");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_nether_brick_or_lapis} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_NETHER_BRICK_OR_LAPIS = ITEMS.registerSimpleItem("plug_gate_nether_brick_or_lapis");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_nether_brick_or_no_modifier} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_NETHER_BRICK_OR_NO_MODIFIER = ITEMS.registerSimpleItem("plug_gate_nether_brick_or_no_modifier");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_nether_brick_or_quartz} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_NETHER_BRICK_OR_QUARTZ = ITEMS.registerSimpleItem("plug_gate_nether_brick_or_quartz");

    /** Placeholder for {@code buildcraftsilicon:plug_lens} (legacy {@code ItemPlugLens}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_LENS = ITEMS.registerSimpleItem("plug_lens");

    /** Placeholder for {@code buildcraftsilicon:plug_light_sensor} (legacy {@code ItemPlugLightSensor}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_LIGHT_SENSOR = ITEMS.registerSimpleItem("plug_light_sensor");

    /** Placeholder for {@code buildcraftsilicon:plug_pulsar} (legacy {@code ItemPlugPulsar}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_PULSAR = ITEMS.registerSimpleItem("plug_pulsar");

    /** Placeholder for {@code buildcraftsilicon:plug_timer} (legacy {@code ItemPlugTimer}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_TIMER = ITEMS.registerSimpleItem("plug_timer");

    /** Item form of the {@code buildcraftsilicon:programming_table} placeholder block (legacy {@code BlockItem} of {@code BlockLaserTable}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> PROGRAMMING_TABLE = ITEMS.registerSimpleBlockItem(BcSiliconBlocks.PROGRAMMING_TABLE);

    /** Placeholder for {@code buildcraftsilicon:redstone_crystal} (legacy {@code ItemRedstoneCrystal}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> REDSTONE_CRYSTAL = ITEMS.registerSimpleItem("redstone_crystal");


    private BcSiliconItems() {
    }
}
