/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 * <p/>
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.robotics.item;

import buildcraft.api.boards.RedstoneBoardNBT;
import buildcraft.api.boards.RedstoneBoardRegistry;
import buildcraft.lib.item.ItemBC_Neptune;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class ItemRedstoneBoard extends ItemBC_Neptune {
    private final RedstoneBoardNBT<?> boardNBT;

    public ItemRedstoneBoard(String idBC, Properties properties, RedstoneBoardNBT<?> boardNBT) {
        super(idBC, properties);
        this.boardNBT = boardNBT;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return getBoardNBT(stack) != RedstoneBoardRegistry.instance.getEmptyRobotBoard() ? 1 : 16;
    }

    @Override
    public Component getName(ItemStack stack) {
        MutableComponent start = (MutableComponent) super.getName(stack);
        RedstoneBoardNBT<?> board = getBoardNBT(stack);
        return start.append(" (" + board.getDisplayName() + ")");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> list, TooltipFlag flag) {
        RedstoneBoardNBT<?> board = getBoardNBT(stack);
        board.addInformation(stack, world, list, flag);
    }

//    @Override


    public static RedstoneBoardNBT<?> getBoardNBT(ItemStack stack) {
        if (stack.getItem() instanceof ItemRedstoneBoard) {
            return ((ItemRedstoneBoard) stack.getItem()).boardNBT;
        } else {
            return RedstoneBoardRegistry.instance.getEmptyRobotBoard();
        }
    }



    public RedstoneBoardNBT<?> getBoardNBT() {
        return boardNBT;
    }

//    @Override
//            /* Neat little trick: we have to register the models, but NEVER for meta 0 (because of the way minecraft
//             * gets its item models). So, provided this number is never 0 it will work */
}
