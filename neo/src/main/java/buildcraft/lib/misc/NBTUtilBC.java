/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * Minimal M2.8 port of legacy {@code buildcraft.lib.misc.NBTUtilBC}: only the block-pos and boolean-array helpers
 * the oil field worldgen ({@code buildcraft.energy.generation.structure}) needs for its structure piece NBT. The
 * wire shape is the legacy one ({@code int[3]} array tags, list-of-int-list patterns). The full legacy utility
 * migrates with its consumers in later Phase 2/3 tasks.
 */
public final class NBTUtilBC {

    private NBTUtilBC() {}

    public static IntArrayTag writeBlockPos(BlockPos pos) {
        if (pos == null) {
            throw new NullPointerException("Cannot return a null NBTTag -- pos was null!");
        }
        return new IntArrayTag(new int[] { pos.getX(), pos.getY(), pos.getZ() });
    }

    public static BlockPos readBlockPos(Tag base) {
        if (base instanceof IntArrayTag intArray) {
            int[] array = intArray.getAsIntArray();
            if (array.length == 3) {
                return new BlockPos(array[0], array[1], array[2]);
            }
            return null;
        }
        return null;
    }

    public static ListTag writeBooleanArray(boolean[] data) {
        ListTag list = new ListTag();
        for (boolean d : data) {
            list.add(IntTag.valueOf(d ? 1 : 0));
        }
        return list;
    }

    public static boolean[] readBooleanArray(ListTag tag) {
        boolean[] arr = new boolean[tag.size()];
        for (int i = 0; i < tag.size(); i++) {
            arr[i] = tag.getIntOr(i, 0) != 0;
        }
        return arr;
    }
}
