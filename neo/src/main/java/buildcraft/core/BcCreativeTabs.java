/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central creative mode tab registration for buildcraftcore (task M2.2a). Creative tabs are a normal registry in
 * NeoForge, so they follow the same pattern as {@link BcBlocks} and {@link BcItems}: one holder class, one
 * {@link DeferredRegister}, constant {@link DeferredHolder} fields, wired on the mod event bus.
 */
public final class BcCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister
            .create(Registries.CREATIVE_MODE_TAB, BuildCraftCore.MOD_ID);

    /** Main BuildCraft tab. Displays the marker placeholder and the M2.2b stone engine slice. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.buildcraftcore"))
                    .icon(() -> new ItemStack(BcItems.MARKER.value()))
                    .displayItems((parameters, output) -> {
                        output.accept(BcItems.MARKER.value());
                        output.accept(BcItems.ENGINE_STONE.value());
                        output.accept(BcItems.PIPE_KINESIS_WOOD.value());
                        output.accept(BcItems.ENERGY_METER.value());
                    })
                    .build());

    private BcCreativeTabs() {
    }
}
