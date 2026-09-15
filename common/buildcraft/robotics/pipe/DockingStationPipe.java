package buildcraft.robotics.pipe;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.gates.IGate;
import buildcraft.api.gates.IGateProvider;
import buildcraft.api.robots.DockingStation;
import buildcraft.api.robots.EntityRobotBase;
import buildcraft.api.robots.IRequestProvider;
import buildcraft.api.robots.RobotManager;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.StatementSlot;
import buildcraft.api.transport.IInjectable;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pluggable.ActionIterator;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.lib.misc.CapUtil;
import buildcraft.lib.misc.StackUtil;
import buildcraft.robotics.BCRoboticsStatements;
import buildcraft.transport.BCTransportPipes;
import buildcraft.transport.pipe.behaviour.PipeBehaviourWood;
import buildcraft.transport.pipe.flow.PipeFlowItems;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;
import java.util.List;

public class DockingStationPipe extends DockingStation implements IRequestProvider {

    private IInjectable injectablePipe = new IInjectable() {
        @Override
        public boolean canInjectItems(Direction from) {
            return true;
        }

        @Override
        public ItemStack injectItem(ItemStack stack, boolean doAdd, Direction from, DyeColor color, double speed) {

            if (getPipe().getPipe().getFlow() instanceof PipeFlowItems) {
                return ((PipeFlowItems) getPipe().getPipe().getFlow()).injectItem(stack, doAdd, from, color, speed);
            }
            return stack;
        }
    };

    private IPipeHolder pipe;

    public DockingStationPipe() {
        // Loading later from NBT - DO NOT TOUCH!
    }

    public DockingStationPipe(IPipeHolder iPipe, Direction side) {
        super(((BlockEntity) iPipe).getBlockPos(), side);
        pipe = iPipe;
        world = iPipe.getPipeWorld();
    }

    public IPipeHolder getPipe() {
        if (pipe == null) {
            BlockEntity tile = world.getBlockEntity(getPos());
            if (tile instanceof IPipeHolder) {
                pipe = (IPipeHolder) tile;
            }
        }

        if (pipe == null || ((BlockEntity) pipe).isRemoved()) {
            // Inconsistency - remove this pipe from the registry.
            RobotManager.registryProvider.getRegistry(world).removeStation(this);
            pipe = null;
        }

        return pipe;
    }

    @Override
    public Iterable<StatementSlot> getActiveActions() {
        return new ActionIterator(getPipe().getPipe());
    }

    @Override
    public IInjectable getItemOutput() {
        if (getPipe().getPipe().getDefinition().flowType != PipeApi.flowItems) {
            return null;
        }

        return injectablePipe;
    }

    @Override
    public EnumPipePart getItemOutputSide() {
        return EnumPipePart.fromFacing(side().getOpposite());
    }

    @Override
    public Container getItemInput() {
        if (getPipe().getPipe().getDefinition().flowType != PipeApi.flowItems) {
            return null;
        }

        if (!(getPipe().getPipe().getDefinition() == BCTransportPipes.woodItem)) {
            return null;
        }

        Direction dir = ((PipeBehaviourWood) getPipe().getPipe().getBehaviour()).getCurrentDir();

        if (dir == null) {
            return null;
        }

        BlockEntity connectedTile = getPipe().getPipeWorld().getBlockEntity(getPos().relative(dir));
        if (connectedTile instanceof Container) {
            return (Container) connectedTile;
        }

        return null;
    }

    @Override
    public EnumPipePart getItemInputSide() {
        if (getPipe().getPipe().getDefinition().flowType != PipeApi.flowItems) {
            return EnumPipePart.CENTER;
        }

        if (!(getPipe().getPipe().getDefinition() == BCTransportPipes.woodItem)) {
            return EnumPipePart.CENTER;
        }

        return EnumPipePart.fromFacing(((PipeBehaviourWood) getPipe().getPipe().getBehaviour()).getCurrentDir()).opposite();
    }

    @Override
    public IFluidHandler getFluidInput() {
        if (getPipe().getPipe().getDefinition().flowType != PipeApi.flowFluids) {
            return null;
        }

        if (!(getPipe().getPipe().getDefinition() == BCTransportPipes.woodFluid)) {
            return null;
        }

        Direction dir = ((PipeBehaviourWood) getPipe().getPipe().getBehaviour()).getCurrentDir();

        if (dir == null) {
            return null;
        }

        BlockEntity connectedTile = getPipe().getPipeWorld().getBlockEntity(getPos().relative(dir));
        if (connectedTile instanceof IFluidHandler) {
            return (IFluidHandler) connectedTile;
        }

        return null;
    }

