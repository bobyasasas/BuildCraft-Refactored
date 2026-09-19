/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.silicon;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import buildcraft.lib.BcLangKeys;
import buildcraft.silicon.block.AdvancedCraftingTableBlock;
import buildcraft.silicon.block.AssemblyTableBlock;
import buildcraft.silicon.block.ChargingTableBlock;
import buildcraft.silicon.block.IntegrationTableBlock;
import buildcraft.silicon.block.LaserBlock;
import buildcraft.silicon.block.ProgrammingTableBlock;

/**
 * Central block registration for buildcraftsilicon (task M2.4a skeleton, registry parity since M2.4b). Every block
 * id the 1.20.1 registry baseline attributes to {@code buildcraftsilicon} registers here; since M4.16 all six
 * silicon machines carry their real behaviour classes (the M4.7 factory precedent) under the unchanged ids, so the
 * baseline blockstate palette stays byte-identical.
 */
public final class BcSiliconBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BuildCraftSilicon.MOD_ID);
    /** {@code buildcraftsilicon:advanced_crafting_table} (legacy {@code BlockLaserTable}); real class since M4.16 ({@link AdvancedCraftingTableBlock}). */
    public static final DeferredBlock<AdvancedCraftingTableBlock> ADVANCED_CRAFTING_TABLE = BcLangKeys.block(BLOCKS, "advanced_crafting_table",
            AdvancedCraftingTableBlock::new, properties -> properties.strength(0.5F));

    /** {@code buildcraftsilicon:assembly_table} (legacy {@code BlockLaserTable}); real class since M4.16 ({@link AssemblyTableBlock}). */
    public static final DeferredBlock<AssemblyTableBlock> ASSEMBLY_TABLE = BcLangKeys.block(BLOCKS, "assembly_table",
            AssemblyTableBlock::new, properties -> properties.strength(0.5F));

    /** {@code buildcraftsilicon:charging_table} (legacy {@code BlockLaserTable}); real class since M4.16 ({@link ChargingTableBlock}). */
    public static final DeferredBlock<ChargingTableBlock> CHARGING_TABLE = BcLangKeys.block(BLOCKS, "charging_table",
            ChargingTableBlock::new, properties -> properties.strength(0.5F));

    /** {@code buildcraftsilicon:integration_table} (legacy {@code BlockLaserTable}); real class since M4.16 ({@link IntegrationTableBlock}). */
    public static final DeferredBlock<IntegrationTableBlock> INTEGRATION_TABLE = BcLangKeys.block(BLOCKS, "integration_table",
            IntegrationTableBlock::new, properties -> properties.strength(0.5F));

    /** {@code buildcraftsilicon:laser} (legacy {@code BlockLaser}); real class since M4.16 ({@link LaserBlock}). */
    public static final DeferredBlock<LaserBlock> LASER = BcLangKeys.block(BLOCKS, "laser",
            LaserBlock::new, properties -> properties.strength(0.5F));

    /** {@code buildcraftsilicon:programming_table} (legacy {@code BlockLaserTable}); real class since M4.16 ({@link ProgrammingTableBlock}). */
    public static final DeferredBlock<ProgrammingTableBlock> PROGRAMMING_TABLE = BcLangKeys.block(BLOCKS, "programming_table",
            ProgrammingTableBlock::new, properties -> properties.strength(0.5F));


    private BcSiliconBlocks() {
    }
}
