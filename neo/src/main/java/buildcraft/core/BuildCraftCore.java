/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Minimal mod skeleton proving the NeoForge 26.1.2 toolchain (task M2.1).
 * Real content migrates in later Phase 2 tasks.
 */
// The value here should match the modId in META-INF/neoforge.mods.toml
@Mod(BuildCraftCore.MOD_ID)
public class BuildCraftCore {

    public static final String MOD_ID = "buildcraftcore";

    private static final Logger LOGGER = LogUtils.getLogger();

    public BuildCraftCore() {
        LOGGER.info("BuildCraft core (neo skeleton) loaded");
    }
}
