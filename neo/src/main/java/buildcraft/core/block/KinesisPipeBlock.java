/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import buildcraft.core.BcBlockEntities;
import buildcraft.core.blockentity.KinesisPipeBlockEntity;

/**
 * The M2.2c wooden kinesis pipe slice block (buildcraftcore:pipe_kinesis_wood), vertical slice edition. Deliberately
 * named {@code KinesisPipeBlock} (not {@code PipeBlock}, which is the vanilla stem-attachment base class) and placed as
 * a plain {@link BaseEntityBlock} because the slice needs a ticking block entity, not connection properties: the visual
 * is a fixed thin-column placeholder model ({@code assets/buildcraftcore/models/block/pipe_kinesis_wood.json}), and the
 * auto-connecting pipe model + BER system is M2.7.
 *
 * <p>This is the pattern template for the M2.4/M2.9 transport module migration (legacy {@code BlockGenericPipe} +
 * {@code PipeTransportPower}); connection states, pipe contents and the network graph are not migrated here (see
 * {@link KinesisPipeBlockEntity} for the slice diffusion model).
 */
public class KinesisPipeBlock extends BaseEntityBlock {

    public static final MapCodec<KinesisPipeBlock> CODEC = simpleCodec(KinesisPipeBlock::new);

    /** Placeholder selection/collision shape matching the thin-column slice model. Real pipe shapes are M2.7. */
    private static final VoxelShape SHAPE = Block.box(6.0, 0.0, 6.0, 10.0, 16.0, 10.0);

    public KinesisPipeBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KinesisPipeBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level instanceof ServerLevel serverLevel) {
            return createTickerHelper(
                type,
                BcBlockEntities.PIPE_KINESIS_WOOD.value(),
                (innerLevel, pos, innerState, entity) -> KinesisPipeBlockEntity.serverTick(serverLevel, pos, innerState, entity));
        }
        return null;
    }
}
