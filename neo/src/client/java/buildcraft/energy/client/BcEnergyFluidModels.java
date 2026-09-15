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
 * <p><b>Source set boundary (M2.7a):</b> this class lives in the {@code client} source set
 * ({@code neo/src/client/java}), so the compiler itself guarantees it is never needed on the dedicated server — the
 * class is not even on the server runs' classpath. No hand-written {@code Dist} guard is necessary: the client entry
 * point {@link BuildCraftEnergyClient} (annotated {@code @Mod(dist = Dist.CLIENT)}) registers the
 * {@link RegisterFluidModelsEvent} listener on the mod event bus, and FML filters that entry point out during its
 * bytecode scan on server dists.
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
