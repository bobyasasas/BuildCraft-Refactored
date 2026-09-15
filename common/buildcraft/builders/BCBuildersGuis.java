/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.builders;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkHooks;

@Deprecated(forRemoval = true)
public enum BCBuildersGuis {
    ARCHITECT,
    BUILDER,
    FILLER,
    LIBRARY,
    REPLACER,
    FILLER_PLANNER;

    public void openGUI(Player player) {
        openGUI(player, BlockPos.ZERO);
    }

    public void openGUI(Player player, BlockPos pos) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.level().getBlockEntity(pos) instanceof MenuProvider menuProvider) {
                NetworkHooks.openScreen(serverPlayer, menuProvider, pos);
            } else {
                ((ServerPlayer) player).sendSystemMessage(Component.translatable("buildcraft.error.open_null_menu"));
            }
        }
    }
}
