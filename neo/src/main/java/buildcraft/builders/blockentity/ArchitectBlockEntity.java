/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders.blockentity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
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
import buildcraft.builders.BcBuildersBlocks;
import buildcraft.builders.BcBuildersItems;
import buildcraft.builders.blueprint.BlueprintData;
import buildcraft.builders.blueprint.BlueprintItems;
import buildcraft.builders.marker.MarkerPair;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The M4.17 architect table block entity (C route, replaces the {@code PlaceholderBlockEntity} under the unchanged id
 * {@code buildcraftbuilders:architect}): it scans the box a nearby construction-marker pair defines into the blank
 * blueprint item in its input slot and moves the written blueprint to its output slot. Legacy counterpart:
 * {@code buildcraft.builders.tile.TileArchitectTable} (frozen 1.20.1 tree).
 *
 * <p><b>Legacy audit and the v1 mapping:</b>
 * <ul>
 * <li><b>Box</b> &mdash; legacy: the volume box at the facing-adjacent position ({@code WorldSavedDataVolumeBoxes}) or
 * an {@code IAreaProvider} tile. v1: {@link ConstructionMarkerBlockEntity#findNearestPairedMarker} (the nearest marker
 * pair box, revalidated every {@link #BOX_RECHECK_TICKS}; the machine's own cell must stay outside the box).</li>
 * <li><b>Scan loop</b> &mdash; legacy: a {@code BoxIterator} (XZY min-to-max) filling {@code blueprintScannedData} with
 * palette indices through the {@code SchematicBlockManager} (per-block schematic strategies), a few cells per tick
 * ({@code EnumSnapshotType#maxPerTick}). v1: the {@link BlueprintData} cell order (y bottom-up, then z, then x) at
 * {@link #BLOCKS_PER_TICK} cells per tick; the palette is plain block ids (the schematic strategy layer has not
 * migrated), and marker cells scan as air.</li>
 * <li><b>Items</b> &mdash; legacy: {@code invSnapshotIn} (blank snapshot, consumed) / {@code invSnapshotOut} (the used
 * snapshot carrying only a hash {@code Header}), payload stored in world saved data. v1: the same in/out split as the
 * two slots of one {@link #inv} handler, but the full {@link BlueprintData} payload rides the output item itself
 * ({@link BlueprintItems}, custom_data component). The blank input item is consumed exactly like the legacy
 * {@code finishScanning}; no ink or extra cost is charged (the baseline charges none either &mdash; the cost is the
 * blank blueprint).</li>
 * <li><b>Snapshot type</b> &mdash; legacy: blueprint vs template ({@code EnumSnapshotType}, template = air/solid only).
 * v1: blueprint shape only; both placeholder items are accepted as blanks (see
 * {@link BlueprintItems#isBlueprintItem}).</li>
 * </ul>
 *
 * <p>There is no GUI in v1 (the legacy {@code ContainerArchitectTable} migrates with the menu framework): a blank
 * blueprint goes in by right-clicking the table with it (or through the input slot's item capability), and a right
 * click with an empty hand logs the [M417] status line. Client sync is the quarry's Beacon pattern.
 */
public class ArchitectBlockEntity extends BlockEntity {

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    /** Scan speed in cells per tick (legacy {@code EnumSnapshotType.BLUEPRINT#maxPerTick}; v1 keeps one fixed pace). */
    public static final int BLOCKS_PER_TICK = 8;
    /** Ticks between marker-box revalidation attempts while idle. */
    public static final int BOX_RECHECK_TICKS = 20;

    /** Slot 0 = the blank input, slot 1 = the written blueprint (legacy {@code invSnapshotIn}/{@code invSnapshotOut}). */
    public static final int SLOT_IN = 0;
    public static final int SLOT_OUT = 1;
    public static final int SLOTS = 2;

    /**
     * The in/out buffers: slot 0 accepts blank blueprint items only, slot 1 accepts no inserts at all (the legacy
     * INSERT/EXTRACT access split of {@code invSnapshotIn}/{@code invSnapshotOut}).
     */
    private final ItemStacksResourceHandler inv = new ItemStacksResourceHandler(SLOTS) {
        @Override
        public boolean isValid(int index, ItemResource resource) {
            if (index == SLOT_OUT) {
                return false;
            }
            ItemStack stack = resource.toStack(1);
            return BlueprintItems.isBlueprintItem(stack) && !BlueprintItems.hasData(stack);
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previous) {
            ArchitectBlockEntity.this.syncToClients();
        }
    };

    /** The box currently being scanned (and the marker anchor it came from), null while no pair is in range. */
    @Nullable
    private MarkerPair box;
    @Nullable
    private BlockPos boxAnchor;
    /** The blueprint-local scan cursor ({@link BlueprintData#cellIndex} order); negative while idle/finished. */
    private int scanCursor = -1;
    /** The partial per-cell palette indices of the running scan ({@code blueprintScannedData} analogue). */
    @Nullable
    private int[] scanData;
    /** The running scan's volume (cells per axis, fixed when the scan begins). */
    private int scanSizeX;
    private int scanSizeY;
    private int scanSizeZ;
    /** The palette collected so far, first-scan order ({@code blueprintScannedPalette} analogue). */
    private final List<String> palette = new ArrayList<>();

    public ArchitectBlockEntity(BlockPos pos, BlockState state) {
        super(BcBuildersBlockEntities.ARCHITECT.value(), pos, state);
    }

    // ----------------------------------------------------------------- access (probe / rig / capability)

    /** The capability view: every side reaches both slots (inserts land only in slot 0, per {@link #inv}). */
    public ItemStacksResourceHandler getInv() {
        return this.inv;
    }

    /** Inserts a blank blueprint into the input slot (the programmatic twin of the right-click path, the
     * {@code FillerBlockEntity#insertResource} precedent). Returns the rejected remainder. */
    public ItemStack insertBlank(ItemStack stack) {
        if (!BlueprintItems.isBlueprintItem(stack) || BlueprintItems.hasData(stack)) {
            return stack;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = this.inv.insert(SLOT_IN, ItemResource.of(stack), stack.getCount(), transaction);
            transaction.commit();
            ItemStack remainder = stack.copy();
            remainder.setCount(stack.getCount() - inserted);
            return remainder;
        }
    }

    /** The written blueprint (the output slot's stack), or empty while idle/scanning. */
    public ItemStack getWrittenBlueprint() {
        ItemStack written = this.inv.getResource(SLOT_OUT).toStack(1);
        return written.isEmpty() ? ItemStack.EMPTY : written;
    }

    public @Nullable MarkerPair getBox() {
        return this.box;
    }

    /** True while a scan is consuming cells (the legacy {@code scanning} flag). */
    public boolean isScanning() {
        return this.scanCursor >= 0;
    }

    /** Right-click/empty-hand status line (the [M417] evidence). */
    public void logStatus() {
        if (this.isScanning() && this.scanData != null && this.box != null) {
            LOGGER.info("[M417] architect at {}: scanning box {} at cell {} / {}",//
                this.worldPosition, this.box.sizeSummary(), this.scanCursor, this.scanData.length);
        } else if (!this.getWrittenBlueprint().isEmpty()) {
            BlueprintData data = BlueprintItems.readData(this.getWrittenBlueprint());
            LOGGER.info("[M417] architect at {}: blueprint ready ({})", this.worldPosition,
                data == null ? "unreadable" : data.summary());
        } else {
            LOGGER.info("[M417] architect at {}: idle (box: {}, blank blueprint in input: {})",//
                this.worldPosition, this.box == null ? "none" : this.box.sizeSummary(),
                !this.inv.getResource(SLOT_IN).isEmpty());
        }
    }

    // ----------------------------------------------------------------- server tick

    /** Per-tick logic, wired through {@code ArchitectBlock#getTicker} (vanilla furnace static-tick pattern). */
    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, ArchitectBlockEntity architect) {
        architect.tickWork(level);
    }

    private void tickWork(ServerLevel level) {
        if (this.scanCursor < 0) {
            this.revalidateBox(level);
            if (this.box != null && this.scanData == null && !this.inv.getResource(SLOT_IN).isEmpty()
                && this.inv.getResource(SLOT_OUT).isEmpty()) {
                this.beginScan();
            }
            return;
        }
        for (int i = 0; i < BLOCKS_PER_TICK && this.scanCursor >= 0; i++) {
            this.scanSingle(level);
        }
        if (this.scanCursor < 0 && this.scanData != null) {
            this.finishScan();
        }
    }

    /** Re-checks the marker box: keeps an intact pair, drops a broken one, picks up a new nearest pair. */
    private void revalidateBox(ServerLevel level) {
        boolean recheckTick = (level.getGameTime() % BOX_RECHECK_TICKS) == 0 || this.boxAnchor == null;
        if (!recheckTick) {
            return;
        }
        if (this.boxAnchor != null) {
            if (level.getBlockEntity(this.boxAnchor) instanceof ConstructionMarkerBlockEntity marker
                && marker.isConnected() && marker.getPair() != null
                && !marker.getPair().contains(this.worldPosition)) {
                this.box = marker.getPair();
                return;
            }
            this.box = null;
            this.boxAnchor = null;
        }
        ConstructionMarkerBlockEntity marker = ConstructionMarkerBlockEntity.findNearestPairedMarker(
            level, this.worldPosition);
        if (marker != null && marker.getPair() != null) {
            this.box = marker.getPair();
            this.boxAnchor = marker.getBlockPos();
            LOGGER.info("[M417] architect at {} linked to marker box {} at {}",//
                this.worldPosition, this.box.sizeSummary(), this.boxAnchor);
        }
    }

    /** Starts one scan: fixes the volume, allocates the per-cell data and arms the cursor. */
    private void beginScan() {
        MarkerPair box = this.box;
        if (box == null) {
            return;
        }
        int sizeX = box.max().getX() - box.min().getX() + 1;
        int sizeY = box.max().getY() - box.min().getY() + 1;
        int sizeZ = box.max().getZ() - box.min().getZ() + 1;
        long cells = (long) sizeX * sizeY * sizeZ;
        if (cells > BlueprintData.MAX_CELLS) {
            LOGGER.info("[M417] architect at {}: box {} too large for one blueprint item, refusing to scan",//
                this.worldPosition, box.sizeSummary());
            return;
        }
        this.scanSizeX = sizeX;
        this.scanSizeY = sizeY;
        this.scanSizeZ = sizeZ;
        this.scanData = new int[(int) cells];
        this.palette.clear();
        this.scanCursor = 0;
        LOGGER.info("[M417] architect at {}: scan start, box {} ({} cells)",//
            this.worldPosition, box.sizeSummary(), cells);
    }

    /** Scans the cell at {@link #scanCursor} and advances; parks the cursor at -1 when the box is exhausted. */
    private void scanSingle(ServerLevel level) {
        int[] data = this.scanData;
        if (data == null || this.box == null || this.scanCursor < 0 || this.scanCursor >= data.length) {
            this.scanCursor = -1;
            return;
        }
        int index = this.scanCursor;
        int y = index / (this.scanSizeZ * this.scanSizeX);
        int rem = index % (this.scanSizeZ * this.scanSizeX);
        int z = rem / this.scanSizeX;
        int x = rem % this.scanSizeX;
        BlockPos worldPos = new BlockPos(
            this.box.min().getX() + x, this.box.min().getY() + y, this.box.min().getZ() + z);
        BlockState state = level.getBlockState(worldPos);
        if (!state.isAir() && state.getBlock() != BcBuildersBlocks.MARKER_CONSTRUCTION.value()) {
            String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            int paletteIndex = this.palette.indexOf(id);
            if (paletteIndex < 0) {
                paletteIndex = this.palette.size();
                this.palette.add(id);
            }
            data[index] = paletteIndex + 1;
        }
        this.scanCursor++;
        if (this.scanCursor >= data.length) {
            this.scanCursor = -1;
        }
    }

    /** Legacy {@code finishScanning}: wraps the scan into a {@link BlueprintData}, consumes the blank input and moves
     * the written blueprint to the output slot. */
    private void finishScan() {
        int[] data = this.scanData;
        if (data == null) {
            return;
        }
        BlueprintData blueprint = BlueprintData.fromRaw(this.scanSizeX, this.scanSizeY, this.scanSizeZ,
            this.palette, data);
        ItemStack written = new ItemStack(BcBuildersItems.SNAPSHOT_BLUEPRINT.get());
        BlueprintItems.writeData(written, blueprint);
        this.inv.set(SLOT_OUT, ItemResource.of(written), 1);
        // consume the blank input (the legacy finishScanning stack shrink)
        ItemResource blank = this.inv.getResource(SLOT_IN);
        if (!blank.isEmpty()) {
            try (Transaction transaction = Transaction.openRoot()) {
                int extracted = this.inv.extract(SLOT_IN, blank, 1, transaction);
                if (extracted > 0) {
                    transaction.commit();
                }
            }
        }
        this.scanData = null;
        LOGGER.info("[M417] architect at {}: blueprint written -> {}", this.worldPosition, blueprint.summary());
        this.syncToClients();
    }

    // ----------------------------------------------------------------- persistence + client sync

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.inv.serialize(output.child("bc_inv"));
        if (this.box != null) {
            output.store("bc_box", CompoundTag.CODEC, this.box.writeTo(new CompoundTag()));
        }
        if (this.boxAnchor != null) {
            output.store("bc_anchor", BlockPos.CODEC, this.boxAnchor);
        }
        output.putInt("bc_scan", this.scanCursor);
        if (this.scanData != null) {
            output.putIntArray("bc_scan_data", this.scanData);
            CompoundTag paletteTag = new CompoundTag();
            paletteTag.putInt("n", this.palette.size());
            for (int i = 0; i < this.palette.size(); i++) {
                paletteTag.putString("p" + i, this.palette.get(i));
            }
            output.store("bc_scan_palette", CompoundTag.CODEC, paletteTag);
            output.putInt("bc_scan_sx", this.scanSizeX);
            output.putInt("bc_scan_sy", this.scanSizeY);
            output.putInt("bc_scan_sz", this.scanSizeZ);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.inv.deserialize(input.childOrEmpty("bc_inv"));
        this.box = input.read("bc_box", CompoundTag.CODEC).map(MarkerPair::from).orElse(null);
        this.boxAnchor = input.read("bc_anchor", BlockPos.CODEC).orElse(null);
        this.scanCursor = input.getIntOr("bc_scan", -1);
        this.scanData = input.getIntArray("bc_scan_data").orElse(null);
        this.palette.clear();
        input.read("bc_scan_palette", CompoundTag.CODEC).ifPresent(tag -> {
            int n = tag.getIntOr("n", 0);
            for (int i = 0; i < n; i++) {
                this.palette.add(tag.getStringOr("p" + i, ""));
            }
        });
        this.scanSizeX = input.getIntOr("bc_scan_sx", 1);
        this.scanSizeY = input.getIntOr("bc_scan_sy", 1);
        this.scanSizeZ = input.getIntOr("bc_scan_sz", 1);
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
