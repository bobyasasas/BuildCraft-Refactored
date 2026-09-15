package buildcraft.energy.generation.biome;

import buildcraft.energy.BCEnergy;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class BCBiomeRegistry {
    public static String BIOME_OIL_OCEAN = "oil_ocean";
    public static String BIOME_OIL_DESERT = "oil_desert";

    public static final DeferredRegister<Biome> BIOMES = DeferredRegister.create(ForgeRegistries.BIOMES, BCEnergy.MODID);


    public static final ResourceLocation RL_BIOME_OIL_DESERT = new ResourceLocation(BCEnergy.MODID, BIOME_OIL_DESERT);
    public static final ResourceLocation RL_BIOME_OIL_OCEAN = new ResourceLocation(BCEnergy.MODID, BIOME_OIL_OCEAN);
    public static final ResourceKey<Biome> RESOURCE_KEY_BIOME_OIL_DESERT = ResourceKey.create(Registries.BIOME, RL_BIOME_OIL_DESERT);
    public static final ResourceKey<Biome> RESOURCE_KEY_BIOME_OIL_OCEAN = ResourceKey.create(Registries.BIOME, RL_BIOME_OIL_OCEAN);



    // static
    public static void init() {
//        // 海洋油田
////            BiomeDictionary.addTypes(
////                    RESOURCE_KEY_BIOME_OIL_OCEAN,
////                    BiomeDictionary.Type.OCEAN
//        // 沙漠油田
////            BiomeDictionary.addTypes(
////                    RESOURCE_KEY_BIOME_OIL_DESERT,
////                    BiomeDictionary.Type.HOT,
////                    BiomeDictionary.Type.DRY,
////                    BiomeDictionary.Type.SANDY
    }
}
