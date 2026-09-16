/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory;

import com.mojang.logging.LogUtils;
import buildcraft.datagen.BcDatagen;
import buildcraft.factory.recipe.BcFactoryRecipes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * BuildCraft factory mod entry point for the NeoForge 26.1.2 port (task M2.4a eight-mod skeleton, full registry
 * parity since M2.4b). Legacy counterpart: {@code buildcraft.factory.BCFactory}. The registrations are
 * placeholders (see the {@code BcFactory*} centres); real behaviour classes migrate in M2.5+.
 */
// The value here should match the modId in META-INF/neoforge.mods.toml
@Mod(BuildCraftFactory.MOD_ID)
public class BuildCraftFactory {

    public static final String MOD_ID = "buildcraftfactory";

    private static final Logger LOGGER = LogUtils.getLogger();

    public BuildCraftFactory(IEventBus modEventBus) {
        LOGGER.info("BuildCraft factory (neo skeleton) loaded");
        BcFactoryBlocks.BLOCKS.register(modEventBus);
        BcFactoryItems.ITEMS.register(modEventBus);
        BcFactoryBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        // M3.4: datagen providers for this mod's namespace (item models, blockstates, lang placeholder).
        BcDatagen.register(modEventBus);
        // M2.10: heat exchange (heatable/coolable) + distillation recipe serializers.
        BcFactoryRecipes.TYPES.register(modEventBus);
        BcFactoryRecipes.SERIALIZERS.register(modEventBus);
    }
}
