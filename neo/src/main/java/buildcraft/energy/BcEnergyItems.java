/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.energy;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central item registration for buildcraftenergy (task M2.4a skeleton, registry parity since M2.4b). Every item id the
 * 1.20.1 registry baseline attributes to {@code buildcraftenergy} registers here: 30 fluid buckets (one per still
 * fluid, via the vanilla {@link BucketItem} like the legacy {@code ItemBucketBC}), the {@code mj_dynamo} block item and
 * the two pure placeholder items. Bucket/fluid references are safe in the item registry because fluids register first.
 */
public final class BcEnergyItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BuildCraftEnergy.MOD_ID);
    /** Placeholder for {@code buildcraftenergy:fuel_dense_heat_0_bucket} (legacy {@code ItemBucketBC}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BucketItem> FUEL_DENSE_HEAT_0_BUCKET = ITEMS.registerItem("fuel_dense_heat_0_bucket",
            properties -> new BucketItem(BcEnergyFluids.FUEL_DENSE_HEAT_0.value(), properties),
            properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET));

    /** Placeholder for {@code buildcraftenergy:fuel_dense_heat_1_bucket} (legacy {@code ItemBucketBC}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BucketItem> FUEL_DENSE_HEAT_1_BUCKET = ITEMS.registerItem("fuel_dense_heat_1_bucket",
            properties -> new BucketItem(BcEnergyFluids.FUEL_DENSE_HEAT_1.value(), properties),
            properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET));

    /** Placeholder for {@code buildcraftenergy:fuel_dense_heat_2_bucket} (legacy {@code ItemBucketBC}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BucketItem> FUEL_DENSE_HEAT_2_BUCKET = ITEMS.registerItem("fuel_dense_heat_2_bucket",
            properties -> new BucketItem(BcEnergyFluids.FUEL_DENSE_HEAT_2.value(), properties),
            properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET));

    /** Placeholder for {@code buildcraftenergy:fuel_gaseous_heat_0_bucket} (legacy {@code ItemBucketBC}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BucketItem> FUEL_GASEOUS_HEAT_0_BUCKET = ITEMS.registerItem("fuel_gaseous_heat_0_bucket",
            properties -> new BucketItem(BcEnergyFluids.FUEL_GASEOUS_HEAT_0.value(), properties),
            properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET));

    /** Placeholder for {@code buildcraftenergy:fuel_gaseous_heat_1_bucket} (legacy {@code ItemBucketBC}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BucketItem> FUEL_GASEOUS_HEAT_1_BUCKET = ITEMS.registerItem("fuel_gaseous_heat_1_bucket",
            properties -> new BucketItem(BcEnergyFluids.FUEL_GASEOUS_HEAT_1.value(), properties),
            properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET));

    /** Placeholder for {@code buildcraftenergy:fuel_gaseous_heat_2_bucket} (legacy {@code ItemBucketBC}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BucketItem> FUEL_GASEOUS_HEAT_2_BUCKET = ITEMS.registerItem("fuel_gaseous_heat_2_bucket",
            properties -> new BucketItem(BcEnergyFluids.FUEL_GASEOUS_HEAT_2.value(), properties),
            properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET));

    /** Placeholder for {@code buildcraftenergy:fuel_light_heat_0_bucket} (legacy {@code ItemBucketBC}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BucketItem> FUEL_LIGHT_HEAT_0_BUCKET = ITEMS.registerItem("fuel_light_heat_0_bucket",
            properties -> new BucketItem(BcEnergyFluids.FUEL_LIGHT_HEAT_0.value(), properties),
            properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET));

    /** Placeholder for {@code buildcraftenergy:fuel_light_heat_1_bucket} (legacy {@code ItemBucketBC}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BucketItem> FUEL_LIGHT_HEAT_1_BUCKET = ITEMS.registerItem("fuel_light_heat_1_bucket",
            properties -> new BucketItem(BcEnergyFluids.FUEL_LIGHT_HEAT_1.value(), properties),
            properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET));

    /** Placeholder for {@code buildcraftenergy:fuel_light_heat_2_bucket} (legacy {@code ItemBucketBC}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BucketItem> FUEL_LIGHT_HEAT_2_BUCKET = ITEMS.registerItem("fuel_light_heat_2_bucket",
            properties -> new BucketItem(BcEnergyFluids.FUEL_LIGHT_HEAT_2.value(), properties),
            properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET));

    /** Placeholder for {@code buildcraftenergy:fuel_mixed_heavy_heat_0_bucket} (legacy {@code ItemBucketBC}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BucketItem> FUEL_MIXED_HEAVY_HEAT_0_BUCKET = ITEMS.registerItem("fuel_mixed_heavy_heat_0_bucket",
            properties -> new BucketItem(BcEnergyFluids.FUEL_MIXED_HEAVY_HEAT_0.value(), properties),
            properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET));

    /** Placeholder for {@code buildcraftenergy:fuel_mixed_heavy_heat_1_bucket} (legacy {@code ItemBucketBC}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BucketItem> FUEL_MIXED_HEAVY_HEAT_1_BUCKET = ITEMS.registerItem("fuel_mixed_heavy_heat_1_bucket",
            properties -> new BucketItem(BcEnergyFluids.FUEL_MIXED_HEAVY_HEAT_1.value(), properties),
            properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET));

    /** Placeholder for {@code buildcraftenergy:fuel_mixed_heavy_heat_2_bucket} (legacy {@code ItemBucketBC}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BucketItem> FUEL_MIXED_HEAVY_HEAT_2_BUCKET = ITEMS.registerItem("fuel_mixed_heavy_heat_2_bucket",
            properties -> new BucketItem(BcEnergyFluids.FUEL_MIXED_HEAVY_HEAT_2.value(), properties),
            properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET));

    /** Placeholder for {@code buildcraftenergy:fuel_mixed_light_heat_0_bucket} (legacy {@code ItemBucketBC}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BucketItem> FUEL_MIXED_LIGHT_HEAT_0_BUCKET = ITEMS.registerItem("fuel_mixed_light_heat_0_bucket",
            properties -> new BucketItem(BcEnergyFluids.FUEL_MIXED_LIGHT_HEAT_0.value(), properties),
            properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET));

    /** Placeholder for {@code buildcraftenergy:fuel_mixed_light_heat_1_bucket} (legacy {@code ItemBucketBC}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BucketItem> FUEL_MIXED_LIGHT_HEAT_1_BUCKET = ITEMS.registerItem("fuel_mixed_light_heat_1_bucket",
            properties -> new BucketItem(BcEnergyFluids.FUEL_MIXED_LIGHT_HEAT_1.value(), properties),
            properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET));

    /** Placeholder for {@code buildcraftenergy:fuel_mixed_light_heat_2_bucket} (legacy {@code ItemBucketBC}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BucketItem> FUEL_MIXED_LIGHT_HEAT_2_BUCKET = ITEMS.registerItem("fuel_mixed_light_heat_2_bucket",
            properties -> new BucketItem(BcEnergyFluids.FUEL_MIXED_LIGHT_HEAT_2.value(), properties),
            properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET));

    /** Placeholder for {@code buildcraftenergy:oil_dense_heat_0_bucket} (legacy {@code ItemBucketBC}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BucketItem> OIL_DENSE_HEAT_0_BUCKET = ITEMS.registerItem("oil_dense_heat_0_bucket",
            properties -> new BucketItem(BcEnergyFluids.OIL_DENSE_HEAT_0.value(), properties),
            properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET));

    /** Placeholder for {@code buildcraftenergy:oil_dense_heat_1_bucket} (legacy {@code ItemBucketBC}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BucketItem> OIL_DENSE_HEAT_1_BUCKET = ITEMS.registerItem("oil_dense_heat_1_bucket",
            properties -> new BucketItem(BcEnergyFluids.OIL_DENSE_HEAT_1.value(), properties),
            properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET));

    /** Placeholder for {@code buildcraftenergy:oil_dense_heat_2_bucket} (legacy {@code ItemBucketBC}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BucketItem> OIL_DENSE_HEAT_2_BUCKET = ITEMS.registerItem("oil_dense_heat_2_bucket",
            properties -> new BucketItem(BcEnergyFluids.OIL_DENSE_HEAT_2.value(), properties),
            properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET));

    /** Placeholder for {@code buildcraftenergy:oil_distilled_heat_0_bucket} (legacy {@code ItemBucketBC}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BucketItem> OIL_DISTILLED_HEAT_0_BUCKET = ITEMS.registerItem("oil_distilled_heat_0_bucket",
            properties -> new BucketItem(BcEnergyFluids.OIL_DISTILLED_HEAT_0.value(), properties),
            properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET));

    /** Placeholder for {@code buildcraftenergy:oil_distilled_heat_1_bucket} (legacy {@code ItemBucketBC}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BucketItem> OIL_DISTILLED_HEAT_1_BUCKET = ITEMS.registerItem("oil_distilled_heat_1_bucket",
            properties -> new BucketItem(BcEnergyFluids.OIL_DISTILLED_HEAT_1.value(), properties),
            properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET));

    /** Placeholder for {@code buildcraftenergy:oil_distilled_heat_2_bucket} (legacy {@code ItemBucketBC}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BucketItem> OIL_DISTILLED_HEAT_2_BUCKET = ITEMS.registerItem("oil_distilled_heat_2_bucket",
            properties -> new BucketItem(BcEnergyFluids.OIL_DISTILLED_HEAT_2.value(), properties),
            properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET));

    /** Placeholder for {@code buildcraftenergy:oil_heat_0_bucket} (legacy {@code ItemBucketBC}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BucketItem> OIL_HEAT_0_BUCKET = ITEMS.registerItem("oil_heat_0_bucket",
            properties -> new BucketItem(BcEnergyFluids.OIL_HEAT_0.value(), properties),
            properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET));

    /** Placeholder for {@code buildcraftenergy:oil_heat_1_bucket} (legacy {@code ItemBucketBC}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BucketItem> OIL_HEAT_1_BUCKET = ITEMS.registerItem("oil_heat_1_bucket",
            properties -> new BucketItem(BcEnergyFluids.OIL_HEAT_1.value(), properties),
            properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET));

    /** Placeholder for {@code buildcraftenergy:oil_heat_2_bucket} (legacy {@code ItemBucketBC}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BucketItem> OIL_HEAT_2_BUCKET = ITEMS.registerItem("oil_heat_2_bucket",
            properties -> new BucketItem(BcEnergyFluids.OIL_HEAT_2.value(), properties),
            properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET));

    /** Placeholder for {@code buildcraftenergy:oil_heavy_heat_0_bucket} (legacy {@code ItemBucketBC}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BucketItem> OIL_HEAVY_HEAT_0_BUCKET = ITEMS.registerItem("oil_heavy_heat_0_bucket",
            properties -> new BucketItem(BcEnergyFluids.OIL_HEAVY_HEAT_0.value(), properties),
            properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET));

    /** Placeholder for {@code buildcraftenergy:oil_heavy_heat_1_bucket} (legacy {@code ItemBucketBC}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BucketItem> OIL_HEAVY_HEAT_1_BUCKET = ITEMS.registerItem("oil_heavy_heat_1_bucket",
            properties -> new BucketItem(BcEnergyFluids.OIL_HEAVY_HEAT_1.value(), properties),
            properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET));

    /** Placeholder for {@code buildcraftenergy:oil_heavy_heat_2_bucket} (legacy {@code ItemBucketBC}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BucketItem> OIL_HEAVY_HEAT_2_BUCKET = ITEMS.registerItem("oil_heavy_heat_2_bucket",
            properties -> new BucketItem(BcEnergyFluids.OIL_HEAVY_HEAT_2.value(), properties),
            properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET));

    /** Placeholder for {@code buildcraftenergy:oil_residue_heat_0_bucket} (legacy {@code ItemBucketBC}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BucketItem> OIL_RESIDUE_HEAT_0_BUCKET = ITEMS.registerItem("oil_residue_heat_0_bucket",
            properties -> new BucketItem(BcEnergyFluids.OIL_RESIDUE_HEAT_0.value(), properties),
            properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET));

    /** Placeholder for {@code buildcraftenergy:oil_residue_heat_1_bucket} (legacy {@code ItemBucketBC}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BucketItem> OIL_RESIDUE_HEAT_1_BUCKET = ITEMS.registerItem("oil_residue_heat_1_bucket",
            properties -> new BucketItem(BcEnergyFluids.OIL_RESIDUE_HEAT_1.value(), properties),
            properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET));

    /** Placeholder for {@code buildcraftenergy:oil_residue_heat_2_bucket} (legacy {@code ItemBucketBC}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BucketItem> OIL_RESIDUE_HEAT_2_BUCKET = ITEMS.registerItem("oil_residue_heat_2_bucket",
            properties -> new BucketItem(BcEnergyFluids.OIL_RESIDUE_HEAT_2.value(), properties),
            properties -> properties.stacksTo(1).craftRemainder(Items.BUCKET));

    /** Placeholder for {@code buildcraftenergy:glob_oil} (legacy {@code ItemBC_Neptune}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> GLOB_OIL = ITEMS.registerSimpleItem("glob_oil");

    /** Placeholder for {@code buildcraftenergy:oil_placer} (legacy {@code ItemOilPlacer}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> OIL_PLACER = ITEMS.registerSimpleItem("oil_placer");


    /** Item form of the {@code buildcraftenergy:mj_dynamo} placeholder block (legacy {@code BlockItem} of {@code BlockDynamoMJ}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> MJ_DYNAMO = ITEMS.registerSimpleBlockItem(BcEnergyBlocks.MJ_DYNAMO);

    private BcEnergyItems() {
    }
}
