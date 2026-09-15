/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.energy;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import buildcraft.energy.fluid.PlaceholderFluidBlock;

/**
 * Central fluid registration for buildcraftenergy (task M2.4b registry parity). Every fluid id the 1.20.1 registry
 * baseline attributes to {@code buildcraftenergy} registers here as a {@link BaseFlowingFluid} still/flowing pair:
 * {@code <name>} (source) and {@code <name>_flow} (flowing), exactly the baseline pairing. The pairs are placeholders
 * with default spreading behaviour; the real oil/fuel behaviour (legacy {@code BCEnergyFluids} + {@code BCFluid})
 * migrates in M2.5+.
 *
 * <p>Wiring notes for 26.1.2: the fluid registry fires before the block/item registries and before
 * {@code neoforge:fluid_types}, so {@link #properties} keeps every collaborator as a lazy {@link Supplier} (the
 * {@code BaseFlowingFluid} contract) and nothing is dereferenced while the fluids register.
 */
public final class BcEnergyFluids {

    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(BuiltInRegistries.FLUID,
            BuildCraftEnergy.MOD_ID);
    /** Placeholder still/source fluid {@code buildcraftenergy:fuel_dense_heat_0} (legacy {@code BCFluid.Source}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> FUEL_DENSE_HEAT_0 = FLUIDS.register("fuel_dense_heat_0",
            () -> new BaseFlowingFluid.Source(properties(BcEnergyFluidTypes.FUEL_DENSE_HEAT_0, BcEnergyFluids.FUEL_DENSE_HEAT_0,
                    BcEnergyFluids.FUEL_DENSE_HEAT_0_FLOW, BcEnergyItems.FUEL_DENSE_HEAT_0_BUCKET, BcEnergyBlocks.FLUID_BLOCK_FUEL_DENSE_HEAT_0)));

    /** Placeholder flowing fluid {@code buildcraftenergy:fuel_dense_heat_0_flow} (legacy {@code BCFluid.Flowing}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FUEL_DENSE_HEAT_0_FLOW = FLUIDS.register("fuel_dense_heat_0_flow",
            () -> new BaseFlowingFluid.Flowing(properties(BcEnergyFluidTypes.FUEL_DENSE_HEAT_0, BcEnergyFluids.FUEL_DENSE_HEAT_0,
                    BcEnergyFluids.FUEL_DENSE_HEAT_0_FLOW, BcEnergyItems.FUEL_DENSE_HEAT_0_BUCKET, BcEnergyBlocks.FLUID_BLOCK_FUEL_DENSE_HEAT_0)));

    /** Placeholder still/source fluid {@code buildcraftenergy:fuel_dense_heat_1} (legacy {@code BCFluid.Source}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> FUEL_DENSE_HEAT_1 = FLUIDS.register("fuel_dense_heat_1",
            () -> new BaseFlowingFluid.Source(properties(BcEnergyFluidTypes.FUEL_DENSE_HEAT_1, BcEnergyFluids.FUEL_DENSE_HEAT_1,
                    BcEnergyFluids.FUEL_DENSE_HEAT_1_FLOW, BcEnergyItems.FUEL_DENSE_HEAT_1_BUCKET, BcEnergyBlocks.FLUID_BLOCK_FUEL_DENSE_HEAT_1)));

    /** Placeholder flowing fluid {@code buildcraftenergy:fuel_dense_heat_1_flow} (legacy {@code BCFluid.Flowing}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FUEL_DENSE_HEAT_1_FLOW = FLUIDS.register("fuel_dense_heat_1_flow",
            () -> new BaseFlowingFluid.Flowing(properties(BcEnergyFluidTypes.FUEL_DENSE_HEAT_1, BcEnergyFluids.FUEL_DENSE_HEAT_1,
                    BcEnergyFluids.FUEL_DENSE_HEAT_1_FLOW, BcEnergyItems.FUEL_DENSE_HEAT_1_BUCKET, BcEnergyBlocks.FLUID_BLOCK_FUEL_DENSE_HEAT_1)));

    /** Placeholder still/source fluid {@code buildcraftenergy:fuel_dense_heat_2} (legacy {@code BCFluid.Source}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> FUEL_DENSE_HEAT_2 = FLUIDS.register("fuel_dense_heat_2",
            () -> new BaseFlowingFluid.Source(properties(BcEnergyFluidTypes.FUEL_DENSE_HEAT_2, BcEnergyFluids.FUEL_DENSE_HEAT_2,
                    BcEnergyFluids.FUEL_DENSE_HEAT_2_FLOW, BcEnergyItems.FUEL_DENSE_HEAT_2_BUCKET, BcEnergyBlocks.FLUID_BLOCK_FUEL_DENSE_HEAT_2)));

    /** Placeholder flowing fluid {@code buildcraftenergy:fuel_dense_heat_2_flow} (legacy {@code BCFluid.Flowing}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FUEL_DENSE_HEAT_2_FLOW = FLUIDS.register("fuel_dense_heat_2_flow",
            () -> new BaseFlowingFluid.Flowing(properties(BcEnergyFluidTypes.FUEL_DENSE_HEAT_2, BcEnergyFluids.FUEL_DENSE_HEAT_2,
                    BcEnergyFluids.FUEL_DENSE_HEAT_2_FLOW, BcEnergyItems.FUEL_DENSE_HEAT_2_BUCKET, BcEnergyBlocks.FLUID_BLOCK_FUEL_DENSE_HEAT_2)));

    /** Placeholder still/source fluid {@code buildcraftenergy:fuel_gaseous_heat_0} (legacy {@code BCFluid.Source}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> FUEL_GASEOUS_HEAT_0 = FLUIDS.register("fuel_gaseous_heat_0",
            () -> new BaseFlowingFluid.Source(properties(BcEnergyFluidTypes.FUEL_GASEOUS_HEAT_0, BcEnergyFluids.FUEL_GASEOUS_HEAT_0,
                    BcEnergyFluids.FUEL_GASEOUS_HEAT_0_FLOW, BcEnergyItems.FUEL_GASEOUS_HEAT_0_BUCKET, BcEnergyBlocks.FLUID_BLOCK_FUEL_GASEOUS_HEAT_0)));

    /** Placeholder flowing fluid {@code buildcraftenergy:fuel_gaseous_heat_0_flow} (legacy {@code BCFluid.Flowing}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FUEL_GASEOUS_HEAT_0_FLOW = FLUIDS.register("fuel_gaseous_heat_0_flow",
            () -> new BaseFlowingFluid.Flowing(properties(BcEnergyFluidTypes.FUEL_GASEOUS_HEAT_0, BcEnergyFluids.FUEL_GASEOUS_HEAT_0,
                    BcEnergyFluids.FUEL_GASEOUS_HEAT_0_FLOW, BcEnergyItems.FUEL_GASEOUS_HEAT_0_BUCKET, BcEnergyBlocks.FLUID_BLOCK_FUEL_GASEOUS_HEAT_0)));

    /** Placeholder still/source fluid {@code buildcraftenergy:fuel_gaseous_heat_1} (legacy {@code BCFluid.Source}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> FUEL_GASEOUS_HEAT_1 = FLUIDS.register("fuel_gaseous_heat_1",
            () -> new BaseFlowingFluid.Source(properties(BcEnergyFluidTypes.FUEL_GASEOUS_HEAT_1, BcEnergyFluids.FUEL_GASEOUS_HEAT_1,
                    BcEnergyFluids.FUEL_GASEOUS_HEAT_1_FLOW, BcEnergyItems.FUEL_GASEOUS_HEAT_1_BUCKET, BcEnergyBlocks.FLUID_BLOCK_FUEL_GASEOUS_HEAT_1)));

    /** Placeholder flowing fluid {@code buildcraftenergy:fuel_gaseous_heat_1_flow} (legacy {@code BCFluid.Flowing}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FUEL_GASEOUS_HEAT_1_FLOW = FLUIDS.register("fuel_gaseous_heat_1_flow",
            () -> new BaseFlowingFluid.Flowing(properties(BcEnergyFluidTypes.FUEL_GASEOUS_HEAT_1, BcEnergyFluids.FUEL_GASEOUS_HEAT_1,
                    BcEnergyFluids.FUEL_GASEOUS_HEAT_1_FLOW, BcEnergyItems.FUEL_GASEOUS_HEAT_1_BUCKET, BcEnergyBlocks.FLUID_BLOCK_FUEL_GASEOUS_HEAT_1)));

    /** Placeholder still/source fluid {@code buildcraftenergy:fuel_gaseous_heat_2} (legacy {@code BCFluid.Source}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> FUEL_GASEOUS_HEAT_2 = FLUIDS.register("fuel_gaseous_heat_2",
            () -> new BaseFlowingFluid.Source(properties(BcEnergyFluidTypes.FUEL_GASEOUS_HEAT_2, BcEnergyFluids.FUEL_GASEOUS_HEAT_2,
                    BcEnergyFluids.FUEL_GASEOUS_HEAT_2_FLOW, BcEnergyItems.FUEL_GASEOUS_HEAT_2_BUCKET, BcEnergyBlocks.FLUID_BLOCK_FUEL_GASEOUS_HEAT_2)));

    /** Placeholder flowing fluid {@code buildcraftenergy:fuel_gaseous_heat_2_flow} (legacy {@code BCFluid.Flowing}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FUEL_GASEOUS_HEAT_2_FLOW = FLUIDS.register("fuel_gaseous_heat_2_flow",
            () -> new BaseFlowingFluid.Flowing(properties(BcEnergyFluidTypes.FUEL_GASEOUS_HEAT_2, BcEnergyFluids.FUEL_GASEOUS_HEAT_2,
                    BcEnergyFluids.FUEL_GASEOUS_HEAT_2_FLOW, BcEnergyItems.FUEL_GASEOUS_HEAT_2_BUCKET, BcEnergyBlocks.FLUID_BLOCK_FUEL_GASEOUS_HEAT_2)));

    /** Placeholder still/source fluid {@code buildcraftenergy:fuel_light_heat_0} (legacy {@code BCFluid.Source}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> FUEL_LIGHT_HEAT_0 = FLUIDS.register("fuel_light_heat_0",
            () -> new BaseFlowingFluid.Source(properties(BcEnergyFluidTypes.FUEL_LIGHT_HEAT_0, BcEnergyFluids.FUEL_LIGHT_HEAT_0,
                    BcEnergyFluids.FUEL_LIGHT_HEAT_0_FLOW, BcEnergyItems.FUEL_LIGHT_HEAT_0_BUCKET, BcEnergyBlocks.FLUID_BLOCK_FUEL_LIGHT_HEAT_0)));

    /** Placeholder flowing fluid {@code buildcraftenergy:fuel_light_heat_0_flow} (legacy {@code BCFluid.Flowing}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FUEL_LIGHT_HEAT_0_FLOW = FLUIDS.register("fuel_light_heat_0_flow",
            () -> new BaseFlowingFluid.Flowing(properties(BcEnergyFluidTypes.FUEL_LIGHT_HEAT_0, BcEnergyFluids.FUEL_LIGHT_HEAT_0,
                    BcEnergyFluids.FUEL_LIGHT_HEAT_0_FLOW, BcEnergyItems.FUEL_LIGHT_HEAT_0_BUCKET, BcEnergyBlocks.FLUID_BLOCK_FUEL_LIGHT_HEAT_0)));

    /** Placeholder still/source fluid {@code buildcraftenergy:fuel_light_heat_1} (legacy {@code BCFluid.Source}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> FUEL_LIGHT_HEAT_1 = FLUIDS.register("fuel_light_heat_1",
            () -> new BaseFlowingFluid.Source(properties(BcEnergyFluidTypes.FUEL_LIGHT_HEAT_1, BcEnergyFluids.FUEL_LIGHT_HEAT_1,
                    BcEnergyFluids.FUEL_LIGHT_HEAT_1_FLOW, BcEnergyItems.FUEL_LIGHT_HEAT_1_BUCKET, BcEnergyBlocks.FLUID_BLOCK_FUEL_LIGHT_HEAT_1)));

    /** Placeholder flowing fluid {@code buildcraftenergy:fuel_light_heat_1_flow} (legacy {@code BCFluid.Flowing}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FUEL_LIGHT_HEAT_1_FLOW = FLUIDS.register("fuel_light_heat_1_flow",
            () -> new BaseFlowingFluid.Flowing(properties(BcEnergyFluidTypes.FUEL_LIGHT_HEAT_1, BcEnergyFluids.FUEL_LIGHT_HEAT_1,
                    BcEnergyFluids.FUEL_LIGHT_HEAT_1_FLOW, BcEnergyItems.FUEL_LIGHT_HEAT_1_BUCKET, BcEnergyBlocks.FLUID_BLOCK_FUEL_LIGHT_HEAT_1)));

    /** Placeholder still/source fluid {@code buildcraftenergy:fuel_light_heat_2} (legacy {@code BCFluid.Source}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> FUEL_LIGHT_HEAT_2 = FLUIDS.register("fuel_light_heat_2",
            () -> new BaseFlowingFluid.Source(properties(BcEnergyFluidTypes.FUEL_LIGHT_HEAT_2, BcEnergyFluids.FUEL_LIGHT_HEAT_2,
                    BcEnergyFluids.FUEL_LIGHT_HEAT_2_FLOW, BcEnergyItems.FUEL_LIGHT_HEAT_2_BUCKET, BcEnergyBlocks.FLUID_BLOCK_FUEL_LIGHT_HEAT_2)));

    /** Placeholder flowing fluid {@code buildcraftenergy:fuel_light_heat_2_flow} (legacy {@code BCFluid.Flowing}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FUEL_LIGHT_HEAT_2_FLOW = FLUIDS.register("fuel_light_heat_2_flow",
            () -> new BaseFlowingFluid.Flowing(properties(BcEnergyFluidTypes.FUEL_LIGHT_HEAT_2, BcEnergyFluids.FUEL_LIGHT_HEAT_2,
                    BcEnergyFluids.FUEL_LIGHT_HEAT_2_FLOW, BcEnergyItems.FUEL_LIGHT_HEAT_2_BUCKET, BcEnergyBlocks.FLUID_BLOCK_FUEL_LIGHT_HEAT_2)));

    /** Placeholder still/source fluid {@code buildcraftenergy:fuel_mixed_heavy_heat_0} (legacy {@code BCFluid.Source}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> FUEL_MIXED_HEAVY_HEAT_0 = FLUIDS.register("fuel_mixed_heavy_heat_0",
            () -> new BaseFlowingFluid.Source(properties(BcEnergyFluidTypes.FUEL_MIXED_HEAVY_HEAT_0, BcEnergyFluids.FUEL_MIXED_HEAVY_HEAT_0,
                    BcEnergyFluids.FUEL_MIXED_HEAVY_HEAT_0_FLOW, BcEnergyItems.FUEL_MIXED_HEAVY_HEAT_0_BUCKET, BcEnergyBlocks.FLUID_BLOCK_FUEL_MIXED_HEAVY_HEAT_0)));

    /** Placeholder flowing fluid {@code buildcraftenergy:fuel_mixed_heavy_heat_0_flow} (legacy {@code BCFluid.Flowing}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FUEL_MIXED_HEAVY_HEAT_0_FLOW = FLUIDS.register("fuel_mixed_heavy_heat_0_flow",
            () -> new BaseFlowingFluid.Flowing(properties(BcEnergyFluidTypes.FUEL_MIXED_HEAVY_HEAT_0, BcEnergyFluids.FUEL_MIXED_HEAVY_HEAT_0,
                    BcEnergyFluids.FUEL_MIXED_HEAVY_HEAT_0_FLOW, BcEnergyItems.FUEL_MIXED_HEAVY_HEAT_0_BUCKET, BcEnergyBlocks.FLUID_BLOCK_FUEL_MIXED_HEAVY_HEAT_0)));

    /** Placeholder still/source fluid {@code buildcraftenergy:fuel_mixed_heavy_heat_1} (legacy {@code BCFluid.Source}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> FUEL_MIXED_HEAVY_HEAT_1 = FLUIDS.register("fuel_mixed_heavy_heat_1",
            () -> new BaseFlowingFluid.Source(properties(BcEnergyFluidTypes.FUEL_MIXED_HEAVY_HEAT_1, BcEnergyFluids.FUEL_MIXED_HEAVY_HEAT_1,
                    BcEnergyFluids.FUEL_MIXED_HEAVY_HEAT_1_FLOW, BcEnergyItems.FUEL_MIXED_HEAVY_HEAT_1_BUCKET, BcEnergyBlocks.FLUID_BLOCK_FUEL_MIXED_HEAVY_HEAT_1)));

    /** Placeholder flowing fluid {@code buildcraftenergy:fuel_mixed_heavy_heat_1_flow} (legacy {@code BCFluid.Flowing}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FUEL_MIXED_HEAVY_HEAT_1_FLOW = FLUIDS.register("fuel_mixed_heavy_heat_1_flow",
            () -> new BaseFlowingFluid.Flowing(properties(BcEnergyFluidTypes.FUEL_MIXED_HEAVY_HEAT_1, BcEnergyFluids.FUEL_MIXED_HEAVY_HEAT_1,
                    BcEnergyFluids.FUEL_MIXED_HEAVY_HEAT_1_FLOW, BcEnergyItems.FUEL_MIXED_HEAVY_HEAT_1_BUCKET, BcEnergyBlocks.FLUID_BLOCK_FUEL_MIXED_HEAVY_HEAT_1)));

    /** Placeholder still/source fluid {@code buildcraftenergy:fuel_mixed_heavy_heat_2} (legacy {@code BCFluid.Source}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> FUEL_MIXED_HEAVY_HEAT_2 = FLUIDS.register("fuel_mixed_heavy_heat_2",
            () -> new BaseFlowingFluid.Source(properties(BcEnergyFluidTypes.FUEL_MIXED_HEAVY_HEAT_2, BcEnergyFluids.FUEL_MIXED_HEAVY_HEAT_2,
                    BcEnergyFluids.FUEL_MIXED_HEAVY_HEAT_2_FLOW, BcEnergyItems.FUEL_MIXED_HEAVY_HEAT_2_BUCKET, BcEnergyBlocks.FLUID_BLOCK_FUEL_MIXED_HEAVY_HEAT_2)));

    /** Placeholder flowing fluid {@code buildcraftenergy:fuel_mixed_heavy_heat_2_flow} (legacy {@code BCFluid.Flowing}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FUEL_MIXED_HEAVY_HEAT_2_FLOW = FLUIDS.register("fuel_mixed_heavy_heat_2_flow",
            () -> new BaseFlowingFluid.Flowing(properties(BcEnergyFluidTypes.FUEL_MIXED_HEAVY_HEAT_2, BcEnergyFluids.FUEL_MIXED_HEAVY_HEAT_2,
                    BcEnergyFluids.FUEL_MIXED_HEAVY_HEAT_2_FLOW, BcEnergyItems.FUEL_MIXED_HEAVY_HEAT_2_BUCKET, BcEnergyBlocks.FLUID_BLOCK_FUEL_MIXED_HEAVY_HEAT_2)));

    /** Placeholder still/source fluid {@code buildcraftenergy:fuel_mixed_light_heat_0} (legacy {@code BCFluid.Source}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> FUEL_MIXED_LIGHT_HEAT_0 = FLUIDS.register("fuel_mixed_light_heat_0",
            () -> new BaseFlowingFluid.Source(properties(BcEnergyFluidTypes.FUEL_MIXED_LIGHT_HEAT_0, BcEnergyFluids.FUEL_MIXED_LIGHT_HEAT_0,
                    BcEnergyFluids.FUEL_MIXED_LIGHT_HEAT_0_FLOW, BcEnergyItems.FUEL_MIXED_LIGHT_HEAT_0_BUCKET, BcEnergyBlocks.FLUID_BLOCK_FUEL_MIXED_LIGHT_HEAT_0)));

    /** Placeholder flowing fluid {@code buildcraftenergy:fuel_mixed_light_heat_0_flow} (legacy {@code BCFluid.Flowing}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FUEL_MIXED_LIGHT_HEAT_0_FLOW = FLUIDS.register("fuel_mixed_light_heat_0_flow",
            () -> new BaseFlowingFluid.Flowing(properties(BcEnergyFluidTypes.FUEL_MIXED_LIGHT_HEAT_0, BcEnergyFluids.FUEL_MIXED_LIGHT_HEAT_0,
                    BcEnergyFluids.FUEL_MIXED_LIGHT_HEAT_0_FLOW, BcEnergyItems.FUEL_MIXED_LIGHT_HEAT_0_BUCKET, BcEnergyBlocks.FLUID_BLOCK_FUEL_MIXED_LIGHT_HEAT_0)));

    /** Placeholder still/source fluid {@code buildcraftenergy:fuel_mixed_light_heat_1} (legacy {@code BCFluid.Source}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> FUEL_MIXED_LIGHT_HEAT_1 = FLUIDS.register("fuel_mixed_light_heat_1",
            () -> new BaseFlowingFluid.Source(properties(BcEnergyFluidTypes.FUEL_MIXED_LIGHT_HEAT_1, BcEnergyFluids.FUEL_MIXED_LIGHT_HEAT_1,
                    BcEnergyFluids.FUEL_MIXED_LIGHT_HEAT_1_FLOW, BcEnergyItems.FUEL_MIXED_LIGHT_HEAT_1_BUCKET, BcEnergyBlocks.FLUID_BLOCK_FUEL_MIXED_LIGHT_HEAT_1)));

    /** Placeholder flowing fluid {@code buildcraftenergy:fuel_mixed_light_heat_1_flow} (legacy {@code BCFluid.Flowing}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FUEL_MIXED_LIGHT_HEAT_1_FLOW = FLUIDS.register("fuel_mixed_light_heat_1_flow",
            () -> new BaseFlowingFluid.Flowing(properties(BcEnergyFluidTypes.FUEL_MIXED_LIGHT_HEAT_1, BcEnergyFluids.FUEL_MIXED_LIGHT_HEAT_1,
                    BcEnergyFluids.FUEL_MIXED_LIGHT_HEAT_1_FLOW, BcEnergyItems.FUEL_MIXED_LIGHT_HEAT_1_BUCKET, BcEnergyBlocks.FLUID_BLOCK_FUEL_MIXED_LIGHT_HEAT_1)));

    /** Placeholder still/source fluid {@code buildcraftenergy:fuel_mixed_light_heat_2} (legacy {@code BCFluid.Source}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> FUEL_MIXED_LIGHT_HEAT_2 = FLUIDS.register("fuel_mixed_light_heat_2",
            () -> new BaseFlowingFluid.Source(properties(BcEnergyFluidTypes.FUEL_MIXED_LIGHT_HEAT_2, BcEnergyFluids.FUEL_MIXED_LIGHT_HEAT_2,
                    BcEnergyFluids.FUEL_MIXED_LIGHT_HEAT_2_FLOW, BcEnergyItems.FUEL_MIXED_LIGHT_HEAT_2_BUCKET, BcEnergyBlocks.FLUID_BLOCK_FUEL_MIXED_LIGHT_HEAT_2)));

    /** Placeholder flowing fluid {@code buildcraftenergy:fuel_mixed_light_heat_2_flow} (legacy {@code BCFluid.Flowing}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FUEL_MIXED_LIGHT_HEAT_2_FLOW = FLUIDS.register("fuel_mixed_light_heat_2_flow",
            () -> new BaseFlowingFluid.Flowing(properties(BcEnergyFluidTypes.FUEL_MIXED_LIGHT_HEAT_2, BcEnergyFluids.FUEL_MIXED_LIGHT_HEAT_2,
                    BcEnergyFluids.FUEL_MIXED_LIGHT_HEAT_2_FLOW, BcEnergyItems.FUEL_MIXED_LIGHT_HEAT_2_BUCKET, BcEnergyBlocks.FLUID_BLOCK_FUEL_MIXED_LIGHT_HEAT_2)));

    /** Placeholder still/source fluid {@code buildcraftenergy:oil_dense_heat_0} (legacy {@code BCFluid.Source}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> OIL_DENSE_HEAT_0 = FLUIDS.register("oil_dense_heat_0",
            () -> new BaseFlowingFluid.Source(properties(BcEnergyFluidTypes.OIL_DENSE_HEAT_0, BcEnergyFluids.OIL_DENSE_HEAT_0,
                    BcEnergyFluids.OIL_DENSE_HEAT_0_FLOW, BcEnergyItems.OIL_DENSE_HEAT_0_BUCKET, BcEnergyBlocks.FLUID_BLOCK_OIL_DENSE_HEAT_0)));

    /** Placeholder flowing fluid {@code buildcraftenergy:oil_dense_heat_0_flow} (legacy {@code BCFluid.Flowing}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> OIL_DENSE_HEAT_0_FLOW = FLUIDS.register("oil_dense_heat_0_flow",
            () -> new BaseFlowingFluid.Flowing(properties(BcEnergyFluidTypes.OIL_DENSE_HEAT_0, BcEnergyFluids.OIL_DENSE_HEAT_0,
                    BcEnergyFluids.OIL_DENSE_HEAT_0_FLOW, BcEnergyItems.OIL_DENSE_HEAT_0_BUCKET, BcEnergyBlocks.FLUID_BLOCK_OIL_DENSE_HEAT_0)));

    /** Placeholder still/source fluid {@code buildcraftenergy:oil_dense_heat_1} (legacy {@code BCFluid.Source}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> OIL_DENSE_HEAT_1 = FLUIDS.register("oil_dense_heat_1",
            () -> new BaseFlowingFluid.Source(properties(BcEnergyFluidTypes.OIL_DENSE_HEAT_1, BcEnergyFluids.OIL_DENSE_HEAT_1,
                    BcEnergyFluids.OIL_DENSE_HEAT_1_FLOW, BcEnergyItems.OIL_DENSE_HEAT_1_BUCKET, BcEnergyBlocks.FLUID_BLOCK_OIL_DENSE_HEAT_1)));

    /** Placeholder flowing fluid {@code buildcraftenergy:oil_dense_heat_1_flow} (legacy {@code BCFluid.Flowing}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> OIL_DENSE_HEAT_1_FLOW = FLUIDS.register("oil_dense_heat_1_flow",
            () -> new BaseFlowingFluid.Flowing(properties(BcEnergyFluidTypes.OIL_DENSE_HEAT_1, BcEnergyFluids.OIL_DENSE_HEAT_1,
                    BcEnergyFluids.OIL_DENSE_HEAT_1_FLOW, BcEnergyItems.OIL_DENSE_HEAT_1_BUCKET, BcEnergyBlocks.FLUID_BLOCK_OIL_DENSE_HEAT_1)));

    /** Placeholder still/source fluid {@code buildcraftenergy:oil_dense_heat_2} (legacy {@code BCFluid.Source}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> OIL_DENSE_HEAT_2 = FLUIDS.register("oil_dense_heat_2",
            () -> new BaseFlowingFluid.Source(properties(BcEnergyFluidTypes.OIL_DENSE_HEAT_2, BcEnergyFluids.OIL_DENSE_HEAT_2,
                    BcEnergyFluids.OIL_DENSE_HEAT_2_FLOW, BcEnergyItems.OIL_DENSE_HEAT_2_BUCKET, BcEnergyBlocks.FLUID_BLOCK_OIL_DENSE_HEAT_2)));

    /** Placeholder flowing fluid {@code buildcraftenergy:oil_dense_heat_2_flow} (legacy {@code BCFluid.Flowing}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> OIL_DENSE_HEAT_2_FLOW = FLUIDS.register("oil_dense_heat_2_flow",
            () -> new BaseFlowingFluid.Flowing(properties(BcEnergyFluidTypes.OIL_DENSE_HEAT_2, BcEnergyFluids.OIL_DENSE_HEAT_2,
                    BcEnergyFluids.OIL_DENSE_HEAT_2_FLOW, BcEnergyItems.OIL_DENSE_HEAT_2_BUCKET, BcEnergyBlocks.FLUID_BLOCK_OIL_DENSE_HEAT_2)));

    /** Placeholder still/source fluid {@code buildcraftenergy:oil_distilled_heat_0} (legacy {@code BCFluid.Source}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> OIL_DISTILLED_HEAT_0 = FLUIDS.register("oil_distilled_heat_0",
            () -> new BaseFlowingFluid.Source(properties(BcEnergyFluidTypes.OIL_DISTILLED_HEAT_0, BcEnergyFluids.OIL_DISTILLED_HEAT_0,
                    BcEnergyFluids.OIL_DISTILLED_HEAT_0_FLOW, BcEnergyItems.OIL_DISTILLED_HEAT_0_BUCKET, BcEnergyBlocks.FLUID_BLOCK_OIL_DISTILLED_HEAT_0)));

    /** Placeholder flowing fluid {@code buildcraftenergy:oil_distilled_heat_0_flow} (legacy {@code BCFluid.Flowing}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> OIL_DISTILLED_HEAT_0_FLOW = FLUIDS.register("oil_distilled_heat_0_flow",
            () -> new BaseFlowingFluid.Flowing(properties(BcEnergyFluidTypes.OIL_DISTILLED_HEAT_0, BcEnergyFluids.OIL_DISTILLED_HEAT_0,
                    BcEnergyFluids.OIL_DISTILLED_HEAT_0_FLOW, BcEnergyItems.OIL_DISTILLED_HEAT_0_BUCKET, BcEnergyBlocks.FLUID_BLOCK_OIL_DISTILLED_HEAT_0)));

    /** Placeholder still/source fluid {@code buildcraftenergy:oil_distilled_heat_1} (legacy {@code BCFluid.Source}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> OIL_DISTILLED_HEAT_1 = FLUIDS.register("oil_distilled_heat_1",
            () -> new BaseFlowingFluid.Source(properties(BcEnergyFluidTypes.OIL_DISTILLED_HEAT_1, BcEnergyFluids.OIL_DISTILLED_HEAT_1,
                    BcEnergyFluids.OIL_DISTILLED_HEAT_1_FLOW, BcEnergyItems.OIL_DISTILLED_HEAT_1_BUCKET, BcEnergyBlocks.FLUID_BLOCK_OIL_DISTILLED_HEAT_1)));

    /** Placeholder flowing fluid {@code buildcraftenergy:oil_distilled_heat_1_flow} (legacy {@code BCFluid.Flowing}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> OIL_DISTILLED_HEAT_1_FLOW = FLUIDS.register("oil_distilled_heat_1_flow",
            () -> new BaseFlowingFluid.Flowing(properties(BcEnergyFluidTypes.OIL_DISTILLED_HEAT_1, BcEnergyFluids.OIL_DISTILLED_HEAT_1,
                    BcEnergyFluids.OIL_DISTILLED_HEAT_1_FLOW, BcEnergyItems.OIL_DISTILLED_HEAT_1_BUCKET, BcEnergyBlocks.FLUID_BLOCK_OIL_DISTILLED_HEAT_1)));

    /** Placeholder still/source fluid {@code buildcraftenergy:oil_distilled_heat_2} (legacy {@code BCFluid.Source}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> OIL_DISTILLED_HEAT_2 = FLUIDS.register("oil_distilled_heat_2",
            () -> new BaseFlowingFluid.Source(properties(BcEnergyFluidTypes.OIL_DISTILLED_HEAT_2, BcEnergyFluids.OIL_DISTILLED_HEAT_2,
                    BcEnergyFluids.OIL_DISTILLED_HEAT_2_FLOW, BcEnergyItems.OIL_DISTILLED_HEAT_2_BUCKET, BcEnergyBlocks.FLUID_BLOCK_OIL_DISTILLED_HEAT_2)));

    /** Placeholder flowing fluid {@code buildcraftenergy:oil_distilled_heat_2_flow} (legacy {@code BCFluid.Flowing}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> OIL_DISTILLED_HEAT_2_FLOW = FLUIDS.register("oil_distilled_heat_2_flow",
            () -> new BaseFlowingFluid.Flowing(properties(BcEnergyFluidTypes.OIL_DISTILLED_HEAT_2, BcEnergyFluids.OIL_DISTILLED_HEAT_2,
                    BcEnergyFluids.OIL_DISTILLED_HEAT_2_FLOW, BcEnergyItems.OIL_DISTILLED_HEAT_2_BUCKET, BcEnergyBlocks.FLUID_BLOCK_OIL_DISTILLED_HEAT_2)));

    /** Placeholder still/source fluid {@code buildcraftenergy:oil_heat_0} (legacy {@code BCFluid.Source}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> OIL_HEAT_0 = FLUIDS.register("oil_heat_0",
            () -> new BaseFlowingFluid.Source(properties(BcEnergyFluidTypes.OIL_HEAT_0, BcEnergyFluids.OIL_HEAT_0,
                    BcEnergyFluids.OIL_HEAT_0_FLOW, BcEnergyItems.OIL_HEAT_0_BUCKET, BcEnergyBlocks.FLUID_BLOCK_OIL_HEAT_0)));

    /** Placeholder flowing fluid {@code buildcraftenergy:oil_heat_0_flow} (legacy {@code BCFluid.Flowing}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> OIL_HEAT_0_FLOW = FLUIDS.register("oil_heat_0_flow",
            () -> new BaseFlowingFluid.Flowing(properties(BcEnergyFluidTypes.OIL_HEAT_0, BcEnergyFluids.OIL_HEAT_0,
                    BcEnergyFluids.OIL_HEAT_0_FLOW, BcEnergyItems.OIL_HEAT_0_BUCKET, BcEnergyBlocks.FLUID_BLOCK_OIL_HEAT_0)));

    /** Placeholder still/source fluid {@code buildcraftenergy:oil_heat_1} (legacy {@code BCFluid.Source}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> OIL_HEAT_1 = FLUIDS.register("oil_heat_1",
            () -> new BaseFlowingFluid.Source(properties(BcEnergyFluidTypes.OIL_HEAT_1, BcEnergyFluids.OIL_HEAT_1,
                    BcEnergyFluids.OIL_HEAT_1_FLOW, BcEnergyItems.OIL_HEAT_1_BUCKET, BcEnergyBlocks.FLUID_BLOCK_OIL_HEAT_1)));

    /** Placeholder flowing fluid {@code buildcraftenergy:oil_heat_1_flow} (legacy {@code BCFluid.Flowing}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> OIL_HEAT_1_FLOW = FLUIDS.register("oil_heat_1_flow",
            () -> new BaseFlowingFluid.Flowing(properties(BcEnergyFluidTypes.OIL_HEAT_1, BcEnergyFluids.OIL_HEAT_1,
                    BcEnergyFluids.OIL_HEAT_1_FLOW, BcEnergyItems.OIL_HEAT_1_BUCKET, BcEnergyBlocks.FLUID_BLOCK_OIL_HEAT_1)));

    /** Placeholder still/source fluid {@code buildcraftenergy:oil_heat_2} (legacy {@code BCFluid.Source}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> OIL_HEAT_2 = FLUIDS.register("oil_heat_2",
            () -> new BaseFlowingFluid.Source(properties(BcEnergyFluidTypes.OIL_HEAT_2, BcEnergyFluids.OIL_HEAT_2,
                    BcEnergyFluids.OIL_HEAT_2_FLOW, BcEnergyItems.OIL_HEAT_2_BUCKET, BcEnergyBlocks.FLUID_BLOCK_OIL_HEAT_2)));

    /** Placeholder flowing fluid {@code buildcraftenergy:oil_heat_2_flow} (legacy {@code BCFluid.Flowing}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> OIL_HEAT_2_FLOW = FLUIDS.register("oil_heat_2_flow",
            () -> new BaseFlowingFluid.Flowing(properties(BcEnergyFluidTypes.OIL_HEAT_2, BcEnergyFluids.OIL_HEAT_2,
                    BcEnergyFluids.OIL_HEAT_2_FLOW, BcEnergyItems.OIL_HEAT_2_BUCKET, BcEnergyBlocks.FLUID_BLOCK_OIL_HEAT_2)));

    /** Placeholder still/source fluid {@code buildcraftenergy:oil_heavy_heat_0} (legacy {@code BCFluid.Source}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> OIL_HEAVY_HEAT_0 = FLUIDS.register("oil_heavy_heat_0",
            () -> new BaseFlowingFluid.Source(properties(BcEnergyFluidTypes.OIL_HEAVY_HEAT_0, BcEnergyFluids.OIL_HEAVY_HEAT_0,
                    BcEnergyFluids.OIL_HEAVY_HEAT_0_FLOW, BcEnergyItems.OIL_HEAVY_HEAT_0_BUCKET, BcEnergyBlocks.FLUID_BLOCK_OIL_HEAVY_HEAT_0)));

    /** Placeholder flowing fluid {@code buildcraftenergy:oil_heavy_heat_0_flow} (legacy {@code BCFluid.Flowing}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> OIL_HEAVY_HEAT_0_FLOW = FLUIDS.register("oil_heavy_heat_0_flow",
            () -> new BaseFlowingFluid.Flowing(properties(BcEnergyFluidTypes.OIL_HEAVY_HEAT_0, BcEnergyFluids.OIL_HEAVY_HEAT_0,
                    BcEnergyFluids.OIL_HEAVY_HEAT_0_FLOW, BcEnergyItems.OIL_HEAVY_HEAT_0_BUCKET, BcEnergyBlocks.FLUID_BLOCK_OIL_HEAVY_HEAT_0)));

    /** Placeholder still/source fluid {@code buildcraftenergy:oil_heavy_heat_1} (legacy {@code BCFluid.Source}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> OIL_HEAVY_HEAT_1 = FLUIDS.register("oil_heavy_heat_1",
            () -> new BaseFlowingFluid.Source(properties(BcEnergyFluidTypes.OIL_HEAVY_HEAT_1, BcEnergyFluids.OIL_HEAVY_HEAT_1,
                    BcEnergyFluids.OIL_HEAVY_HEAT_1_FLOW, BcEnergyItems.OIL_HEAVY_HEAT_1_BUCKET, BcEnergyBlocks.FLUID_BLOCK_OIL_HEAVY_HEAT_1)));

    /** Placeholder flowing fluid {@code buildcraftenergy:oil_heavy_heat_1_flow} (legacy {@code BCFluid.Flowing}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> OIL_HEAVY_HEAT_1_FLOW = FLUIDS.register("oil_heavy_heat_1_flow",
            () -> new BaseFlowingFluid.Flowing(properties(BcEnergyFluidTypes.OIL_HEAVY_HEAT_1, BcEnergyFluids.OIL_HEAVY_HEAT_1,
                    BcEnergyFluids.OIL_HEAVY_HEAT_1_FLOW, BcEnergyItems.OIL_HEAVY_HEAT_1_BUCKET, BcEnergyBlocks.FLUID_BLOCK_OIL_HEAVY_HEAT_1)));

    /** Placeholder still/source fluid {@code buildcraftenergy:oil_heavy_heat_2} (legacy {@code BCFluid.Source}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> OIL_HEAVY_HEAT_2 = FLUIDS.register("oil_heavy_heat_2",
            () -> new BaseFlowingFluid.Source(properties(BcEnergyFluidTypes.OIL_HEAVY_HEAT_2, BcEnergyFluids.OIL_HEAVY_HEAT_2,
                    BcEnergyFluids.OIL_HEAVY_HEAT_2_FLOW, BcEnergyItems.OIL_HEAVY_HEAT_2_BUCKET, BcEnergyBlocks.FLUID_BLOCK_OIL_HEAVY_HEAT_2)));

    /** Placeholder flowing fluid {@code buildcraftenergy:oil_heavy_heat_2_flow} (legacy {@code BCFluid.Flowing}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> OIL_HEAVY_HEAT_2_FLOW = FLUIDS.register("oil_heavy_heat_2_flow",
            () -> new BaseFlowingFluid.Flowing(properties(BcEnergyFluidTypes.OIL_HEAVY_HEAT_2, BcEnergyFluids.OIL_HEAVY_HEAT_2,
                    BcEnergyFluids.OIL_HEAVY_HEAT_2_FLOW, BcEnergyItems.OIL_HEAVY_HEAT_2_BUCKET, BcEnergyBlocks.FLUID_BLOCK_OIL_HEAVY_HEAT_2)));

    /** Placeholder still/source fluid {@code buildcraftenergy:oil_residue_heat_0} (legacy {@code BCFluid.Source}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> OIL_RESIDUE_HEAT_0 = FLUIDS.register("oil_residue_heat_0",
            () -> new BaseFlowingFluid.Source(properties(BcEnergyFluidTypes.OIL_RESIDUE_HEAT_0, BcEnergyFluids.OIL_RESIDUE_HEAT_0,
                    BcEnergyFluids.OIL_RESIDUE_HEAT_0_FLOW, BcEnergyItems.OIL_RESIDUE_HEAT_0_BUCKET, BcEnergyBlocks.FLUID_BLOCK_OIL_RESIDUE_HEAT_0)));

    /** Placeholder flowing fluid {@code buildcraftenergy:oil_residue_heat_0_flow} (legacy {@code BCFluid.Flowing}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> OIL_RESIDUE_HEAT_0_FLOW = FLUIDS.register("oil_residue_heat_0_flow",
            () -> new BaseFlowingFluid.Flowing(properties(BcEnergyFluidTypes.OIL_RESIDUE_HEAT_0, BcEnergyFluids.OIL_RESIDUE_HEAT_0,
                    BcEnergyFluids.OIL_RESIDUE_HEAT_0_FLOW, BcEnergyItems.OIL_RESIDUE_HEAT_0_BUCKET, BcEnergyBlocks.FLUID_BLOCK_OIL_RESIDUE_HEAT_0)));

    /** Placeholder still/source fluid {@code buildcraftenergy:oil_residue_heat_1} (legacy {@code BCFluid.Source}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> OIL_RESIDUE_HEAT_1 = FLUIDS.register("oil_residue_heat_1",
            () -> new BaseFlowingFluid.Source(properties(BcEnergyFluidTypes.OIL_RESIDUE_HEAT_1, BcEnergyFluids.OIL_RESIDUE_HEAT_1,
                    BcEnergyFluids.OIL_RESIDUE_HEAT_1_FLOW, BcEnergyItems.OIL_RESIDUE_HEAT_1_BUCKET, BcEnergyBlocks.FLUID_BLOCK_OIL_RESIDUE_HEAT_1)));

    /** Placeholder flowing fluid {@code buildcraftenergy:oil_residue_heat_1_flow} (legacy {@code BCFluid.Flowing}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> OIL_RESIDUE_HEAT_1_FLOW = FLUIDS.register("oil_residue_heat_1_flow",
            () -> new BaseFlowingFluid.Flowing(properties(BcEnergyFluidTypes.OIL_RESIDUE_HEAT_1, BcEnergyFluids.OIL_RESIDUE_HEAT_1,
                    BcEnergyFluids.OIL_RESIDUE_HEAT_1_FLOW, BcEnergyItems.OIL_RESIDUE_HEAT_1_BUCKET, BcEnergyBlocks.FLUID_BLOCK_OIL_RESIDUE_HEAT_1)));

    /** Placeholder still/source fluid {@code buildcraftenergy:oil_residue_heat_2} (legacy {@code BCFluid.Source}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> OIL_RESIDUE_HEAT_2 = FLUIDS.register("oil_residue_heat_2",
            () -> new BaseFlowingFluid.Source(properties(BcEnergyFluidTypes.OIL_RESIDUE_HEAT_2, BcEnergyFluids.OIL_RESIDUE_HEAT_2,
                    BcEnergyFluids.OIL_RESIDUE_HEAT_2_FLOW, BcEnergyItems.OIL_RESIDUE_HEAT_2_BUCKET, BcEnergyBlocks.FLUID_BLOCK_OIL_RESIDUE_HEAT_2)));

    /** Placeholder flowing fluid {@code buildcraftenergy:oil_residue_heat_2_flow} (legacy {@code BCFluid.Flowing}); behaviour migrates in M2.5+. */
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> OIL_RESIDUE_HEAT_2_FLOW = FLUIDS.register("oil_residue_heat_2_flow",
            () -> new BaseFlowingFluid.Flowing(properties(BcEnergyFluidTypes.OIL_RESIDUE_HEAT_2, BcEnergyFluids.OIL_RESIDUE_HEAT_2,
                    BcEnergyFluids.OIL_RESIDUE_HEAT_2_FLOW, BcEnergyItems.OIL_RESIDUE_HEAT_2_BUCKET, BcEnergyBlocks.FLUID_BLOCK_OIL_RESIDUE_HEAT_2)));


