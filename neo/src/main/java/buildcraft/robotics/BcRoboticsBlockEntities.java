/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.robotics;

import buildcraft.robotics.blockentity.RequesterBlockEntity;
import buildcraft.robotics.blockentity.ZonePlannerBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central block entity type registration for buildcraftrobotics (task M2.4c registry parity). Every block entity id
 * the 1.20.1 registry baseline attributes to {@code buildcraftrobotics} registers here, bound to its block; the two
 * M4.17 support machines (requester, zone planner) carry their real behaviour classes under the unchanged ids (the
 * registry parity gate is unaffected), the remaining placeholders behaviour-migrate in M2.5+.
 */
public final class BcRoboticsBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
            .create(BuiltInRegistries.BLOCK_ENTITY_TYPE, BuildCraftRobotics.MOD_ID);

    /** {@code buildcraftrobotics:requester}; real class since M4.17 ({@link RequesterBlockEntity}). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RequesterBlockEntity>> REQUESTER = BLOCK_ENTITIES
            .register("requester", () -> new BlockEntityType<>(RequesterBlockEntity::new,
                    BcRoboticsBlocks.REQUESTER.value()));

    /** {@code buildcraftrobotics:zone_planner}; real class since M4.17 ({@link ZonePlannerBlockEntity}). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ZonePlannerBlockEntity>> ZONE_PLANNER = BLOCK_ENTITIES
            .register("zone_planner", () -> new BlockEntityType<>(ZonePlannerBlockEntity::new,
                    BcRoboticsBlocks.ZONE_PLANNER.value()));

    private BcRoboticsBlockEntities() {
    }
}
