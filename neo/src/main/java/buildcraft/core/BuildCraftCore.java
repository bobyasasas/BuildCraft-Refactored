/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core;

import com.mojang.logging.LogUtils;
import buildcraft.lib.expression.DefaultContexts;
import buildcraft.lib.expression.GenericExpressionCompiler;
import buildcraft.lib.expression.api.InvalidExpressionException;
import buildcraft.lib.expression.api.IExpressionNode.INodeLong;
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
        expressionSmokeTest();
    }

    /**
     * M2.3: runtime proof that the expression library shared from
     * sub_projects/expression is really on the mod classpath, by compiling and
     * evaluating a constant expression through the real compiler.
     */
    private static void expressionSmokeTest() {
        try {
            INodeLong node = GenericExpressionCompiler.compileExpressionLong("1+2*3", DefaultContexts.createWithAll());
            LOGGER.info("BuildCraft expression smoke: 1+2*3 = " + node.evaluate());
        } catch (InvalidExpressionException e) {
            throw new IllegalStateException("BuildCraft expression smoke test failed to compile", e);
        }
    }
}
