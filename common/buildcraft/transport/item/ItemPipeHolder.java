/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.item;

import buildcraft.api.transport.pipe.IItemPipe;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pipe.PipeDefinition;
import buildcraft.lib.client.render.font.SpecialColourFontRenderer;
import buildcraft.lib.item.IItemBuildCraft;
import buildcraft.lib.misc.ColourUtil;
import buildcraft.lib.misc.LocaleUtil;
import buildcraft.lib.registry.CreativeTabManager;
import buildcraft.lib.registry.TagManager;
import buildcraft.transport.BCTransport;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.pipe.PipeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.List;

public class ItemPipeHolder extends BlockItem implements IItemBuildCraft, IItemPipe {
    public final PipeDefinition definition;
    private final String id;
    private String unlocalizedName;
    private final DyeColor colour;

    protected ItemPipeHolder(PipeDefinition definition, String tagId, DyeColor colour) {
        super(BCTransportBlocks.pipeHolder.get(), definition.properties);
        tab(CreativeTabManager.getTab(TagManager.getTag(tagId, TagManager.EnumTagType.CREATIVE_TAB)));
        this.definition = definition;
        this.id = tagId;
        if (!"".equals(id)) {
            init();
        }
        this.colour = colour;
    }

    @Override
    public DyeColor getColour() {
        return colour;
    }


//    /** Creates a new {@link ItemPipeHolder} without requiring a tag. */

    /** Creates a new {@link ItemPipeHolder} with a tag that will be taken from {@link TagManager}. */
    public static RegistryObject<ItemPipeHolder> createAndTag(PipeDefinition definition, DyeColor colour) {
        ResourceLocation reg = definition.identifier;
        String suffix = colour == null ? "_colorless" : "_" + colour.getName();
        String tagId = "item.pipe." + reg.getNamespace() + "." + reg.getPath();
        String regName = TagManager.getTag(tagId, TagManager.EnumTagType.REGISTRY_NAME).replace(BCTransport.MODID + ":", "") + suffix;
        return PipeRegistry.helper.addForcedItem(regName, () -> new ItemPipeHolder(definition, tagId, colour));
    }


    @Override
    public void fillItemCategory(NonNullList<ItemStack> items) {
        items.add(new ItemStack(this));
    }

    @Override
    public String getIdBC() {
        return id;
    }

    @Override
    public PipeDefinition getDefinition() {
        return definition;
    }

//    @Override

    @Override
    public Component getName(ItemStack stack) {
        String colourComponent = this.colour == null ? "" : (ColourUtil.getTextFullTooltipSpecial(this.colour) + " ");
        return Component.literal(colourComponent).append(Component.translatable(this.getDescriptionId(stack)));
    }

//    @Override
    @OnlyIn(Dist.CLIENT)
    public Font getFontRenderer(ItemStack stack) {
        return SpecialColourFontRenderer.INSTANCE;
    }

    // ItemBlock overrides these to point to the block

    @Override
    public void setUnlocalizedName(String unlocalizedName) {
        this.unlocalizedName = unlocalizedName;
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return this.unlocalizedName;
    }

//    @Override

//    @Override

//    @Override

    // Misc usefulness

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        String tipName = "tip." + this.unlocalizedName.replace(".name", "").replace("item.", "");
        String localised = I18n.get(tipName);
        if (!localised.equals(tipName)) {
            tooltip.add(Component.literal(ChatFormatting.GRAY + localised));
        }
        if (definition.flowType == PipeApi.flowFluids) {
            PipeApi.FluidTransferInfo fti = PipeApi.getFluidTransferInfo(definition);
            tooltip.add(LocaleUtil.localizeFluidFlowToTranslatableComponent(fti.transferPerTick));
            tooltip.add(LocaleUtil.localizeFluidFlowToTranslatableComponent(fti.transferPerTick));
        } else if (definition.flowType == PipeApi.flowPower) {
            PipeApi.PowerTransferInfo pti = PipeApi.getPowerTransferInfo(definition);
            tooltip.add(LocaleUtil.localizeMjFlowComponent(pti.transferPerTick));
        } else if (definition.flowType == PipeApi.flowRf && PipeApi.flowRf != null) {
            PipeApi.RedstoneFluxTransferInfo pti = PipeApi.getRfTransferInfo(definition);
            tooltip.add(Component.literal(pti.transferPerTick + " RF/t"));//TODO: Locale!
        }
    }
}
