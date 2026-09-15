package buildcraft.silicon.client;

import buildcraft.lib.misc.BlockUtil;
import buildcraft.silicon.item.ItemPluggableFacade;
import buildcraft.silicon.plug.FacadeInstance;
import buildcraft.silicon.plug.FacadePhasedState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public enum FacadeItemColours implements ItemColor {
    INSTANCE;

    @Override
    public int getColor(ItemStack stack, int tintIndex) {
        FacadeInstance states = ItemPluggableFacade.getStates(stack);
        FacadePhasedState state = states.getCurrentStateForStack();
        int colour = -1;
        ResourceLocation id = BlockUtil.getRegistryName(state.stateInfo.state.getBlock());
        if (id != null && "wildnature".equals(id.getNamespace())) {
            // Fixes https://github.com/BuildCraft/BuildCraft/issues/4435
            return -1;
        }
        try {
            colour = Minecraft.getInstance().getBlockColors().getColor(state.stateInfo.state, null, null);
        } catch (NullPointerException ex) {
            // the block didn't like the null world or player
        }
        return colour;
    }
}
