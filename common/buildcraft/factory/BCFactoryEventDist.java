/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory;

import buildcraft.factory.tile.TileMiner;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public enum BCFactoryEventDist {
    INSTANCE;

//    @SubscribeEvent

//    @SubscribeEvent

    @SubscribeEvent
    public void onPlayerDestroyBlock(BlockEvent.BreakEvent event) {
        if (event.getState().getBlock() != BCFactoryBlocks.tube.get()) {
            return;
        }
        BlockPos currentPos = event.getPos();
        Level world = event.getPlayer().level();
        // noinspection StatementWithEmptyBody
        while (world.getBlockState(currentPos = currentPos.above()).getBlock() == BCFactoryBlocks.tube.get()) {
        }
        if (world.getBlockEntity(currentPos) instanceof TileMiner) {
            event.setCanceled(true);
        }
    }
}