    @Override
    public EnumPipePart getFluidInputSide() {
        if (getPipe().getPipe().getDefinition().flowType != PipeApi.flowFluids) {
            return EnumPipePart.CENTER;
        }

        if (!(getPipe().getPipe().getDefinition() == BCTransportPipes.woodFluid)) {
            return EnumPipePart.CENTER;
        }

        return EnumPipePart.fromFacing(((PipeBehaviourWood) getPipe().getPipe().getBehaviour()).getCurrentDir()).opposite();
    }

    @Override
    public IFluidHandler getFluidOutput() {
        if (getPipe().getPipe().getDefinition().flowType != PipeApi.flowFluids) {
            return null;
        }

        return (getPipe().getPipe().getFlow()).getCapability(CapUtil.CAP_FLUIDS, side).orElse(null);
    }

    @Override
    public EnumPipePart getFluidOutputSide() {
        return EnumPipePart.CENTER;
    }

    @Override
    public boolean providesPower() {
        return getPipe().getPipe().getDefinition().flowType == PipeApi.flowPower;
    }

    @Override
    public IRequestProvider getRequestProvider() {
        for (Direction dir : Direction.values()) {
            BlockEntity nearbyTile = getPipe().getPipeWorld().getBlockEntity(getPos().relative(dir));
            if (nearbyTile instanceof IRequestProvider) {
                return (IRequestProvider) nearbyTile;
            }
        }
        return this;
    }

    @Override
    public boolean isInitialized() {
        if (getPipe() == null || getPipe().getPipe() == null) {
            return false;
        }
        return true;
    }

    @Override
    public boolean take(EntityRobotBase robot) {
        boolean result = super.take(robot);
        if (result) {
            getPipe().scheduleRenderUpdate();
        }
        return result;
    }

    @Override
    public boolean takeAsMain(EntityRobotBase robot) {
        boolean result = super.takeAsMain(robot);
        if (result) {
            getPipe().scheduleRenderUpdate();
        }
        return result;
    }

    @Override
    public void unsafeRelease(EntityRobotBase robot) {
        super.unsafeRelease(robot);
        if (robotTaking() == null && getPipe() != null) {
            getPipe().scheduleRenderUpdate();
        }
    }

    @Override
    public void onChunkUnload() {
        pipe = null;
    }

    @Override
    public int getRequestsCount() {
        return 127;
    }

    @Nonnull
    @Override
    public ItemStack getRequest(int slot) {
        int facing = (slot & 0x70) >> 4;
        int action = (slot & 0xc) >> 2;
        int param = slot & 0x3;

        if (facing >= 6) {
            return StackUtil.EMPTY;
        }

        Direction side = Direction.from3DDataValue(facing);
        PipePluggable plug = getPipe().getPluggable(side);
        IGate gate = null;
        if (plug instanceof IGateProvider) {
            gate = ((IGateProvider) plug).getGate();
        } else {
            return StackUtil.EMPTY;
        }

        List<IStatement> actions = gate.getActions();
        if (actions.size() <= action) {
            return StackUtil.EMPTY;
        }

        if (actions.get(action) != BCRoboticsStatements.actionStationRequestItems) {
            return StackUtil.EMPTY;
        }

        List<StatementSlot> activeActions = gate.getActiveActions();

        StatementSlot slotStmt = null;
        for (StatementSlot stmt : activeActions) {
            if (stmt.statement == actions.get(action)) {
                slotStmt = stmt;
                break;
            }
        }
        if (slotStmt == null) {
            return StackUtil.EMPTY;
        }
        if (slotStmt.parameters.length <= param) {
            return StackUtil.EMPTY;
        }

        if (slotStmt.parameters[param] == null) {
            return StackUtil.EMPTY;
        }

        return slotStmt.parameters[param].getItemStack();
    }

    @Nonnull
    @Override
    public ItemStack offerItem(int slot, ItemStack stack) {
        ItemStack notConsumed = injectablePipe.injectItem(stack, true, side, null, 0);
        if (notConsumed.getCount() > 0) {
            return notConsumed.copy();
        }
        return StackUtil.EMPTY;
    }
}
