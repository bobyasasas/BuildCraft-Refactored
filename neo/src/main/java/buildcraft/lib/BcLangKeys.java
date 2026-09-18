/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central bridge between the migrated registry ids and the frozen legacy lang keys (task M4.15).
 *
 * <p>The 1.20.1 migration kept the legacy {@code BuildCraft-Localization} en_us.json verbatim (857 old-style keys like
 * {@code tile.engineIron.name} / {@code item.PipeItemsWood.name}, see {@code migration/scripts/lang_diff.py}), while the
 * NeoForge registration derives modern default description ids ({@code block.<ns>.<path>} /
 * {@code item.<ns>.<path>}). The two never match, so every BC block/item displayed its raw translation key in game
 * (inventory, tooltips, JEI, Jade).
 *
 * <p><em>One mechanism, one table.</em> Vanilla 26.1 made {@code getDescriptionId()} final, so the legacy name is
 * injected through the platform's own single extension point instead: {@link Item.Properties#overrideDescription} and
 * {@link BlockBehaviour.Properties#overrideDescription}. The registration helpers below wrap the plain
 * {@link DeferredRegister} calls and pin the description id to the legacy key <em>iff</em> {@link #DESCRIPTION_IDS}
 * maps the id; unmapped ids keep the vanilla default (no anonymous classes at the call sites, no new item subclasses).
 * Block items and their blocks share the same key so the inventory tooltip and the Jade first line agree.
 *
 * <p>Rules of the table: only keys that exist in the frozen 857-key set are referenced (lang is a global flat KV at
 * runtime, so a buildcraftenergy item may reference a buildcraftcore lang key); ids without a defensible legacy key
 * stay unmapped and are listed in the task report for adjudication rather than getting invented keys. Mappings that
 * reuse a legacy key with slightly lossy wording are marked {@code approximate}.
 */
public final class BcLangKeys {

    /**
     * Registry id (namespace:path, exactly as registered) to legacy lang key. Entries marked {@code approximate}
     * deliberately reuse the closest frozen key although the legacy display was composed or more specific (the loss is
     * noted inline). Colour variants of the transport pipes are resolved via {@link #PIPE_FAMILY_KEYS} instead.
     */
    private static final Map<String, String> DESCRIPTION_IDS = buildDescriptionIds();

    /**
     * The 46 legacy pipe family prefixes (buildcrafttransport only) to their legacy item key. Legacy registered every
     * colour variant of a family under the same display name, so {@code <family>_colorless} and
     * {@code <family>_<colour>} all resolve to the family key (longest family prefix wins, so
     * {@code pipe_items_diamond_wood_...} resolves to the Wooden Diamond family, not plain Diamond).
     */
    private static final Map<String, String> PIPE_FAMILY_KEYS = buildPipeFamilyKeys();

    private static Map<String, String> buildDescriptionIds() {
        Map<String, String> map = new HashMap<>();
        // ------------------------------------------------------------------
        // buildcraftcore blocks (BcBlocks) and their block items (BcItems)
        // ------------------------------------------------------------------
        map.put("buildcraftcore:marker", "tile.markerBlock.name");
        map.put("buildcraftcore:engine_stone", "tile.engineStone.name");
        // The kinesis pipe block only ever had an item-form name in the legacy lang set.
        map.put("buildcraftcore:pipe_kinesis_wood", "item.PipePowerWood.name");
        map.put("buildcraftcore:spring_water", "tile.spring.water.name");
        map.put("buildcraftcore:spring_oil", "tile.spring.oil.name");
        map.put("buildcraftcore:decorated_blueprint", "tile.decorated.blueprint.name");
        map.put("buildcraftcore:decorated_laser_back", "tile.decorated.laser_back.name");
        map.put("buildcraftcore:decorated_template", "tile.decorated.template.name");
        map.put("buildcraftcore:engine_wood", "tile.engineWood.name");
        map.put("buildcraftcore:engine_creative", "tile.engineCreative.name");
        map.put("buildcraftcore:engine_iron", "tile.engineIron.name");
        map.put("buildcraftcore:engine_rf", "tile.engineRf.name");
        // approximate: legacy had no separate volume-marker key; the corner "Land Mark" is the closest frozen name.
        map.put("buildcraftcore:marker_volume", "tile.markerBlock.name");
        map.put("buildcraftcore:marker_path", "tile.pathMarkerBlock.name");
        map.put("buildcraftcore:power_tester", "tile.power_tester.name");
        // ------------------------------------------------------------------
        // buildcraftcore pure items (BcItems)
        // ------------------------------------------------------------------
        map.put("buildcraftcore:gear_wood", "item.woodenGearItem.name");
        map.put("buildcraftcore:gear_stone", "item.stoneGearItem.name");
        map.put("buildcraftcore:gear_iron", "item.ironGearItem.name");
        map.put("buildcraftcore:gear_gold", "item.goldGearItem.name");
        map.put("buildcraftcore:gear_diamond", "item.diamondGearItem.name");
        // approximate: legacy core goggles shared the "Robot Goggles" name.
        map.put("buildcraftcore:goggles", "item.buildcraft.robot_goggles.name");
        map.put("buildcraftcore:list", "item.list.name");
        map.put("buildcraftcore:map_location", "item.mapLocation.name");
        map.put("buildcraftcore:marker_connector", "item.markerConnector.name");
        // approximate: legacy lang had a single "Paintbrush" key for every colour, clean included.
        for (String colour : new String[] {
                "black", "blue", "brown", "clean", "cyan", "gray", "green", "light_blue", "light_gray", "lime",
                "magenta", "orange", "pink", "purple", "red", "white", "yellow" }) {
            map.put("buildcraftcore:paintbrush_" + colour, "item.paintbrush.name");
        }
        map.put("buildcraftcore:volume_box", "item.volume_box.name");
        map.put("buildcraftcore:wrench", "item.wrenchItem.name");
        // ------------------------------------------------------------------
        // buildcraftbuilders blocks (BcBuildersBlocks) + pure items (BcBuildersItems)
        // ------------------------------------------------------------------
        map.put("buildcraftbuilders:architect", "tile.architectBlock.name");
        map.put("buildcraftbuilders:builder", "tile.builderBlock.name");
        map.put("buildcraftbuilders:filler", "tile.fillerBlock.name");
        map.put("buildcraftbuilders:frame", "tile.frameBlock.name");
        map.put("buildcraftbuilders:library", "tile.libraryBlock.name");
        map.put("buildcraftbuilders:marker_construction", "tile.constructionMarkerBlock.name");
        map.put("buildcraftbuilders:quarry", "tile.quarryBlock.name");
        map.put("buildcraftbuilders:replacer", "tile.replacerBlock.name");
        map.put("buildcraftbuilders:filler_planner", "item.buildcraft.filler_planner.name");
        map.put("buildcraftbuilders:schematic_single", "item.schematicSingle.name");
        map.put("buildcraftbuilders:snapshot_blueprint", "item.blueprintItem.name");
        map.put("buildcraftbuilders:snapshot_template", "item.templateItem.name");
        // ------------------------------------------------------------------
        // buildcraftenergy blocks (BcEnergyBlocks) + items (BcEnergyItems)
        // ------------------------------------------------------------------
        // approximate: the fluid world blocks reuse the fluid display keys; the legacy "(Cool/Hot/Searing)" heat
        // suffix was composed in code and has no frozen key of its own.
        for (int heat = 0; heat <= 2; heat++) {
            map.put("buildcraftenergy:fluid_block_oil_heat_" + heat, "fluid.oil");
            map.put("buildcraftenergy:fluid_block_oil_dense_heat_" + heat, "fluid.oil_dense");
            map.put("buildcraftenergy:fluid_block_oil_distilled_heat_" + heat, "fluid.oil_distilled");
            map.put("buildcraftenergy:fluid_block_oil_heavy_heat_" + heat, "fluid.oil_heavy");
            map.put("buildcraftenergy:fluid_block_oil_residue_heat_" + heat, "fluid.oil_residue");
            map.put("buildcraftenergy:fluid_block_fuel_heat_" + heat, "fluid.fuel");
            map.put("buildcraftenergy:fluid_block_fuel_dense_heat_" + heat, "fluid.fuel_dense");
            map.put("buildcraftenergy:fluid_block_fuel_gaseous_heat_" + heat, "fluid.fuel_gaseous");
            map.put("buildcraftenergy:fluid_block_fuel_light_heat_" + heat, "fluid.fuel_light");
            map.put("buildcraftenergy:fluid_block_fuel_mixed_heavy_heat_" + heat, "fluid.fuel_mixed_heavy");
            map.put("buildcraftenergy:fluid_block_fuel_mixed_light_heat_" + heat, "fluid.fuel_mixed_light");
            // approximate: buckets likewise, via the two plain bucket keys ("Oil Bucket" / "Fuel Bucket").
            map.put("buildcraftenergy:oil_heat_" + heat + "_bucket", "item.bucketOil.name");
            map.put("buildcraftenergy:oil_dense_heat_" + heat + "_bucket", "item.bucketOil.name");
            map.put("buildcraftenergy:oil_distilled_heat_" + heat + "_bucket", "item.bucketOil.name");
            map.put("buildcraftenergy:oil_heavy_heat_" + heat + "_bucket", "item.bucketOil.name");
            map.put("buildcraftenergy:oil_residue_heat_" + heat + "_bucket", "item.bucketOil.name");
            map.put("buildcraftenergy:fuel_heat_" + heat + "_bucket", "item.bucketFuel.name");
            map.put("buildcraftenergy:fuel_dense_heat_" + heat + "_bucket", "item.bucketFuel.name");
            map.put("buildcraftenergy:fuel_gaseous_heat_" + heat + "_bucket", "item.bucketFuel.name");
            map.put("buildcraftenergy:fuel_light_heat_" + heat + "_bucket", "item.bucketFuel.name");
            map.put("buildcraftenergy:fuel_mixed_heavy_heat_" + heat + "_bucket", "item.bucketFuel.name");
            map.put("buildcraftenergy:fuel_mixed_light_heat_" + heat + "_bucket", "item.bucketFuel.name");
        }
        map.put("buildcraftenergy:mj_dynamo", "tile.mjDynamo.name");
        map.put("buildcraftenergy:glob_oil", "item.globOil.name");
        map.put("buildcraftenergy:oil_placer", "item.oilPlacer.name");
        // ------------------------------------------------------------------
        // buildcraftfactory blocks (BcFactoryBlocks) + pure items (BcFactoryItems)
        // ------------------------------------------------------------------
        map.put("buildcraftfactory:autoworkbench_item", "tile.autoWorkbenchBlock.name");
        map.put("buildcraftfactory:chute", "tile.chuteBlock.name");
        map.put("buildcraftfactory:distiller", "tile.distiller.name");
        map.put("buildcraftfactory:flood_gate", "tile.floodGateBlock.name");
        map.put("buildcraftfactory:heat_exchange", "tile.heat_exchange.name");
        map.put("buildcraftfactory:mining_well", "tile.miningWellBlock.name");
        map.put("buildcraftfactory:pump", "tile.pumpBlock.name");
        map.put("buildcraftfactory:tank", "tile.tankBlock.name");
        map.put("buildcraftfactory:tube", "tile.tubeBlock.name");
        map.put("buildcraftfactory:water_gel", "tile.waterGel.name");
        map.put("buildcraftfactory:gel", "item.gel.name");
        map.put("buildcraftfactory:plastic_sheet", "item.plasticSheet.name");
        map.put("buildcraftfactory:water_gel_spawn", "item.waterGel.name");
        // ------------------------------------------------------------------
        // buildcraftrobotics blocks (BcRoboticsBlocks) + items (BcRoboticsItems)
        // ------------------------------------------------------------------
        map.put("buildcraftrobotics:requester", "tile.requester.name");
        map.put("buildcraftrobotics:zone_planner", "tile.zonePlannerBlock.name");
        // approximate: the empty board is the plain "Redstone Board".
        map.put("buildcraftrobotics:board_robot_empty", "item.redstone_board.name");
        map.put("buildcraftrobotics:board_robot_bomber", "buildcraft.boardRobotBomber");
        map.put("buildcraftrobotics:board_robot_builder", "buildcraft.boardRobotBuilder");
        map.put("buildcraftrobotics:board_robot_butcher", "buildcraft.boardRobotButcher");
        map.put("buildcraftrobotics:board_robot_carrier", "buildcraft.boardRobotCarrier");
        map.put("buildcraftrobotics:board_robot_delivery", "buildcraft.boardRobotDelivery");
        map.put("buildcraftrobotics:board_robot_farmer", "buildcraft.boardRobotFarmer");
        map.put("buildcraftrobotics:board_robot_fluid_carrier", "buildcraft.boardRobotFluidCarrier");
        map.put("buildcraftrobotics:board_robot_harvester", "buildcraft.boardRobotHarvester");
        map.put("buildcraftrobotics:board_robot_knight", "buildcraft.boardRobotKnight");
        map.put("buildcraftrobotics:board_robot_leave_cutter", "buildcraft.boardRobotLeaveCutter");
        map.put("buildcraftrobotics:board_robot_lumberjack", "buildcraft.boardRobotLumberjack");
        map.put("buildcraftrobotics:board_robot_miner", "buildcraft.boardRobotMiner");
        map.put("buildcraftrobotics:board_robot_picker", "buildcraft.boardRobotPicker");
        map.put("buildcraftrobotics:board_robot_planter", "buildcraft.boardRobotPlanter");
        map.put("buildcraftrobotics:board_robot_pump", "buildcraft.boardRobotPump");
        map.put("buildcraftrobotics:board_robot_shovelman", "buildcraft.boardRobotShovelman");
        map.put("buildcraftrobotics:board_robot_stripes", "buildcraft.boardRobotStripes");
        // approximate: legacy robot names composed "<Board> Robot"; only the plain "Robot" key is frozen.
        map.put("buildcraftrobotics:robot_base", "item.robot.name");
        for (String board : new String[] {
                "bomber", "builder", "butcher", "carrier", "delivery", "farmer", "fluid_carrier", "harvester",
                "knight", "leave_cutter", "lumberjack", "miner", "picker", "planter", "pump", "shovelman",
                "stripes" }) {
            map.put("buildcraftrobotics:robot_" + board, "item.robot.name");
        }
        map.put("buildcraftrobotics:robot_googles", "item.buildcraft.robot_goggles.name");
        // approximate: legacy robot station shared the "Docking Station" pipe-pluggable name.
        map.put("buildcraftrobotics:robot_station", "item.PipeRobotStation.name");
        // ------------------------------------------------------------------
        // buildcraftsilicon blocks (BcSiliconBlocks) + pure items (BcSiliconItems)
        // ------------------------------------------------------------------
        map.put("buildcraftsilicon:advanced_crafting_table", "tile.assemblyWorkbenchBlock.name");
        map.put("buildcraftsilicon:assembly_table", "tile.assemblyTableBlock.name");
        map.put("buildcraftsilicon:charging_table", "tile.chargingTableBlock.name");
        map.put("buildcraftsilicon:integration_table", "tile.integrationTableBlock.name");
        map.put("buildcraftsilicon:laser", "tile.laserBlock.name");
        map.put("buildcraftsilicon:programming_table", "tile.programmingTableBlock.name");
        map.put("buildcraftsilicon:chipset_diamond", "item.redstone_diamond_chipset.name");
        map.put("buildcraftsilicon:chipset_gold", "item.redstone_gold_chipset.name");
        map.put("buildcraftsilicon:chipset_iron", "item.redstone_iron_chipset.name");
        map.put("buildcraftsilicon:chipset_quartz", "item.redstone_quartz_chipset.name");
        map.put("buildcraftsilicon:chipset_redstone", "item.redstone_red_chipset.name");
        map.put("buildcraftsilicon:gate_copier", "item.gateCopier.name");
        map.put("buildcraftsilicon:plug_facade", "item.Facade.name");
        map.put("buildcraftsilicon:plug_lens", "item.Lens.name");
        map.put("buildcraftsilicon:plug_light_sensor", "item.light_sensor.name");
        map.put("buildcraftsilicon:plug_pulsar", "item.pulsar.name");
        map.put("buildcraftsilicon:plug_timer", "item.timer.name");
        map.put("buildcraftsilicon:redstone_crystal", "item.redstoneCrystal.name");
        // (The 25 plug_gate ids stay unmapped on purpose: legacy composed "%s %s Gate" per variant.)
        // ------------------------------------------------------------------
        // buildcraftlib items (BcLibItems)
        // ------------------------------------------------------------------
        map.put("buildcraftlib:guide", "item.buildcraft.guide.name");
        map.put("buildcraftlib:guide_note", "item.buildcraft.guide_note.name");
        map.put("buildcraftlib:debugger", "item.debugger.name");
        // ------------------------------------------------------------------
        // buildcrafttransport blocks (BcTransportBlocks) + pure items (BcTransportItems)
        // ------------------------------------------------------------------
        map.put("buildcrafttransport:filtered_buffer", "tile.filteredBufferBlock.name");
        map.put("buildcrafttransport:plug_blocker", "item.PipePlug.name");
        map.put("buildcrafttransport:plug_power_adaptor", "item.PipePowerAdapter.name");
        map.put("buildcrafttransport:waterproof", "item.pipeWaterproof.name");
        map.put("buildcrafttransport:wire", "item.pipeWire.name");
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, String> buildPipeFamilyKeys() {
        Map<String, String> map = new HashMap<>();
        // structure pipes (1)
        map.put("pipe_structure_cobblestone", "item.PipeStructureCobblestone.name");
        // item pipes (16)
        map.put("pipe_items_wood", "item.PipeItemsWood.name");
        map.put("pipe_items_cobblestone", "item.PipeItemsCobblestone.name");
        map.put("pipe_items_stone", "item.PipeItemsStone.name");
        map.put("pipe_items_quartz", "item.PipeItemsQuartz.name");
        map.put("pipe_items_iron", "item.PipeItemsIron.name");
        map.put("pipe_items_gold", "item.PipeItemsGold.name");
        map.put("pipe_items_clay", "item.PipeItemsClay.name");
        map.put("pipe_items_sandstone", "item.PipeItemsSandstone.name");
        map.put("pipe_items_void", "item.PipeItemsVoid.name");
        map.put("pipe_items_obsidian", "item.PipeItemsObsidian.name");
        map.put("pipe_items_diamond", "item.PipeItemsDiamond.name");
        map.put("pipe_items_diamond_wood", "item.PipeItemsWoodenDiamond.name");
        map.put("pipe_items_lapis", "item.PipeItemsLapis.name");
        map.put("pipe_items_daizuli", "item.PipeItemsDaizuli.name");
        map.put("pipe_items_emzuli", "item.PipeItemsEmzuli.name");
        map.put("pipe_items_stripes", "item.PipeItemsStripes.name");
        // fluid pipes (11)
        map.put("pipe_fluids_wood", "item.PipeFluidsWood.name");
        map.put("pipe_fluids_cobblestone", "item.PipeFluidsCobblestone.name");
        map.put("pipe_fluids_stone", "item.PipeFluidsStone.name");
        map.put("pipe_fluids_quartz", "item.PipeFluidsQuartz.name");
        map.put("pipe_fluids_gold", "item.PipeFluidsGold.name");
        map.put("pipe_fluids_iron", "item.PipeFluidsIron.name");
        map.put("pipe_fluids_clay", "item.PipeFluidsClay.name");
        map.put("pipe_fluids_sandstone", "item.PipeFluidsSandstone.name");
        map.put("pipe_fluids_void", "item.PipeFluidsVoid.name");
        map.put("pipe_fluids_diamond", "item.PipeFluidsDiamond.name");
        map.put("pipe_fluids_diamond_wood", "item.PipeFluidsWoodenDiamond.name");
        // kinesis pipes (9)
        map.put("pipe_power_wood", "item.PipePowerWood.name");
        map.put("pipe_power_cobblestone", "item.PipePowerCobblestone.name");
        map.put("pipe_power_stone", "item.PipePowerStone.name");
        map.put("pipe_power_quartz", "item.PipePowerQuartz.name");
        map.put("pipe_power_iron", "item.PipePowerIron.name");
        map.put("pipe_power_gold", "item.PipePowerGold.name");
        map.put("pipe_power_sandstone", "item.PipePowerSandstone.name");
        map.put("pipe_power_diamond", "item.PipePowerDiamond.name");
        map.put("pipe_power_diamond_wood", "item.PipePowerWoodenDiamond.name");
        // rf pipes (9)
        map.put("pipe_rf_wood", "item.PipeRedstoneFluxWood.name");
        map.put("pipe_rf_cobblestone", "item.PipeRedstoneFluxCobblestone.name");
        map.put("pipe_rf_stone", "item.PipeRedstoneFluxStone.name");
        map.put("pipe_rf_quartz", "item.PipeRedstoneFluxQuartz.name");
        map.put("pipe_rf_iron", "item.PipeRedstoneFluxIron.name");
        map.put("pipe_rf_gold", "item.PipeRedstoneFluxGold.name");
        map.put("pipe_rf_sandstone", "item.PipeRedstoneFluxSandstone.name");
        map.put("pipe_rf_diamond", "item.PipeRedstoneFluxDiamond.name");
        map.put("pipe_rf_diamond_wood", "item.PipeRedstoneFluxWoodenDiamond.name");
        return Collections.unmodifiableMap(map);
    }

    /**
     * @param namespace the registering mod id (the {@link DeferredRegister} namespace)
     * @param path      the registry path of the block/item
     * @return the legacy lang key for the id, or {@code null} to keep the vanilla default description id
     */
    static String descriptionIdFor(String namespace, String path) {
        String exact = DESCRIPTION_IDS.get(namespace + ":" + path);
        if (exact != null) {
            return exact;
        }
        // Every colour variant of a pipe family displayed the family name in legacy, so fall back to the longest
        // matching family prefix (<family>_colorless and <family>_<colour> both resolve to <family>).
        if ("buildcrafttransport".equals(namespace)) {
            String bestFamily = null;
            String bestKey = null;
            for (Map.Entry<String, String> entry : PIPE_FAMILY_KEYS.entrySet()) {
                String family = entry.getKey();
                if (path.length() > family.length() + 1 && path.startsWith(family + "_")
                        && (bestFamily == null || family.length() > bestFamily.length())) {
                    bestFamily = family;
                    bestKey = entry.getValue();
                }
            }
            return bestKey;
        }
        return null;
    }

    // ---------------------------------------------------------------------
    // One mechanism: registration helpers that pin the legacy key when mapped
    // ---------------------------------------------------------------------

    /**
     * {@link DeferredRegister.Items#registerSimpleItem(String)} through the legacy-name bridge: plain {@link Item},
     * default properties, description id pinned to the legacy key when the id is mapped.
     */
    public static DeferredItem<Item> item(DeferredRegister.Items items, String path) {
        String key = descriptionIdFor(items.getNamespace(), path);
        return key == null ? items.registerSimpleItem(path)
                : items.registerItem(path, Item::new, properties -> properties.overrideDescription(key));
    }

    /**
     * {@link DeferredRegister.Items#registerSimpleItem(String, UnaryOperator)} through the legacy-name bridge.
     * (Named apart from {@link #item(DeferredRegister.Items, String, Function)} because an implicitly typed lambda
     * cannot pick between a {@code UnaryOperator} and a {@code Function} overload.)
     */
    public static DeferredItem<Item> simpleItem(DeferredRegister.Items items, String path,
            UnaryOperator<Item.Properties> properties) {
        String key = descriptionIdFor(items.getNamespace(), path);
        return key == null ? items.registerSimpleItem(path, properties)
                : items.registerItem(path, Item::new, prop -> properties.apply(prop).overrideDescription(key));
    }

    /**
     * {@link DeferredRegister.Items#registerItem(String, Function)} through the legacy-name bridge: custom item class,
     * default properties (the 26.1 item id is still injected into the properties).
     */
    public static <I extends Item> DeferredItem<I> item(DeferredRegister.Items items, String path,
            Function<Item.Properties, ? extends I> factory) {
        String key = descriptionIdFor(items.getNamespace(), path);
        return key == null ? items.registerItem(path, factory)
                : items.registerItem(path, factory, properties -> properties.overrideDescription(key));
    }

    /**
     * {@link DeferredRegister.Items#registerItem(String, Function, UnaryOperator)} through the legacy-name bridge:
     * custom item class plus custom properties.
     */
    public static <I extends Item> DeferredItem<I> item(DeferredRegister.Items items, String path,
            Function<Item.Properties, ? extends I> factory, UnaryOperator<Item.Properties> properties) {
        String key = descriptionIdFor(items.getNamespace(), path);
        return key == null ? items.registerItem(path, factory, properties)
                : items.registerItem(path, factory, prop -> properties.apply(prop).overrideDescription(key));
    }

    /**
     * {@link DeferredRegister.Items#registerSimpleBlockItem(Holder)} through the legacy-name bridge: the item name is
     * derived from the block id (so it always matches the block's own key from {@link #block}).
     */
    public static DeferredItem<BlockItem> blockItem(DeferredRegister.Items items, DeferredBlock<? extends Block> block) {
        String path = block.unwrapKey().orElseThrow().identifier().getPath();
        String namespace = block.unwrapKey().orElseThrow().identifier().getNamespace();
        String key = descriptionIdFor(namespace, path);
        return key == null ? items.registerSimpleBlockItem(block)
                : items.registerItem(path, properties -> new BlockItem(block.get(), properties),
                        () -> new Item.Properties().overrideDescription(key));
    }

    /**
     * {@link DeferredRegister.Blocks#registerSimpleBlock(String, UnaryOperator)} through the legacy-name bridge: plain
     * {@link Block} placeholder whose Jade/tooltip name is the legacy key when mapped. (Named apart from
     * {@link #block(DeferredRegister.Blocks, String, Function)} because an implicitly typed lambda cannot pick between
     * a {@code UnaryOperator} and a {@code Function} overload.)
     */
    public static DeferredBlock<Block> simpleBlock(DeferredRegister.Blocks blocks, String path,
            UnaryOperator<BlockBehaviour.Properties> properties) {
        String key = descriptionIdFor(blocks.getNamespace(), path);
        return key == null ? blocks.registerSimpleBlock(path, properties)
                : blocks.registerSimpleBlock(path, props -> properties.apply(props).overrideDescription(key));
    }

    /**
     * {@link DeferredRegister.Blocks#registerBlock(String, Function)} through the legacy-name bridge: custom block
     * class, default properties.
     */
    public static <B extends Block> DeferredBlock<B> block(DeferredRegister.Blocks blocks, String path,
            Function<BlockBehaviour.Properties, ? extends B> factory) {
        String key = descriptionIdFor(blocks.getNamespace(), path);
        return key == null ? blocks.registerBlock(path, factory)
                : blocks.registerBlock(path, factory, properties -> properties.overrideDescription(key));
    }

    /**
     * {@link DeferredRegister.Blocks#registerBlock(String, Function, UnaryOperator)} through the legacy-name bridge:
     * custom block class plus a properties operator. (Not overloaded against the {@link Supplier} variant below on
     * purpose: the two are told apart by lambda arity, exactly like the {@link DeferredRegister} methods they wrap.)
     */
    public static <B extends Block> DeferredBlock<B> block(DeferredRegister.Blocks blocks, String path,
            Function<BlockBehaviour.Properties, ? extends B> factory, UnaryOperator<BlockBehaviour.Properties> properties) {
        String key = descriptionIdFor(blocks.getNamespace(), path);
        return key == null ? blocks.registerBlock(path, factory, properties)
                : blocks.registerBlock(path, factory, props -> properties.apply(props).overrideDescription(key));
    }

    /**
     * {@link DeferredRegister.Blocks#registerBlock(String, Function, Supplier)} through the legacy-name bridge: custom
     * block class plus supplied properties.
     */
    public static <B extends Block> DeferredBlock<B> block(DeferredRegister.Blocks blocks, String path,
            Function<BlockBehaviour.Properties, ? extends B> factory, Supplier<BlockBehaviour.Properties> properties) {
        String key = descriptionIdFor(blocks.getNamespace(), path);
        return key == null ? blocks.registerBlock(path, factory, properties)
                : blocks.registerBlock(path, factory, () -> properties.get().overrideDescription(key));
    }

    private BcLangKeys() {
    }
}
