/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared placeholder block entity for the M2.4a registry parity: carries no behaviour and saves no data, it only makes
 * every baseline {@code buildcraftcore} block entity id (see {@code migration/snapshots/registry-baseline.json}) a
 * registered {@link BlockEntityType} bound to its block. The registrations using this class live in
 * {@code BcBlockEntities} and are replaced by the real behaviour classes in M2.5+.
 */
public class PlaceholderBlockEntity extends BlockEntity {

    public PlaceholderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
