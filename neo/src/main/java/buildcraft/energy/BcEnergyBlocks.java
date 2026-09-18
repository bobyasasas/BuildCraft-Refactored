/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.energy;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import buildcraft.lib.BcLangKeys;
import buildcraft.energy.fluid.PlaceholderFluidBlock;

/**
 * Central block registration for buildcraftenergy (task M2.4a skeleton, registry parity since M2.4b). Every block id
 * the 1.20.1 registry baseline attributes to {@code buildcraftenergy} registers here: 30 fluid world blocks (one
 * {@code fluid_block_*} per oil/fuel heat variant, mirroring the legacy {@code BCEnergyFluids} naming) plus the
 * {@code mj_dynamo} block. Fluid blocks share {@link PlaceholderFluidBlock} with vanilla-water-like properties; the
 * real block behaviour (legacy {@code BlockDynamoMJ} and {@code BCFluidBlock}) migrates in M2.5+.
 */
public final class BcEnergyBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BuildCraftEnergy.MOD_ID);
    /** Placeholder for {@code buildcraftenergy:fluid_block_fuel_dense_heat_0} (legacy {@code BCFluidBlock}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<PlaceholderFluidBlock> FLUID_BLOCK_FUEL_DENSE_HEAT_0 = BcLangKeys.block(BLOCKS, "fluid_block_fuel_dense_heat_0",
            properties -> new PlaceholderFluidBlock(BcEnergyFluids.FUEL_DENSE_HEAT_0.value(), properties),
            properties -> properties.replaceable().noCollision().randomTicks().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    /** Placeholder for {@code buildcraftenergy:fluid_block_fuel_dense_heat_1} (legacy {@code BCFluidBlock}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<PlaceholderFluidBlock> FLUID_BLOCK_FUEL_DENSE_HEAT_1 = BcLangKeys.block(BLOCKS, "fluid_block_fuel_dense_heat_1",
            properties -> new PlaceholderFluidBlock(BcEnergyFluids.FUEL_DENSE_HEAT_1.value(), properties),
            properties -> properties.replaceable().noCollision().randomTicks().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    /** Placeholder for {@code buildcraftenergy:fluid_block_fuel_dense_heat_2} (legacy {@code BCFluidBlock}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<PlaceholderFluidBlock> FLUID_BLOCK_FUEL_DENSE_HEAT_2 = BcLangKeys.block(BLOCKS, "fluid_block_fuel_dense_heat_2",
            properties -> new PlaceholderFluidBlock(BcEnergyFluids.FUEL_DENSE_HEAT_2.value(), properties),
            properties -> properties.replaceable().noCollision().randomTicks().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    /** Placeholder for {@code buildcraftenergy:fluid_block_fuel_gaseous_heat_0} (legacy {@code BCFluidBlock}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<PlaceholderFluidBlock> FLUID_BLOCK_FUEL_GASEOUS_HEAT_0 = BcLangKeys.block(BLOCKS, "fluid_block_fuel_gaseous_heat_0",
            properties -> new PlaceholderFluidBlock(BcEnergyFluids.FUEL_GASEOUS_HEAT_0.value(), properties),
            properties -> properties.replaceable().noCollision().randomTicks().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    /** Placeholder for {@code buildcraftenergy:fluid_block_fuel_gaseous_heat_1} (legacy {@code BCFluidBlock}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<PlaceholderFluidBlock> FLUID_BLOCK_FUEL_GASEOUS_HEAT_1 = BcLangKeys.block(BLOCKS, "fluid_block_fuel_gaseous_heat_1",
            properties -> new PlaceholderFluidBlock(BcEnergyFluids.FUEL_GASEOUS_HEAT_1.value(), properties),
            properties -> properties.replaceable().noCollision().randomTicks().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    /** Placeholder for {@code buildcraftenergy:fluid_block_fuel_gaseous_heat_2} (legacy {@code BCFluidBlock}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<PlaceholderFluidBlock> FLUID_BLOCK_FUEL_GASEOUS_HEAT_2 = BcLangKeys.block(BLOCKS, "fluid_block_fuel_gaseous_heat_2",
            properties -> new PlaceholderFluidBlock(BcEnergyFluids.FUEL_GASEOUS_HEAT_2.value(), properties),
            properties -> properties.replaceable().noCollision().randomTicks().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    /** Placeholder for {@code buildcraftenergy:fluid_block_fuel_light_heat_0} (legacy {@code BCFluidBlock}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<PlaceholderFluidBlock> FLUID_BLOCK_FUEL_LIGHT_HEAT_0 = BcLangKeys.block(BLOCKS, "fluid_block_fuel_light_heat_0",
            properties -> new PlaceholderFluidBlock(BcEnergyFluids.FUEL_LIGHT_HEAT_0.value(), properties),
            properties -> properties.replaceable().noCollision().randomTicks().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    /** Placeholder for {@code buildcraftenergy:fluid_block_fuel_light_heat_1} (legacy {@code BCFluidBlock}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<PlaceholderFluidBlock> FLUID_BLOCK_FUEL_LIGHT_HEAT_1 = BcLangKeys.block(BLOCKS, "fluid_block_fuel_light_heat_1",
            properties -> new PlaceholderFluidBlock(BcEnergyFluids.FUEL_LIGHT_HEAT_1.value(), properties),
            properties -> properties.replaceable().noCollision().randomTicks().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    /** Placeholder for {@code buildcraftenergy:fluid_block_fuel_light_heat_2} (legacy {@code BCFluidBlock}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<PlaceholderFluidBlock> FLUID_BLOCK_FUEL_LIGHT_HEAT_2 = BcLangKeys.block(BLOCKS, "fluid_block_fuel_light_heat_2",
            properties -> new PlaceholderFluidBlock(BcEnergyFluids.FUEL_LIGHT_HEAT_2.value(), properties),
            properties -> properties.replaceable().noCollision().randomTicks().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    /** Placeholder for {@code buildcraftenergy:fluid_block_fuel_mixed_heavy_heat_0} (legacy {@code BCFluidBlock}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<PlaceholderFluidBlock> FLUID_BLOCK_FUEL_MIXED_HEAVY_HEAT_0 = BcLangKeys.block(BLOCKS, "fluid_block_fuel_mixed_heavy_heat_0",
            properties -> new PlaceholderFluidBlock(BcEnergyFluids.FUEL_MIXED_HEAVY_HEAT_0.value(), properties),
            properties -> properties.replaceable().noCollision().randomTicks().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    /** Placeholder for {@code buildcraftenergy:fluid_block_fuel_mixed_heavy_heat_1} (legacy {@code BCFluidBlock}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<PlaceholderFluidBlock> FLUID_BLOCK_FUEL_MIXED_HEAVY_HEAT_1 = BcLangKeys.block(BLOCKS, "fluid_block_fuel_mixed_heavy_heat_1",
            properties -> new PlaceholderFluidBlock(BcEnergyFluids.FUEL_MIXED_HEAVY_HEAT_1.value(), properties),
            properties -> properties.replaceable().noCollision().randomTicks().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    /** Placeholder for {@code buildcraftenergy:fluid_block_fuel_mixed_heavy_heat_2} (legacy {@code BCFluidBlock}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<PlaceholderFluidBlock> FLUID_BLOCK_FUEL_MIXED_HEAVY_HEAT_2 = BcLangKeys.block(BLOCKS, "fluid_block_fuel_mixed_heavy_heat_2",
            properties -> new PlaceholderFluidBlock(BcEnergyFluids.FUEL_MIXED_HEAVY_HEAT_2.value(), properties),
            properties -> properties.replaceable().noCollision().randomTicks().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    /** Placeholder for {@code buildcraftenergy:fluid_block_fuel_mixed_light_heat_0} (legacy {@code BCFluidBlock}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<PlaceholderFluidBlock> FLUID_BLOCK_FUEL_MIXED_LIGHT_HEAT_0 = BcLangKeys.block(BLOCKS, "fluid_block_fuel_mixed_light_heat_0",
            properties -> new PlaceholderFluidBlock(BcEnergyFluids.FUEL_MIXED_LIGHT_HEAT_0.value(), properties),
            properties -> properties.replaceable().noCollision().randomTicks().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    /** Placeholder for {@code buildcraftenergy:fluid_block_fuel_mixed_light_heat_1} (legacy {@code BCFluidBlock}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<PlaceholderFluidBlock> FLUID_BLOCK_FUEL_MIXED_LIGHT_HEAT_1 = BcLangKeys.block(BLOCKS, "fluid_block_fuel_mixed_light_heat_1",
            properties -> new PlaceholderFluidBlock(BcEnergyFluids.FUEL_MIXED_LIGHT_HEAT_1.value(), properties),
            properties -> properties.replaceable().noCollision().randomTicks().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    /** Placeholder for {@code buildcraftenergy:fluid_block_fuel_mixed_light_heat_2} (legacy {@code BCFluidBlock}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<PlaceholderFluidBlock> FLUID_BLOCK_FUEL_MIXED_LIGHT_HEAT_2 = BcLangKeys.block(BLOCKS, "fluid_block_fuel_mixed_light_heat_2",
            properties -> new PlaceholderFluidBlock(BcEnergyFluids.FUEL_MIXED_LIGHT_HEAT_2.value(), properties),
            properties -> properties.replaceable().noCollision().randomTicks().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    /** Placeholder for {@code buildcraftenergy:fluid_block_oil_dense_heat_0} (legacy {@code BCFluidBlock}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<PlaceholderFluidBlock> FLUID_BLOCK_OIL_DENSE_HEAT_0 = BcLangKeys.block(BLOCKS, "fluid_block_oil_dense_heat_0",
            properties -> new PlaceholderFluidBlock(BcEnergyFluids.OIL_DENSE_HEAT_0.value(), properties),
            properties -> properties.replaceable().noCollision().randomTicks().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    /** Placeholder for {@code buildcraftenergy:fluid_block_oil_dense_heat_1} (legacy {@code BCFluidBlock}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<PlaceholderFluidBlock> FLUID_BLOCK_OIL_DENSE_HEAT_1 = BcLangKeys.block(BLOCKS, "fluid_block_oil_dense_heat_1",
            properties -> new PlaceholderFluidBlock(BcEnergyFluids.OIL_DENSE_HEAT_1.value(), properties),
            properties -> properties.replaceable().noCollision().randomTicks().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    /** Placeholder for {@code buildcraftenergy:fluid_block_oil_dense_heat_2} (legacy {@code BCFluidBlock}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<PlaceholderFluidBlock> FLUID_BLOCK_OIL_DENSE_HEAT_2 = BcLangKeys.block(BLOCKS, "fluid_block_oil_dense_heat_2",
            properties -> new PlaceholderFluidBlock(BcEnergyFluids.OIL_DENSE_HEAT_2.value(), properties),
            properties -> properties.replaceable().noCollision().randomTicks().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    /** Placeholder for {@code buildcraftenergy:fluid_block_oil_distilled_heat_0} (legacy {@code BCFluidBlock}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<PlaceholderFluidBlock> FLUID_BLOCK_OIL_DISTILLED_HEAT_0 = BcLangKeys.block(BLOCKS, "fluid_block_oil_distilled_heat_0",
            properties -> new PlaceholderFluidBlock(BcEnergyFluids.OIL_DISTILLED_HEAT_0.value(), properties),
            properties -> properties.replaceable().noCollision().randomTicks().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    /** Placeholder for {@code buildcraftenergy:fluid_block_oil_distilled_heat_1} (legacy {@code BCFluidBlock}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<PlaceholderFluidBlock> FLUID_BLOCK_OIL_DISTILLED_HEAT_1 = BcLangKeys.block(BLOCKS, "fluid_block_oil_distilled_heat_1",
            properties -> new PlaceholderFluidBlock(BcEnergyFluids.OIL_DISTILLED_HEAT_1.value(), properties),
            properties -> properties.replaceable().noCollision().randomTicks().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    /** Placeholder for {@code buildcraftenergy:fluid_block_oil_distilled_heat_2} (legacy {@code BCFluidBlock}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<PlaceholderFluidBlock> FLUID_BLOCK_OIL_DISTILLED_HEAT_2 = BcLangKeys.block(BLOCKS, "fluid_block_oil_distilled_heat_2",
            properties -> new PlaceholderFluidBlock(BcEnergyFluids.OIL_DISTILLED_HEAT_2.value(), properties),
            properties -> properties.replaceable().noCollision().randomTicks().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    /** Placeholder for {@code buildcraftenergy:fluid_block_oil_heat_0} (legacy {@code BCFluidBlock}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<PlaceholderFluidBlock> FLUID_BLOCK_OIL_HEAT_0 = BcLangKeys.block(BLOCKS, "fluid_block_oil_heat_0",
            properties -> new PlaceholderFluidBlock(BcEnergyFluids.OIL_HEAT_0.value(), properties),
            properties -> properties.replaceable().noCollision().randomTicks().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    /** Placeholder for {@code buildcraftenergy:fluid_block_oil_heat_1} (legacy {@code BCFluidBlock}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<PlaceholderFluidBlock> FLUID_BLOCK_OIL_HEAT_1 = BcLangKeys.block(BLOCKS, "fluid_block_oil_heat_1",
            properties -> new PlaceholderFluidBlock(BcEnergyFluids.OIL_HEAT_1.value(), properties),
            properties -> properties.replaceable().noCollision().randomTicks().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    /** Placeholder for {@code buildcraftenergy:fluid_block_oil_heat_2} (legacy {@code BCFluidBlock}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<PlaceholderFluidBlock> FLUID_BLOCK_OIL_HEAT_2 = BcLangKeys.block(BLOCKS, "fluid_block_oil_heat_2",
            properties -> new PlaceholderFluidBlock(BcEnergyFluids.OIL_HEAT_2.value(), properties),
            properties -> properties.replaceable().noCollision().randomTicks().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    /** Placeholder for {@code buildcraftenergy:fluid_block_oil_heavy_heat_0} (legacy {@code BCFluidBlock}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<PlaceholderFluidBlock> FLUID_BLOCK_OIL_HEAVY_HEAT_0 = BcLangKeys.block(BLOCKS, "fluid_block_oil_heavy_heat_0",
            properties -> new PlaceholderFluidBlock(BcEnergyFluids.OIL_HEAVY_HEAT_0.value(), properties),
            properties -> properties.replaceable().noCollision().randomTicks().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    /** Placeholder for {@code buildcraftenergy:fluid_block_oil_heavy_heat_1} (legacy {@code BCFluidBlock}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<PlaceholderFluidBlock> FLUID_BLOCK_OIL_HEAVY_HEAT_1 = BcLangKeys.block(BLOCKS, "fluid_block_oil_heavy_heat_1",
            properties -> new PlaceholderFluidBlock(BcEnergyFluids.OIL_HEAVY_HEAT_1.value(), properties),
            properties -> properties.replaceable().noCollision().randomTicks().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    /** Placeholder for {@code buildcraftenergy:fluid_block_oil_heavy_heat_2} (legacy {@code BCFluidBlock}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<PlaceholderFluidBlock> FLUID_BLOCK_OIL_HEAVY_HEAT_2 = BcLangKeys.block(BLOCKS, "fluid_block_oil_heavy_heat_2",
            properties -> new PlaceholderFluidBlock(BcEnergyFluids.OIL_HEAVY_HEAT_2.value(), properties),
            properties -> properties.replaceable().noCollision().randomTicks().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    /** Placeholder for {@code buildcraftenergy:fluid_block_oil_residue_heat_0} (legacy {@code BCFluidBlock}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<PlaceholderFluidBlock> FLUID_BLOCK_OIL_RESIDUE_HEAT_0 = BcLangKeys.block(BLOCKS, "fluid_block_oil_residue_heat_0",
            properties -> new PlaceholderFluidBlock(BcEnergyFluids.OIL_RESIDUE_HEAT_0.value(), properties),
            properties -> properties.replaceable().noCollision().randomTicks().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    /** Placeholder for {@code buildcraftenergy:fluid_block_oil_residue_heat_1} (legacy {@code BCFluidBlock}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<PlaceholderFluidBlock> FLUID_BLOCK_OIL_RESIDUE_HEAT_1 = BcLangKeys.block(BLOCKS, "fluid_block_oil_residue_heat_1",
            properties -> new PlaceholderFluidBlock(BcEnergyFluids.OIL_RESIDUE_HEAT_1.value(), properties),
            properties -> properties.replaceable().noCollision().randomTicks().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    /** Placeholder for {@code buildcraftenergy:fluid_block_oil_residue_heat_2} (legacy {@code BCFluidBlock}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<PlaceholderFluidBlock> FLUID_BLOCK_OIL_RESIDUE_HEAT_2 = BcLangKeys.block(BLOCKS, "fluid_block_oil_residue_heat_2",
            properties -> new PlaceholderFluidBlock(BcEnergyFluids.OIL_RESIDUE_HEAT_2.value(), properties),
            properties -> properties.replaceable().noCollision().randomTicks().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid());

    /** Placeholder for {@code buildcraftenergy:mj_dynamo} (legacy {@code BlockDynamoMJ}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> MJ_DYNAMO = BcLangKeys.simpleBlock(BLOCKS, "mj_dynamo",
            properties -> properties.strength(0.5F));


    private BcEnergyBlocks() {
    }
}
