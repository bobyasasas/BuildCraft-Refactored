package buildcraft.lib.gui;

import buildcraft.lib.gui.json.BuildCraftJsonGui;
import buildcraft.lib.gui.ledger.LedgerHelp;
import buildcraft.lib.gui.pos.IGuiArea;
import buildcraft.lib.misc.GuiUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.function.Function;

/** Reference implementation for a gui that delegates to a {@link BuildCraftGui} for most of its functionality. */
// public class GuiScreenBuildCraft extends GuiScreen
public class GuiScreenBuildCraft<C extends ContainerBC_Neptune<?>> extends Screen {

    public final BuildCraftGui mainGui;

    /** Creates a new {@link GuiScreenBuildCraft} that will occupy the entire screen. */
    public GuiScreenBuildCraft(C container, Inventory inventory, Component component) {
        this(g -> new BuildCraftGui(g), component);
    }

    /**
     * Creates a new {@link GuiScreenBuildCraft} that will occupy the given {@link IGuiArea} Call
     * {@link GuiUtil#moveAreaToCentre(IGuiArea)} if you want a centred gui. (Ignoring ledgers, which will display off
     * to the side)
     */
    public GuiScreenBuildCraft(IGuiArea area, Component component) {
        this(g -> new BuildCraftGui(g, area), component);
    }

    public GuiScreenBuildCraft(Function<GuiScreenBuildCraft<?>, BuildCraftGui> constructor, Component component) {
        super(component);
        this.mainGui = constructor.apply(this);
        standardLedgerInit();
    }

    /** Creates a new gui that will load its elements from the given json resource. */
    public GuiScreenBuildCraft(ResourceLocation jsonGuiDef, Component component) {
        super(component);
        BuildCraftJsonGui jsonGui = new BuildCraftJsonGui(this, jsonGuiDef);
        this.mainGui = jsonGui;
        standardLedgerInit();
    }

    /**
     * Creates a new gui that will load its elements from the given json resource. Like
     * {@link #GuiScreenBuildCraft(IGuiArea, Component)} this will occupy only the given {@link IGuiArea}
     */
    public GuiScreenBuildCraft(ResourceLocation jsonGuiDef, IGuiArea area, Component component) {
        super(component);
        BuildCraftJsonGui jsonGui = new BuildCraftJsonGui(this, area, jsonGuiDef);
        this.mainGui = jsonGui;
        standardLedgerInit();
    }

    private final void standardLedgerInit() {
        if (shouldAddHelpLedger()) {
            mainGui.shownElements.add(new LedgerHelp(mainGui, false));
        }
    }

    protected boolean shouldAddHelpLedger() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        mainGui.tick();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        PoseStack poseStack = guiGraphics.pose();
        mainGui.drawBackgroundLayer(guiGraphics, partialTicks, mouseX, mouseY, () -> drawMenuBackground(guiGraphics));
        mainGui.drawElementBackgrounds(guiGraphics);
        mainGui.drawElementForegrounds(() -> drawMenuBackground(guiGraphics), guiGraphics);
    }

    private void drawMenuBackground(GuiGraphics guiGraphics) {
        super.renderBackground(guiGraphics);
    }

    @Override
//    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (!mainGui.onMouseClicked(mouseX, mouseY, mouseButton)) {
            return super.mouseClicked(mouseX, mouseY, mouseButton);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        mainGui.onMouseReleased(mouseX, mouseY, state);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int clickedMouseButton, double startX, double startY) {
        super.mouseDragged(mouseX, mouseY, clickedMouseButton, startX, startY);
        mainGui.onMouseDragged(mouseX, mouseY, clickedMouseButton);
        return true;
    }

    @Override
//    protected void keyTyped(char typedChar, int keyCode) throws IOException
    public boolean keyPressed(int typedChar, int keyCode, int modifiers) {
        if (!mainGui.onKeyTyped(typedChar, keyCode, modifiers)) {
            return super.keyPressed(typedChar, keyCode, modifiers);
        } else {
            return true;
        }
    }

    public boolean charTyped(char typedChar, int keyCode) {
        if (!mainGui.charTyped(typedChar, keyCode)) {
            return super.charTyped(typedChar, keyCode);
        } else {
            return true;
        }
    }
}
