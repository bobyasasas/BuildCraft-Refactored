/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.energy;

import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Central fluid type registration for buildcraftenergy (task M2.4b registry parity). NeoForge 26.1.2 requires every
 * {@link net.neoforged.neoforge.fluids.BaseFlowingFluid} to carry a {@link FluidType}; one placeholder type per still
 * fluid id is registered here (the still id is the conventional fluid type name). All of them use the bare
 * {@link FluidType.Properties#create() defaults} until the real oil/fuel physical properties (legacy
 * {@code BCFluidAttributes}: density/viscosity/temperature/heat) migrate in M2.5+.
 *
 * <p>The {@code neoforge:fluid_types} registry fires after every vanilla registry, so the {@link BcEnergyFluids}
 * fluids must only ever reference these holders lazily (they do: {@code BaseFlowingFluid} stores the supplier and
 * dereferences it on first use).
 */
public final class BcEnergyFluidTypes {

    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES,
            BuildCraftEnergy.MOD_ID);
    /** Placeholder for the {@code buildcraftenergy:fuel_dense_heat_0} fluid type (legacy {@code BCFluidAttributes}); real properties migrate in M2.5+. */
    public static final DeferredHolder<FluidType, FluidType> FUEL_DENSE_HEAT_0 = FLUID_TYPES.register("fuel_dense_heat_0",
            () -> new FluidType(FluidType.Properties.create()));

    /** Placeholder for the {@code buildcraftenergy:fuel_dense_heat_1} fluid type (legacy {@code BCFluidAttributes}); real properties migrate in M2.5+. */
    public static final DeferredHolder<FluidType, FluidType> FUEL_DENSE_HEAT_1 = FLUID_TYPES.register("fuel_dense_heat_1",
            () -> new FluidType(FluidType.Properties.create()));

    /** Placeholder for the {@code buildcraftenergy:fuel_dense_heat_2} fluid type (legacy {@code BCFluidAttributes}); real properties migrate in M2.5+. */
    public static final DeferredHolder<FluidType, FluidType> FUEL_DENSE_HEAT_2 = FLUID_TYPES.register("fuel_dense_heat_2",
            () -> new FluidType(FluidType.Properties.create()));

    /** Placeholder for the {@code buildcraftenergy:fuel_gaseous_heat_0} fluid type (legacy {@code BCFluidAttributes}); real properties migrate in M2.5+. */
    public static final DeferredHolder<FluidType, FluidType> FUEL_GASEOUS_HEAT_0 = FLUID_TYPES.register("fuel_gaseous_heat_0",
            () -> new FluidType(FluidType.Properties.create()));

    /** Placeholder for the {@code buildcraftenergy:fuel_gaseous_heat_1} fluid type (legacy {@code BCFluidAttributes}); real properties migrate in M2.5+. */
    public static final DeferredHolder<FluidType, FluidType> FUEL_GASEOUS_HEAT_1 = FLUID_TYPES.register("fuel_gaseous_heat_1",
            () -> new FluidType(FluidType.Properties.create()));

    /** Placeholder for the {@code buildcraftenergy:fuel_gaseous_heat_2} fluid type (legacy {@code BCFluidAttributes}); real properties migrate in M2.5+. */
    public static final DeferredHolder<FluidType, FluidType> FUEL_GASEOUS_HEAT_2 = FLUID_TYPES.register("fuel_gaseous_heat_2",
            () -> new FluidType(FluidType.Properties.create()));

    /** Placeholder for the {@code buildcraftenergy:fuel_light_heat_0} fluid type (legacy {@code BCFluidAttributes}); real properties migrate in M2.5+. */
    public static final DeferredHolder<FluidType, FluidType> FUEL_LIGHT_HEAT_0 = FLUID_TYPES.register("fuel_light_heat_0",
            () -> new FluidType(FluidType.Properties.create()));

    /** Placeholder for the {@code buildcraftenergy:fuel_light_heat_1} fluid type (legacy {@code BCFluidAttributes}); real properties migrate in M2.5+. */
    public static final DeferredHolder<FluidType, FluidType> FUEL_LIGHT_HEAT_1 = FLUID_TYPES.register("fuel_light_heat_1",
            () -> new FluidType(FluidType.Properties.create()));

    /** Placeholder for the {@code buildcraftenergy:fuel_light_heat_2} fluid type (legacy {@code BCFluidAttributes}); real properties migrate in M2.5+. */
    public static final DeferredHolder<FluidType, FluidType> FUEL_LIGHT_HEAT_2 = FLUID_TYPES.register("fuel_light_heat_2",
            () -> new FluidType(FluidType.Properties.create()));

    /** Placeholder for the {@code buildcraftenergy:fuel_mixed_heavy_heat_0} fluid type (legacy {@code BCFluidAttributes}); real properties migrate in M2.5+. */
    public static final DeferredHolder<FluidType, FluidType> FUEL_MIXED_HEAVY_HEAT_0 = FLUID_TYPES.register("fuel_mixed_heavy_heat_0",
            () -> new FluidType(FluidType.Properties.create()));

    /** Placeholder for the {@code buildcraftenergy:fuel_mixed_heavy_heat_1} fluid type (legacy {@code BCFluidAttributes}); real properties migrate in M2.5+. */
    public static final DeferredHolder<FluidType, FluidType> FUEL_MIXED_HEAVY_HEAT_1 = FLUID_TYPES.register("fuel_mixed_heavy_heat_1",
            () -> new FluidType(FluidType.Properties.create()));

    /** Placeholder for the {@code buildcraftenergy:fuel_mixed_heavy_heat_2} fluid type (legacy {@code BCFluidAttributes}); real properties migrate in M2.5+. */
    public static final DeferredHolder<FluidType, FluidType> FUEL_MIXED_HEAVY_HEAT_2 = FLUID_TYPES.register("fuel_mixed_heavy_heat_2",
            () -> new FluidType(FluidType.Properties.create()));

    /** Placeholder for the {@code buildcraftenergy:fuel_mixed_light_heat_0} fluid type (legacy {@code BCFluidAttributes}); real properties migrate in M2.5+. */
    public static final DeferredHolder<FluidType, FluidType> FUEL_MIXED_LIGHT_HEAT_0 = FLUID_TYPES.register("fuel_mixed_light_heat_0",
            () -> new FluidType(FluidType.Properties.create()));

    /** Placeholder for the {@code buildcraftenergy:fuel_mixed_light_heat_1} fluid type (legacy {@code BCFluidAttributes}); real properties migrate in M2.5+. */
    public static final DeferredHolder<FluidType, FluidType> FUEL_MIXED_LIGHT_HEAT_1 = FLUID_TYPES.register("fuel_mixed_light_heat_1",
            () -> new FluidType(FluidType.Properties.create()));

    /** Placeholder for the {@code buildcraftenergy:fuel_mixed_light_heat_2} fluid type (legacy {@code BCFluidAttributes}); real properties migrate in M2.5+. */
    public static final DeferredHolder<FluidType, FluidType> FUEL_MIXED_LIGHT_HEAT_2 = FLUID_TYPES.register("fuel_mixed_light_heat_2",
            () -> new FluidType(FluidType.Properties.create()));

    /** Placeholder for the {@code buildcraftenergy:oil_dense_heat_0} fluid type (legacy {@code BCFluidAttributes}); real properties migrate in M2.5+. */
    public static final DeferredHolder<FluidType, FluidType> OIL_DENSE_HEAT_0 = FLUID_TYPES.register("oil_dense_heat_0",
            () -> new FluidType(FluidType.Properties.create()));

    /** Placeholder for the {@code buildcraftenergy:oil_dense_heat_1} fluid type (legacy {@code BCFluidAttributes}); real properties migrate in M2.5+. */
    public static final DeferredHolder<FluidType, FluidType> OIL_DENSE_HEAT_1 = FLUID_TYPES.register("oil_dense_heat_1",
            () -> new FluidType(FluidType.Properties.create()));

    /** Placeholder for the {@code buildcraftenergy:oil_dense_heat_2} fluid type (legacy {@code BCFluidAttributes}); real properties migrate in M2.5+. */
    public static final DeferredHolder<FluidType, FluidType> OIL_DENSE_HEAT_2 = FLUID_TYPES.register("oil_dense_heat_2",
            () -> new FluidType(FluidType.Properties.create()));

    /** Placeholder for the {@code buildcraftenergy:oil_distilled_heat_0} fluid type (legacy {@code BCFluidAttributes}); real properties migrate in M2.5+. */
    public static final DeferredHolder<FluidType, FluidType> OIL_DISTILLED_HEAT_0 = FLUID_TYPES.register("oil_distilled_heat_0",
            () -> new FluidType(FluidType.Properties.create()));

    /** Placeholder for the {@code buildcraftenergy:oil_distilled_heat_1} fluid type (legacy {@code BCFluidAttributes}); real properties migrate in M2.5+. */
    public static final DeferredHolder<FluidType, FluidType> OIL_DISTILLED_HEAT_1 = FLUID_TYPES.register("oil_distilled_heat_1",
            () -> new FluidType(FluidType.Properties.create()));

    /** Placeholder for the {@code buildcraftenergy:oil_distilled_heat_2} fluid type (legacy {@code BCFluidAttributes}); real properties migrate in M2.5+. */
    public static final DeferredHolder<FluidType, FluidType> OIL_DISTILLED_HEAT_2 = FLUID_TYPES.register("oil_distilled_heat_2",
            () -> new FluidType(FluidType.Properties.create()));

    /** Placeholder for the {@code buildcraftenergy:oil_heat_0} fluid type (legacy {@code BCFluidAttributes}); real properties migrate in M2.5+. */
    public static final DeferredHolder<FluidType, FluidType> OIL_HEAT_0 = FLUID_TYPES.register("oil_heat_0",
            () -> new FluidType(FluidType.Properties.create()));

    /** Placeholder for the {@code buildcraftenergy:oil_heat_1} fluid type (legacy {@code BCFluidAttributes}); real properties migrate in M2.5+. */
    public static final DeferredHolder<FluidType, FluidType> OIL_HEAT_1 = FLUID_TYPES.register("oil_heat_1",
            () -> new FluidType(FluidType.Properties.create()));

    /** Placeholder for the {@code buildcraftenergy:oil_heat_2} fluid type (legacy {@code BCFluidAttributes}); real properties migrate in M2.5+. */
    public static final DeferredHolder<FluidType, FluidType> OIL_HEAT_2 = FLUID_TYPES.register("oil_heat_2",
            () -> new FluidType(FluidType.Properties.create()));

    /** Placeholder for the {@code buildcraftenergy:oil_heavy_heat_0} fluid type (legacy {@code BCFluidAttributes}); real properties migrate in M2.5+. */
    public static final DeferredHolder<FluidType, FluidType> OIL_HEAVY_HEAT_0 = FLUID_TYPES.register("oil_heavy_heat_0",
            () -> new FluidType(FluidType.Properties.create()));

    /** Placeholder for the {@code buildcraftenergy:oil_heavy_heat_1} fluid type (legacy {@code BCFluidAttributes}); real properties migrate in M2.5+. */
    public static final DeferredHolder<FluidType, FluidType> OIL_HEAVY_HEAT_1 = FLUID_TYPES.register("oil_heavy_heat_1",
            () -> new FluidType(FluidType.Properties.create()));

    /** Placeholder for the {@code buildcraftenergy:oil_heavy_heat_2} fluid type (legacy {@code BCFluidAttributes}); real properties migrate in M2.5+. */
    public static final DeferredHolder<FluidType, FluidType> OIL_HEAVY_HEAT_2 = FLUID_TYPES.register("oil_heavy_heat_2",
            () -> new FluidType(FluidType.Properties.create()));

    /** Placeholder for the {@code buildcraftenergy:oil_residue_heat_0} fluid type (legacy {@code BCFluidAttributes}); real properties migrate in M2.5+. */
    public static final DeferredHolder<FluidType, FluidType> OIL_RESIDUE_HEAT_0 = FLUID_TYPES.register("oil_residue_heat_0",
            () -> new FluidType(FluidType.Properties.create()));

    /** Placeholder for the {@code buildcraftenergy:oil_residue_heat_1} fluid type (legacy {@code BCFluidAttributes}); real properties migrate in M2.5+. */
    public static final DeferredHolder<FluidType, FluidType> OIL_RESIDUE_HEAT_1 = FLUID_TYPES.register("oil_residue_heat_1",
            () -> new FluidType(FluidType.Properties.create()));

    /** Placeholder for the {@code buildcraftenergy:oil_residue_heat_2} fluid type (legacy {@code BCFluidAttributes}); real properties migrate in M2.5+. */
    public static final DeferredHolder<FluidType, FluidType> OIL_RESIDUE_HEAT_2 = FLUID_TYPES.register("oil_residue_heat_2",
            () -> new FluidType(FluidType.Properties.create()));


    private BcEnergyFluidTypes() {
    }
}
