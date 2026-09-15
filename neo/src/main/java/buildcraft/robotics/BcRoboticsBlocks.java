/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.robotics;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central block registration for buildcraftrobotics (task M2.4c registry parity). Every block id the 1.20.1 registry
 * baseline attributes to {@code buildcraftrobotics} registers here as a plain placeholder {@link Block}; the real
 * behaviour classes (legacy {@code BlockRequester} / {@code BlockZonePlanner}) migrate in M2.5+.
 */
public final class BcRoboticsBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BuildCraftRobotics.MOD_ID);

    /** Placeholder for {@code buildcraftrobotics:requester} (legacy {@code BlockRequester}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> REQUESTER = BLOCKS.registerSimpleBlock("requester",
            properties -> properties.strength(5F, 10F));

    /** Placeholder for {@code buildcraftrobotics:zone_planner} (legacy {@code BlockZonePlanner}); behaviour class migrates in M2.5+. */
    public static final DeferredBlock<Block> ZONE_PLANNER = BLOCKS.registerSimpleBlock("zone_planner",
            properties -> properties.strength(5F, 10F));

    private BcRoboticsBlocks() {
    }
}
