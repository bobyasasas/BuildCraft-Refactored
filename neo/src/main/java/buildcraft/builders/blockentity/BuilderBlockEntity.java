/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders.blockentity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import buildcraft.builders.BcBuildersBlockEntities;
import buildcraft.builders.blueprint.BlueprintData;
import buildcraft.builders.blueprint.BlueprintItems;
import buildcraft.core.blockentity.MjReceiver;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The M4.17 builder block entity (C route, replaces the {@code PlaceholderBlockEntity} under the unchanged id
 * {@code buildcraftbuilders:builder}): it replays a blueprint's blocks into the world one cell per tick, consuming one
 * matching resource stack entry and MJ per placement. Legacy counterpart:
 * {@code buildcraft.builders.tile.TileBuilder} (frozen 1.20.1 tree).
 *
 * <p><b>Legacy audit and the v1 mapping:</b>
 * <ul>
 * <li><b>Power input</b> &mdash; legacy: {@code MjBattery(16000 * MjAPI.MJ)} fed through
 * {@code MjCapabilityHelper(new MjBatteryReceiver(battery))}. Slice: the same receiver semantics through
 * {@link MjReceiver} on the M2.2 micro-MJ scale (the filler/quarry precedent): battery capacity {@link #CAPACITY} =
 * 1,600,000&nbsp;&micro;MJ, refilled by a kinesis pipe neighbour.</li>
 * <li><b>Per-block power</b> &mdash; the frozen 8.x tree drains no MJ per placed block ({@code TemplateBuilder} never
 * touches the battery); the task requires powered building, so the slice prices each placement with the quarry's
 * {@link QuarryBlockEntity#blockWorkCost} ({@code 3,200 * (hardness + 1)} &micro;MJ &mdash; the exact formula the
 * filler slice already ships; flagged for the real builders migration to revisit).</li>
 * <li><b>Blueprint</b> &mdash; legacy: an {@code ItemSnapshot} in {@code invSnapshot} resolving through
 * {@code GlobalSavedDataSnapshots}, rotated by the block's facing. v1: the {@link BlueprintData} carried by the
 * blueprint item itself ({@code invSnapshot} = slot 0 of {@link #inv}), placed unrotated.</li>
 * <li><b>Build origin</b> &mdash; legacy: facing-dependent base position plus the snapshot {@code offset}. v1: the
 * blueprint's min corner builds at {@code pos.relative(EAST, 2)} (no facing blockstate &mdash; the strict registry
 * palette keeps the empty property set; flagged: facing + offset return with those systems).</li>
 * <li><b>Resources</b> &mdash; legacy: the 27-slot {@code invResources}; the builder stalls while the blueprint needs
 * a block the slots do not carry. v1: the same 27 slots ({@link #inv} slots 1..27, insertable through the item
 * capability like the M4.16 machines); each placement takes one matching item.</li>
 * <li><b>Placement loop</b> &mdash; legacy: the {@code BlueprintBuilder} task list (break/place, per-cell schematics,
 * entities). v1: the blueprint's non-air cells in bottom-up order; a cell whose world block already matches is skipped
 * free (the legacy matching-cell skip), a non-air non-matching cell is skipped too (the legacy excavate/break pass is
 * not migrated), one placement per tick with the {@code placed n/total} [M417] progress line every
 * {@link #PROGRESS_EVERY} blocks.</li>
 * </ul>
 *
 * <p>Client sync is the quarry's Beacon pattern; the current cell rides the update tag for a future renderer.
 */
public class BuilderBlockEntity extends BlockEntity implements MjReceiver {

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    /** Internal energy buffer size in &micro;MJ (legacy 16,000 MJ &times;10&#8315;&#8308;, the filler slice value). */
    public static final long CAPACITY = 1_600_000;
    /** Resource slot count (legacy {@code invResources}); plus the blueprint slot 0 makes {@link #INV_SLOTS}. */
    public static final int RESOURCE_SLOTS = 27;
    public static final int INV_SLOTS = 1 + RESOURCE_SLOTS;
    /** Progress log pace in placed blocks. */
    public static final int PROGRESS_EVERY = 8;
    /** The fixed v1 build origin: the blueprint's min corner lands two blocks east of the machine. */
    public static final int ORIGIN_OFFSET_EAST = 2;

    /** Slot 0 = the blueprint ({@code invSnapshot}, must carry {@link BlueprintData}); slots 1.. = the resources. */
    private final ItemStacksResourceHandler inv = new ItemStacksResourceHandler(INV_SLOTS) {
        @Override
        public boolean isValid(int index, ItemResource resource) {
            ItemStack stack = resource.toStack(1);
            if (index == 0) {
                return BlueprintItems.hasData(stack);
            }
            return stack.getItem() instanceof BlockItem;
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previous) {
            if (index == 0) {
                BuilderBlockEntity.this.reloadBlueprint();
            }
            BuilderBlockEntity.this.syncToClients();
        }
    };

    /** Internal energy buffer, &micro;MJ. */
    private long energyStored;
    /** Lifetime energy received (the evidence assertion counter, the filler/quarry precedent). */
    private long totalReceived;
    /** The blueprint currently being built, null without a readable blueprint in slot 0. */
    @Nullable
    private BlueprintData blueprint;
    /** The non-air cells of {@link #blueprint} in placement order, null until reloaded. */
    @Nullable
    private List<BlockPos> cells;
    /** The next index into {@link #cells}; negative while idle/finished. */
    private int cursor;
    /** How many blueprint cells are already satisfied in the world (placed or pre-existing). */
    private int placed;
    /** True once every non-air cell is satisfied. */
    private boolean finished;

    public BuilderBlockEntity(BlockPos pos, BlockState state) {
        super(BcBuildersBlockEntities.BUILDER.value(), pos, state);
    }

    // ----------------------------------------------------------------- access (probe / rig / capability)

    public ResourceHandler<ItemResource> getInv() {
        return this.inv;
    }

    public long getEnergyStored() {
        return this.energyStored;
    }

    public long getTotalReceived() {
        return this.totalReceived;
    }

    public boolean isFinished() {
        return this.finished;
    }

    public int getPlaced() {
        return this.placed;
    }

    public int getTotal() {
        return this.cells == null ? 0 : this.cells.size();
    }

    /** The cell the builder is currently satisfying (client renderer hook), null between placements. */
    public @Nullable BlockPos getCurrentCell() {
        if (this.blueprint == null || this.cells == null || this.cursor < 0 || this.cursor >= this.cells.size()) {
            return null;
        }
        return this.buildOrigin().offset(this.cells.get(this.cursor));
    }

    public @Nullable BlueprintData getBlueprint() {
        return this.blueprint;
    }

    /** The fixed v1 build origin (see the class javadoc). */
    public BlockPos buildOrigin() {
        return this.worldPosition.relative(Direction.EAST, ORIGIN_OFFSET_EAST);
    }

    /** Right-click/empty-hand status line (the [M417] evidence). */
    public void logStatus() {
        LOGGER.info("[M417] builder at {}: blueprint {}, placed {}/{}, energy {} / {} uMJ (totalReceived={})",//
            this.worldPosition, this.blueprint == null ? "none" : this.blueprint.summary(),//
            this.placed, this.getTotal(), this.energyStored, CAPACITY, this.totalReceived);
    }

    // ----------------------------------------------------------------- blueprint lifecycle

    private void reloadBlueprint() {
        ItemStack stack = this.inv.getResource(0).toStack(1);
        BlueprintData data = BlueprintItems.readData(stack);
        if (data != null && data.equals(this.blueprint)) {
            return;
        }
        this.blueprint = data;
        this.cells = data == null ? null : data.nonAirCells();
        this.cursor = this.cells == null || this.cells.isEmpty() ? -1 : 0;
        this.placed = 0;
        this.finished = this.cells == null || this.cells.isEmpty();
    }

    /** True when one of the resource slots can satisfy the cell block {@code blockId}. */
    private boolean hasResourceFor(String blockId) {
        for (int slot = 1; slot < INV_SLOTS; slot++) {
            if (this.inv.getAmountAsLong(slot) <= 0) {
                continue;
            }
            ItemStack stack = this.inv.getResource(slot).toStack(1);
            if (stack.getItem() instanceof BlockItem blockItem
                && BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).toString().equals(blockId)) {
                return true;
            }
        }
        return false;
    }

    /** Takes one matching item out of the resource slots (the per-placement consumption). */
    private boolean consumeOneResource(String blockId) {
        for (int slot = 1; slot < INV_SLOTS; slot++) {
            if (this.inv.getAmountAsLong(slot) <= 0) {
                continue;
            }
            ItemStack stack = this.inv.getResource(slot).toStack(1);
            if (stack.getItem() instanceof BlockItem blockItem
                && BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).toString().equals(blockId)) {
                try (Transaction transaction = Transaction.openRoot()) {
                    int extracted = this.inv.extract(slot, ItemResource.of(stack), 1, transaction);
                    if (extracted > 0) {
                        transaction.commit();
                        return true;
                    }
                }
                return false;
            }
        }
        return false;
    }

    // ----------------------------------------------------------------- MjReceiver

    @Override
    public long getPowerRequested() {
        if (this.finished || this.blueprint == null || this.cells == null
            || this.cursor < 0 || this.cursor >= this.cells.size()) {
            return 0;
        }
        BlockPos cell = this.cells.get(this.cursor);
        String blockId = this.blueprint.idAt(cell.getX(), cell.getY(), cell.getZ());
        if (blockId == null || !this.hasResourceFor(blockId)) {
            return 0; // the legacy builder refuses power while the resources for the next block are missing
        }
        return CAPACITY - this.energyStored;
    }

    @Override
    public long receivePower(long microJoules, boolean simulate) {
        long accepted = Math.min(microJoules, CAPACITY - this.energyStored);
        if (!simulate && accepted > 0) {
            this.energyStored += accepted;
            this.totalReceived += accepted;
            this.syncToClients();
        }
        return microJoules - accepted;
    }

    // ----------------------------------------------------------------- server tick

    /** Per-tick work logic, wired through {@code BuilderBlock#getTicker} (vanilla furnace static-tick pattern). */
    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, BuilderBlockEntity builder) {
        builder.tickWork(level);
    }

    /** One placement per tick (the filler loop shape): skips satisfied/non-buildable cells free, charges
     * {@link QuarryBlockEntity#blockWorkCost} + one resource per real placement. */
    private void tickWork(ServerLevel level) {
        if (this.blueprint == null || this.cells == null || this.cursor < 0) {
            return;
        }
        if (this.cursor >= this.cells.size()) {
            if (!this.finished) {
                this.finished = true;
                LOGGER.info("[M417] builder at {}: finished ({}/{})", this.worldPosition, this.placed,
                    this.cells.size());
                this.syncToClients();
            }
            return;
        }
        BlockPos cell = this.cells.get(this.cursor);
        String blockId = this.blueprint.idAt(cell.getX(), cell.getY(), cell.getZ());
        if (blockId == null) {
            this.cursor++;
            return;
        }
        BlockPos worldPos = this.buildOrigin().offset(cell);
        BlockState target = level.getBlockState(worldPos);
        String targetId = target.isAir() ? null : BuiltInRegistries.BLOCK.getKey(target.getBlock()).toString();
        if (blockId.equals(targetId)) {
            // already built: the matching-cell skip costs nothing (legacy SnapshotBuilder behaviour)
            this.cursor++;
            this.placed++;
            return;
        }
        if (!target.isAir()) {
            // the legacy break/excavate pass has not migrated: skip the occupied cell (and count it handled)
            this.cursor++;
            this.placed++;
            return;
        }
        if (!this.hasResourceFor(blockId)) {
            return; // missing resources: the legacy builder stalls (power request already reads 0)
        }
        BlockState toPlace = BuiltInRegistries.BLOCK.getValue(Identifier.parse(blockId)).defaultBlockState();
        long cost = QuarryBlockEntity.blockWorkCost(toPlace);
        if (this.energyStored < cost) {
            return; // stall until the battery recharges (the legacy builder out-of-power stall)
        }
        this.energyStored -= cost;
        level.setBlock(worldPos, toPlace, Block.UPDATE_ALL);
        this.consumeOneResource(blockId);
        this.cursor++;
        this.placed++;
        if (this.placed % PROGRESS_EVERY == 0 || this.cursor >= this.cells.size()) {
            LOGGER.info("[M417] builder at {}: placed {} / {}", this.worldPosition, this.placed, this.cells.size());
        }
        this.syncToClients();
    }

    // ----------------------------------------------------------------- persistence + client sync

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.inv.serialize(output.child("bc_inv"));
        output.putLong("bc_energy_stored", this.energyStored);
        output.putLong("bc_total_received", this.totalReceived);
        output.putInt("bc_cursor", this.cursor);
        output.putInt("bc_placed", this.placed);
        output.putBoolean("bc_finished", this.finished);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.inv.deserialize(input.childOrEmpty("bc_inv"));
        this.energyStored = input.getLongOr("bc_energy_stored", 0L);
        this.totalReceived = input.getLongOr("bc_total_received", 0L);
        this.cursor = input.getIntOr("bc_cursor", 0);
        this.placed = input.getIntOr("bc_placed", 0);
        this.finished = input.getBooleanOr("bc_finished", false);
        // rebuild the blueprint mirror from slot 0 (the item carries the data; nothing else to persist)
        ItemStack stack = this.inv.getResource(0).toStack(1);
        this.blueprint = BlueprintItems.readData(stack);
        this.cells = this.blueprint == null ? null : this.blueprint.nonAirCells();
        if (this.blueprint == null) {
            this.cursor = -1;
            this.finished = true;
        }
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
