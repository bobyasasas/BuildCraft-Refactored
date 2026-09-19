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
import buildcraft.core.blockentity.PowerTesterBlockEntity;

/**
 * The M4.16 power consumer tester block ({@code buildcraftcore:power_tester}), first real behaviour class under the id
 * the M2.4a registry parity registered as a placeholder (legacy counterpart:
 * {@code buildcraft.core.block.BlockPowerConsumerTester}). Deliberately no block state properties: the M2.4a baseline
 * blockstate palette ({@code buildcraftcore:power_tester} with the empty property set) must stay byte-identical for the
 * strict registry diff, so the placeholder's plain {@code Block} registration in {@code BcBlocks} is swapped for this
 * class under the unchanged id.
 *
 * <p>Right-click has no behaviour (the legacy tester carries no GUI either &mdash; it is a debug block meant to be read
 * through its {@code IDebuggable} output, which in this slice surfaces as the per-second
 * {@link PowerTesterBlockEntity} log line).
 */
public class PowerTesterBlock extends BaseEntityBlock {

    public static final MapCodec<PowerTesterBlock> CODEC = simpleCodec(PowerTesterBlock::new);

    public PowerTesterBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PowerTesterBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level instanceof ServerLevel serverLevel) {
            return createTickerHelper(
                type,
                BcBlockEntities.POWER_TESTER.value(),
                (innerLevel, pos, innerState, entity) -> PowerTesterBlockEntity.serverTick(serverLevel, pos, innerState,
                        entity));
        }
        return null;
    }
}
