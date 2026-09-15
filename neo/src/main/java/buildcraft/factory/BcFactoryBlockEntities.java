/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import buildcraft.core.blockentity.PlaceholderBlockEntity;

/**
 * Central block entity type registration for buildcraftfactory (task M2.4a skeleton, registry parity since M2.4b).
 * Every baseline block entity id registers as the shared {@link PlaceholderBlockEntity} bound to its (placeholder)
 * block; the real behaviour classes (legacy {@code Tile*}) migrate in M2.5+.
 */
public final class BcFactoryBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
            .create(BuiltInRegistries.BLOCK_ENTITY_TYPE, BuildCraftFactory.MOD_ID);
    /** Placeholder for {@code buildcraftfactory:autoworkbench_item}; behaviour class migrates in M2.5+. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceholderBlockEntity>> AUTOWORKBENCH_ITEM = BLOCK_ENTITIES
            .register("autoworkbench_item", () -> new BlockEntityType<>(
                    (pos, state) -> new PlaceholderBlockEntity(BcFactoryBlockEntities.AUTOWORKBENCH_ITEM.value(), pos, state),
                    BcFactoryBlocks.AUTOWORKBENCH_ITEM.value()));

    /** Placeholder for {@code buildcraftfactory:chute}; behaviour class migrates in M2.5+. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceholderBlockEntity>> CHUTE = BLOCK_ENTITIES
            .register("chute", () -> new BlockEntityType<>(
                    (pos, state) -> new PlaceholderBlockEntity(BcFactoryBlockEntities.CHUTE.value(), pos, state),
                    BcFactoryBlocks.CHUTE.value()));

    /** Placeholder for {@code buildcraftfactory:distiller}; behaviour class migrates in M2.5+. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceholderBlockEntity>> DISTILLER = BLOCK_ENTITIES
            .register("distiller", () -> new BlockEntityType<>(
                    (pos, state) -> new PlaceholderBlockEntity(BcFactoryBlockEntities.DISTILLER.value(), pos, state),
                    BcFactoryBlocks.DISTILLER.value()));

    /** Placeholder for {@code buildcraftfactory:flood_gate}; behaviour class migrates in M2.5+. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceholderBlockEntity>> FLOOD_GATE = BLOCK_ENTITIES
            .register("flood_gate", () -> new BlockEntityType<>(
                    (pos, state) -> new PlaceholderBlockEntity(BcFactoryBlockEntities.FLOOD_GATE.value(), pos, state),
                    BcFactoryBlocks.FLOOD_GATE.value()));

    /** Placeholder for {@code buildcraftfactory:heat_exchange}; behaviour class migrates in M2.5+. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceholderBlockEntity>> HEAT_EXCHANGE = BLOCK_ENTITIES
            .register("heat_exchange", () -> new BlockEntityType<>(
                    (pos, state) -> new PlaceholderBlockEntity(BcFactoryBlockEntities.HEAT_EXCHANGE.value(), pos, state),
                    BcFactoryBlocks.HEAT_EXCHANGE.value()));

    /** Placeholder for {@code buildcraftfactory:mining_well}; behaviour class migrates in M2.5+. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceholderBlockEntity>> MINING_WELL = BLOCK_ENTITIES
            .register("mining_well", () -> new BlockEntityType<>(
                    (pos, state) -> new PlaceholderBlockEntity(BcFactoryBlockEntities.MINING_WELL.value(), pos, state),
                    BcFactoryBlocks.MINING_WELL.value()));

    /** Placeholder for {@code buildcraftfactory:pump}; behaviour class migrates in M2.5+. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceholderBlockEntity>> PUMP = BLOCK_ENTITIES
            .register("pump", () -> new BlockEntityType<>(
                    (pos, state) -> new PlaceholderBlockEntity(BcFactoryBlockEntities.PUMP.value(), pos, state),
                    BcFactoryBlocks.PUMP.value()));

    /** Placeholder for {@code buildcraftfactory:tank}; behaviour class migrates in M2.5+. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceholderBlockEntity>> TANK = BLOCK_ENTITIES
            .register("tank", () -> new BlockEntityType<>(
                    (pos, state) -> new PlaceholderBlockEntity(BcFactoryBlockEntities.TANK.value(), pos, state),
                    BcFactoryBlocks.TANK.value()));


    private BcFactoryBlockEntities() {
    }
}
