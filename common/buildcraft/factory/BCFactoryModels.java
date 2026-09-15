/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory;

import buildcraft.factory.client.model.ModelHeatExchange;
import buildcraft.factory.client.render.*;
import buildcraft.factory.tile.TileDistiller_BC8;
import buildcraft.lib.client.model.ModelHolderVariable;
import buildcraft.lib.client.model.ModelItemSimple;
import buildcraft.lib.client.model.MutableQuad;
import buildcraft.lib.misc.ExpressionCompat;
import buildcraft.lib.misc.RegistryUtil;
import com.google.common.collect.Lists;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.LazyLoadedValue;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.javafmlmod.FMLModContainer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class BCFactoryModels {
    public static final ModelHolderVariable DISTILLER;
    public static final ModelHolderVariable HEAT_EXCHANGE_STATIC;

    static {
        // or this will cause IllegalArgumentException: Unknown NodeType class net.minecraft.core.Direction
        ExpressionCompat.setup();

        DISTILLER = new ModelHolderVariable(
                "buildcraftfactory:models/tile/distiller.jsonbc",
                TileDistiller_BC8.MODEL_FUNC_CTX
        );
        HEAT_EXCHANGE_STATIC = new ModelHolderVariable(
                "buildcraftfactory:models/tile/heat_exchange_static.jsonbc",
                ModelHeatExchange.FUNCTION_CONTEXT
        );
    }

    public static void fmlPreInit() {
        // 1.18.2: following events are IModBusEvent
        IEventBus modEventBus = ((FMLModContainer) ModList.get().getModContainerById(BCFactory.MODID).get()).getEventBus();
        modEventBus.register(BCFactoryModels.class);
    }

//    @SubscribeEvent
//            ModelLoader.setCustomStateMapper(
//                    BCFactoryBlocks.heatExchange,
//                        @Nonnull
//                        @Override

    @SubscribeEvent
    public static void onTesrReg(RegisterRenderers event) {
        RegistryUtil.regTesrIfTilePresent(BCFactoryBlocks.miningWellTile, RenderMiningWell::new);
        RegistryUtil.regTesrIfTilePresent(BCFactoryBlocks.pumpTile, RenderPump::new);
        RegistryUtil.regTesrIfTilePresent(BCFactoryBlocks.tankTile, RenderTank::new);
        RegistryUtil.regTesrIfTilePresent(BCFactoryBlocks.distillerTile, RenderDistiller::new);
        RegistryUtil.regTesrIfTilePresent(BCFactoryBlocks.heatExchangeTile, RenderHeatExchange::new);
    }

    private static final List<Runnable> spriteTasks = Lists.newLinkedList();

    @SubscribeEvent
    public static void onTextureStitchEvent$Post(TextureStitchEvent.Post event) {
        if (event.getAtlas().location().equals(TextureAtlas.LOCATION_BLOCKS)) {
            spriteTasks.forEach(Runnable::run);
        }
    }

    @SubscribeEvent
    public static void onModelBake(ModelEvent.ModifyBakingResult event) {
        // the model path contains blockstate props
        ModelHeatExchange modelHeatExchange = new ModelHeatExchange(spriteTasks::add);
        event.getModels().replaceAll((rl, m) -> (
                rl.getNamespace().equals(BCFactory.MODID)
                        && rl.getPath().contains("heat_exchange")
                        && !rl.getPath().contains("inventory")
        ) ? modelHeatExchange : m);
        event.getModels().replace(
                new ModelResourceLocation(BCFactoryBlocks.heatExchange.getId(), "inventory"),
                new ModelItemSimple(
                        new LazyLoadedValue<>(
                                () -> Arrays.stream(BCFactoryModels.HEAT_EXCHANGE_STATIC.getCutoutQuads())
                                        .map(MutableQuad::multShade)
                                        .map(MutableQuad::toBakedItem)
                                        .collect(Collectors.toList())
                        ),
                        ModelItemSimple.TRANSFORM_BLOCK,
                        true,
                        spriteTasks::add
                )
        );
    }
}
