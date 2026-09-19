/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import buildcraft.core.BcBlockEntities;
import buildcraft.core.blockentity.SpringOilBlockEntity;

/**
 * The M4.16 oil spring block ({@code buildcraftcore:spring_oil}), first real behaviour class under the id the M2.4a
 * registry parity registered as a placeholder (legacy counterpart: {@code buildcraft.core.block.BlockSpring} for
 * {@code EnumSpring.OIL}). Deliberately no block state properties: the M2.4a baseline blockstate palette
 * ({@code buildcraftcore:spring_oil} with the empty property set) must stay byte-identical for the strict registry
 * diff, so the legacy {@code SPRING_TYPE} property does NOT return here.
 *
 * <p>The generation loop lives in the {@link SpringOilBlockEntity} ticker (the M4.16 slice routes spring behaviour
 * through the block entity, which also carries the pump-progress bookkeeping slot of the legacy
 * {@code TileSpringOil}); this class only binds it. The block is unbreakable in legacy
 * ({@code setBlockUnbreakable}) &mdash; the placeholder strength is kept here so the datagen output stays identical
 * (flagged for the real core migration to revisit).
 */
public class SpringOilBlock extends BaseEntityBlock {

    public static final MapCodec<SpringOilBlock> CODEC = simpleCodec(SpringOilBlock::new);

    public SpringOilBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpringOilBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level instanceof ServerLevel serverLevel) {
            return createTickerHelper(
                type,
                BcBlockEntities.SPRING_OIL.value(),
                (innerLevel, pos, innerState, entity) -> SpringOilBlockEntity.serverTick(serverLevel, pos, innerState,
                        entity));
        }
        return null;
    }
}
