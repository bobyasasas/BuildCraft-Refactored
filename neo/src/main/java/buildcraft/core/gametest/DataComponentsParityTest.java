/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import buildcraft.lib.datacomponent.BcDataComponents;
import buildcraft.lib.datacomponent.filter.BcFilter;
import buildcraft.lib.datacomponent.filter.BcFilterNbt;
import buildcraft.lib.datacomponent.filter.BcFilterSpec;
import buildcraft.lib.datacomponent.filter.BcItemStack;
import buildcraft.lib.datacomponent.gate.BcGateConfig;
import buildcraft.lib.datacomponent.gate.BcGateNbt;
import buildcraft.lib.datacomponent.gate.BcGateParam;
import buildcraft.lib.datacomponent.gate.BcGateSlot;
import buildcraft.lib.datacomponent.gate.BcGateStatement;
import buildcraft.lib.datacomponent.gate.EnumGateLogic;
import buildcraft.lib.datacomponent.gate.EnumGateMaterial;
import buildcraft.lib.datacomponent.gate.EnumGateModifier;
import buildcraft.lib.datacomponent.gate.GateVariantData;
import buildcraft.lib.datacomponent.robot.BcRobotParams;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * M2.6 data component parity gate (test id {@code buildcraftcore:data_components_parity}, runs on the headless
 * gametest server like {@link RegistryParityTest}). Asserts, in the live runtime:
 * <ol>
 * <li>the three M2.6 component types are registered under their legacy-carrier ids;</li>
 * <li>an {@link ItemStack} survives a full persistence round trip through the vanilla component codec with the
 * component compound byte-identical to the legacy NBT shape (the conversion classes back the persistent codec, see
 * {@code BcGateNbt#CODEC} etc.), and parsing an ItemStack carrying the legacy compound reproduces the component
 * value.</li>
 * </ol>
 */
public final class DataComponentsParityTest {

    private DataComponentsParityTest() {}

    public static void run(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();

        checkRegistered(Identifier.parse("buildcraftlib:filter"), problems);
        checkRegistered(Identifier.parse("buildcraftlib:gate_config"), problems);
        checkRegistered(Identifier.parse("buildcraftrobotics:robot_params"), problems);
        if (!problems.isEmpty()) {
            helper.fail("data component registration violated: " + String.join("; ", problems));
            return;
        }

        RegistryOps<Tag> ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);

        // gate config: set/get round trip, then persistence round trip with the exact legacy gate_data compound
        BcGateConfig config = buildGateConfig();
        ItemStack stack = new ItemStack(Items.STONE);
        stack.set(BcDataComponents.GATE_CONFIG.get(), config);
        if (!config.equals(stack.get(BcDataComponents.GATE_CONFIG.get()))) {
            helper.fail("gate config component set/get round trip diverged");
            return;
        }
        CompoundTag stored = persistedCompound(ops, stack, "buildcraftlib:gate_config");
        CompoundTag legacyShape = BcGateNbt.write(config);
        if (!legacyShape.equals(stored)) {
            helper.fail("persisted gate config compound diverged from legacy shape: " + stored + " vs " + legacyShape);
            return;
        }
        ItemStack parsed = ItemStack.CODEC.parse(ops, stackEncode(ops, stack)).getOrThrow();
        if (!config.equals(parsed.get(BcDataComponents.GATE_CONFIG.get()))) {
            helper.fail("gate config ItemStack parse round trip diverged");
            return;
        }

        // filter and robot params: one-shot persistence shape checks
        BcFilter filter = new BcFilter(List.of(new BcFilterSpec.StackFilter(BcItemStack.of("minecraft:stone", 16))));
        assertPersistedShape(helper, ops, new ItemStack(Items.STONE), BcDataComponents.FILTER.get(), filter,
            "buildcraftlib:filter", BcFilterNbt::write);

        BcRobotParams robot = new BcRobotParams(1_000_000L);
        assertPersistedShape(helper, ops, new ItemStack(Items.STONE), BcDataComponents.ROBOT_PARAMS.get(), robot,
            "buildcraftrobotics:robot_params", BcRobotParams::writeToNbt);

        helper.succeed();
    }

    private static void checkRegistered(Identifier id, List<String> problems) {
        if (!BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(id)) {
            problems.add(id + " missing from DATA_COMPONENT_TYPE");
        }
    }

    private static Tag stackEncode(RegistryOps<Tag> ops, ItemStack stack) {
        return ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
    }

    private static CompoundTag persistedCompound(RegistryOps<Tag> ops, ItemStack stack, String componentKey) {
        CompoundTag root = (CompoundTag) stackEncode(ops, stack);
        return root.getCompoundOrEmpty("components").getCompoundOrEmpty(componentKey);
    }

    private static <T> void assertPersistedShape(GameTestHelper helper, RegistryOps<Tag> ops, ItemStack stack,
        DataComponentType<T> type, T value, String componentKey, Function<T, CompoundTag> legacy) {
        stack.set(type, value);
        CompoundTag stored = persistedCompound(ops, stack, componentKey);
        if (!legacy.apply(value).equals(stored)) {
            helper.fail("persisted " + componentKey + " compound diverged from legacy shape: " + stored);
        }
    }

    /** A small real gate config: two-slot iron OR gate (material 2 slots / LAPIS divisor 1) with one configured
     * trigger/action pair and a wire colour parameter. */
    private static BcGateConfig buildGateConfig() {
        GateVariantData variant = new GateVariantData(EnumGateLogic.OR, EnumGateMaterial.IRON,
            EnumGateModifier.LAPIS);
        BcGateStatement trigger = new BcGateStatement("buildcraft:redstone.input.active", (byte) 2);
        BcGateStatement action = new BcGateStatement("buildcraft:redstone.output", BcGateStatement.SIDE_CENTER,
            Map.of(0, new BcGateParam.DirectionParam((byte) 1)));
        List<BcGateSlot> slots = List.of(
            new BcGateSlot(Optional.of(trigger), Optional.empty()),
            new BcGateSlot(Optional.empty(), Optional.of(action)));
        return new BcGateConfig(variant, (short) 1, slots, (short) 1, (short) 0);
    }
}
