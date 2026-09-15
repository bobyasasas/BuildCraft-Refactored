/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central block registration for buildcraftfactory (task M2.4a skeleton, registry parity since M2.4b). Every block
 * id the 1.20.1 registry baseline attributes to {@code buildcraftfactory} registers here as a plain placeholder
 * {@link Block}; the real behaviour classes (legacy {@code BCFactoryBlocks}) migrate in M2.5+.
 */
public final class BcFactoryBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BuildCraftFactory.MOD_ID);
    /** Placeholder for {@code buildcraftfactory:autoworkbench_item} (legacy {@code BlockAutoWorkbenchItems}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> AUTOWORKBENCH_ITEM = BLOCKS.registerSimpleBlock("autoworkbench_item",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftfactory:chute} (legacy {@code BlockChute}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> CHUTE = BLOCKS.registerSimpleBlock("chute",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftfactory:distiller} (legacy {@code BlockDistiller_BC8}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> DISTILLER = BLOCKS.registerSimpleBlock("distiller",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftfactory:flood_gate} (legacy {@code BlockFloodGate}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> FLOOD_GATE = BLOCKS.registerSimpleBlock("flood_gate",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftfactory:heat_exchange} (legacy {@code BlockHeatExchange}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> HEAT_EXCHANGE = BLOCKS.registerSimpleBlock("heat_exchange",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftfactory:mining_well} (legacy {@code BlockMiningWell}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> MINING_WELL = BLOCKS.registerSimpleBlock("mining_well",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftfactory:pump} (legacy {@code BlockPump}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> PUMP = BLOCKS.registerSimpleBlock("pump",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftfactory:tank} (legacy {@code BlockTank}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> TANK = BLOCKS.registerSimpleBlock("tank",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftfactory:tube} (legacy {@code BlockTube}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> TUBE = BLOCKS.registerSimpleBlock("tube",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftfactory:water_gel} (legacy {@code BlockWaterGel}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> WATER_GEL = BLOCKS.registerSimpleBlock("water_gel",
            properties -> properties.strength(0.5F));


    private BcFactoryBlocks() {
    }
}
