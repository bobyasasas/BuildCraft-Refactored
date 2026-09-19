/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.silicon;

import com.mojang.logging.LogUtils;
import buildcraft.datagen.BcDatagen;
import buildcraft.silicon.recipe.BcSiliconRecipes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.slf4j.Logger;

/**
 * BuildCraft silicon mod entry point for the NeoForge 26.1.2 port (task M2.4a eight-mod skeleton, full registry
 * parity since M2.4b). Legacy counterpart: {@code buildcraft.silicon.BCSilicon}. Since M4.16 the six machines
 * (laser + five tables) carry their real behaviour classes and expose the tables' inventories through the 26.1.2
 * item capability.
 */
// The value here should match the modId in META-INF/neoforge.mods.toml
@Mod(BuildCraftSilicon.MOD_ID)
public class BuildCraftSilicon {

    public static final String MOD_ID = "buildcraftsilicon";

    private static final Logger LOGGER = LogUtils.getLogger();

    public BuildCraftSilicon(IEventBus modEventBus) {
        LOGGER.info("BuildCraft silicon (neo skeleton) loaded");
        BcSiliconBlocks.BLOCKS.register(modEventBus);
        BcSiliconItems.ITEMS.register(modEventBus);
        BcSiliconBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        // M3.4: datagen providers for this mod's namespace (item models, blockstates, lang placeholder).
        BcDatagen.register(modEventBus);
        // M2.10: assembly/programming/integration/facade-swap machine recipe serializers (the programming JSONs also
        // live in the buildcraftrobotics namespace, exactly like 1.20.1).
        BcSiliconRecipes.TYPES.register(modEventBus);
        BcSiliconRecipes.SERIALIZERS.register(modEventBus);
        // M4.16: the tables with inventories reach automation from every face (the M4.6 pipe-inbox precedent);
        // M4.17 adds the charging table's single slot and the programming table's input/output pair (the laser has
        // no item inventory).
        modEventBus.addListener(this::onRegisterCapabilities);
    }

    private void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, BcSiliconBlockEntities.ASSEMBLY_TABLE.value(),
            (blockEntity, side) -> blockEntity.getItemHandler(side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, BcSiliconBlockEntities.INTEGRATION_TABLE.value(),
            (blockEntity, side) -> blockEntity.getItemHandler(side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, BcSiliconBlockEntities.ADVANCED_CRAFTING_TABLE.value(),
            (blockEntity, side) -> blockEntity.getItemHandler(side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, BcSiliconBlockEntities.CHARGING_TABLE.value(),
            (blockEntity, side) -> blockEntity.getItemHandler(side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, BcSiliconBlockEntities.PROGRAMMING_TABLE.value(),
            (blockEntity, side) -> blockEntity.getItemHandler(side));
    }
}
