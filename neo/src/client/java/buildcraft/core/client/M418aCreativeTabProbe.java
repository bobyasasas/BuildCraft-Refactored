/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.WorldDataConfiguration;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import buildcraft.builders.BcBuildersItems;
import buildcraft.core.BcCreativeTabs;
import buildcraft.core.BcItems;
import buildcraft.core.BuildCraftCore;
import buildcraft.energy.BcEnergyItems;
import buildcraft.factory.BcFactoryItems;
import buildcraft.lib.BcLibItems;
import buildcraft.robotics.BcRoboticsItems;
import buildcraft.silicon.BcSiliconItems;
import buildcraft.transport.BcTransportItems;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;

/**
 * M4.18a in-game evidence rig (the {@link buildcraft.factory.client.M47MachineSmokeProbe} pattern, one milestone
 * later): a user on real hardware reported "jei和创造模式栏位只有很少的一点物品" — the creative tab streamed only the
 * buildcraftcore register instead of every module's. The fix aggregates all module registers in
 * {@link BcCreativeTabs#MAIN}; this rig proves the tab (and JEI's view of the same items) now carries the full set:
 * <ol>
 * <li>programmatic count reconciliation, straight from the running game: every module register's entry count, the
 * creative tab's built display list, and the creative screen's live menu item list must all agree (and every
 * registered id must appear in the tab);</li>
 * <li>the creative inventory opened on the BuildCraft tab, screenshotted page by page while scrolling through all
 * rows (first/last visible item logged per page);</li>
 * <li>JEI's ingredient list filtered to BuildCraft ({@code @BuildCraft}, then {@code buildcraft}), with the runtime
 * filtered-stack counts logged and the overlay screenshotted — the exact check the user's report was about.</li>
 * </ol>
 *
 * <p>Everything is inert unless the run passes {@code -Dbuildcraft.m418aprobe=true} (dev-run evidence only, never
 * active in normal play; the nested {@link JeiRuntimeCapture} JEI plugin only stores a reference). Evidence = the
 * {@code [M418]} log lines + the screenshots under {@code screenshots/} in the run's game directory.
 */
@EventBusSubscriber(modid = BuildCraftCore.MOD_ID, value = Dist.CLIENT)
public final class M418aCreativeTabProbe {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Set (and only set) by the M4.18a evidence run's JVM arguments; keeps the rig inert everywhere else. */
    private static final boolean ENABLED = Boolean.getBoolean("buildcraft.m418aprobe");

    /** Rows visible per creative-list page (vanilla {@code NUM_COLS=9} x {@code NUM_ROWS=5}). */
    private static final int ROWS_PER_PAGE = 5;

    /** Final paging state: 5-row steps from state 2 up to here, then one clamped bottom shot. */
    private static final int PAGE_STATES = 22;

    private static int state = 0;
    private static int wait = 0;
    /** Page screenshots taken so far (drives the m418a_tab_pageN names). */
    private static int page = 0;
    /** Set once the rig asked the game to create its own quickPlay-style world (quickPlay cannot create worlds). */
    private static boolean worldRequested = false;

