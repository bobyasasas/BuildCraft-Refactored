/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import buildcraft.core.blockentity.StoneEngineBlockEntity;

/**
 * Central block entity type registration for buildcraftcore (task M2.2a, first entry M2.2b). Every block entity type
 * this mod registers gets a constant {@link DeferredHolder} field here, registered through the single
 * {@link #BLOCK_ENTITIES} holder on the mod event bus (see
 * {@link BuildCraftCore#BuildCraftCore(net.neoforged.bus.api.IEventBus)}).
 *
 * <p>NeoForge 26.1.2 removed {@code BlockEntityType.Builder}; the block entity type constructor is public instead:
 *
 * <pre>{@code
 * public static final Supplier<BlockEntityType<TileEngine>> ENGINE = BLOCK_ENTITIES.register("engine",
 *         () -> new BlockEntityType<>(TileEngine::new, BcBlocks.ENGINE.value()));
 * }</pre>
 */
public final class BcBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
            .create(BuiltInRegistries.BLOCK_ENTITY_TYPE, BuildCraftCore.MOD_ID);

    /**
     * M2.2b stone engine slice block entity type. The varargs constructor is a NeoForge convenience
     * ({@code BlockEntityType#BlockEntityType(BlockEntitySupplier, Block...)}) over the vanilla
     * {@code Set<Block>} one. Referencing {@link BcBlocks#ENGINE_STONE} inside the supplier is safe: blocks register
     * before block entity types.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StoneEngineBlockEntity>> ENGINE_STONE = BLOCK_ENTITIES
            .register("engine_stone", () -> new BlockEntityType<>(StoneEngineBlockEntity::new, BcBlocks.ENGINE_STONE.value()));

    private BcBlockEntities() {
    }
}
