/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import buildcraft.builders.BcBuildersBlockEntities;
import buildcraft.builders.blueprint.BlueprintData;
import buildcraft.builders.blueprint.BlueprintItems;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

/**
 * The M4.17 electronic library block entity (C route, replaces the {@code PlaceholderBlockEntity} under the unchanged
 * id {@code buildcraftbuilders:library}): a nine-slot blueprint shelf exposed to automation through the item
 * capability, plus the [M417] index line listing every stored blueprint and its volume summary. Legacy counterpart:
 * {@code buildcraft.builders.tile.TileElectronicLibrary} (frozen 1.20.1 tree).
 *
 * <p><b>Legacy audit and the v1 mapping:</b>
 * <ul>
 * <li><b>Storage</b> &mdash; legacy: four single slots ({@code invDownIn/invDownOut/invUpIn/invUpOut}) streaming
 * payloads to and from {@code GlobalSavedDataSnapshots} over a 55-tick progress bar (the 8.x network-download
 * metaphor). v1: one real shelf ({@link #SLOTS}, the legacy GUI grid cut to one row); blueprints move in and out whole
 * through the capability (a hopper, a pipe, the probe rig), so there is nothing to stream and the progress bars are
 * gone.</li>
 * <li><b>Index</b> &mdash; legacy: the GUI list of snapshot keys with names. v1: {@link #logIndex()}, the [M417] line
 * per stored blueprint ({@code slot n: <size> blocks=<k>: <block> x<k>}) &mdash; fired on every content change and on
 * right-click, since there is no GUI to draw the list into yet.</li>
 * <li><b>Slot filter</b> &mdash; legacy: snapshot items only ({@code used} for the down flow). v1: blueprint items
 * with a readable {@link BlueprintData} payload only (blank ones carry nothing to index and are rejected; see
 * {@link BlueprintItems}).</li>
 * </ul>
 *
 * <p>Client sync is the quarry's Beacon pattern. No GUI, no MJ (the baseline library has no battery either).
 */
public class LibraryBlockEntity extends BlockEntity {

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    /** The shelf size (the legacy GUI's first row). */
    public static final int SLOTS = 9;

    /** The shelf: blueprint items with data only (inserts and extracts both run through the capability). */
    private final ItemStacksResourceHandler inv = new ItemStacksResourceHandler(SLOTS) {
        @Override
        public boolean isValid(int index, ItemResource resource) {
            return BlueprintItems.hasData(resource.toStack(1));
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previous) {
            LibraryBlockEntity.this.syncToClients();
            if (LibraryBlockEntity.this.level != null && !LibraryBlockEntity.this.level.isClientSide()) {
                LibraryBlockEntity.this.logIndex("contents changed");
            }
        }
    };

    public LibraryBlockEntity(BlockPos pos, BlockState state) {
        super(BcBuildersBlockEntities.LIBRARY.value(), pos, state);
    }

    public ResourceHandler<ItemResource> getInv() {
        return this.inv;
    }

    /** The [M417] index: one line per stored blueprint ({@code slot n: <summary>}) plus the shelf total. */
    public void logIndex(String cause) {
        int stored = 0;
        for (int slot = 0; slot < SLOTS; slot++) {
            if (this.inv.getAmountAsLong(slot) <= 0) {
                continue;
            }
            BlueprintData data = BlueprintItems.readData(this.inv.getResource(slot).toStack(1));
            stored++;
            LOGGER.info("[M417] library at {} index ({}): slot {} -> {}",//
                this.worldPosition, cause, slot, data == null ? "unreadable" : data.summary());
        }
        if (stored == 0) {
            LOGGER.info("[M417] library at {} index ({}): shelf empty", this.worldPosition, cause);
        } else {
            LOGGER.info("[M417] library at {} index ({}): {} blueprint(s) on shelf", this.worldPosition, cause,
                stored);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.inv.serialize(output.child("bc_inv"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.inv.deserialize(input.childOrEmpty("bc_inv"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void syncToClients() {
        this.setChanged();
        if (this.level instanceof ServerLevel serverLevel) {
            BlockState state = this.getBlockState();
            serverLevel.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }
}
