/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 * <p/>
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.robotics.item;

import buildcraft.api.boards.RedstoneBoardRegistry;
import buildcraft.api.boards.RedstoneBoardRobotNBT;
import buildcraft.api.events.RobotEvent;
import buildcraft.api.mj.IMjContainerItem;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;
import buildcraft.api.robots.DockingStation;
import buildcraft.api.robots.EntityRobotBase;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.lib.item.ItemBC_Neptune;
import buildcraft.lib.misc.LocaleUtil;
import buildcraft.lib.misc.NBTUtilBC;
import buildcraft.robotics.BCRoboticsEntities;
import buildcraft.robotics.BCRoboticsItems;
import buildcraft.robotics.entity.EntityRobot;
import buildcraft.robotics.plug.PluggableRobotStation;
import buildcraft.transport.block.BlockPipeHolder;
import buildcraft.transport.tile.TilePipeHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.List;

// public class ItemRobot extends ItemBC_Neptune implements IEnergyContainerItem
public class ItemRobot extends ItemBC_Neptune implements IMjContainerItem {

    private final RedstoneBoardRobotNBT robotNBT;

    public ItemRobot(String idBC, Properties properties, RedstoneBoardRobotNBT robotNBT) {
        super(idBC, properties);
        this.robotNBT = robotNBT;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        RedstoneBoardRobotNBT boardNBT = this.robotNBT;

        if (boardNBT != RedstoneBoardRegistry.instance.getEmptyRobotBoard()) {
            return 1;
        } else {
            return 16;
        }
    }

