/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.energy.client;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import buildcraft.energy.BcEnergyFluids;

/**
 * Client fluid models for the buildcraftenergy placeholder fluids (task M2.4b). NeoForge 26.1.2 warns once per fluid
 * without a {@link FluidModel}, so this hook registers one per still/flowing pair from {@link BcEnergyFluids#PAIRS},
 * reusing the vanilla water still/flow textures as the placeholder textures (no own texture files yet). The real
 * per-fluid rendering (legacy {@code BCFluidAttributes} textures/tints) migrates in M2.5+.
 *
 * <p>Client-only classes ({@link FluidModel}, {@link Material}) are referenced here, so the listener must only be
 * registered on the client dist (see the guard in {@code BuildCraftEnergy}).
 */
public final class BcEnergyFluidModels {

    private static final Material STILL_TEXTURE = new Material(Identifier.withDefaultNamespace("block/water_still"));
    private static final Material FLOW_TEXTURE = new Material(Identifier.withDefaultNamespace("block/water_flow"));

    public static void onRegisterFluidModels(RegisterFluidModelsEvent event) {
        for (BcEnergyFluids.Pair pair : BcEnergyFluids.PAIRS) {
            event.register(new FluidModel.Unbaked(STILL_TEXTURE, FLOW_TEXTURE, null, null),
                    pair.still().value(), pair.flowing().value());
        }
    }

    private BcEnergyFluidModels() {
    }
}
