package buildcraft.core;

import buildcraft.core.client.CoreItemModelPredicates;
import buildcraft.core.marker.PathCache;
import buildcraft.core.marker.VolumeCache;
import buildcraft.lib.BCLibItems;
import buildcraft.lib.BCLibRegistries;
import buildcraft.lib.marker.MarkerCache;
import buildcraft.lib.registry.CreativeTabManager;
import buildcraft.lib.registry.TagManager;
import buildcraft.lib.registry.TagManager.EnumTagType;
import buildcraft.lib.registry.TagManager.TagEntry;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.registries.RegisterEvent;

import java.util.function.Consumer;

//@formatter:off
//@Mod(
//        modid = BCCore.MODID,
//        name = "BuildCraft Core",
//        version = BCLib.VERSION,
//        updateJSON = "https://mod-buildcraft.com/version/versions.json",
//        dependencies = "required-after:buildcraftlib@[" + BCLib.VERSION + "]",
//        guiFactory = "buildcraft.core.client.ConfigGuiFactoryBC"
//@formatter:on
@Mod(BCCore.MODID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class BCCore {
    public static final String MODID = "buildcraftcore";
    public static final String MOD_VERSION = ModList.get().getModContainerById(MODID).get().getModInfo().getVersion().toString();

    public static BCCore INSTANCE = null;

    public static CreativeTabManager.CreativeTabBC mainTab;

    static {
        BCLibItems.enableGuide();
        BCLibItems.enableDebugger();
    }

    public BCCore() {
        INSTANCE = this;
    }

    @SubscribeEvent
    public static void preInit(FMLConstructModEvent event) {
        BCLibRegistries.fmlPreInit(); // this should be called in BCLib#<clinit> before BCTransport#preInit called, but sometimes the order is incorrect?

        BCCoreConfig.clinit();

        mainTab = CreativeTabManager.createTab("buildcraft.main");

        BCCoreBlocks.preInit();
        BCCoreItems.preInit();
        BCCoreStatements.preInit();
//        BCCoreRecipes.fmlPreInit(); // 1.18.2: use datagen

        BCCoreProxy.getProxy().fmlPreInit();

        mainTab.setItem(BCCoreItems.wrench);

        // 1.18.2: the item object not created yet


//        OreDictionary.registerOre("craftingTableWood", Blocks.CRAFTING_TABLE); // 1.18.2: use datagen
        MinecraftForge.EVENT_BUS.register(BCCoreEventDist.INSTANCE);
    }

    @SubscribeEvent
    public static void init(FMLCommonSetupEvent event) {

        BCCoreProxy.getProxy().fmlInit();

        MarkerCache.registerCache(VolumeCache.INSTANCE);
        MarkerCache.registerCache(PathCache.INSTANCE);
    }

    @SubscribeEvent
    public static void postInit(FMLLoadCompleteEvent event) {
        BCCoreConfig.saveCoreConfigs();
        BCCoreConfig.saveObjConfigs();
        BCCoreProxy.getProxy().fmlPostInit();
        BCCoreConfig.postInit();
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        ItemBlockRenderTypes.setRenderLayer(BCCoreBlocks.markerVolume.get(), RenderType.cutout());
        CoreItemModelPredicates.register(event);
    }

    @SubscribeEvent
    public static void onRegisterEvent(RegisterEvent event) {
        ResourceKey<? extends Registry<?>> registry = event.getRegistryKey();
        if (registry == Registries.CREATIVE_MODE_TAB) {
            // Creative Tab
            Registry.register(event.getVanillaRegistry(), mainTab.getId(), mainTab);
        } else if (registry == Registries.BLOCK) {
            // GUI
            BCCoreMenuTypes.registerAll();
        }
    }

    // java.util.NoSuchElementException
    private static final TagManager tagManager = new TagManager();

    static {
        startBatch();
        // Items
        registerTag("item.wrench").reg("wrench").locale("wrenchItem");
        registerTag("item.diamond_shard").reg("diamond_shard").locale("diamondShard").tab("vanilla.materials");
        registerTag("item.gear.wood").reg("gear_wood").locale("woodenGearItem").oreDict("gearWood");
        registerTag("item.gear.stone").reg("gear_stone").locale("stoneGearItem").oreDict("gearStone");
        registerTag("item.gear.iron").reg("gear_iron").locale("ironGearItem").oreDict("gearIron");
        registerTag("item.gear.gold").reg("gear_gold").locale("goldGearItem").oreDict("gearGold");
        registerTag("item.gear.diamond").reg("gear_diamond").locale("diamondGearItem").oreDict("gearDiamond");
        registerTag("item.list").reg("list").locale("list");
        registerTag("item.map_location").reg("map_location").locale("mapLocation");
        registerTag("item.paintbrush.clean").reg("paintbrush_clean").locale("paintbrush");
        registerTag("item.paintbrush.white").reg("paintbrush_white").locale("paintbrush");
        registerTag("item.paintbrush.orange").reg("paintbrush_orange").locale("paintbrush");
        registerTag("item.paintbrush.magenta").reg("paintbrush_magenta").locale("paintbrush");
        registerTag("item.paintbrush.light_blue").reg("paintbrush_light_blue").locale("paintbrush");
        registerTag("item.paintbrush.yellow").reg("paintbrush_yellow").locale("paintbrush");
        registerTag("item.paintbrush.lime").reg("paintbrush_lime").locale("paintbrush");
        registerTag("item.paintbrush.pink").reg("paintbrush_pink").locale("paintbrush");
        registerTag("item.paintbrush.light_gray").reg("paintbrush_light_gray").locale("paintbrush");
        registerTag("item.paintbrush.cyan").reg("paintbrush_cyan").locale("paintbrush");
        registerTag("item.paintbrush.purple").reg("paintbrush_purple").locale("paintbrush");
        registerTag("item.paintbrush.blue").reg("paintbrush_blue").locale("paintbrush");
        registerTag("item.paintbrush.brown").reg("paintbrush_brown").locale("paintbrush");
        registerTag("item.paintbrush.green").reg("paintbrush_green").locale("paintbrush");
        registerTag("item.paintbrush.red").reg("paintbrush_red").locale("paintbrush");
        registerTag("item.paintbrush.black").reg("paintbrush_black").locale("paintbrush");
        registerTag("item.paintbrush.gray").reg("paintbrush_gray").locale("paintbrush");
        registerTag("item.marker_connector").reg("marker_connector").locale("markerConnector");
        registerTag("item.volume_box").reg("volume_box").locale("volume_box");
        registerTag("item.goggles").reg("goggles").locale("goggles");
        registerTag("item.fragile_fluid_shard").reg("fragile_fluid_shard").locale("fragile_fluid_shard");
        // Item Blocks
        registerTag("item.block.marker.volume").reg("marker_volume").locale("markerBlock");
        registerTag("item.block.marker.path").reg("marker_path").locale("pathMarkerBlock");
        registerTag("item.block.spring.water").reg("spring_water").locale("spring.water");
        registerTag("item.block.spring.oil").reg("spring_oil").locale("spring.oil");
        registerTag("item.block.power_tester").reg("power_tester").locale("power_tester");
        registerTag("item.block.decorated").reg("decorated").locale("decorated");
        registerTag("item.block.engine.bc.wood").reg("engine_wood").locale("engineWood");
        registerTag("item.block.engine.bc.creative").reg("engine_creative").locale("engineCreative");
        // Blocks
        registerTag("block.spring.water").reg("spring_water").locale("spring.oil");
        registerTag("block.spring.oil").reg("spring_oil").locale("spring.oil");
        registerTag("block.decorated").reg("decorated").locale("decorated");
        registerTag("block.engine.bc.wood").reg("engine_wood").locale("engineWood");
        registerTag("block.engine.bc.creative").reg("engine_creative").locale("engineCreative");
        registerTag("block.engine.bc.rf").reg("engine_rf").locale("engineRf");
        registerTag("block.marker.volume").reg("marker_volume").locale("markerBlock");
        registerTag("block.marker.path").reg("marker_path").locale("pathMarkerBlock");
        registerTag("block.power_tester").reg("power_tester").locale("power_tester");
        // Tiles
        registerTag("tile.marker.volume").reg("marker_volume");
        registerTag("tile.marker.path").reg("marker_path");
        registerTag("tile.engine.wood").reg("engine_wood");
        registerTag("tile.engine.creative").reg("engine_creative");
        registerTag("tile.power_tester").reg("power_tester");

        endBatch(TagManager.prependTags("buildcraftcore:", EnumTagType.REGISTRY_NAME).andThen(TagManager.setTab("buildcraft.main")));
//        engine.model("");// Clear model so that subtypes can set it properly
    }


    private static TagEntry registerTag(String id) {
        return tagManager.registerTag(id);
    }

    private static void startBatch() {
        tagManager.startBatch();
    }

    private static void endBatch(Consumer<TagEntry> consumer) {
        tagManager.endBatch(consumer);
    }
}
