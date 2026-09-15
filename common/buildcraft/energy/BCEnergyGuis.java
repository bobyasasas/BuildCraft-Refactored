/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy;

import buildcraft.api.net.IMessage;
import buildcraft.lib.tile.TileBC_Neptune;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkHooks;

@Deprecated(forRemoval = true)
public enum BCEnergyGuis {
    ENGINE_STONE,
    ENGINE_IRON;

    public static final BCEnergyGuis[] VALUES = values();

    public static BCEnergyGuis get(int id) {
        if (id < 0 || id >= VALUES.length) return null;
        return VALUES[id];
    }


    public void openGUI(Player player, TileBC_Neptune tile) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (tile instanceof MenuProvider menuProvider) {

                IMessage msg = tile.onServerPlayerOpenNoSend(player);
                NetworkHooks.openScreen(
                        serverPlayer, menuProvider, buf ->
                        {
                            buf.writeBlockPos(tile.getBlockPos());

                            msg.toBytes(buf);
                        }
                );
            } else {
                ((ServerPlayer) player).sendSystemMessage(Component.translatable("buildcraft.error.open_null_menu"));
            }
        }
    }

}
