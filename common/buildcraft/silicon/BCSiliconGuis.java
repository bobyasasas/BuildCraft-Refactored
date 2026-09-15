/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.silicon;

import buildcraft.lib.net.MessageUpdateTile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkHooks;

@Deprecated(forRemoval = true)
public enum BCSiliconGuis {
    ASSEMBLY_TABLE,
    ADVANCED_CRAFTING_TABLE,
    INTEGRATION_TABLE,
    GATE;




    public void openGui(Player player, MenuProvider provider, BlockPos pos, int data, MessageUpdateTile msg) {
        int fullId = data << 8;
        if (player instanceof ServerPlayer serverPlayer) {
            if (this == GATE) {
                NetworkHooks.openScreen(
                        serverPlayer, provider, buf ->
                        {
                            buf.writeBlockPos(pos);
                            buf.writeInt(fullId);

                            msg.toBytes(buf);
                        }
                );
            } else {
                NetworkHooks.openScreen(serverPlayer, provider, pos);
            }
        }
    }
}
