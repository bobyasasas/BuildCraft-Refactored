/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.tile;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.net.IMessage;
import buildcraft.api.recipes.BuildcraftRecipeRegistry;
import buildcraft.api.recipes.IProgrammingRecipe;
import buildcraft.api.tiles.IHasWork;
import buildcraft.lib.misc.InventoryUtil;
import buildcraft.lib.misc.NBTUtilBC;
import buildcraft.lib.misc.StackUtil;
import buildcraft.lib.misc.data.IdAllocator;
import buildcraft.lib.net.MessageManager;
import buildcraft.lib.net.PacketBufferBC;
import buildcraft.lib.tile.TileBC_Neptune;
import buildcraft.lib.tile.item.ItemHandlerManager;
import buildcraft.lib.tile.item.ItemHandlerSimple;
import buildcraft.silicon.BCSiliconBlocks;
import buildcraft.silicon.BCSiliconMenuTypes;
import buildcraft.silicon.container.ContainerProgrammingTable_Neptune;
import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class TileProgrammingTable_Neptune extends TileLaserTableBase implements IHasWork {
    public static final IdAllocator IDS = TileBC_Neptune.IDS.makeChild("assembly_table");
    public static final int NET_RECIPE_STATE = IDS.allocId("RECIPE_STATE");

    public static final int WIDTH = 6;
    public static final int HEIGHT = 4;

    @Nonnull
    public final List<ResourceLocation> availableRecipeIds = Lists.newArrayList();
    @Nonnull
    public List<IProgrammingRecipe> optionRecipes = List.of();
    public int optionId;
    private boolean queuedNetworkUpdate = false;

    public final ItemHandlerSimple input = itemManager.addInvHandler(
            "input",
            1,
            ItemHandlerManager.EnumAccess.BOTH,
            EnumPipePart.VALUES
    );

    public final ItemHandlerSimple output = itemManager.addInvHandler(
            "output",
            1,
            ItemHandlerManager.EnumAccess.EXTRACT,
            EnumPipePart.VALUES
    );

    private void queueNetworkUpdate() {
        queuedNetworkUpdate = true;
    }

    public TileProgrammingTable_Neptune(BlockPos pos, BlockState blockState) {
        super(BCSiliconBlocks.programmingTableTile.get(), pos, blockState);
    }

    @Override
    public void update() {
        super.update();

        if (level.isClientSide) {
            return;
        }

        if (queuedNetworkUpdate) {
            sendNetworkUpdate(NET_GUI_DATA);
            queuedNetworkUpdate = false;
        }

        if (optionRecipes.isEmpty()) {
            return;
        }

        if (this.input.getStackInSlot(0).isEmpty()) {
            optionRecipes = List.of();
            return;
        }

        if (optionId >= 0 && power >= optionRecipes.get(optionId).getEnergyCost()) {
            if (optionRecipes.get(optionId).canCraft(this.input.getStackInSlot(0))) {
                ItemStack remaining = optionRecipes.get(optionId).craft(this.input.getStackInSlot(0));
                if (remaining != null && remaining.getCount() > 0) {
                    power = 0;
                    this.input.extractItem(0, remaining.getCount(), false);
                    outputStack(remaining, this.output, 0);
                }
            }
            findRecipe();
        }
    }

    protected void outputStack(ItemStack remaining, ItemHandlerSimple inv, int slot) {
        if (inv != null && !remaining.isEmpty()) {
            ItemStack inside = inv.getStackInSlot(slot);

            if (inside.isEmpty() || inside.getCount() <= 0) {
                inv.setStackInSlot(slot, remaining);
                return;
            } else if (StackUtil.canMerge(inside, remaining)) {
                remaining.shrink(StackUtil.mergeStacks(remaining, inside, true));
            }
            InventoryUtil.addToBestAcceptor(level, getBlockPos(), null, remaining.copy());
        }
    }

//    /* IINVENTORY */
//    @Override

//    @Override

    @Override
    protected void onSlotChange(IItemHandlerModifiable handler, int slot, @Nonnull ItemStack before, @Nonnull ItemStack after) {
        super.onSlotChange(handler, slot, before, after);

        if (handler == input && !StackUtil.isSameItemSameDamageSameTagSameCount(before, after)) {
            findRecipe();
        }
    }

//    @Override

//    @Override

//    @Override

    @Override
    public void readPayload(int id, PacketBufferBC stream, NetworkDirection side, NetworkEvent.Context ctx) throws IOException {
        super.readPayload(id, stream, side, ctx);

        if (id == NET_GUI_DATA) {
            if (stream.readBoolean()) {
                int size = stream.readInt();
                availableRecipeIds.clear();
                for (int i = 0; i < size; i++) {
                    availableRecipeIds.add(stream.readResourceLocation());
                }
            } else {
                availableRecipeIds.clear();
            }
            optionId = stream.readByte();
            updateRecipe();
        } else if (side == NetworkDirection.PLAY_TO_SERVER && id == NET_RECIPE_STATE) {
            optionId = stream.readByte();
            if (optionId >= optionRecipes.size()) {
                optionId = -1;
            } else if (optionId < -1) {
                optionId = -1;
            }

            queueNetworkUpdate();
        }
    }

    @Override
    public void writePayload(int id, PacketBufferBC stream, Dist side) {
        super.writePayload(id, stream, side);
        if (id == NET_GUI_DATA) {
            stream.writeBoolean(!availableRecipeIds.isEmpty());
            if (!availableRecipeIds.isEmpty()) {
                stream.writeInt(availableRecipeIds.size());
                availableRecipeIds.forEach(stream::writeResourceLocation);
            }
            stream.writeByte(optionId);
        }
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);

        if (nbt.contains("recipeId") && nbt.contains("optionId")) {
            ListTag recipeIdTag = nbt.getList("recipeId", Tag.TAG_STRING);
            availableRecipeIds.addAll(NBTUtilBC.readStringList(recipeIdTag).map(ResourceLocation::new).collect(Collectors.toList()));
            optionId = nbt.getByte("optionId");
        } else {
            availableRecipeIds.clear();
        }
        runWhenWorldNotNull(this::updateRecipe, false);
    }

    @Override
    public void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);

        if (!availableRecipeIds.isEmpty()) {
            ListTag recipeIdTag = NBTUtilBC.writeStringList(availableRecipeIds.stream().map(ResourceLocation::toString));
            nbt.put("recipeId", recipeIdTag);
            nbt.putByte("optionId", (byte) optionId);
        }
    }

    @Override
    public long getTarget() {
        if (hasWork()) {
            return optionRecipes.get(optionId).getEnergyCost();
        } else {
            return 0;
        }
    }

    public void findRecipe() {
        List<ResourceLocation> oldIds = List.copyOf(availableRecipeIds);
        availableRecipeIds.clear();

        if (!this.input.getStackInSlot(0).isEmpty()) {
            for (IProgrammingRecipe recipe : BuildcraftRecipeRegistry.programmingRecipes.getRecipes(level)) {
                if (recipe.canCraft(this.input.getStackInSlot(0))) {
                    availableRecipeIds.add(recipe.getId());
                }
            }
        }

        if (
                (
                        !oldIds.isEmpty()
                                && !availableRecipeIds.isEmpty() &&
                                !(oldIds.containsAll(availableRecipeIds) && availableRecipeIds.containsAll(oldIds))
                )
                        || (oldIds.isEmpty() && !availableRecipeIds.isEmpty())
                        || (!oldIds.isEmpty() && availableRecipeIds.isEmpty())
        ) {
            optionId = -1;
            updateRecipe();
            queueNetworkUpdate();
        }
    }

    public void updateRecipe() {
        List<IProgrammingRecipe> currentRecipes = Lists.newArrayList();
        availableRecipeIds.forEach(id -> currentRecipes.add(BuildcraftRecipeRegistry.programmingRecipes.getRecipe(level, id)));
        if (!currentRecipes.isEmpty()) {
            optionRecipes = BuildcraftRecipeRegistry.programmingRecipes.getOptions(currentRecipes, WIDTH, HEIGHT);
        } else {
            optionRecipes = List.of();
        }
    }

    public void rpcSelectOption(final int pos) {
//            @Override
        IMessage message = createMessage(NET_RECIPE_STATE, (data) ->
        {
            data.writeByte(pos);
        });
        MessageManager.sendToServer(message);
    }

    @Override
    public boolean hasWork() {
        return !optionRecipes.isEmpty() && optionId >= 0 && this.output.getStackInSlot(0).isEmpty();
    }

//    @Override

//    @Override

//    @Override

//    @Override

//    @Override

//    @Override

    // MenuProvider

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerProgrammingTable_Neptune(BCSiliconMenuTypes.PROGRAMMING_TABLE, id, player, this);
    }

//    @Override
}
