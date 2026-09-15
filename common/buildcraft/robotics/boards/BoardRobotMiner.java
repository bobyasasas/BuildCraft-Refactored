/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 * <p/>
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.robotics.boards;

import buildcraft.api.boards.RedstoneBoardRobotNBT;
import buildcraft.api.robots.AIRobot;
import buildcraft.api.robots.EntityRobotBase;
import buildcraft.robotics.ai.AIRobotFetchAndEquipItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.TierSortingRegistry;

import javax.annotation.Nonnull;

public class BoardRobotMiner extends BoardRobotGenericBreakBlock {
    private Tier harvestLevel = Tiers.WOOD;

    public BoardRobotMiner(EntityRobotBase iRobot) {
        super(iRobot);
        detectHarvestLevel();
    }

    @Override
    public void delegateAIEnded(AIRobot ai) {
        super.delegateAIEnded(ai);

        if (ai instanceof AIRobotFetchAndEquipItemStack) {
            if (ai.success()) {
                detectHarvestLevel();
            }
        }
    }

    private void detectHarvestLevel() {
        ItemStack stack = robot.getMainHandItem();

        if (!stack.isEmpty() && stack.getItem() instanceof PickaxeItem) {
            harvestLevel = ((PickaxeItem) stack.getItem()).getTier();
        }
    }

    @Override
    public RedstoneBoardRobotNBT getNBTHandler() {
        return BCBoardNBT.REGISTRY.get("miner");
    }

    @Override
    public boolean isExpectedTool(@Nonnull ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof PickaxeItem;
    }

    @Override
    public boolean isExpectedBlock(Level world, BlockPos pos) {
        if (harvestLevel != null) {
            BlockState state = world.getBlockState(pos);
            return state.is(Tags.Blocks.ORES) && TierSortingRegistry.isCorrectTierForDrops(harvestLevel, state);
        } else {
            return false;
        }
    }

}
