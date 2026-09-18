/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.silicon;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import buildcraft.lib.BcLangKeys;
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
    public static final DeferredItem<BlockItem> ADVANCED_CRAFTING_TABLE = BcLangKeys.blockItem(ITEMS, BcSiliconBlocks.ADVANCED_CRAFTING_TABLE);

    /** Item form of the {@code buildcraftsilicon:assembly_table} placeholder block (legacy {@code BlockItem} of {@code BlockLaserTable}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> ASSEMBLY_TABLE = BcLangKeys.blockItem(ITEMS, BcSiliconBlocks.ASSEMBLY_TABLE);

    /** Item form of the {@code buildcraftsilicon:charging_table} placeholder block (legacy {@code BlockItem} of {@code BlockLaserTable}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> CHARGING_TABLE = BcLangKeys.blockItem(ITEMS, BcSiliconBlocks.CHARGING_TABLE);

    /** Placeholder for {@code buildcraftsilicon:chipset_diamond} (legacy {@code ItemRedstoneChipset}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> CHIPSET_DIAMOND = BcLangKeys.item(ITEMS, "chipset_diamond");

    /** Placeholder for {@code buildcraftsilicon:chipset_gold} (legacy {@code ItemRedstoneChipset}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> CHIPSET_GOLD = BcLangKeys.item(ITEMS, "chipset_gold");

    /** Placeholder for {@code buildcraftsilicon:chipset_iron} (legacy {@code ItemRedstoneChipset}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> CHIPSET_IRON = BcLangKeys.item(ITEMS, "chipset_iron");

    /** Placeholder for {@code buildcraftsilicon:chipset_quartz} (legacy {@code ItemRedstoneChipset}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> CHIPSET_QUARTZ = BcLangKeys.item(ITEMS, "chipset_quartz");

    /** Placeholder for {@code buildcraftsilicon:chipset_redstone} (legacy {@code ItemRedstoneChipset}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> CHIPSET_REDSTONE = BcLangKeys.item(ITEMS, "chipset_redstone");

    /** Placeholder for {@code buildcraftsilicon:gate_copier} (legacy {@code ItemGateCopier}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> GATE_COPIER = BcLangKeys.item(ITEMS, "gate_copier");

    /** Item form of the {@code buildcraftsilicon:integration_table} placeholder block (legacy {@code BlockItem} of {@code BlockLaserTable}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> INTEGRATION_TABLE = BcLangKeys.blockItem(ITEMS, BcSiliconBlocks.INTEGRATION_TABLE);

    /** Item form of the {@code buildcraftsilicon:laser} placeholder block (legacy {@code BlockItem} of {@code BlockLaser}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> LASER = BcLangKeys.blockItem(ITEMS, BcSiliconBlocks.LASER);

    /** Placeholder for {@code buildcraftsilicon:plug_facade} (legacy {@code ItemPlugFacade}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_FACADE = BcLangKeys.item(ITEMS, "plug_facade");

    /** Placeholder for {@code buildcraftsilicon:plug_gate} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE = BcLangKeys.item(ITEMS, "plug_gate");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_gold_and_diamond} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_GOLD_AND_DIAMOND = BcLangKeys.item(ITEMS, "plug_gate_gold_and_diamond");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_gold_and_lapis} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_GOLD_AND_LAPIS = BcLangKeys.item(ITEMS, "plug_gate_gold_and_lapis");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_gold_and_no_modifier} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_GOLD_AND_NO_MODIFIER = BcLangKeys.item(ITEMS, "plug_gate_gold_and_no_modifier");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_gold_and_quartz} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_GOLD_AND_QUARTZ = BcLangKeys.item(ITEMS, "plug_gate_gold_and_quartz");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_gold_or_diamond} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_GOLD_OR_DIAMOND = BcLangKeys.item(ITEMS, "plug_gate_gold_or_diamond");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_gold_or_lapis} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_GOLD_OR_LAPIS = BcLangKeys.item(ITEMS, "plug_gate_gold_or_lapis");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_gold_or_no_modifier} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_GOLD_OR_NO_MODIFIER = BcLangKeys.item(ITEMS, "plug_gate_gold_or_no_modifier");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_gold_or_quartz} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_GOLD_OR_QUARTZ = BcLangKeys.item(ITEMS, "plug_gate_gold_or_quartz");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_iron_and_diamond} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_IRON_AND_DIAMOND = BcLangKeys.item(ITEMS, "plug_gate_iron_and_diamond");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_iron_and_lapis} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_IRON_AND_LAPIS = BcLangKeys.item(ITEMS, "plug_gate_iron_and_lapis");

    /** The M2.11 gate slice variant (legacy {@code GateVariant}: AND x IRON x no_modifier, 2 slots). */
    public static final GateVariantData IRON_GATE_VARIANT = new GateVariantData(EnumGateLogic.AND,
            EnumGateMaterial.IRON, EnumGateModifier.NO_MODIFIER);

    /** Item form of the {@code buildcraftsilicon:iron_and_no_modifier} gate variant (legacy {@code ItemPluggableGate});
     * the M2.11 gate slice: attaches a real 2-slot gate to a slice pipe face (see {@link ItemPluggableGate}).
     * Registered through the M4.15 {@link BcLangKeys} bridge (which uses {@code registerItem}, not the raw supplier
     * register, so the 26.1.2 {@code Item.Properties} item id still gets injected); the gate variant ids stay
     * unmapped in the legacy lang bridge, so this keeps the default description id. */
    public static final DeferredItem<ItemPluggableGate> PLUG_GATE_IRON_AND_NO_MODIFIER = BcLangKeys.item(ITEMS,
            "plug_gate_iron_and_no_modifier",
            properties -> new ItemPluggableGate(properties, IRON_GATE_VARIANT));

    /** Placeholder for {@code buildcraftsilicon:plug_gate_iron_and_quartz} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_IRON_AND_QUARTZ = BcLangKeys.item(ITEMS, "plug_gate_iron_and_quartz");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_iron_or_diamond} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_IRON_OR_DIAMOND = BcLangKeys.item(ITEMS, "plug_gate_iron_or_diamond");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_iron_or_lapis} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_IRON_OR_LAPIS = BcLangKeys.item(ITEMS, "plug_gate_iron_or_lapis");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_iron_or_no_modifier} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_IRON_OR_NO_MODIFIER = BcLangKeys.item(ITEMS, "plug_gate_iron_or_no_modifier");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_iron_or_quartz} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_IRON_OR_QUARTZ = BcLangKeys.item(ITEMS, "plug_gate_iron_or_quartz");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_nether_brick_and_diamond} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_NETHER_BRICK_AND_DIAMOND = BcLangKeys.item(ITEMS, "plug_gate_nether_brick_and_diamond");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_nether_brick_and_lapis} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_NETHER_BRICK_AND_LAPIS = BcLangKeys.item(ITEMS, "plug_gate_nether_brick_and_lapis");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_nether_brick_and_no_modifier} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_NETHER_BRICK_AND_NO_MODIFIER = BcLangKeys.item(ITEMS, "plug_gate_nether_brick_and_no_modifier");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_nether_brick_and_quartz} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_NETHER_BRICK_AND_QUARTZ = BcLangKeys.item(ITEMS, "plug_gate_nether_brick_and_quartz");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_nether_brick_or_diamond} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_NETHER_BRICK_OR_DIAMOND = BcLangKeys.item(ITEMS, "plug_gate_nether_brick_or_diamond");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_nether_brick_or_lapis} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_NETHER_BRICK_OR_LAPIS = BcLangKeys.item(ITEMS, "plug_gate_nether_brick_or_lapis");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_nether_brick_or_no_modifier} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_NETHER_BRICK_OR_NO_MODIFIER = BcLangKeys.item(ITEMS, "plug_gate_nether_brick_or_no_modifier");

    /** Placeholder for {@code buildcraftsilicon:plug_gate_nether_brick_or_quartz} (legacy {@code ItemPlugGate}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_GATE_NETHER_BRICK_OR_QUARTZ = BcLangKeys.item(ITEMS, "plug_gate_nether_brick_or_quartz");

    /** Placeholder for {@code buildcraftsilicon:plug_lens} (legacy {@code ItemPlugLens}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_LENS = BcLangKeys.item(ITEMS, "plug_lens");

    /** Placeholder for {@code buildcraftsilicon:plug_light_sensor} (legacy {@code ItemPlugLightSensor}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_LIGHT_SENSOR = BcLangKeys.item(ITEMS, "plug_light_sensor");

    /** Placeholder for {@code buildcraftsilicon:plug_pulsar} (legacy {@code ItemPlugPulsar}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_PULSAR = BcLangKeys.item(ITEMS, "plug_pulsar");

    /** Placeholder for {@code buildcraftsilicon:plug_timer} (legacy {@code ItemPlugTimer}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PLUG_TIMER = BcLangKeys.item(ITEMS, "plug_timer");

    /** Item form of the {@code buildcraftsilicon:programming_table} placeholder block (legacy {@code BlockItem} of {@code BlockLaserTable}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> PROGRAMMING_TABLE = BcLangKeys.blockItem(ITEMS, BcSiliconBlocks.PROGRAMMING_TABLE);

    /** Placeholder for {@code buildcraftsilicon:redstone_crystal} (legacy {@code ItemRedstoneCrystal}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> REDSTONE_CRYSTAL = BcLangKeys.item(ITEMS, "redstone_crystal");


    private BcSiliconItems() {
    }
}
