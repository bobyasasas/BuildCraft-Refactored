/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import buildcraft.factory.block.DistillerBlock;
import buildcraft.factory.block.HeatExchangeBlock;
import buildcraft.factory.block.PumpBlock;
import buildcraft.factory.block.TankBlock;
import buildcraft.lib.BcLangKeys;

/**
 * Central block registration for buildcraftfactory (task M2.4a skeleton, registry parity since M2.4b). Most block ids
 * the 1.20.1 registry baseline attributes to {@code buildcraftfactory} register as plain placeholder {@link Block}s;
 * the M4.7 machines (tank, distiller, heat exchange, pump) carry their real behaviour classes.
 */
public final class BcFactoryBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BuildCraftFactory.MOD_ID);
    /** Placeholder for {@code buildcraftfactory:autoworkbench_item} (legacy {@code BlockAutoWorkbenchItems}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> AUTOWORKBENCH_ITEM = BcLangKeys.simpleBlock(BLOCKS, "autoworkbench_item",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftfactory:chute} (legacy {@code BlockChute}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> CHUTE = BcLangKeys.simpleBlock(BLOCKS, "chute",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftfactory:distiller} (legacy {@code BlockDistiller_BC8}); real class since M4.7 ({@link DistillerBlock}). */
    public static final DeferredBlock<DistillerBlock> DISTILLER = BcLangKeys.block(BLOCKS, "distiller",
            DistillerBlock::new, properties -> properties.strength(0.5F).noOcclusion());

    /** Placeholder for {@code buildcraftfactory:flood_gate} (legacy {@code BlockFloodGate}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> FLOOD_GATE = BcLangKeys.simpleBlock(BLOCKS, "flood_gate",
            properties -> properties.strength(0.5F));

    /** {@code buildcraftfactory:heat_exchange} (legacy {@code BlockHeatExchange}); real class since M4.7 ({@link HeatExchangeBlock}), the BER draws the jsonbc geometry. */
    public static final DeferredBlock<HeatExchangeBlock> HEAT_EXCHANGE = BcLangKeys.block(BLOCKS, "heat_exchange",
            HeatExchangeBlock::new, properties -> properties.strength(0.5F).noOcclusion());

    /** Placeholder for {@code buildcraftfactory:mining_well} (legacy {@code BlockMiningWell}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> MINING_WELL = BcLangKeys.simpleBlock(BLOCKS, "mining_well",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftfactory:pump} (legacy {@code BlockPump}); real class since M4.7 ({@link PumpBlock}). */
    public static final DeferredBlock<PumpBlock> PUMP = BcLangKeys.block(BLOCKS, "pump",
            PumpBlock::new, properties -> properties.strength(0.5F));

    /** {@code buildcraftfactory:tank} (legacy {@code BlockTank}); real class since M4.7 ({@link TankBlock}). */
    public static final DeferredBlock<TankBlock> TANK = BcLangKeys.block(BLOCKS, "tank",
            TankBlock::new, properties -> properties.strength(0.5F).noOcclusion());

    /** Placeholder for {@code buildcraftfactory:tube} (legacy {@code BlockTube}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> TUBE = BcLangKeys.simpleBlock(BLOCKS, "tube",
            properties -> properties.strength(0.5F));

    /** Placeholder for {@code buildcraftfactory:water_gel} (legacy {@code BlockWaterGel}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> WATER_GEL = BcLangKeys.simpleBlock(BLOCKS, "water_gel",
            properties -> properties.strength(0.5F));


    private BcFactoryBlocks() {
    }
}
