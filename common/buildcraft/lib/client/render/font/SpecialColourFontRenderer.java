package buildcraft.lib.client.render.font;

import buildcraft.api.core.BCLog;
import buildcraft.lib.misc.ColourUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

public class SpecialColourFontRenderer extends Font {
    public static final SpecialColourFontRenderer INSTANCE = new SpecialColourFontRenderer();

    private SpecialColourFontRenderer() {
        super((resourceLocation -> new FontSet(Minecraft.getInstance().textureManager, new ResourceLocation("textures/font/ascii.png"))), false);
//        super(Minecraft.getInstance().gameSettings, new ResourceLocation("textures/font/ascii.png"),
    }

//    @Override
//            // Render some of it normally
//            // 似乎drawShaw是文字+shadow 不是纯shadow

    private static Font getRealRenderer() {
        return Minecraft.getInstance().font;
    }


//    @Override
//        // NO-OP

    @Override
    public int width(String text) {
        return getRealRenderer().width(text);
    }

//    @Override

    @Override
    public String plainSubstrByWidth(String text, int width) {
        return getRealRenderer().plainSubstrByWidth(text, width);
    }

    @Override
    public String plainSubstrByWidth(String text, int width, boolean reverse) {
        return getRealRenderer().plainSubstrByWidth(text, width, reverse);
    }

    @Override
    public int wordWrapHeight(String str, int maxLength) {
        return getRealRenderer().wordWrapHeight(str, maxLength);
    }

//    @Override

//    @Override

//    @Override

    @Override
    public FormattedText substrByWidth(FormattedText str, int wrapWidth) {
        return getRealRenderer().substrByWidth(str, wrapWidth);
    }

    @Override
    public boolean isBidirectional() {
        return getRealRenderer().isBidirectional();
    }

//    @Override
}
