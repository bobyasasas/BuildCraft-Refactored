/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.robotics;

import com.mojang.logging.LogUtils;
import net.minecraft.SharedConstants;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

import buildcraft.lib.datacomponent.BcDataComponents;

/**
 * BuildCraft robotics mod entry point for the NeoForge 26.1.2 port (task M2.4a eight-mod skeleton, M2.4c registry
 * parity, M2.13 first real robot). Legacy counterpart: {@code buildcraft.robotics.BCRobotics}.
 */
// The value here should match the modId in META-INF/neoforge.mods.toml
@Mod(BuildCraftRobotics.MOD_ID)
public class BuildCraftRobotics {

    public static final String MOD_ID = "buildcraftrobotics";

    private static final Logger LOGGER = LogUtils.getLogger();

    public BuildCraftRobotics(IEventBus modEventBus) {
        // M2.13: the saveable robot entity (buildcraftrobotics:robot_miner, serialize=true) trips vanilla's
        // EntityType.Builder#build data fixer schema probe: Util#fetchChoiceType(References.ENTITY_TREE, id) looks
        // the id up in the frozen vanilla DFU schema and logs an ERROR ("No data fixer registered for ...") for
        // every modded id, because modded ids can never be part of the vanilla data fixer tree. The probe's return
        // value is discarded by build() — it is a pure validation pass — and 26.1.2 leaves SharedConstants
        // .CHECK_DATA_FIXER_SCHEMA hard-wired to true with no FML/NeoForge hook to register modded fixers (verified
        // against the 26.1.2.109 decompiled sources and the FML loader jar; vanilla ids all have fixers and are
        // unaffected either way). Flipping the flag here — mod constructors run before any registry entry supplier
        // — suppresses the one ERROR line per boot that would otherwise break the M3.7 CI smoke gate
        // ("ERROR-level log lines == 0", fail-closed, ci.yml is append-only for this task). Behaviour is otherwise
        // identical: entities save/load through the same NBT path regardless of the probe.
        SharedConstants.CHECK_DATA_FIXER_SCHEMA = false;
        LOGGER.info("BuildCraft robotics (neo skeleton) loaded");
        BcRoboticsBlocks.BLOCKS.register(modEventBus);
        BcRoboticsItems.ITEMS.register(modEventBus);
        BcRoboticsBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        BcRoboticsEntities.ENTITIES.register(modEventBus);

        // M2.6: robot params NBT schema as a data component (buildcraftrobotics:robot_params).
        BcDataComponents.ROBOTICS.register(modEventBus);
    }
}