    /** JEI's runtime, stored by {@link JeiRuntimeCapture}; null whenever JEI is absent. */
    static volatile IJeiRuntime jeiRuntime;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            // quickPlay cannot create worlds (it only loads existing ids), so the rig creates its own creative world
            // as soon as the game shows any UI (title screen or the "Failed to Quick Play" notice).
            if (!worldRequested && mc.screen != null) {
                worldRequested = true;
                LOGGER.info("[M418] creating fresh creative world 'm418a' (quickPlay cannot create worlds)");
                mc.createWorldOpenFlows().createFreshLevel("m418a",
                    new LevelSettings("m418a", GameType.CREATIVE, LevelSettings.DifficultySettings.DEFAULT, false,
                        WorldDataConfiguration.DEFAULT),
                    new WorldOptions(418_418L, true, false),
                    WorldPresets::createNormalWorldDimensions,
                    null);
            }
            return;
        }
        if (mc.player == null || mc.getSingleplayerServer() == null) {
            return;
        }
        if (--wait > 0) {
            return;
        }
        switch (state) {
            case 0 -> {
                logRegisterCounts();
                mc.options.hideGui = false;
                boolean selected = setStaticSelectedTab(BcCreativeTabs.MAIN.value());
                mc.setScreen(new CreativeModeInventoryScreen(mc.player, mc.level.enabledFeatures(), false));
                LOGGER.info("[M418] creative screen opened on buildcraftcore:main (selectedTab-set={})", selected);
                wait = 60;
            }
            case 1 -> {
                logReconciliation(mc);
                page = 1;
                screenshot(mc, "m418a_tab_page" + page);
                logVisiblePage(mc);
                scroll(mc, ROWS_PER_PAGE);
                wait = 20;
            }
            default -> {
                // paging states: 2..PAGE_STATES shoot every 5-row window, state PAGE_STATES also clamps to the
                // bottom of the list; then the two JEI filter scenes.
                if (state >= 2 && state <= PAGE_STATES) {
                    page = state;
                    screenshot(mc, "m418a_tab_page" + page);
                    logVisiblePage(mc);
                    boolean lastTabPage = state == PAGE_STATES;
                    scroll(mc, lastTabPage ? 100 : ROWS_PER_PAGE);
                    if (lastTabPage) {
                        LOGGER.info("[M418] creative tab pages captured: {}", page);
                        if (jeiRuntime == null) {
                            LOGGER.error("[M418] JEI runtime NOT captured — is -PbcCompatDev set? (screenshots will lack the JEI panel)");
                        } else {
                            jeiRuntime.getIngredientFilter().setFilterText("@BuildCraft");
                        }
                        wait = 40;
                    } else {
                        wait = 20;
                    }
                } else if (state == PAGE_STATES + 1) {
                    logJeiCounts("@BuildCraft");
                    screenshot(mc, "m418a_jei_atBuildCraft");
                    if (jeiRuntime != null) {
                        jeiRuntime.getIngredientFilter().setFilterText("buildcraft");
                    }
                    wait = 40;
                } else if (state == PAGE_STATES + 2) {
                    logJeiCounts("buildcraft");
                    screenshot(mc, "m418a_jei_buildcraft");
                    LOGGER.info("[M418] rig done - all evidence logged (game will be killed externally per M4.16 discipline)");
                    state = 90;
                    return;
                } else {
                    return;
                }
            }
        }
        state++;
        wait = Math.max(wait, 30);
    }

    // ---------------------------------------------------------------- counts

    /** One line per module register: how many items each {@code DeferredRegister.Items} holds. */
    private static void logRegisterCounts() {
        Map<String, Integer> counts = registerCounts();
        counts.forEach((name, count) -> LOGGER.info("[M418] register {}: {} entries", name, count));
        LOGGER.info("[M418] register entries SUM: {}", counts.values().stream().mapToInt(Integer::intValue).sum());
    }

    private static Map<String, Integer> registerCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("buildcraftcore/BcItems", BcItems.ITEMS.getEntries().size());
        counts.put("buildcraftbuilders/BcBuildersItems", BcBuildersItems.ITEMS.getEntries().size());
        counts.put("buildcraftfactory/BcFactoryItems", BcFactoryItems.ITEMS.getEntries().size());
        counts.put("buildcraftenergy/BcEnergyItems", BcEnergyItems.ITEMS.getEntries().size());
        counts.put("buildcraftsilicon/BcSiliconItems", BcSiliconItems.ITEMS.getEntries().size());
        counts.put("buildcrafttransport/BcTransportItems", BcTransportItems.ITEMS.getEntries().size());
        counts.put("buildcraftrobotics/BcRoboticsItems", BcRoboticsItems.ITEMS.getEntries().size());
        counts.put("buildcraftlib/BcLibItems", BcLibItems.ITEMS.getEntries().size());
        return counts;
    }

    /**
     * The hard reconciliation, all from the live game: the tab's built display list vs the sum of every register's
     * entries vs the creative screen's own menu list, plus an id-level diff (every registered id must be in the tab).
     */
    private static void logReconciliation(Minecraft mc) {
        Map<String, Integer> counts = registerCounts();
        int sum = counts.values().stream().mapToInt(Integer::intValue).sum();

        CreativeModeTab tab = BcCreativeTabs.MAIN.value();
        List<ItemStack> display = List.copyOf(tab.getDisplayItems());
        LOGGER.info("[M418] MAIN tab getDisplayItems: {} items", display.size());

        int menuSize = -1;
        if (mc.player.containerMenu instanceof CreativeModeInventoryScreen.ItemPickerMenu menu) {
            menuSize = menu.items.size();
            LOGGER.info("[M418] creative screen menu items: {} items", menuSize);
        }
        LOGGER.info("[M418] RECONCILIATION: registers sum {} vs tab {} vs menu {} -> {}", sum, display.size(),
            menuSize, sum == display.size() && (menuSize < 0 || menuSize == sum) ? "PASS" : "FAIL");

        Set<String> registeredIds = new LinkedHashSet<>();
        BcItems.ITEMS.getEntries().forEach(e -> registeredIds.add(id(e.value())));
        BcBuildersItems.ITEMS.getEntries().forEach(e -> registeredIds.add(id(e.value())));
        BcFactoryItems.ITEMS.getEntries().forEach(e -> registeredIds.add(id(e.value())));
        BcEnergyItems.ITEMS.getEntries().forEach(e -> registeredIds.add(id(e.value())));
        BcSiliconItems.ITEMS.getEntries().forEach(e -> registeredIds.add(id(e.value())));
        BcTransportItems.ITEMS.getEntries().forEach(e -> registeredIds.add(id(e.value())));
        BcRoboticsItems.ITEMS.getEntries().forEach(e -> registeredIds.add(id(e.value())));
        BcLibItems.ITEMS.getEntries().forEach(e -> registeredIds.add(id(e.value())));

        Set<String> tabIds = new LinkedHashSet<>();
        display.forEach(stack -> tabIds.add(id(stack.getItem())));
        Set<String> missing = new LinkedHashSet<>(registeredIds);
        missing.removeAll(tabIds);
        Set<String> extra = new LinkedHashSet<>(tabIds);
        extra.removeAll(registeredIds);
        LOGGER.info("[M418] id diff: registered distinct {} vs tab distinct {} — missing {} extra {} -> {}",//
            registeredIds.size(), tabIds.size(), missing, extra,
            missing.isEmpty() && extra.isEmpty() ? "PASS" : "FAIL");
        LOGGER.info("[M418] full tab list ({}): {}", tabIds.size(), String.join(", ", tabIds));
    }

    private static String id(ItemStack stack) {
        return id(stack.getItem());
    }

    private static String id(net.minecraft.world.item.Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    // ---------------------------------------------------------------- paging

    /** Scrolls the creative list down by {@code rows} rows through the screen's real scroll path. */
    private static void scroll(Minecraft mc, int rows) {
        if (mc.screen instanceof CreativeModeInventoryScreen screen) {
            screen.mouseScrolled(0.0, 0.0, 0.0, -rows);
        }
    }

    /** First + last visible grid item of the current page (proof each screenshot's window really moved). */
    private static void logVisiblePage(Minecraft mc) {
        if (!(mc.player.containerMenu instanceof CreativeModeInventoryScreen.ItemPickerMenu menu)) {
            LOGGER.error("[M418] no ItemPickerMenu while paging");
            return;
        }
        String first = null;
        String last = null;
        List<String> visible = new ArrayList<>();
        for (int i = 0; i < Math.min(45, menu.slots.size()); i++) {
            ItemStack stack = menu.slots.get(i).getItem();
            if (!stack.isEmpty()) {
                String id = id(stack);
                visible.add(id);
                if (first == null) {
                    first = id;
                }
                last = id;
            }
        }
        LOGGER.info("[M418] page {}: firstVisible={} lastVisible={} visibleCount={}", page + 1, first, last,
            visible.size());
    }

    // ---------------------------------------------------------------- JEI

    private static void logJeiCounts(String filter) {
        IJeiRuntime runtime = jeiRuntime;
        if (runtime == null) {
            return;
        }
        int filteredAll = runtime.getIngredientFilter().getFilteredItemStacks().size();
        long filteredBc = runtime.getIngredientFilter().getFilteredItemStacks().stream()//
            .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace())//
            .filter(namespace -> namespace.startsWith("buildcraft"))//
            .count();
        int allItems = runtime.getIngredientManager().getAllItemStacks().size();
        LOGGER.info("[M418] JEI filter '{}': filteredItems={} buildcraftOfThem={} jeiAllItems={}", filter,
            filteredAll, filteredBc, allItems);
    }

    /**
     * JEI hands its runtime to every {@code @JeiPlugin} class it scans; this nested capture exists only so the rig
     * can read the ingredient filter the way a player's search box would. Storing the reference is inert — nothing
     * else is registered, and the rig itself is {@link #ENABLED}-gated.
     */
    @JeiPlugin
    public static final class JeiRuntimeCapture implements IModPlugin {

        @Override
        public Identifier getPluginUid() {
            return Identifier.fromNamespaceAndPath(BuildCraftCore.MOD_ID, "m418a_probe");
        }

        @Override
        public void onRuntimeAvailable(IJeiRuntime runtime) {
            jeiRuntime = runtime;
        }
    }

    // ---------------------------------------------------------------- shared helpers

    /**
     * Points the creative screen's static {@code selectedTab} at the BuildCraft tab (M49MatrixProbe's helper) so the
     * next {@link CreativeModeInventoryScreen} opens on it.
     */
    private static boolean setStaticSelectedTab(CreativeModeTab tab) {
        try {
            java.lang.reflect.Field field = CreativeModeInventoryScreen.class.getDeclaredField("selectedTab");
            try {
                field.setAccessible(true);
                field.set(null, tab);
                return true;
            } catch (Throwable reflectionFailure) {
                sun.misc.Unsafe unsafe = unsafeInstance();
                Object base = unsafe.staticFieldBase(field);
                long offset = unsafe.staticFieldOffset(field);
                unsafe.putObject(base, offset, tab);
                return true;
            }
        } catch (Throwable failure) {
            LOGGER.error("[M418] could not select the buildcraft creative tab: {}", failure.toString());
            return false;
        }
    }

    private static sun.misc.Unsafe unsafeInstance() throws Exception {
        java.lang.reflect.Field theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        return (sun.misc.Unsafe) theUnsafe.get(null);
    }

    private static void screenshot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), 1,
            (Component component) -> LOGGER.info("[M418] screenshot {}: {}", name, component.getString()));
    }

    private M418aCreativeTabProbe() {
    }
}
