/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.datacomponent;

import buildcraft.lib.BCLib;
import buildcraft.lib.datacomponent.filter.BcFilter;
import buildcraft.lib.datacomponent.gate.BcGateConfig;
import buildcraft.lib.datacomponent.robot.BcRobotParams;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central data component registration for the M2.6 item NBT schemas (task M2.6). Registration path verified against
 * the local NeoForge 26.1.2.109 sources:
 * {@code DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, namespace)} returns the specialized
 * {@code DeferredRegister.DataComponents} whose
 * {@code registerComponentType(String, UnaryOperator<DataComponentType.Builder>)} applies
 * {@code DataComponentType.builder().persistent(Codec).networkSynchronized(StreamCodec).build()}.
 *
 * <p>Three schemas, one per legacy item NBT carrier (see the conversion classes for the exact legacy shapes):
 * <ul>
 * <li>{@code buildcraftlib:filter} - filter slot list, from the legacy filter carriers;</li>
 * <li>{@code buildcraftlib:gate_config} - the {@code gate_data} payload of {@code ItemGateCopier};</li>
 * <li>{@code buildcraftrobotics:robot_params} - the robot energy root NBT of {@code ItemRobot}.</li>
 * </ul>
 *
 * <p>The robotics entry lives in its own register because the legacy robot namespace is {@code buildcraftrobotics}
 * (the namespace decides the registry id, so one register per mod id is required).
 */
public final class BcDataComponents {

    /** Mirrored from {@code buildcraft.robotics.BuildCraftRobotics#MOD_ID} of the neo port; kept literal here so the
     * lib mod does not need a (circular) reference to the robotics mod class. */
    public static final String ROBOTICS_MOD_ID = "buildcraftrobotics";

    public static final DeferredRegister.DataComponents LIB = DeferredRegister.createDataComponents(
        Registries.DATA_COMPONENT_TYPE, BCLib.MOD_ID);
    public static final DeferredRegister.DataComponents ROBOTICS = DeferredRegister.createDataComponents(
        Registries.DATA_COMPONENT_TYPE, ROBOTICS_MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BcFilter>> FILTER = LIB
        .registerComponentType("filter", builder -> builder
            .persistent(BcFilter.CODEC)
            .networkSynchronized(BcFilter.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BcGateConfig>> GATE_CONFIG = LIB
        .registerComponentType("gate_config", builder -> builder
            .persistent(BcGateConfig.CODEC)
            .networkSynchronized(BcGateConfig.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BcRobotParams>> ROBOT_PARAMS = ROBOTICS
        .registerComponentType("robot_params", builder -> builder
            .persistent(BcRobotParams.CODEC)
            .networkSynchronized(BcRobotParams.STREAM_CODEC));

    private BcDataComponents() {}
}
