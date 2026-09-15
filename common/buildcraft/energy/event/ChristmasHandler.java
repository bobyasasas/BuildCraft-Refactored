package buildcraft.energy.event;

import buildcraft.energy.BCEnergyConfig;
import buildcraft.energy.BCEnergyFluids;
import buildcraft.lib.fluid.BCFluid;
import buildcraft.lib.registry.TagManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.registries.RegistryObject;

import java.time.Month;
import java.time.MonthDay;

/** Used for automatically changing lang entries, fluid colours, and a few other things around christmas time. This is
 * in energy rather than lib because no other module does anything at christmas. */
public class ChristmasHandler {

    private static Boolean enabled;

    public static boolean isEnabled() {
        if (enabled == null) {
            throw new IllegalStateException("Unknown until init!");
        }
        return enabled;
    }

    public static final int[][] colours =
            {
                    { 0xC0_75_34, 0x5A_1D_0c },
                    { 0xD4_82_39, 0xD8_7D_33 },
                    { 0xD4_82_39, 0x5A_1D_0C },
                    { 0xD4_82_39, 0x30_0E_05 },
                    { 0xC0_75_34, 0x8a_3D_1C },
                    { 0x4F_33_2F, 0x30_0E_05 },
                    { 0x88_44_2D, 0x5A_1d_0C },
                    { 0x9B_61_39, 0x94_59_31 },
                    { 0xC0_75_34, 0xB3_68_2C },
                    { 0xD6_C9_90, 0xCF_BF_8E },
            };

    private static void fmlPreInit() {
        enabled = BCEnergyConfig.christmasEventStatus.isEnabled(MonthDay.of(Month.DECEMBER, 25));
        if (isEnabled()) {

            BCEnergyFluids.STILL_SUFFIX = BCEnergyFluids.STILL_SUFFIX + "_christmas";
            BCEnergyFluids.FLOW_SUFFIX = BCEnergyFluids.FLOW_SUFFIX + "_christmas";
            BCEnergyFluids.FLUID_TRANSLATION_PREFIX = "buildcraft.christmas." + BCEnergyFluids.FLUID_TRANSLATION_PREFIX;
            BCEnergyFluids.HEAT_TRANSLATION_PREFIX = "buildcraft.christmas." + BCEnergyFluids.HEAT_TRANSLATION_PREFIX;

            TagManager.getTag("block.engine.bc.iron")
                    .locale(
                            "buildcraft.christmas." +
                                    TagManager.getTag("block.engine.bc.iron").getSingleTag(TagManager.EnumTagType.UNLOCALIZED_NAME)
                    );

            // boilPoint -> no gas
            BCEnergyFluids.allowGas = false;
            for (int index = 0; index < BCEnergyFluids.data.length; index++) {
                changeData(index);
            }
        }
    }

    public static void fmlPreInitDedicatedServer() {
        fmlPreInit();
    }

    public static void fmlPreInitClient() {
        fmlPreInit();
    }

    private static void changeData(int fluidIndex) {
        // density
        BCEnergyFluids.data[fluidIndex][0] = -BCEnergyFluids.data[fluidIndex][0];
    }

    public static void regBucketNoFlipModel(ModelEvent.RegisterAdditional event) {
        if (isEnabled()) {
            for (RegistryObject<BCFluid.Source> fluid : BCEnergyFluids.allStill) {
                ResourceLocation bucketRegRL = fluid.get().getReg().getBucket().getRegistryName();
                String namespace = bucketRegRL.getNamespace();
                String normalPath = bucketRegRL.getPath();
                String christmasPath = normalPath + "_christmas";
                ResourceLocation christmasModelRL = new ResourceLocation(namespace, "item/" + christmasPath);
                event.register(christmasModelRL);
            }
        }
    }

    public static void replaceBucketNoFlipModel(ModelEvent.ModifyBakingResult event) {
        if (isEnabled()) {
            for (RegistryObject<BCFluid.Source> fluid : BCEnergyFluids.allStill) {
                ResourceLocation normalBucketRegRL = fluid.get().getReg().getBucket().getRegistryName();
                String namespace = normalBucketRegRL.getNamespace();
                String normalPath = normalBucketRegRL.getPath();
                String christmasPath = normalPath + "_christmas";
                ResourceLocation normalModelRL = new ResourceLocation(namespace, "item/" + normalPath);
                ResourceLocation christmasModelRL = new ResourceLocation(namespace, "item/" + christmasPath);
                event.getModels().replace(normalModelRL, event.getModels().get(christmasModelRL));
            }
        }
    }


////                    throw new ReflectiveOperationException(
////                    throw new ReflectiveOperationException(
////
////        // never cast to a Map<String, String> as a mod
////        // might change it with bytecode manipulation
////        // Fortunately we can just replace the entry ourselves,
////        // As Map.get() takes an object, not a generic value.

}
