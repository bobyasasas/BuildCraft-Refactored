/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.energy;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import buildcraft.core.blockentity.PlaceholderBlockEntity;

/**
 * Central block entity type registration for buildcraftenergy (task M2.4a skeleton, registry parity since M2.4b). The
 * single baseline id ({@code mj_dynamo}) registers as the shared {@link PlaceholderBlockEntity} bound to its block;
 * the real dynamo behaviour (legacy {@code TileDynamoMJ}) migrates in M2.5+.
 */
public final class BcEnergyBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
            .create(BuiltInRegistries.BLOCK_ENTITY_TYPE, BuildCraftEnergy.MOD_ID);

    /** Placeholder for {@code buildcraftenergy:mj_dynamo} (legacy {@code TileDynamoMJ}); behaviour class migrates in M2.5+. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceholderBlockEntity>> MJ_DYNAMO = BLOCK_ENTITIES
            .register("mj_dynamo", () -> new BlockEntityType<>(
                    (pos, state) -> new PlaceholderBlockEntity(BcEnergyBlockEntities.MJ_DYNAMO.value(), pos, state),
                    BcEnergyBlocks.MJ_DYNAMO.value()));

    private BcEnergyBlockEntities() {
    }
}