    /**
     * All still/flowing pairs in registration order. Used by the client fluid model hook
     * ({@code BcEnergyFluidModels}) so it can register one {@code FluidModel} per pair without hard-coding ids again.
     */
    public static final List<Pair> PAIRS = List.of(
            new Pair(FUEL_DENSE_HEAT_0, FUEL_DENSE_HEAT_0_FLOW),
            new Pair(FUEL_DENSE_HEAT_1, FUEL_DENSE_HEAT_1_FLOW),
            new Pair(FUEL_DENSE_HEAT_2, FUEL_DENSE_HEAT_2_FLOW),
            new Pair(FUEL_GASEOUS_HEAT_0, FUEL_GASEOUS_HEAT_0_FLOW),
            new Pair(FUEL_GASEOUS_HEAT_1, FUEL_GASEOUS_HEAT_1_FLOW),
            new Pair(FUEL_GASEOUS_HEAT_2, FUEL_GASEOUS_HEAT_2_FLOW),
            new Pair(FUEL_LIGHT_HEAT_0, FUEL_LIGHT_HEAT_0_FLOW),
            new Pair(FUEL_LIGHT_HEAT_1, FUEL_LIGHT_HEAT_1_FLOW),
            new Pair(FUEL_LIGHT_HEAT_2, FUEL_LIGHT_HEAT_2_FLOW),
            new Pair(FUEL_MIXED_HEAVY_HEAT_0, FUEL_MIXED_HEAVY_HEAT_0_FLOW),
            new Pair(FUEL_MIXED_HEAVY_HEAT_1, FUEL_MIXED_HEAVY_HEAT_1_FLOW),
            new Pair(FUEL_MIXED_HEAVY_HEAT_2, FUEL_MIXED_HEAVY_HEAT_2_FLOW),
            new Pair(FUEL_MIXED_LIGHT_HEAT_0, FUEL_MIXED_LIGHT_HEAT_0_FLOW),
            new Pair(FUEL_MIXED_LIGHT_HEAT_1, FUEL_MIXED_LIGHT_HEAT_1_FLOW),
            new Pair(FUEL_MIXED_LIGHT_HEAT_2, FUEL_MIXED_LIGHT_HEAT_2_FLOW),
            new Pair(OIL_DENSE_HEAT_0, OIL_DENSE_HEAT_0_FLOW),
            new Pair(OIL_DENSE_HEAT_1, OIL_DENSE_HEAT_1_FLOW),
            new Pair(OIL_DENSE_HEAT_2, OIL_DENSE_HEAT_2_FLOW),
            new Pair(OIL_DISTILLED_HEAT_0, OIL_DISTILLED_HEAT_0_FLOW),
            new Pair(OIL_DISTILLED_HEAT_1, OIL_DISTILLED_HEAT_1_FLOW),
            new Pair(OIL_DISTILLED_HEAT_2, OIL_DISTILLED_HEAT_2_FLOW),
            new Pair(OIL_HEAT_0, OIL_HEAT_0_FLOW),
            new Pair(OIL_HEAT_1, OIL_HEAT_1_FLOW),
            new Pair(OIL_HEAT_2, OIL_HEAT_2_FLOW),
            new Pair(OIL_HEAVY_HEAT_0, OIL_HEAVY_HEAT_0_FLOW),
            new Pair(OIL_HEAVY_HEAT_1, OIL_HEAVY_HEAT_1_FLOW),
            new Pair(OIL_HEAVY_HEAT_2, OIL_HEAVY_HEAT_2_FLOW),
            new Pair(OIL_RESIDUE_HEAT_0, OIL_RESIDUE_HEAT_0_FLOW),
            new Pair(OIL_RESIDUE_HEAT_1, OIL_RESIDUE_HEAT_1_FLOW),
            new Pair(OIL_RESIDUE_HEAT_2, OIL_RESIDUE_HEAT_2_FLOW)
            );

    /**
     * One still/flowing fluid pair.
     */
    public record Pair(DeferredHolder<Fluid, BaseFlowingFluid.Source> still,
            DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing) {
    }

    /**
     * Builds the placeholder {@link BaseFlowingFluid.Properties} shared by one still/flowing pair: fluid type, still,
     * flowing, bucket and world block. All arguments are holders, i.e. lazy suppliers, because the fluid type/item/
     * block registries populate in a different order than this registry (see the class javadoc).
     */
    private static BaseFlowingFluid.Properties properties(DeferredHolder<FluidType, FluidType> type,
            Supplier<? extends Fluid> still, Supplier<? extends Fluid> flowing, Supplier<? extends Item> bucket,
            Supplier<? extends PlaceholderFluidBlock> block) {
        return new BaseFlowingFluid.Properties(type, still, flowing).bucket(bucket).block(block);
    }

    private BcEnergyFluids() {
    }
}
