/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import buildcraft.builders.menu.FillerMenu;

/**
 * Central menu type registration for buildcraftbuilders (task M4.8, the lib/gui framework's first builders consumer).
 * Menus are not part of the M3.3 registry parity gate, so this is a new id with no baseline registry constraint; the
 * legacy 1.20.1 baseline registered its filler menu under the same path ({@code BCBuildersMenuTypes}:
 * {@code "filler"}), so the id itself matches the frozen tree.
 *
 * <p>BE-bound menus register through {@link IMenuTypeExtension#create}: the {@code BlockPos} written by the NeoForge
 * {@code player.openMenu(MenuProvider, BlockPos)} extra data arrives in the registry-friendly buffer, and the client
 * half re-resolves the block entity from it (see {@code buildcraft.lib.gui.menu.BcBlockEntityMenu}).
 */
public final class BcBuildersMenus {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister
            .create(BuiltInRegistries.MENU, BuildCraftBuilders.MOD_ID);

    /** {@code buildcraftbuilders:filler} &mdash; the filler's resource GUI (see {@link FillerMenu}). */
    public static final DeferredHolder<MenuType<?>, MenuType<FillerMenu>> FILLER = MENUS
            .register("filler", () -> IMenuTypeExtension.create(FillerMenu::clientCreate));

    private BcBuildersMenus() {
    }
}
