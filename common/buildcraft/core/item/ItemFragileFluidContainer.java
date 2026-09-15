package buildcraft.core.item;

import buildcraft.api.items.IItemFluidShard;
import buildcraft.lib.fluid.BCFluid;
import buildcraft.lib.fluid.BCFluidAttributes;
import buildcraft.lib.item.ItemBC_Neptune;
import buildcraft.lib.misc.CapUtil;
import buildcraft.lib.misc.LocaleUtil;
import buildcraft.lib.misc.NBTUtilBC;
import buildcraft.lib.misc.StackUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemFragileFluidContainer extends ItemBC_Neptune implements IItemFluidShard {

    // Half of a bucket
    public static final int MAX_FLUID_HELD = 500;

    public ItemFragileFluidContainer(String idBC, Item.Properties properties) {
        super(idBC, properties);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag nbt) {
        return new FragileFluidHandler(stack);
    }

    @Override
    protected void addSubItems(NonNullList<ItemStack> items) {
        // Never allow this to be displayed in a creative tab -- we don't want to list every single fluid...
    }

    @Override
    public Component getName(ItemStack stack) {
        FluidStack fluid = getFluid(stack);

        Component localized;

        if (fluid == null) {
            localized = Component.literal("ERROR! NULL FLUID!");
        } else if (fluid.getRawFluid() instanceof BCFluid bcFluid) {
            if (((BCFluidAttributes) bcFluid.getFluidType()).isHeatable()) {
                // Add the heatable bit to the end of the name
                localized = bcFluid.getFluidType().getDescription(fluid);
                return Component.translatable(getDescriptionId(stack), localized);
            } else {
                localized = fluid.getDisplayName();
            }
        } else {
            localized = fluid.getDisplayName();
        }
        return Component.translatable(this.getDescriptionId(stack), localized);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        CompoundTag fluidTag = stack.getTagElement("fluid");
        if (fluidTag != null) {
            FluidStack fluid = FluidStack.loadFluidStackFromNBT(fluidTag);
            if (fluid != null && fluid.getAmount() > 0) {
                tooltip.add(LocaleUtil.localizeFluidStaticAmountComponent(fluid.getAmount(), MAX_FLUID_HELD));
            }
        }
    }

    @Override
    public void addFluidDrops(NonNullList<ItemStack> toDrop, FluidStack fluid) {
        if (fluid == null) {
            return;
        }
        int amount = fluid.getAmount();
        if (amount >= MAX_FLUID_HELD) {
            FluidStack fluid2 = fluid.copy();
            fluid2.setAmount(MAX_FLUID_HELD);
            while (amount >= MAX_FLUID_HELD) {
                ItemStack stack = new ItemStack(this);
                setFluid(stack, fluid2);
                amount -= MAX_FLUID_HELD;
                toDrop.add(stack);
            }
        }
        if (amount > 0) {
            ItemStack stack = new ItemStack(this);
            setFluid(stack, new FluidStack(fluid, amount));
            toDrop.add(stack);
        }
    }

    static void setFluid(ItemStack container, FluidStack fluid) {
        CompoundTag nbt = NBTUtilBC.getItemData(container);
        nbt.put("fluid", fluid.writeToNBT(new CompoundTag()));
    }

    @Nullable
    static FluidStack getFluid(ItemStack container) {
        if (container.isEmpty()) {
            return null;
        }
        CompoundTag fluidNbt = container.getTagElement("fluid");
        if (fluidNbt == null) {
            return null;
        }
        return FluidStack.loadFluidStackFromNBT(fluidNbt);
    }

    public class FragileFluidHandler implements IFluidHandlerItem, ICapabilityProvider {

        @Nonnull
        private ItemStack container;

        public FragileFluidHandler(@Nonnull ItemStack container) {
            this.container = container;
        }

//        @Override

        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull final Capability<T> capability, final @Nullable Direction side) {
            if (capability == ForgeCapabilities.FLUID_HANDLER_ITEM
                    || capability == CapUtil.CAP_FLUIDS) {
                return LazyOptional.of(() -> this).cast();
            }
            return LazyOptional.empty();
        }

        // 1.18.2: divided into 3 methods
//        @Override

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public int getTankCapacity(int tank) {
            return MAX_FLUID_HELD;
        }

        @NotNull
        @Override
        public FluidStack getFluidInTank(int tank) {
            FluidStack fluid = ItemFragileFluidContainer.getFluid(container);
            return fluid != null ? fluid : StackUtil.EMPTY_FLUID;
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction doFill) {
            return 0;
        }

        @NotNull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction doDrain) {
            FluidStack fluid = ItemFragileFluidContainer.getFluid(container);
            if (fluid == null || resource == null) {
                return StackUtil.EMPTY_FLUID;
            }
            if (!fluid.isFluidEqual(resource)) {
                return StackUtil.EMPTY_FLUID;
            }
            return drain(resource.getAmount(), doDrain);
        }

        @NotNull
        @Override
        public FluidStack drain(int maxDrain, FluidAction doDrain) {
            FluidStack fluid = ItemFragileFluidContainer.getFluid(container);
            if (fluid == null || maxDrain <= 0) {
                return StackUtil.EMPTY_FLUID;
            }
            int toDrain = Math.min(maxDrain, fluid.getAmount());
            FluidStack f = new FluidStack(fluid, toDrain);
            if (doDrain.execute()) {
                fluid.setAmount(fluid.getAmount() - toDrain);
                if (fluid.getAmount() <= 0) {
                    fluid = StackUtil.EMPTY_FLUID;
                    container = StackUtil.EMPTY;
                } else {
                    setFluid(container, fluid);
                }
            }
            return f;
        }

        @Override
        public ItemStack getContainer() {
            return container;
        }
    }
}
