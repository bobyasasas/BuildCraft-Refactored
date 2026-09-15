/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.robotics;

import buildcraft.core.blockentity.PlaceholderBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central block entity type registration for buildcraftrobotics (task M2.4c registry parity). Every block entity id
 * the 1.20.1 registry baseline attributes to {@code buildcraftrobotics} registers here, bound to its (placeholder)
 * block through the shared {@link PlaceholderBlockEntity}. All placeholder, behaviour classes (legacy
 * {@code TileRequester} / {@code TileZonePlanner}) migrate in M2.5+.
 */
public final class BcRoboticsBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
            .create(BuiltInRegistries.BLOCK_ENTITY_TYPE, BuildCraftRobotics.MOD_ID);

    /** Placeholder for {@code buildcraftrobotics:requester}; behaviour class migrates in M2.5+. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceholderBlockEntity>> REQUESTER = BLOCK_ENTITIES
            .register("requester", () -> new BlockEntityType<>(
                    (pos, state) -> new PlaceholderBlockEntity(BcRoboticsBlockEntities.REQUESTER.value(), pos, state),
                    BcRoboticsBlocks.REQUESTER.value()));

    /** Placeholder for {@code buildcraftrobotics:zone_planner}; behaviour class migrates in M2.5+. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceholderBlockEntity>> ZONE_PLANNER = BLOCK_ENTITIES
            .register("zone_planner", () -> new BlockEntityType<>(
                    (pos, state) -> new PlaceholderBlockEntity(BcRoboticsBlockEntities.ZONE_PLANNER.value(), pos, state),
                    BcRoboticsBlocks.ZONE_PLANNER.value()));

    private BcRoboticsBlockEntities() {
    }
}
