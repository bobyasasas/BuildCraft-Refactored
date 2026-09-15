/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.silicon.gui;

import buildcraft.lib.gui.GuiBC8;
import buildcraft.lib.gui.GuiIcon;
import buildcraft.lib.gui.ledger.LedgerHelp;
import buildcraft.lib.gui.pos.GuiRectangle;
import buildcraft.lib.registry.TagManager;
import buildcraft.silicon.container.ContainerAdvancedCraftingTable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.recipebook.RecipeShownListener;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.common.crafting.IShapedRecipe;

import java.util.ArrayList;
import java.util.List;

//public class GuiAdvancedCraftingTable extends GuiWithRecipeBookBC8<ContainerAdvancedCraftingTable> implements RecipeShownListener
//public class GuiAdvancedCraftingTable<T extends AbstractContainerMenu> extends RecipeBookMenu<ContainerAdvancedCraftingTable> implements RecipeShownListener
public class GuiAdvancedCraftingTable extends GuiBC8<ContainerAdvancedCraftingTable> implements RecipeShownListener {
    private static final ResourceLocation TEXTURE_BASE = new ResourceLocation("buildcraftsilicon:textures/gui/advanced_crafting_table.png");
    private static final ResourceLocation VANILLA_CRAFTING_TABLE = new ResourceLocation("textures/gui/container/crafting_table.png");
    private static final int SIZE_X = 176, SIZE_Y = 241;
    private static final GuiIcon ICON_GUI = new GuiIcon(TEXTURE_BASE, 0, 0, SIZE_X, SIZE_Y);
    private static final GuiIcon ICON_PROGRESS = new GuiIcon(TEXTURE_BASE, SIZE_X, 0, 4, 70);
    private static final GuiRectangle RECT_PROGRESS = new GuiRectangle(164, 7, 4, 70);

    /** If true then the recipe book will be drawn on top of this GUI, rather than beside it */
    private boolean widthTooNarrow;

    public GuiAdvancedCraftingTable(ContainerAdvancedCraftingTable container, Inventory inventory, Component component) {
        super(container, inventory, component);
        imageWidth = SIZE_X;
        imageHeight = SIZE_Y;
        widthTooNarrow = this.width < SIZE_X + 176;
        mainGui.shownElements.add(new LedgerHelp(mainGui, true));
    }

//    @Override

    private void sendRecipe(Recipe recipe) {
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
//            recipeButton = new ImageButton(
////                    10,
//                    leftPos + 5,
//                    height / 2 - 90,
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

        long target = container.tile.getTarget();
        if (target != 0) {
            double v = (double) container.tile.power / target;
            ICON_PROGRESS.drawCutInside(
                    new GuiRectangle(
                            RECT_PROGRESS.x,
                            (int) (RECT_PROGRESS.y + RECT_PROGRESS.height * Math.max(1 - v, 0)),
                            RECT_PROGRESS.width,
                            (int) Math.ceil(RECT_PROGRESS.height * Math.min(v, 1))
                    ).offset(mainGui.rootElement),
                    guiGraphics
            );
        }
    }

    @Override
    protected void drawForegroundLayer(GuiGraphics guiGraphics) {
        String title = I18n.get("tile." + TagManager.getTag("block.advanced_crafting_table", TagManager.EnumTagType.UNLOCALIZED_NAME) + ".name");
        guiGraphics.drawString(font, title, leftPos + (float) (imageWidth - font.width(title)) / 2, topPos + 5, 0x404040, false);
    }

//    @Override
////    protected void actionPerformed(GuiButton button) throws IOException

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
