/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.blockentity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import buildcraft.core.BcBlockEntities;
import buildcraft.energy.BcEnergyBlocks;

/**
 * The M4.16 oil spring block entity ({@code buildcraftcore:spring_oil}), replacing the M2.4a
 * {@link PlaceholderBlockEntity} under the unchanged id. Minimal faithful slice of the legacy spring pair
 * (frozen 8.0.x-1.20.1 tree): the legacy generation ran in {@code BlockSpring#generateSpringBlock} &mdash; on the
 * scheduled {@code EnumSpring#tickRate} tick, if the block above is air, place the spring's liquid block there
 * ({@code EnumSpring.OIL}); the paired {@code energy/tile/TileSpringOil} only tracked per-player pump progress for the
 * {@code black_gold} advancement (progress tracking and the advancement hook are NOT migrated &mdash; no pump reads
 * {@code ITileOilSpring} yet, flagged for the energy spring migration).
 *
 * <p>Slice mapping:
 * <ul>
 * <li>The legacy random-tick + {@code chance} roll becomes a fixed {@link #TICK_RATE} interval on the block entity
 * ticker (deterministic for evidence runs; {@code EnumSpring.OIL#chance == -1} meant "always place" anyway).</li>
 * <li>The liquid is the migrated base oil still fluid {@code buildcraftenergy:oil_heat_0} through its
 * {@code LiquidBlock} ({@code BcEnergyBlocks.FLUID_BLOCK_OIL_HEAT_0}, read-only reference into the energy module &mdash;
 * the legacy spring equally reached across to the energy fluids via {@code EnumSpring#liquidBlock}).</li>
 * <li>Placement is strictly "the block above is air" ({@code level.isEmptyBlock(pos.above())}), exactly the legacy
 * guard; oil then spreads as any vanilla liquid source.</li>
 * </ul>
 *
 * <p>Every placement logs one {@code [M416]} line naming the source block position (the slice stand-in for observing
 * the spring, no client state involved).
 */
public class SpringOilBlockEntity extends BlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Placement interval in ticks (2 s). Slice value standing in for the legacy {@code EnumSpring#tickRate} schedule +
     * random-tick roll (the exact legacy tick constant is not recoverable from the frozen tree's API submodule).
     */
    public static final int TICK_RATE = 40;

    public SpringOilBlockEntity(BlockPos pos, BlockState state) {
        super(BcBlockEntities.SPRING_OIL.value(), pos, state);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, SpringOilBlockEntity spring) {
        if (level.getGameTime() % TICK_RATE != 0) {
            return;
        }
        BlockPos above = pos.above();
        if (!level.isEmptyBlock(above)) {
            return;
        }
        level.setBlockAndUpdate(above, BcEnergyBlocks.FLUID_BLOCK_OIL_HEAT_0.value().defaultBlockState());
        LOGGER.info("[M416] spring_oil at {} placed an oil source block at {}", pos, above);
    }
}
