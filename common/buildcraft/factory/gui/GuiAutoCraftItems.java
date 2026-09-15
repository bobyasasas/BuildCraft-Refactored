/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.factory.gui;

import buildcraft.factory.container.ContainerAutoCraftItems;
import buildcraft.lib.gui.GuiBC8;
import buildcraft.lib.gui.GuiIcon;
import buildcraft.lib.gui.ledger.LedgerHelp;
import buildcraft.lib.gui.pos.GuiRectangle;
import buildcraft.lib.gui.slot.SlotBase;
import buildcraft.lib.misc.RenderUtil;
import buildcraft.lib.misc.StackUtil;
import buildcraft.lib.tile.item.ItemHandlerSimple;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.recipebook.RecipeShownListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.common.crafting.IShapedRecipe;

import java.util.ArrayList;
import java.util.List;

public class GuiAutoCraftItems extends GuiBC8<ContainerAutoCraftItems> implements RecipeShownListener
//public class GuiAutoCraftItems extends GuiWithRecipeBookBC8<ContainerAutoCraftItems> implements RecipeShownListener
{
    private static final ResourceLocation TEXTURE_BASE =
            new ResourceLocation("buildcraftfactory:textures/gui/autobench_item.png");
    private static final ResourceLocation TEXTURE_MISC =
            new ResourceLocation("buildcraftlib:textures/gui/misc_slots.png");
    private static final ResourceLocation VANILLA_CRAFTING_TABLE =
            new ResourceLocation("textures/gui/container/crafting_table.png");
    private static final int SIZE_X = 176, SIZE_Y = 197;
    private static final GuiIcon ICON_GUI = new GuiIcon(TEXTURE_BASE, 0, 0, SIZE_X, SIZE_Y);
    private static final GuiIcon ICON_FILTER_OVERLAY_SAME = new GuiIcon(TEXTURE_MISC, 54, 0, 18, 18);
    private static final GuiIcon ICON_FILTER_OVERLAY_DIFFERENT = new GuiIcon(TEXTURE_MISC, 72, 0, 18, 18);
    private static final GuiIcon ICON_FILTER_OVERLAY_SIMILAR = new GuiIcon(TEXTURE_MISC, 90, 0, 18, 18);
    private static final GuiIcon ICON_PROGRESS = new GuiIcon(TEXTURE_BASE, SIZE_X, 0, 23, 10);
    private static final GuiRectangle RECT_PROGRESS = new GuiRectangle(90, 47, 23, 10);

//    /** If true then the recipe book will be drawn on top of this GUI, rather than beside it */

    public GuiAutoCraftItems(ContainerAutoCraftItems container, Inventory inventory, Component component) {
        super(container, inventory, component);
        imageWidth = SIZE_X;
        imageHeight = SIZE_Y;
        mainGui.shownElements.add(new LedgerHelp(mainGui, true));
    }

    private void sendRecipe(CraftingRecipe recipe) {
        List<ItemStack> stacks = new ArrayList<>(9);

        int maxX = recipe instanceof IShapedRecipe ? ((IShapedRecipe) recipe).getRecipeWidth() : 3;
        int maxY = recipe instanceof IShapedRecipe ? ((IShapedRecipe) recipe).getRecipeHeight() : 3;
        int offsetX = maxX == 1 ? 1 : 0;
        int offsetY = maxY == 1 ? 1 : 0;
        List<Ingredient> ingredients = recipe.getIngredients();
        if (ingredients.isEmpty()) {
            return;
        }
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                if (x < offsetX || y < offsetY) {
                    stacks.add(ItemStack.EMPTY);
                    continue;
                }
                int i = x - offsetX + (y - offsetY) * maxX;
                if (i >= ingredients.size() || x - offsetX >= maxX) {
                    stacks.add(ItemStack.EMPTY);
                } else {
                    Ingredient ing = ingredients.get(i);
                    ItemStack[] matching = ing.getItems();
                    if (matching.length >= 1) {
                        stacks.add(matching[0]);
                    } else {
                        stacks.add(ItemStack.EMPTY);
                    }
                }
            }
        }

        container.sendSetPhantomSlots(container.tile.invBlueprint, stacks);
    }

    @Override
    protected boolean shouldAddHelpLedger() {
        // Don't add it on the left side because it clashes with the recipe book
        return false;
    }

    @Override
    public void initGui() {
        super.initGui();
//            recipeButton = new ImageButton(
////                    10,
//                    leftPos + 5,
//                    height / 2 - 66,
//                    20,
//                    18,
//                    0,
//                    168,
//                    19,
//                    VANILLA_CRAFTING_TABLE,
//                    (button) ->

    }

    @Override
    public void containerTick() {
        super.containerTick();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);


    }

    @Override
    protected void drawBackgroundLayer(float partialTicks, GuiGraphics guiGraphics) {
        ICON_GUI.drawAt(mainGui.rootElement, guiGraphics);

        double progress = container.tile.getProgress(partialTicks);

        drawProgress(RECT_PROGRESS, ICON_PROGRESS, guiGraphics, progress, 1);

        if (hasFilters()) {
            Lighting.setupForFlatItems();
            forEachFilter((slot, filterStack) ->
            {
                int x = slot.x + (int) mainGui.rootElement.getX();
                int y = slot.y + (int) mainGui.rootElement.getY();
                guiGraphics.renderItem(minecraft.player, filterStack, x, y, 0);
                guiGraphics.renderItemDecorations(minecraft.font, filterStack, x, y, null);
            });

            RenderUtil.disableDepth();
            RenderUtil.enableBlend();
            RenderSystem.defaultBlendFunc();

            forEachFilter((slot, filterStack) ->
            {
                ItemStack real = slot.getItem();
                final GuiIcon icon;
                if (real.isEmpty() || StackUtil.canMerge(real, filterStack)) {
                    icon = ICON_FILTER_OVERLAY_SAME;
                } else {
                    icon = ICON_FILTER_OVERLAY_DIFFERENT;
                }
                int x = slot.x + (int) mainGui.rootElement.getX();
                int y = slot.y + (int) mainGui.rootElement.getY();
                icon.drawAt(guiGraphics, x - 1, y - 1);
            });
            RenderUtil.enableDepth();
        }
    }

    private boolean hasFilters() {
        ItemHandlerSimple filters = container.tile.invMaterialFilter;
        for (int s = 0; s < filters.getSlots(); s++) {
            ItemStack filter = filters.getStackInSlot(s);
            if (!filter.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void forEachFilter(IFilterSlotIterator iter) {
        ItemHandlerSimple filters = container.tile.invMaterialFilter;
        for (int s = 0; s < filters.getSlots(); s++) {
            ItemStack filter = filters.getStackInSlot(s);
            if (!filter.isEmpty()) {
                iter.iterate(container.materialSlots[s], filter);
            }
        }
    }

    @FunctionalInterface
    private interface IFilterSlotIterator {
        void iterate(SlotBase drawSlot, ItemStack filterStack);
    }

//    @Override

    @Override
//    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {

        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
//    protected void keyTyped(char typedChar, int keyCode) throws IOException
    public boolean keyPressed(int typedChar, int keyCode, int modifiers) {

        return super.keyPressed(typedChar, keyCode, modifiers);
    }

    @Override
    public boolean charTyped(char typedChar, int keyCode) {

        return super.charTyped(typedChar, keyCode);
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        super.slotClicked(slot, slotId, mouseButton, type);
    }

    @Override
    protected boolean isHovering(int rectX, int rectY, int rectWidth, int rectHeight, double pointX, double pointY) {


        return super.isHovering(rectX, rectY, rectWidth, rectHeight, pointX, pointY);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int _guiLeft, int _guiTop, int p_97761_) {

        return super.hasClickedOutside(mouseX, mouseY, _guiLeft, _guiTop, p_97761_);
    }

    @Override
    public void onClose() {
        super.onClose();
    }

    // IRecipeShownListener

//    @Override

    @Override
    public void recipesShown(List<Recipe<?>> p_100518_) {
        for (Recipe<?> recipe : p_100518_) {
            this.minecraft.player.removeRecipeHighlight(recipe);
        }
    }
}
