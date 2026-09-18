/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import buildcraft.lib.BcLangKeys;

/**
 * Central item registration for buildcraftcore (task M2.2a, M2.4a registry parity). Every item this mod registers gets
 * a constant {@link DeferredItem} field here, registered through the single {@link #ITEMS} holder on the mod event bus
 * (see {@link BuildCraftCore#BuildCraftCore(net.neoforged.bus.api.IEventBus)}). Block items reference their block from
 * {@link BcBlocks}.
 *
 * <p>Since M2.4a this class carries every item id the 1.20.1 registry baseline (see
 * {@code migration/snapshots/registry-baseline.json}) attributes to {@code buildcraftcore}: items without a migrated
 * behaviour class register as plain {@link Item} placeholders and get replaced by the real classes in M2.5+.
 */
public final class BcItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BuildCraftCore.MOD_ID);

    /** Item form of the {@link BcBlocks#MARKER} placeholder block. */
    public static final DeferredItem<BlockItem> MARKER = BcLangKeys.blockItem(ITEMS, BcBlocks.MARKER);

    /** Item form of the {@link BcBlocks#ENGINE_STONE} slice block (M2.2b). */
    public static final DeferredItem<BlockItem> ENGINE_STONE = BcLangKeys.blockItem(ITEMS, BcBlocks.ENGINE_STONE);

    /** Item form of the {@link BcBlocks#PIPE_KINESIS_WOOD} slice block (M2.2c). */
    public static final DeferredItem<BlockItem> PIPE_KINESIS_WOOD = BcLangKeys.blockItem(ITEMS, BcBlocks.PIPE_KINESIS_WOOD);

    /** Item form of the {@link BcBlocks#ENERGY_METER} slice-only measurement block (M2.2c). */
    public static final DeferredItem<BlockItem> ENERGY_METER = BcLangKeys.blockItem(ITEMS, BcBlocks.ENERGY_METER);

    // -------------------------------------------------------------------------
    // M2.4a registry parity: block items of the M2.4a placeholder blocks (legacy
    // ItemEngine_BC8 / ItemBlockSpring / ItemBlockDecorated). Placeholder, real
    // behaviour classes migrate in M2.5+.
    // -------------------------------------------------------------------------

    /** Placeholder for {@code buildcraftcore:spring_water}; behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> SPRING_WATER = BcLangKeys.blockItem(ITEMS, BcBlocks.SPRING_WATER);

    /** Placeholder for {@code buildcraftcore:spring_oil}; behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> SPRING_OIL = BcLangKeys.blockItem(ITEMS, BcBlocks.SPRING_OIL);

    /** Placeholder for {@code buildcraftcore:decorated_blueprint}; behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> DECORATED_BLUEPRINT = BcLangKeys.blockItem(ITEMS, BcBlocks.DECORATED_BLUEPRINT);

    /** Placeholder for {@code buildcraftcore:decorated_destroy}; behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> DECORATED_DESTROY = BcLangKeys.blockItem(ITEMS, BcBlocks.DECORATED_DESTROY);

    /** Placeholder for {@code buildcraftcore:decorated_laser_back}; behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> DECORATED_LASER_BACK = BcLangKeys.blockItem(ITEMS, BcBlocks.DECORATED_LASER_BACK);

    /** Placeholder for {@code buildcraftcore:decorated_leather}; behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> DECORATED_LEATHER = BcLangKeys.blockItem(ITEMS, BcBlocks.DECORATED_LEATHER);

    /** Placeholder for {@code buildcraftcore:decorated_paper}; behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> DECORATED_PAPER = BcLangKeys.blockItem(ITEMS, BcBlocks.DECORATED_PAPER);

    /** Placeholder for {@code buildcraftcore:decorated_template}; behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> DECORATED_TEMPLATE = BcLangKeys.blockItem(ITEMS, BcBlocks.DECORATED_TEMPLATE);

    /** Placeholder for {@code buildcraftcore:engine_wood}; behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> ENGINE_WOOD = BcLangKeys.blockItem(ITEMS, BcBlocks.ENGINE_WOOD);

    /** Placeholder for {@code buildcraftcore:engine_creative}; behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> ENGINE_CREATIVE = BcLangKeys.blockItem(ITEMS, BcBlocks.ENGINE_CREATIVE);

    /** Placeholder for {@code buildcraftcore:engine_iron}; behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> ENGINE_IRON = BcLangKeys.blockItem(ITEMS, BcBlocks.ENGINE_IRON);

    /** Placeholder for {@code buildcraftcore:engine_rf}; behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> ENGINE_RF = BcLangKeys.blockItem(ITEMS, BcBlocks.ENGINE_RF);

    /** Placeholder for {@code buildcraftcore:marker_volume}; behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> MARKER_VOLUME = BcLangKeys.blockItem(ITEMS, BcBlocks.MARKER_VOLUME);

    /** Placeholder for {@code buildcraftcore:marker_path}; behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> MARKER_PATH = BcLangKeys.blockItem(ITEMS, BcBlocks.MARKER_PATH);

    /** Placeholder for {@code buildcraftcore:power_tester}; behaviour class migrates in M2.5+. */
    public static final DeferredItem<BlockItem> POWER_TESTER = BcLangKeys.blockItem(ITEMS, BcBlocks.POWER_TESTER);

    // -------------------------------------------------------------------------
    // M2.4a registry parity: pure items (legacy BCCoreItems). Placeholder, real
    // behaviour classes migrate in M2.5+.
    // -------------------------------------------------------------------------

    /** Placeholder for {@code buildcraftcore:fragile_fluid_shard} (legacy {@code ItemFragileFluidShard}); migrates in M2.5+. */
    public static final DeferredItem<Item> FRAGILE_FLUID_SHARD = BcLangKeys.item(ITEMS, "fragile_fluid_shard");

    /** Placeholder for {@code buildcraftcore:gear_wood} (legacy {@code ItemGear}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> GEAR_WOOD = BcLangKeys.item(ITEMS, "gear_wood");

    /** Placeholder for {@code buildcraftcore:gear_stone} (legacy {@code ItemGear}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> GEAR_STONE = BcLangKeys.item(ITEMS, "gear_stone");

    /** Placeholder for {@code buildcraftcore:gear_iron} (legacy {@code ItemGear}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> GEAR_IRON = BcLangKeys.item(ITEMS, "gear_iron");

    /** Placeholder for {@code buildcraftcore:gear_gold} (legacy {@code ItemGear}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> GEAR_GOLD = BcLangKeys.item(ITEMS, "gear_gold");

    /** Placeholder for {@code buildcraftcore:gear_diamond} (legacy {@code ItemGear}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> GEAR_DIAMOND = BcLangKeys.item(ITEMS, "gear_diamond");

    /** Placeholder for {@code buildcraftcore:goggles} (legacy {@code ItemGoggles}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> GOGGLES = BcLangKeys.item(ITEMS, "goggles");

    /** Placeholder for {@code buildcraftcore:list} (legacy {@code ItemList}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> LIST = BcLangKeys.item(ITEMS, "list");

    /** Placeholder for {@code buildcraftcore:map_location} (legacy {@code ItemMapLocation}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> MAP_LOCATION = BcLangKeys.item(ITEMS, "map_location");

    /** Placeholder for {@code buildcraftcore:marker_connector} (legacy {@code ItemMarkerConnector}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> MARKER_CONNECTOR = BcLangKeys.item(ITEMS, "marker_connector");

    /** Placeholder for {@code buildcraftcore:paintbrush_black} (legacy {@code ItemPaintbrush}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PAINTBRUSH_BLACK = BcLangKeys.item(ITEMS, "paintbrush_black");

    /** Placeholder for {@code buildcraftcore:paintbrush_blue} (legacy {@code ItemPaintbrush}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PAINTBRUSH_BLUE = BcLangKeys.item(ITEMS, "paintbrush_blue");

    /** Placeholder for {@code buildcraftcore:paintbrush_brown} (legacy {@code ItemPaintbrush}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PAINTBRUSH_BROWN = BcLangKeys.item(ITEMS, "paintbrush_brown");

    /** Placeholder for {@code buildcraftcore:paintbrush_clean} (legacy {@code ItemPaintbrush}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PAINTBRUSH_CLEAN = BcLangKeys.item(ITEMS, "paintbrush_clean");

    /** Placeholder for {@code buildcraftcore:paintbrush_cyan} (legacy {@code ItemPaintbrush}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PAINTBRUSH_CYAN = BcLangKeys.item(ITEMS, "paintbrush_cyan");

    /** Placeholder for {@code buildcraftcore:paintbrush_gray} (legacy {@code ItemPaintbrush}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PAINTBRUSH_GRAY = BcLangKeys.item(ITEMS, "paintbrush_gray");

    /** Placeholder for {@code buildcraftcore:paintbrush_green} (legacy {@code ItemPaintbrush}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PAINTBRUSH_GREEN = BcLangKeys.item(ITEMS, "paintbrush_green");

    /** Placeholder for {@code buildcraftcore:paintbrush_light_blue} (legacy {@code ItemPaintbrush}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PAINTBRUSH_LIGHT_BLUE = BcLangKeys.item(ITEMS, "paintbrush_light_blue");

    /** Placeholder for {@code buildcraftcore:paintbrush_light_gray} (legacy {@code ItemPaintbrush}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PAINTBRUSH_LIGHT_GRAY = BcLangKeys.item(ITEMS, "paintbrush_light_gray");

    /** Placeholder for {@code buildcraftcore:paintbrush_lime} (legacy {@code ItemPaintbrush}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PAINTBRUSH_LIME = BcLangKeys.item(ITEMS, "paintbrush_lime");

    /** Placeholder for {@code buildcraftcore:paintbrush_magenta} (legacy {@code ItemPaintbrush}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PAINTBRUSH_MAGENTA = BcLangKeys.item(ITEMS, "paintbrush_magenta");

    /** Placeholder for {@code buildcraftcore:paintbrush_orange} (legacy {@code ItemPaintbrush}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PAINTBRUSH_ORANGE = BcLangKeys.item(ITEMS, "paintbrush_orange");

    /** Placeholder for {@code buildcraftcore:paintbrush_pink} (legacy {@code ItemPaintbrush}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PAINTBRUSH_PINK = BcLangKeys.item(ITEMS, "paintbrush_pink");

    /** Placeholder for {@code buildcraftcore:paintbrush_purple} (legacy {@code ItemPaintbrush}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PAINTBRUSH_PURPLE = BcLangKeys.item(ITEMS, "paintbrush_purple");

    /** Placeholder for {@code buildcraftcore:paintbrush_red} (legacy {@code ItemPaintbrush}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PAINTBRUSH_RED = BcLangKeys.item(ITEMS, "paintbrush_red");

    /** Placeholder for {@code buildcraftcore:paintbrush_white} (legacy {@code ItemPaintbrush}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PAINTBRUSH_WHITE = BcLangKeys.item(ITEMS, "paintbrush_white");

    /** Placeholder for {@code buildcraftcore:paintbrush_yellow} (legacy {@code ItemPaintbrush}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> PAINTBRUSH_YELLOW = BcLangKeys.item(ITEMS, "paintbrush_yellow");

    /** Placeholder for {@code buildcraftcore:volume_box} (legacy {@code ItemVolumeBox}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> VOLUME_BOX = BcLangKeys.item(ITEMS, "volume_box");

    /** Placeholder for {@code buildcraftcore:wrench} (legacy {@code ItemWrench}); behaviour class migrates in M2.5+. */
    public static final DeferredItem<Item> WRENCH = BcLangKeys.item(ITEMS, "wrench");

    private BcItems() {
    }
}
