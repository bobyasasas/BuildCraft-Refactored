/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import buildcraft.core.menu.StoneEngineMenu;

/**
 * Central menu type registration for buildcraftcore (task M4.8, the lib/gui framework's first consumer). Menus are not
 * part of the M3.3 registry parity gate (its five sections are blocks/items/block_entities/entities/fluids), so the id
 * here is free of parity constraints; the legacy 1.20.1 baseline registered its stone engine menu as
 * {@code buildcraftenergy:engine_stone} ({@code BCEnergyMenuTypes}), which the engine slice now carries under its
 * current module home {@code buildcraftcore}.
 *
 * <p>BE-bound menus register through {@link IMenuTypeExtension#create}: the {@code BlockPos} written by the NeoForge
 * {@code player.openMenu(MenuProvider, BlockPos)} extra data arrives in the registry-friendly buffer, and the client
 * half re-resolves the block entity from it (see {@code buildcraft.lib.gui.menu.BcBlockEntityMenu}).
 */
public final class BcMenus {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister
            .create(BuiltInRegistries.MENU, BuildCraftCore.MOD_ID);

    /** {@code buildcraftcore:engine_stone} &mdash; the stone engine's fuel GUI (see {@link StoneEngineMenu}). */
    public static final DeferredHolder<MenuType<?>, MenuType<StoneEngineMenu>> ENGINE_STONE = MENUS
            .register("engine_stone", () -> IMenuTypeExtension.create(StoneEngineMenu::clientCreate));

    private BcMenus() {
    }
}
