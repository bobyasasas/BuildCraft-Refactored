/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders.blueprint;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import buildcraft.builders.BcBuildersItems;

/**
 * The bridge between {@link BlueprintData} and the blueprint item stacks (M4.17 C route): the data rides the vanilla
 * {@code minecraft:custom_data} component under {@link BlueprintData#NBT_KEY} &mdash; the hard zero-registry constraint
 * (no new {@code DataComponentType}, the plain {@code BcBuildersItems#SNAPSHOT_BLUEPRINT}/{@code #SNAPSHOT_TEMPLATE}
 * placeholder items carry the data). Legacy counterpart: {@code ItemSnapshot#getHeader}/{@code #getUsed}, which stored
 * only a hash {@code Header} and resolved the payload from world saved data; v1 is self-contained instead.
 *
 * <p>Not unit tested: {@code DataComponents} initializes through the registry bootstrap (the {@code BlueprintData}
 * javadoc), so only this thin class touches it.
 */
public final class BlueprintItems {

    private BlueprintItems() {
    }

    /** True for the two blueprint items the baseline registers ({@code snapshot_blueprint}/{@code snapshot_template};
     * v1 treats both identically &mdash; the template's air-only variant is a legacy distinction not cut back in yet). */
    public static boolean isBlueprintItem(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(BcBuildersItems.SNAPSHOT_BLUEPRINT.get())
            || stack.is(BcBuildersItems.SNAPSHOT_TEMPLATE.get()));
    }

    /** True when the stack carries a readable {@link BlueprintData} payload ("used" blueprint, legacy wording). */
    public static boolean hasData(ItemStack stack) {
        return readData(stack) != null;
    }

    /** Writes {@code data} into the stack's custom_data component (replacing any previous payload). */
    public static void writeData(ItemStack stack, BlueprintData data) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
            customData -> data.writeTo(putCompound(customData)));
    }

    /** The payload behind the stack, or null when the item is not a blueprint or carries no (valid) data. */
    public static @Nullable BlueprintData readData(ItemStack stack) {
        if (stack.isEmpty() || !isBlueprintItem(stack)) {
            return null;
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || !customData.contains(BlueprintData.NBT_KEY)) {
            return null;
        }
        return BlueprintData.from(customData.copyTag().getCompoundOrEmpty(BlueprintData.NBT_KEY));
    }

    /** {@code CompoundTag#put} returns the previous tag, so the plain put needs this one wrapper to stay an expression. */
    private static CompoundTag putCompound(CompoundTag tag) {
        CompoundTag child = new CompoundTag();
        tag.put(BlueprintData.NBT_KEY, child);
        return child;
    }
}
