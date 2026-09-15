/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.javafmlmod.FMLModContainer;

public class BCEnergySprites {
    public static void fmlPreInit() {
        // 1.18.2: following events are IModBusEvent
        IEventBus modEventBus = ((FMLModContainer) ModList.get().getModContainerById(BCEnergy.MODID).get()).getEventBus();
        modEventBus.register(BCEnergySprites.class);
    }

//    @SubscribeEvent
//                // So this doesn't work properly as we don't have the sprites.
//                // but that's ok as we said that these don't work if disabled ~anyway~
}
