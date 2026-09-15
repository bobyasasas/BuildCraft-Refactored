/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.transport;

import buildcraft.core.blockentity.PlaceholderBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central block entity type registration for buildcrafttransport (task M2.4c registry parity). Every block entity id
 * the 1.20.1 registry baseline attributes to {@code buildcrafttransport} registers here, bound to its (placeholder)
 * block through the shared {@link PlaceholderBlockEntity}. All placeholder, behaviour classes (legacy
 * {@code TileFilteredBuffer} / {@code TilePipeHolder}) migrate in M2.5+.
 */
public final class BcTransportBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
            .create(BuiltInRegistries.BLOCK_ENTITY_TYPE, BuildCraftTransport.MOD_ID);

    /** Placeholder for {@code buildcrafttransport:filtered_buffer}; behaviour class migrates in M2.5+. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceholderBlockEntity>> FILTERED_BUFFER = BLOCK_ENTITIES
            .register("filtered_buffer", () -> new BlockEntityType<>(
                    (pos, state) -> new PlaceholderBlockEntity(BcTransportBlockEntities.FILTERED_BUFFER.value(), pos, state),
                    BcTransportBlocks.FILTERED_BUFFER.value()));

    /** Placeholder for {@code buildcrafttransport:pipe_holder}; behaviour class migrates in M2.5+. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceholderBlockEntity>> PIPE_HOLDER = BLOCK_ENTITIES
            .register("pipe_holder", () -> new BlockEntityType<>(
                    (pos, state) -> new PlaceholderBlockEntity(BcTransportBlockEntities.PIPE_HOLDER.value(), pos, state),
                    BcTransportBlocks.PIPE_HOLDER.value()));

    private BcTransportBlockEntities() {
    }
}