    public EntityRobot createRobot(ItemStack stack, Level world) {
        try {
            CompoundTag nbt = getNBT(stack);

            RedstoneBoardRobotNBT robotNBT = this.robotNBT;
            if (robotNBT == RedstoneBoardRegistry.instance.getEmptyRobotBoard()) {
                return null;
            }
            EntityRobot robot = BCRoboticsEntities.robotMap.get(robotNBT).get().create(world);
            robot.getBattery().deserializeNBT(nbt);

            return robot;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    public static RedstoneBoardRobotNBT getRobotNBT(ItemStack stack) {
        return stack.getItem() instanceof ItemRobot ? ((ItemRobot) stack.getItem()).robotNBT : RedstoneBoardRegistry.instance.getEmptyRobotBoard();
    }

    public static long getEnergy(ItemStack stack) {
        return getEnergy(getNBT(stack));
    }

    public ResourceLocation getTextureRobot(ItemStack stack) {
        return getRobotNBT(stack).getRobotTexture();
    }

    @Override
    public Component getName(ItemStack stack) {
        return this.robotNBT.getDisplayNameComponent();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> list, TooltipFlag flag) {
        CompoundTag cpt = getNBT(stack);
        RedstoneBoardRobotNBT boardNBT = this.robotNBT;

        if (boardNBT != RedstoneBoardRegistry.instance.getEmptyRobotBoard()) {
            boardNBT.addInformation(stack, world, list, flag);

            long energy = getEnergy(cpt);
            long pct = energy * 100 / EntityRobotBase.MAX_POWER;
            String enInfo = pct + "% " + LocaleUtil.localize("tip.gate.charged");
            if (energy == EntityRobotBase.MAX_POWER) {
                enInfo = LocaleUtil.localize("tip.gate.fullcharge");
            } else if (energy == 0) {
                enInfo = LocaleUtil.localize("tip.gate.nocharge");
            }
            enInfo = (pct >= 80 ? ChatFormatting.GREEN : (pct >= 50 ? ChatFormatting.YELLOW : (pct >= 30 ? ChatFormatting.GOLD
                    : (pct >= 20 ? ChatFormatting.RED : ChatFormatting.DARK_RED)))) + enInfo;
            list.add(Component.literal(enInfo));
        }
    }

    public static ItemStack createRobotStack(RedstoneBoardRobotNBT board, long energy) {
        ItemStack robot = new ItemStack(BCRoboticsItems.robot.get(board).get());
        NBTUtilBC.getItemData(robot).putLong(MjBattery.NBT_STORED, energy);
        return robot;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected void addSubItems(NonNullList<ItemStack> itemList) {


        itemList.add(createRobotStack(robotNBT, 0));
        if (this.robotNBT != RedstoneBoardRegistry.instance.getEmptyRobotBoard()) {
            itemList.add(createRobotStack(robotNBT, EntityRobotBase.MAX_POWER));
        }
    }

    @Override
    public long receivePower(ItemStack container, long maxReceive, boolean simulate) {
        CompoundTag cpt = getNBT(container);
        if (this.robotNBT == RedstoneBoardRegistry.instance.getEmptyRobotBoard()) {
            return 0;
        }
        long currentEnergy = getEnergy(cpt);
        long energyReceived = Math.min(EntityRobotBase.MAX_POWER - currentEnergy, maxReceive);
        if (!simulate) {
            setEnergy(cpt, currentEnergy + energyReceived);
        }
        return energyReceived;
    }

    @Override
    public long extractPower(ItemStack container, long maxExtract, boolean simulate) {
        CompoundTag cpt = getNBT(container);
        if (this.robotNBT == RedstoneBoardRegistry.instance.getEmptyRobotBoard()) {
            return 0;
        }
        long currentEnergy = getEnergy(cpt);
        long energyExtracted = Math.min(currentEnergy, maxExtract);
        if (!simulate) {
            setEnergy(cpt, currentEnergy - energyExtracted);
        }
        return energyExtracted;
    }

    @Override
    public long getPowerStored(ItemStack container) {
        return getEnergy(container);
    }

    @Override
    public long getMaxPowerStored(ItemStack container) {
        if (getRobotNBT(container) == RedstoneBoardRegistry.instance.getEmptyRobotBoard()) {
            return 0;
        }
        return EntityRobotBase.MAX_POWER;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack currentItem = context.getItemInHand();
        Player player = context.getPlayer();
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction side = context.getClickedFace();
        if (!world.isClientSide) {
            Block b = world.getBlockState(pos).getBlock();
            if (!(b instanceof BlockPipeHolder)) {
                return InteractionResult.PASS;
            }

            TilePipeHolder pipe = BlockPipeHolder.getPipe(world, pos, true);
            if (pipe == null) {
                return InteractionResult.PASS;
            }


            PipePluggable pluggable = pipe.getPluggable(side);

            if (pluggable instanceof PluggableRobotStation) {
                PluggableRobotStation robotPluggable = (PluggableRobotStation) pluggable;
                DockingStation station = robotPluggable.getStation();

                if (!station.isTaken()) {
                    RedstoneBoardRobotNBT robotNBT = ItemRobot.getRobotNBT(currentItem);
                    if (robotNBT == RedstoneBoardRegistry.instance.getEmptyRobotBoard()) {
                        return InteractionResult.SUCCESS;
                    }

                    EntityRobot robot = ((ItemRobot) currentItem.getItem()).createRobot(currentItem, world);

                    RobotEvent.Place robotEvent = new RobotEvent.Place(robot, player);
                    MinecraftForge.EVENT_BUS.post(robotEvent);
                    if (robotEvent.isCanceled()) {
                        return InteractionResult.SUCCESS;
                    }

                    if (robot != null && robot.getRegistry() != null) {
                        robot.setUniqueRobotId(robot.getRegistry().getNextRobotId());

                        float px = pos.getX() + 0.5F + side.getStepX() * 0.5F;
                        float py = pos.getY() + 0.5F + side.getStepY() * 0.5F;
                        float pz = pos.getZ() + 0.5F + side.getStepZ() * 0.5F;

                        robot.setPos(px, py, pz);
                        station.takeAsMain(robot);
                        robot.dock(robot.getLinkedStation());
                        world.addFreshEntity(robot);

                        if (!player.isCreative()) {
                            currentItem.shrink(1);
                        }
                    }
                }

                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    private static CompoundTag getNBT(ItemStack stack) {
        CompoundTag cpt = NBTUtilBC.getItemData(stack);
        return cpt;
    }


    private static long getEnergy(CompoundTag cpt) {
        return cpt.getLong(MjBattery.NBT_STORED);
    }

    private static void setEnergy(CompoundTag cpt, long energy) {
        cpt.putLong(MjBattery.NBT_STORED, energy);
    }

    @Override
    public boolean isDamaged(ItemStack stack) {
        CompoundTag cpt = getNBT(stack);
        RedstoneBoardRobotNBT boardNBT = this.robotNBT;

        if (boardNBT != RedstoneBoardRegistry.instance.getEmptyRobotBoard()) {

            long energy = getEnergy(cpt);
            return energy < EntityRobotBase.MAX_POWER;
        } else {
            return false;
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return isDamaged(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        CompoundTag cpt = getNBT(stack);
        RedstoneBoardRobotNBT boardNBT = this.robotNBT;

        if (boardNBT != RedstoneBoardRegistry.instance.getEmptyRobotBoard()) {
            long energy = getEnergy(cpt);
            long pct = energy * 100 / EntityRobotBase.MAX_POWER;
            return (pct >= 80 ? ChatFormatting.GREEN : (pct >= 50 ? ChatFormatting.YELLOW : (pct >= 30 ? ChatFormatting.GOLD : (pct >= 20 ? ChatFormatting.RED : ChatFormatting.DARK_RED)))).getColor();
        } else {
            return 0;
        }
    }

    @Override
    public void setDamage(ItemStack stack, int damage) {
    }

    @Override
    public int getDamage(ItemStack stack) {
        CompoundTag cpt = getNBT(stack);
        RedstoneBoardRobotNBT boardNBT = this.robotNBT;

        if (boardNBT != RedstoneBoardRegistry.instance.getEmptyRobotBoard()) {
            long energy = getEnergy(cpt);
            return (int) ((EntityRobotBase.MAX_POWER - energy) / MjAPI.MJ);
        } else {
            return 0;
        }
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        RedstoneBoardRobotNBT boardNBT = this.robotNBT;

        if (boardNBT != RedstoneBoardRegistry.instance.getEmptyRobotBoard()) {
            return (int) (EntityRobotBase.MAX_POWER / MjAPI.MJ);
        } else {
            return 0;
        }
    }
}
