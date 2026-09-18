/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central item registration for buildcraftlib (task M2.4a registry parity). Every item this mod registers gets a
 * constant {@link DeferredItem} field here, registered through the single {@link #ITEMS} holder on the mod event bus
 * (see {@link BCLib#BCLib(net.neoforged.bus.api.IEventBus)}).
 *
 * <p>The three ids match the 1.20.1 registry baseline (see {@code migration/snapshots/registry-baseline.json}):
 * without a migrated behaviour class they register as plain {@link Item} placeholders and get replaced by the real
 * classes in M2.5+.
 */
public final class BcLibItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BCLib.MOD_ID);

    /** Placeholder for {@code buildcraftlib:guide} (legacy {@code ItemGuide}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> GUIDE = BcLangKeys.item(ITEMS, "guide");

    /** Placeholder for {@code buildcraftlib:guide_note} (legacy {@code ItemGuideNote}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> GUIDE_NOTE = BcLangKeys.item(ITEMS, "guide_note");

    /** Placeholder for {@code buildcraftlib:debugger} (legacy {@code ItemDebugger}, stacks to 1); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> DEBUGGER = BcLangKeys.simpleItem(ITEMS, "debugger", properties -> properties.stacksTo(1));

    private BcLibItems() {
    }
}
