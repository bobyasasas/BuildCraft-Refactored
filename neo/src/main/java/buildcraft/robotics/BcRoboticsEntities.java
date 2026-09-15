/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.robotics;

import buildcraft.robotics.entity.PlaceholderEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central entity type registration for buildcraftrobotics (task M2.4c registry parity). The 17 robot entity ids the
 * 1.20.1 registry baseline attributes to {@code buildcraftrobotics} (legacy registers one {@code EntityRobot} type
 * per {@code RedstoneBoardRobotNBT} via {@code BCRoboticsEntities}) register here as placeholders; the real robot
 * behaviour migrates in M2.5+.
 *
 * <p>NeoForge 26.1.2 API notes (verified against the decompiled sources):
 * <ul>
 * <li>{@code EntityType.Builder.of(EntityType.EntityFactory, MobCategory)} + {@code build(ResourceKey)}: since
 * {@code build} now takes the registry {@code ResourceKey}, entries use the
 * {@link DeferredRegister#register(String, java.util.function.Function)} overload which hands the
 * {@code Identifier} to the supplier; the key is created inside the supplier.</li>
 * <li>{@code noSave()} is deliberate on every placeholder: with serialization enabled the builder runs
 * {@code Util#fetchChoiceType(References.ENTITY_TREE, id)}, which logs an {@code ERROR} line for every id absent
 * from the vanilla data fixer schema (i.e. every modded id) in non-IDE runs. Placeholders are never spawned nor
 * saved, so the schema probe is skipped instead of spamming the log; M2.5+ restores full saveability together with
 * the real {@code EntityRobot}.</li>
 * <li>No {@code EntityAttributeCreationEvent} registration: placeholders are plain entities in
 * {@link MobCategory#MISC} (no {@code DefaultAttributes} required, see {@link PlaceholderEntity}). The real
 * {@code EntityRobot} is a {@code LivingEntity} and will register its attributes in M2.5+.</li>
 * </ul>
 */
public final class BcRoboticsEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister
            .create(Registries.ENTITY_TYPE, BuildCraftRobotics.MOD_ID);

    /** Placeholder for {@code buildcraftrobotics:robot_bomber} (legacy per-board {@code EntityRobot} type); behaviour class migrates in M2.5+. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_BOMBER = registerRobot("robot_bomber");

    /** Placeholder for {@code buildcraftrobotics:robot_builder} (legacy per-board {@code EntityRobot} type); behaviour class migrates in M2.5+. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_BUILDER = registerRobot("robot_builder");

    /** Placeholder for {@code buildcraftrobotics:robot_butcher} (legacy per-board {@code EntityRobot} type); behaviour class migrates in M2.5+. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_BUTCHER = registerRobot("robot_butcher");

    /** Placeholder for {@code buildcraftrobotics:robot_carrier} (legacy per-board {@code EntityRobot} type); behaviour class migrates in M2.5+. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_CARRIER = registerRobot("robot_carrier");

    /** Placeholder for {@code buildcraftrobotics:robot_delivery} (legacy per-board {@code EntityRobot} type); behaviour class migrates in M2.5+. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_DELIVERY = registerRobot("robot_delivery");

    /** Placeholder for {@code buildcraftrobotics:robot_farmer} (legacy per-board {@code EntityRobot} type); behaviour class migrates in M2.5+. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_FARMER = registerRobot("robot_farmer");

    /** Placeholder for {@code buildcraftrobotics:robot_fluid_carrier} (legacy per-board {@code EntityRobot} type); behaviour class migrates in M2.5+. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_FLUID_CARRIER = registerRobot("robot_fluid_carrier");

    /** Placeholder for {@code buildcraftrobotics:robot_harvester} (legacy per-board {@code EntityRobot} type); behaviour class migrates in M2.5+. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_HARVESTER = registerRobot("robot_harvester");

    /** Placeholder for {@code buildcraftrobotics:robot_knight} (legacy per-board {@code EntityRobot} type); behaviour class migrates in M2.5+. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_KNIGHT = registerRobot("robot_knight");

    /** Placeholder for {@code buildcraftrobotics:robot_leave_cutter} (legacy per-board {@code EntityRobot} type); behaviour class migrates in M2.5+. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_LEAVE_CUTTER = registerRobot("robot_leave_cutter");

    /** Placeholder for {@code buildcraftrobotics:robot_lumberjack} (legacy per-board {@code EntityRobot} type); behaviour class migrates in M2.5+. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_LUMBERJACK = registerRobot("robot_lumberjack");

    /** Placeholder for {@code buildcraftrobotics:robot_miner} (legacy per-board {@code EntityRobot} type); behaviour class migrates in M2.5+. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_MINER = registerRobot("robot_miner");

    /** Placeholder for {@code buildcraftrobotics:robot_picker} (legacy per-board {@code EntityRobot} type); behaviour class migrates in M2.5+. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_PICKER = registerRobot("robot_picker");

    /** Placeholder for {@code buildcraftrobotics:robot_planter} (legacy per-board {@code EntityRobot} type); behaviour class migrates in M2.5+. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_PLANTER = registerRobot("robot_planter");

    /** Placeholder for {@code buildcraftrobotics:robot_pump} (legacy per-board {@code EntityRobot} type); behaviour class migrates in M2.5+. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_PUMP = registerRobot("robot_pump");

    /** Placeholder for {@code buildcraftrobotics:robot_shovelman} (legacy per-board {@code EntityRobot} type); behaviour class migrates in M2.5+. */
    public static final DeferredHolder<EntityType<?>, EntityType<PlaceholderEntity>> ROBOT_SHOVELMAN = registerRobot("robot_shovelman");

    /** Placeholder for {@code buildcraftrobotics:robot_stripes} (legacy per-board {@code EntityRobot} type); behaviour class migrates in M2.5+. */
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
