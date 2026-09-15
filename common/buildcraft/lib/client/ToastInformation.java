package buildcraft.lib.client;

import buildcraft.lib.gui.ISimpleDrawable;
import buildcraft.lib.misc.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;

public class ToastInformation implements Toast {
    public final String localeKey;
    public final ISimpleDrawable icon;
    private final Object type;

    public ToastInformation(String localeKey, ISimpleDrawable icon, Object type) {
        this.localeKey = localeKey;
        this.icon = icon;
        this.type = type;
    }

    public ToastInformation(String localeKey, ISimpleDrawable icon) {
        this(localeKey, icon, NO_TOKEN);
    }

    @Override
    public Visibility render(GuiGraphics guiGraphics, ToastComponent toastGui, long delta) {
        RenderUtil.color(1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, 0, 0, 0, 0, 160, 32);
        int x = 10;
        if (icon != null) {
            icon.drawAt(guiGraphics, 0, 0);
            x = 30;
        }
        guiGraphics.drawString(toastGui.getMinecraft().font, Component.translatable(localeKey), x, 13, -1);
        return delta >= 5000L ? Visibility.HIDE : Visibility.SHOW;
    }

    @Override
    public Object getToken() {
        return type;
    }
}
