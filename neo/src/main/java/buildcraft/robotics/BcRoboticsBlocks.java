/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.robotics;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import buildcraft.lib.BcLangKeys;
import buildcraft.robotics.block.RequesterBlock;
import buildcraft.robotics.block.ZonePlannerBlock;

/**
 * Central block registration for buildcraftrobotics (task M2.4c registry parity). Every block id the 1.20.1 registry
 * baseline attributes to {@code buildcraftrobotics} registers here as a plain placeholder {@link Block} except the two
 * M4.17 support machines (requester, zone planner), which carry their real behaviour classes (the ids are unchanged, so
 * the registry parity gate is unaffected).
 */
public final class BcRoboticsBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BuildCraftRobotics.MOD_ID);

    /** {@code buildcraftrobotics:requester} (legacy {@code BlockRequester}); real class since M4.17 ({@link RequesterBlock}). */
    public static final DeferredBlock<RequesterBlock> REQUESTER = BcLangKeys.block(BLOCKS, "requester",
            RequesterBlock::new, properties -> properties.strength(5F, 10F));

    /** {@code buildcraftrobotics:zone_planner} (legacy {@code BlockZonePlanner}); real class since M4.17 ({@link ZonePlannerBlock}). */
    public static final DeferredBlock<ZonePlannerBlock> ZONE_PLANNER = BcLangKeys.block(BLOCKS, "zone_planner",
            ZonePlannerBlock::new, properties -> properties.strength(5F, 10F));

    private BcRoboticsBlocks() {
    }
}
