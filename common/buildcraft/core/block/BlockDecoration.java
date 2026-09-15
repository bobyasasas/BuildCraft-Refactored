/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.core.block;

import buildcraft.api.enums.EnumDecoratedBlock;
import buildcraft.lib.block.BlockBCBase_Neptune;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class BlockDecoration extends BlockBCBase_Neptune {
    public final EnumDecoratedBlock DECORATED_TYPE;

    public BlockDecoration(String idBC, BlockBehaviour.Properties properties, EnumDecoratedBlock decoratedType) {
        super(idBC, properties);
        this.DECORATED_TYPE = decoratedType;
    }

    // IBlockState

//    @Override

//    @Override

//    @Override

    // Other

//    @Override

//    @Override

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return this.DECORATED_TYPE.lightValue;
    }
}
