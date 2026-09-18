/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.silicon;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import buildcraft.lib.BcLangKeys;

/**
 * Central block registration for buildcraftsilicon (task M2.4a skeleton, registry parity since M2.4b). Every block
 * id the 1.20.1 registry baseline attributes to {@code buildcraftsilicon} registers here as a plain placeholder
 * {@link Block}; the real behaviour classes (legacy {@code BCSiliconBlocks}) migrate in M2.5+.
 */
public final class BcSiliconBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BuildCraftSilicon.MOD_ID);
    /** Placeholder for {@code buildcraftsilicon:advanced_crafting_table} (legacy {@code BlockLaserTable}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> ADVANCED_CRAFTING_TABLE = BcLangKeys.simpleBlock(BLOCKS, "advanced_crafting_table",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftsilicon:assembly_table} (legacy {@code BlockLaserTable}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> ASSEMBLY_TABLE = BcLangKeys.simpleBlock(BLOCKS, "assembly_table",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftsilicon:charging_table} (legacy {@code BlockLaserTable}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> CHARGING_TABLE = BcLangKeys.simpleBlock(BLOCKS, "charging_table",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftsilicon:integration_table} (legacy {@code BlockLaserTable}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> INTEGRATION_TABLE = BcLangKeys.simpleBlock(BLOCKS, "integration_table",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftsilicon:laser} (legacy {@code BlockLaser}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> LASER = BcLangKeys.simpleBlock(BLOCKS, "laser",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftsilicon:programming_table} (legacy {@code BlockLaserTable}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> PROGRAMMING_TABLE = BcLangKeys.simpleBlock(BLOCKS, "programming_table",
            properties -> properties.strength(0.5F));


    private BcSiliconBlocks() {
    }
}
