/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core;

import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central item registration for buildcraftcore (task M2.2a). Every item this mod registers gets a constant
 * {@link DeferredItem} field here, registered through the single {@link #ITEMS} holder on the mod event bus (see
 * {@link BuildCraftCore#BuildCraftCore(net.neoforged.bus.api.IEventBus)}). Block items reference their block from
 * {@link BcBlocks}, mirroring the layout of the M2.4 legacy migration.
 */
public final class BcItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BuildCraftCore.MOD_ID);

    /** Item form of the {@link BcBlocks#MARKER} placeholder block. */
    public static final DeferredItem<BlockItem> MARKER = ITEMS.registerSimpleBlockItem(BcBlocks.MARKER);

    /** Item form of the {@link BcBlocks#ENGINE_STONE} slice block (M2.2b). */
    public static final DeferredItem<BlockItem> ENGINE_STONE = ITEMS.registerSimpleBlockItem(BcBlocks.ENGINE_STONE);

    /** Item form of the {@link BcBlocks#PIPE_KINESIS_WOOD} slice block (M2.2c). */
    public static final DeferredItem<BlockItem> PIPE_KINESIS_WOOD = ITEMS.registerSimpleBlockItem(BcBlocks.PIPE_KINESIS_WOOD);

    /** Item form of the {@link BcBlocks#ENERGY_METER} slice-only measurement block (M2.2c). */
    public static final DeferredItem<BlockItem> ENERGY_METER = ITEMS.registerSimpleBlockItem(BcBlocks.ENERGY_METER);

    private BcItems() {
    }
}
