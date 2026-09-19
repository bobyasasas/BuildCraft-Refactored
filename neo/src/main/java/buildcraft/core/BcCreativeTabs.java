/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core;

import buildcraft.builders.BcBuildersItems;
import buildcraft.energy.BcEnergyItems;
import buildcraft.factory.BcFactoryItems;
import buildcraft.lib.BcLibItems;
import buildcraft.robotics.BcRoboticsItems;
import buildcraft.silicon.BcSiliconItems;
import buildcraft.transport.BcTransportItems;
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

    /** Main BuildCraft tab. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    // M3.5: keyed to the BuildCraft-Localization baseline ("itemGroup.buildcraft.main" = "BuildCraft"),
                    // replacing the M2.x placeholder key so the lang key set stays at baseline parity (diff=0).
                    .title(Component.translatable("itemGroup.buildcraft.main"))
                    .icon(() -> new ItemStack(BcItems.MARKER.value()))
                    .displayItems((parameters, output) -> {
                        // M4.18a (user report: "jei和创造模式栏位只有很少的一点物品"): the tab used to stream only
                        // BcItems.ITEMS (buildcraftcore, ~49 of ~205 items) — every other module's items were
                        // invisible to the creative tab (and to JEI's tab-filtered view). Aggregate every module's
                        // item register here instead. Referencing the register classes inside this lazy runtime
                        // callback is safe: their static init ran when each mod registered ITEMS on its mod bus, long
                        // before any creative screen builds this list. The 1.20.1 baseline split the same items over
                        // five BC tabs (main/pipes/plugs/facades/boards, see CreativeTabManager); this port keeps the
                        // single-tab structure and therefore accepts everything here, in the fixed module order
                        // core -> builders -> factory -> energy -> silicon -> transport -> robotics, with the three
                        // buildcraftlib items (baseline BCLib tags them "buildcraft.main") last.
                        BcItems.ITEMS.getEntries().forEach(entry -> output.accept(entry.value()));
                        BcBuildersItems.ITEMS.getEntries().forEach(entry -> output.accept(entry.value()));
                        BcFactoryItems.ITEMS.getEntries().forEach(entry -> output.accept(entry.value()));
                        BcEnergyItems.ITEMS.getEntries().forEach(entry -> output.accept(entry.value()));
                        BcSiliconItems.ITEMS.getEntries().forEach(entry -> output.accept(entry.value()));
                        BcTransportItems.ITEMS.getEntries().forEach(entry -> output.accept(entry.value()));
                        BcRoboticsItems.ITEMS.getEntries().forEach(entry -> output.accept(entry.value()));
                        BcLibItems.ITEMS.getEntries().forEach(entry -> output.accept(entry.value()));
                    })
                    .build());

    private BcCreativeTabs() {
    }
}
