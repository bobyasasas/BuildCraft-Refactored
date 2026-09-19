/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.silicon.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import buildcraft.silicon.BcSiliconBlockEntities;

/**
 * M4.16 programming table block entity (legacy counterpart:
 * {@code buildcraft.silicon.tile.TileProgrammingTable_Neptune}, id {@code buildcraftsilicon:programming_table}
 * unchanged).
 *
 * <p><b>M4.16 slice status &mdash; v2 placeholder, honestly so:</b> the legacy table programs robots (a
 * {@code RedstoneBoardRobotNBT} written onto a {@code robot_base} against the board's energy cost, driven by a laser
 * through the shared {@code TileLaserTableBase} economics). Robot boards/AI and their item data components are v2
 * scope ({@code BcProgrammingRecipe} data is already migrated and load-tested since M2.10), so this class carries no
 * fabricated behaviour: it is a registered, placed, rendered, right-click-safe block entity under the legacy id,
 * with no inventory, no power and no ticker &mdash; the real programming loop replaces it in the robotics milestone.
 */
public class ProgrammingTableBlockEntity extends BlockEntity {

    public ProgrammingTableBlockEntity(BlockPos pos, BlockState state) {
        super(BcSiliconBlockEntities.PROGRAMMING_TABLE.value(), pos, state);
    }
}
