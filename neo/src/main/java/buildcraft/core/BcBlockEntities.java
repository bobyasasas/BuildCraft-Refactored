/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central block entity type registration for buildcraftcore (task M2.2a). Intentionally empty for now: the first real
 * entries arrive with the engines in M2.2b. The registration centre is already wired to the mod event bus so M2.4 only
 * has to add fields, in the shape shown below.
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

    private BcBlockEntities() {
    }
}
