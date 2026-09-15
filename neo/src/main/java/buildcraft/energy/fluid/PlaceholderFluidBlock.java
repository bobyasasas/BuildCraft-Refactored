/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.energy.fluid;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * Shared placeholder fluid block for the M2.4b buildcraftenergy registry parity: exists only so every baseline
 * {@code fluid_block_*} id (see {@code migration/snapshots/registry-baseline.json}) is a registered {@link LiquidBlock}
 * wired to its {@code BaseFlowingFluid} still/flowing pair. NeoForge 26.1.2 keeps the vanilla
 * {@link LiquidBlock#LiquidBlock(FlowingFluid, BlockBehaviour.Properties) LiquidBlock} constructor protected, so mods
 * subclass it exactly like the legacy {@code BCFluidBlock} did; the subclass adds nothing. The real oil/fuel behaviour
 * (legacy {@code BCFluidBlock}) migrates in M2.5+.
 */
public class PlaceholderFluidBlock extends LiquidBlock {

    public PlaceholderFluidBlock(FlowingFluid fluid, BlockBehaviour.Properties properties) {
        super(fluid, properties);
    }
}
