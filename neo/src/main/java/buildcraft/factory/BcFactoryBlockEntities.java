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
import buildcraft.factory.blockentity.DistillerBlockEntity;
import buildcraft.factory.blockentity.HeatExchangeBlockEntity;
import buildcraft.factory.blockentity.PumpBlockEntity;
import buildcraft.factory.blockentity.TankBlockEntity;

/**
 * Central block entity type registration for buildcraftfactory (task M2.4a skeleton, registry parity since M2.4b).
 * Most baseline block entity ids register as the shared {@link PlaceholderBlockEntity} bound to their (placeholder)
 * block; the M4.7 machines (tank, distiller, heat exchange, pump) carry their real behaviour classes (the ids are
 * unchanged, so the registry parity gate is unaffected).
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

    /** Placeholder for {@code buildcraftfactory:distiller}; real class since M4.7 ({@link DistillerBlockEntity}). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DistillerBlockEntity>> DISTILLER =
            BLOCK_ENTITIES.register("distiller", () -> new BlockEntityType<>(DistillerBlockEntity::new,
                    BcFactoryBlocks.DISTILLER.value()));

    /** Placeholder for {@code buildcraftfactory:flood_gate}; behaviour class migrates in M2.5+. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceholderBlockEntity>> FLOOD_GATE = BLOCK_ENTITIES
            .register("flood_gate", () -> new BlockEntityType<>(
                    (pos, state) -> new PlaceholderBlockEntity(BcFactoryBlockEntities.FLOOD_GATE.value(), pos, state),
                    BcFactoryBlocks.FLOOD_GATE.value()));

    /** Placeholder for {@code buildcraftfactory:heat_exchange}; real class since M4.7 ({@link HeatExchangeBlockEntity}). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeatExchangeBlockEntity>> HEAT_EXCHANGE =
            BLOCK_ENTITIES.register("heat_exchange", () -> new BlockEntityType<>(HeatExchangeBlockEntity::new,
                    BcFactoryBlocks.HEAT_EXCHANGE.value()));

    /** Placeholder for {@code buildcraftfactory:mining_well}; behaviour class migrates in M2.5+. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlaceholderBlockEntity>> MINING_WELL = BLOCK_ENTITIES
            .register("mining_well", () -> new BlockEntityType<>(
                    (pos, state) -> new PlaceholderBlockEntity(BcFactoryBlockEntities.MINING_WELL.value(), pos, state),
                    BcFactoryBlocks.MINING_WELL.value()));

    /** Placeholder for {@code buildcraftfactory:pump}; real class since M4.7 ({@link PumpBlockEntity}). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PumpBlockEntity>> PUMP =
            BLOCK_ENTITIES.register("pump", () -> new BlockEntityType<>(PumpBlockEntity::new,
                    BcFactoryBlocks.PUMP.value()));

    /** {@code buildcraftfactory:tank}; real class since M4.7 ({@link TankBlockEntity}). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TankBlockEntity>> TANK =
            BLOCK_ENTITIES.register("tank", () -> new BlockEntityType<>(TankBlockEntity::new,
                    BcFactoryBlocks.TANK.value()));


    private BcFactoryBlockEntities() {
    }
}
