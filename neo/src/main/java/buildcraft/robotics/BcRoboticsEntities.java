/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.robotics;

import buildcraft.robotics.entity.EntityRobot;
import buildcraft.robotics.entity.PlaceholderEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central entity type registration for buildcraftrobotics (task M2.4c registry parity, M2.13 first real robot). The
 * 17 robot entity ids the 1.20.1 registry baseline attributes to {@code buildcraftrobotics} (legacy registers one
 * {@code EntityRobot} type per {@code RedstoneBoardRobotNBT} via {@code BCRoboticsEntities}) register here with
 * unchanged ids: {@code robot_miner} carries the real M2.13 slice behaviour ({@link EntityRobot}), the other 16 are
 * still placeholders replaced by the full boards migration (M2.5+).
 *
 * <p>NeoForge 26.1.2 API notes (verified against the decompiled sources):
 * <ul>
 * <li>{@code EntityType.Builder.of(EntityType.EntityFactory, MobCategory)} + {@code build(ResourceKey)}: since
 * {@code build} now takes the registry {@code ResourceKey}, entries use the
 * {@link DeferredRegister#register(String, java.util.function.Function)} overload which hands the
 * {@code Identifier} to the supplier; the key is created inside the supplier.</li>
 * <li>The 16 placeholders keep {@code noSave()}: with serialization enabled the builder probes the vanilla data
 * fixer schema ({@code Util#fetchChoiceType(References.ENTITY_TREE, id)}), which logs an {@code ERROR} line for
 * every id absent from that schema (i.e. every modded id) and would break the M3.7 CI smoke gate
 * ("ERROR-level log lines == 0").</li>
 * <li>{@code robot_miner} <em>is</em> saveable since M2.13 (the robot must persist); the resulting one-line probe
 * noise for its id is silenced globally in {@link BuildCraftRobotics}' constructor (see the rationale there) &mdash;
 * the probe's result is discarded by {@code build()}, so the only observable change is the absence of the log
 * line.</li>
 * <li>No {@code EntityAttributeCreationEvent} registration: every entry is a plain entity in
 * {@link MobCategory#MISC} (no {@code DefaultAttributes} required; the M2.13 robot deliberately stays a plain
 * entity, see {@link EntityRobot}).</li>
 * </ul>
 */
public final class BcRoboticsEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister
            .create(Registries.ENTITY_TYPE, BuildCraftRobotics.MOD_ID);

    /** Placeholder for {@code buildcraftrobotics:robot_bomber} (legacy per-board {@code EntityRobot} type); behaviour class migrates with the full boards port. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_BOMBER = registerRobot("robot_bomber");

    /** Placeholder for {@code buildcraftrobotics:robot_builder} (legacy per-board {@code EntityRobot} type); behaviour class migrates with the full boards port. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_BUILDER = registerRobot("robot_builder");

    /** Placeholder for {@code buildcraftrobotics:robot_butcher} (legacy per-board {@code EntityRobot} type); behaviour class migrates with the full boards port. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_BUTCHER = registerRobot("robot_butcher");

    /** Placeholder for {@code buildcraftrobotics:robot_carrier} (legacy per-board {@code EntityRobot} type); behaviour class migrates with the full boards port. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_CARRIER = registerRobot("robot_carrier");

    /** Placeholder for {@code buildcraftrobotics:robot_delivery} (legacy per-board {@code EntityRobot} type); behaviour class migrates with the full boards port. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_DELIVERY = registerRobot("robot_delivery");

    /** Placeholder for {@code buildcraftrobotics:robot_farmer} (legacy per-board {@code EntityRobot} type); behaviour class migrates with the full boards port. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_FARMER = registerRobot("robot_farmer");

    /** Placeholder for {@code buildcraftrobotics:robot_fluid_carrier} (legacy per-board {@code EntityRobot} type); behaviour class migrates with the full boards port. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_FLUID_CARRIER = registerRobot("robot_fluid_carrier");

    /** Placeholder for {@code buildcraftrobotics:robot_harvester} (legacy per-board {@code EntityRobot} type); behaviour class migrates with the full boards port. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_HARVESTER = registerRobot("robot_harvester");

    /** Placeholder for {@code buildcraftrobotics:robot_knight} (legacy per-board {@code EntityRobot} type); behaviour class migrates with the full boards port. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_KNIGHT = registerRobot("robot_knight");

    /** Placeholder for {@code buildcraftrobotics:robot_leave_cutter} (legacy per-board {@code EntityRobot} type); behaviour class migrates with the full boards port. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_LEAVE_CUTTER = registerRobot("robot_leave_cutter");

    /** Placeholder for {@code buildcraftrobotics:robot_lumberjack} (legacy per-board {@code EntityRobot} type); behaviour class migrates with the full boards port. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_LUMBERJACK = registerRobot("robot_lumberjack");

    /**
     * The M2.13 slice robot: the real {@link EntityRobot} (miner behaviour &mdash; legacy
     * {@code BoardRobotMiner}) under the unchanged id {@code buildcraftrobotics:robot_miner}. Saveable (persisted
     * with the world, summonable with configuration NBT), tick driven, with the data fixer schema probe handled at
     * the mod level (see the class javadoc and {@link BuildCraftRobotics}).
     */
    public static final DeferredHolder<EntityType<?>, EntityType<EntityRobot>> ROBOT_MINER = ENTITIES.register(
            "robot_miner", id -> EntityType.Builder.<EntityRobot>of(EntityRobot::new, MobCategory.MISC)
                    .fireImmune()
                    .sized(0.5F, 0.5F)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

    /** Placeholder for {@code buildcraftrobotics:robot_picker} (legacy per-board {@code EntityRobot} type); behaviour class migrates with the full boards port. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_PICKER = registerRobot("robot_picker");

    /** Placeholder for {@code buildcraftrobotics:robot_planter} (legacy per-board {@code EntityRobot} type); behaviour class migrates with the full boards port. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_PLANTER = registerRobot("robot_planter");

    /** Placeholder for {@code buildcraftrobotics:robot_pump} (legacy per-board {@code EntityRobot} type); behaviour class migrates with the full boards port. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_PUMP = registerRobot("robot_pump");

    /** Placeholder for {@code buildcraftrobotics:robot_shovelman} (legacy per-board {@code EntityRobot} type); behaviour class migrates with the full boards port. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_SHOVELMAN = registerRobot("robot_shovelman");

    /** Placeholder for {@code buildcraftrobotics:robot_stripes} (legacy per-board {@code EntityRobot} type); behaviour class migrates with the full boards port. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_STRIPES = registerRobot("robot_stripes");

    /**
     * Shared placeholder registration: same dimensions, fire immunity and {@code MobCategory.MISC} as the legacy
     * {@code EntityRobot} registration, but plain-entity based and with the data fixer schema probe disabled (see the
     * class javadoc).
     */
    private static DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> registerRobot(String name) {
        return ENTITIES.register(name, id -> EntityType.Builder.<PlaceholderEntity>of(PlaceholderEntity::new, MobCategory.MISC)
                .fireImmune()
                .sized(0.5F, 0.5F)
                .noSave()
                .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));
    }

    private BcRoboticsEntities() {
    }
}
