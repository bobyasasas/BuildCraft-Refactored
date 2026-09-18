/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
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
import buildcraft.builders.menu.FillerMenu;
import buildcraft.core.blockentity.MjReceiver;

/**
 * Minimal filler block entity for the M2.12 vertical slice of buildcraftbuilders (replaces the M2.4c
 * {@code PlaceholderBlockEntity} under the unchanged id {@code buildcraftbuilders:filler}). Deliberately reduced
 * stand-in for the legacy {@code buildcraft.builders.tile.TileFiller}: no marker/volume box discovery, no filler
 * pattern statement parameters, no pattern-picker GUI (M4.8 added the resource-inventory GUI, the statement slots
 * arrive with the statement framework), no {@code TemplateBuilder} snapshot machinery and no excavate
 * (replace non-matching blocks) support &mdash; those arrive with the full builders module migration.
 *
 * <p><b>Legacy audit (frozen 1.20.1 tree) and how the slice maps to it:</b>
 * <ul>
 * <li><b>Power input</b> &mdash; legacy: {@code MjBattery(16000 * MjAPI.MJ)} fed through
 * {@code MjCapabilityHelper(new MjBatteryReceiver(battery))} ({@code IMjReceiver} semantics). Slice: the same receiver
 * semantics through {@link MjReceiver} on the M2.2 micro-MJ scale (quantities &times;10&#8315;&#8308;, see
 * {@link QuarryBlockEntity}): battery capacity {@link #CAPACITY} = 1,600,000&nbsp;&micro;MJ.</li>
 * <li><b>Pattern</b> &mdash; legacy: {@code IFillerPattern} resolved through {@code FillerRegistry} by unique tag; the
 * frozen tree ships 20+ patterns ({@code PatternFill}, {@code PatternBox}, {@code PatternSphere}, the 2d shapes, ...).
 * Slice: exactly one pattern, {@code buildcraft:fill} (legacy {@code PatternFill#fillTemplate} = {@code setAll(true)},
 * the simplest and verifiable one) as the single {@link Pattern#FILL} value; the pattern persists by its legacy unique
 * tag string. The remaining legacy patterns are deferred.</li>
 * <li><b>Work area (box)</b> &mdash; legacy: discovered from volume boxes/markers/{@code IAreaProvider} on placement.
 * Slice: flat {@code bc_min_*}/{@code bc_max_*} BE fields set programmatically via {@link #setWorkArea} (gametests /
 * the m212 evidence rig / {@code /data merge}).</li>
 * <li><b>Resources</b> &mdash; legacy: the 27-slot {@code invResources} restricted to placeable block items; the
 * builder stalls while resources are missing. Slice: the same 27-slot shape as a real {@link Container} ({@code bc_items}
 * through the vanilla {@code ContainerHelper} persistence) so the M4.8 filler GUI works on the actual inventory; blocks
 * to place are inserted programmatically via {@link #insertResource}, by right-clicking the filler with a block item, or
 * through the filler GUI's resource slots.</li>
 * <li><b>Placement loop</b> &mdash; legacy: the pattern's template marks the cells that must be solid and the
 * {@code TemplateBuilder} iterates them. Slice: {@link #tickWork} scans the box (x, then z, then y ascending), skips
 * cells that are already solid (the template match) and places the buffer block's default state into air cells, one
 * placement per tick.</li>
 * <li><b>Per-block power cost</b> &mdash; legacy 8.x builders drain no MJ per placed block ({@code TemplateBuilder}
 * never touches the battery &mdash; the 7.x-era powered building was dropped in the 8.x rewrite). The task requires
 * powered placement, so the slice prices each placement with the quarry's own break formula
 * {@link QuarryBlockEntity#blockWorkCost} ({@code 3,200 * (hardness + 1)} &micro;MJ &mdash; dirt: 4,800), restoring
 * powered building in slice units. flagged for the real builders migration to revisit.</li>
 * </ul>
 *
 * <p><b>Client sync (M2.12, the M2.7b Beacon pattern):</b> {@link #getUpdatePacket()} returns
 * {@code ClientboundBlockEntityDataPacket.create(this)} and {@link #getUpdateTag} returns {@link #saveCustomOnly}, so
 * the client renderer sees the work area, the current placement cell and the finished flag through
 * {@code loadAdditional} &mdash; the same channel the engine/pipe/gate slices already use.
 *
 * <p><b>GUI (M4.8):</b> this block entity is a {@link MenuProvider}: {@code FillerBlock#useWithoutItem} calls
 * {@code player.openMenu(this, pos)}, which builds the server-side {@link FillerMenu} through {@link #createMenu} and
 * pushes the position to the client. The GUI-visible scalar state (stored energy, selected pattern, finished flag)
 * reaches the open menu through {@link #guiData}, the vanilla {@code ContainerData} sync channel.
 */
public class FillerBlockEntity extends BlockEntity implements MjReceiver, Container, MenuProvider {

    /** Internal energy buffer size in &micro;MJ (slice value: legacy 16,000 MJ &times;10&#8315;&#8308;, see javadoc). */
    public static final long CAPACITY = 1_600_000;

    /** The resource inventory size (the legacy {@code invResources}: 27 slots, 3 rows of 9). */
    public static final int RESOURCE_SLOTS = 27;

    /** The one migrated filler pattern (legacy {@code PatternFill}, unique tag {@code buildcraft:fill}). */
    public enum Pattern {
        /** Legacy {@code PatternFill}: every cell of the box must become solid ({@code setAll(true)}). */
        FILL("buildcraft:fill");

        /** The legacy {@code IFillerPattern#getUniqueTag()} value this slice pattern stands for. */
        public final String uniqueTag;

        Pattern(String uniqueTag) {
            this.uniqueTag = uniqueTag;
        }

        /** Resolves a persisted legacy unique tag; only the migrated {@code buildcraft:fill} exists in the slice. */
        public static @Nullable Pattern byUniqueTag(String tag) {
            for (Pattern pattern : values()) {
                if (pattern.uniqueTag.equals(tag)) {
                    return pattern;
                }
            }
            return null;
        }
    }

    /** Internal energy buffer, &micro;MJ (see class javadoc). */
    private long energyStored;
    /** Lifetime energy received through {@link #receivePower}, in &micro;MJ (evidence/test assertion counter). */
    private long totalReceived;
    /** The selected filler pattern (slice: exactly {@link Pattern#FILL}; null = none selected, no work). */
    @Nullable
    private Pattern pattern;
    /** Work area corners (absolute coords); unset (null) means "no box &mdash; no work" (legacy: no markers placed). */
    @Nullable
    private BlockPos areaMin;
    @Nullable
    private BlockPos areaMax;
    /** Next cell the placement scan will look at (absolute coords); null while idle/finished. */
    @Nullable
    private BlockPos scanCursor;
    /** The cell currently being placed into, or null between placements (client renderer: "当前作业格"). */
    @Nullable
    private BlockPos currentCell;
    /** The resource buffer (legacy {@code invResources}): the blocks the filler places, 3 rows of 9 like the baseline. */
    private final NonNullList<ItemStack> resourceItems = NonNullList.withSize(RESOURCE_SLOTS, ItemStack.EMPTY);
    /** True once every cell of the box matches the pattern (legacy {@code TileFiller#finished} analogue). */
    private boolean finished;

    /**
     * The GUI-visible scalar state of this filler, synced to the open {@link FillerMenu} through the vanilla
     * {@code ContainerData} channel (server side reads the live fields; the client half gets a zero-initialised
     * {@code SimpleContainerData} that vanilla keeps in sync).
     */
    private final ContainerData guiData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case FillerMenu.DATA_ENERGY_STORED -> (int) FillerBlockEntity.this.energyStored;
                case FillerMenu.DATA_PATTERN -> FillerBlockEntity.this.pattern == null
                        ? -1
                        : FillerBlockEntity.this.pattern.ordinal();
                case FillerMenu.DATA_FINISHED -> FillerBlockEntity.this.finished ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Server-authoritative: the client never writes back (vanilla AbstractFurnaceBlockEntity pattern).
        }

        @Override
        public int getCount() {
            return FillerMenu.DATA_COUNT;
        }
    };

    public FillerBlockEntity(BlockPos pos, BlockState state) {
        super(BcBuildersBlockEntities.FILLER.value(), pos, state);
    }

    // ---------------------------------------------------------------------
    // Slice setup API (gametests / m212 evidence rig / right-click; legacy GUI/markers)
    // ---------------------------------------------------------------------

    /** Selects the filler pattern by its legacy unique tag (slice: only {@code buildcraft:fill} resolves). */
    public boolean setPattern(String uniqueTag) {
        Pattern resolved = Pattern.byUniqueTag(uniqueTag);
        if (resolved == null) {
            return false;
        }
        this.pattern = resolved;
        this.resetProgress();
        return true;
    }

    /** Sets the work area (legacy: marker/volume-box discovery) and resets the scan/cell state. */
    public void setWorkArea(BlockPos min, BlockPos max) {
        if (this.level != null && this.level.isClientSide()) {
            return;
        }
        this.areaMin = min;
        this.areaMax = max;
        this.resetProgress();
    }

    /**
     * Inserts a block stack into the resource buffer, filling partial stacks first and then empty slots (legacy
     * {@code invResources#insert}). Returns the rejected remainder (empty when fully accepted); only placeable block
     * items are accepted (legacy slot predicate {@code ItemBlocks.getList()}).
     */
    public ItemStack insertResource(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
            return stack;
        }
        ItemStack remainder = stack.copy();
        // Partial stacks first, then empty slots (the standard insertion order).
        for (int slot = 0; slot < RESOURCE_SLOTS && !remainder.isEmpty(); slot++) {
            ItemStack current = this.resourceItems.get(slot);
            if (!current.isEmpty() && ItemStack.isSameItemSameComponents(current, remainder)) {
                int moved = Math.min(remainder.getCount(), current.getMaxStackSize() - current.getCount());
                if (moved > 0) {
                    current.grow(moved);
                    remainder.shrink(moved);
                }
            }
        }
        for (int slot = 0; slot < RESOURCE_SLOTS && !remainder.isEmpty(); slot++) {
            if (this.resourceItems.get(slot).isEmpty()) {
                int moved = Math.min(remainder.getCount(), remainder.getMaxStackSize());
                this.resourceItems.set(slot, remainder.copyWithCount(moved));
                remainder.shrink(moved);
            }
        }
        if (remainder.getCount() != stack.getCount()) {
            this.syncToClients();
        }
        return remainder;
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

    @Nullable
    public BlockPos getAreaMin() {
        return this.areaMin;
    }

    @Nullable
    public BlockPos getAreaMax() {
        return this.areaMax;
    }

    /** The cell currently being filled (client renderer: "当前作业格"), or null between placements. */
    @Nullable
    public BlockPos getCurrentCell() {
        return this.currentCell;
    }

    /** The resource buffer contents (read-only copy of the first non-empty stack), or empty when out of resources. */
    public ItemStack getResource() {
        for (ItemStack stack : this.resourceItems) {
            if (!stack.isEmpty()) {
                return stack.copy();
            }
        }
        return ItemStack.EMPTY;
    }

    /** True while every resource slot is empty (the "missing resources" stall condition for the work loop). */
    private boolean hasNoResources() {
        for (ItemStack stack : this.resourceItems) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    public Pattern getPattern() {
        return this.pattern;
    }

    // ---------------------------------------------------------------------
    // MjReceiver (legacy IMjReceiver semantics, see the interface javadoc)
    // ---------------------------------------------------------------------

    @Override
    public long getPowerRequested() {
        if (this.finished || this.pattern == null || this.areaMin == null || this.areaMax == null
                || hasNoResources()) {
            return 0;
        }
        return CAPACITY - this.energyStored;
    }

    @Override
    public long receivePower(long microJoules, boolean simulate) {
        long accepted = Math.min(microJoules, CAPACITY - this.energyStored);
        if (!simulate && accepted > 0) {
            this.energyStored += accepted;
            this.totalReceived += accepted;
            setChanged();
        }
        return microJoules - accepted;
    }

    // ---------------------------------------------------------------------
    // Server tick: the (7.x-style powered) placement loop
    // ---------------------------------------------------------------------

    /** Per-tick work logic, wired through {@code FillerBlock#getTicker} (vanilla furnace static-tick pattern). */
    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, FillerBlockEntity filler) {
        filler.tickWork(level);
    }

    /** One placement per tick while the battery covers the cell's cost; skips solid cells free (the template match),
     * stalls without power or resources (legacy builder stalls), finishes when the box matches the pattern. */
    private void tickWork(Level level) {
        if (this.finished || this.pattern != Pattern.FILL || this.areaMin == null || this.areaMax == null) {
            return;
        }
        boolean sync = false;
        BlockPos pos = this.scanCursor != null ? this.scanCursor : this.areaMin;
        while (pos.getY() <= this.areaMax.getY()) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                // already solid: the template match costs nothing (legacy SnapshotBuilder skips matching cells)
                pos = this.nextScanPos(pos);
                continue;
            }
            if (hasNoResources()) {
                // out of resources: park on the first pending air cell (the legacy builder "missing resources"
                // stall). The scan itself keeps running on later ticks, so a box that is already fully solid
                // still walks off the end and reports finished instead of idling forever.
                if (this.currentCell == null || !this.currentCell.equals(pos)) {
                    this.currentCell = pos;
                    sync = true;
                }
                break;
            }
            if (this.currentCell == null || !this.currentCell.equals(pos)) {
                this.currentCell = pos;
                sync = true; // the client renderer highlights the newly targeted cell
            }
            long cost = QuarryBlockEntity.blockWorkCost(this.placedState());
            if (this.energyStored < cost) {
                break; // stall until the battery recharges (legacy: build stalls when out of power)
            }
            this.energyStored -= cost;
            level.setBlock(pos, this.placedState(), Block.UPDATE_ALL);
            this.consumeOneResource();
            sync = true;
            this.scanCursor = this.nextScanPos(pos);
            this.currentCell = null;
            break; // one placement per tick
        }
        if (pos.getY() > this.areaMax.getY()) {
            // the whole box matches the pattern
            this.scanCursor = null;
            this.currentCell = null;
            if (!this.finished) {
                this.finished = true;
                sync = true;
            }
        }
        if (sync) {
            this.syncToClients();
        }
    }

    /** The block state one placement puts into the world (legacy: the template's default-state replay). */
    private BlockState placedState() {
        ItemStack stack = this.getResource();
        return ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
    }

    /** Takes one item out of the first non-empty resource slot (the per-placement consumption). */
    private void consumeOneResource() {
        for (int slot = 0; slot < RESOURCE_SLOTS; slot++) {
            ItemStack stack = this.resourceItems.get(slot);
            if (!stack.isEmpty()) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    this.resourceItems.set(slot, ItemStack.EMPTY);
                }
                setChanged();
                return;
            }
        }
    }

    /** Scan order helper: x ascending, then z ascending, then y ascending (flat, deterministic slice order). */
    @Nullable
    private BlockPos nextScanPos(BlockPos pos) {
        if (this.areaMin == null || this.areaMax == null) {
            return null;
        }
        if (pos.getX() < this.areaMax.getX()) {
            return pos.offset(1, 0, 0);
        }
        if (pos.getZ() < this.areaMax.getZ()) {
            return new BlockPos(this.areaMin.getX(), pos.getY(), pos.getZ() + 1);
        }
        return new BlockPos(this.areaMin.getX(), pos.getY() + 1, this.areaMin.getZ());
    }

    private void resetProgress() {
        this.scanCursor = this.areaMin;
        this.currentCell = null;
        this.finished = false;
        this.syncToClients();
    }

    // ---------------------------------------------------------------------
    // Persistence + client sync (Beacon pattern, see class javadoc)
    // ---------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("bc_energy_stored", this.energyStored);
        output.putLong("bc_total_received", this.totalReceived);
        output.putBoolean("bc_finished", this.finished);
        if (this.pattern != null) {
            // the legacy unique tag, so the persisted shape stays recognisable against FillerRegistry
            output.putString("bc_pattern", this.pattern.uniqueTag);
        }
        if (this.areaMin != null) {
            output.putInt("bc_min_x", this.areaMin.getX());
            output.putInt("bc_min_y", this.areaMin.getY());
            output.putInt("bc_min_z", this.areaMin.getZ());
        }
        if (this.areaMax != null) {
            output.putInt("bc_max_x", this.areaMax.getX());
            output.putInt("bc_max_y", this.areaMax.getY());
            output.putInt("bc_max_z", this.areaMax.getZ());
        }
        if (this.scanCursor != null) {
            output.putInt("bc_scan_x", this.scanCursor.getX());
            output.putInt("bc_scan_y", this.scanCursor.getY());
            output.putInt("bc_scan_z", this.scanCursor.getZ());
        }
        if (this.currentCell != null) {
            // rides the update tag like the quarry's bc_target_*: the client renderer's amber marker
            output.putInt("bc_cell_x", this.currentCell.getX());
            output.putInt("bc_cell_y", this.currentCell.getY());
            output.putInt("bc_cell_z", this.currentCell.getZ());
        }
        ContainerHelper.saveAllItems(output, this.resourceItems, false);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energyStored = input.getLongOr("bc_energy_stored", 0L);
        this.totalReceived = input.getLongOr("bc_total_received", 0L);
        this.finished = input.getBooleanOr("bc_finished", false);
        this.pattern = input.getString("bc_pattern").map(Pattern::byUniqueTag).orElse(null);
        this.areaMin = input.getIntOr("bc_min_x", Integer.MIN_VALUE) == Integer.MIN_VALUE ? null
                : new BlockPos(input.getIntOr("bc_min_x", 0), input.getIntOr("bc_min_y", 0),
                        input.getIntOr("bc_min_z", 0));
        this.areaMax = input.getIntOr("bc_max_x", Integer.MAX_VALUE) == Integer.MAX_VALUE ? null
                : new BlockPos(input.getIntOr("bc_max_x", 0), input.getIntOr("bc_max_y", 0),
                        input.getIntOr("bc_max_z", 0));
        if (input.getIntOr("bc_scan_x", Integer.MIN_VALUE) != Integer.MIN_VALUE) {
            this.scanCursor = new BlockPos(input.getIntOr("bc_scan_x", 0), input.getIntOr("bc_scan_y", 0),
                    input.getIntOr("bc_scan_z", 0));
        } else {
            this.scanCursor = null;
        }
        if (input.getIntOr("bc_cell_x", Integer.MIN_VALUE) != Integer.MIN_VALUE) {
            this.currentCell = new BlockPos(input.getIntOr("bc_cell_x", 0), input.getIntOr("bc_cell_y", 0),
                    input.getIntOr("bc_cell_z", 0));
        } else {
            this.currentCell = null;
        }
        ContainerHelper.loadAllItems(input, this.resourceItems);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Marks the state dirty and pushes it to clients (Beacon-pattern update tag, see class javadoc). */
    private void syncToClients() {
        this.setChanged();
        if (this.level instanceof ServerLevel serverLevel) {
            BlockState state = this.getBlockState();
            serverLevel.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    // ---------------------------------------------------------------------
    // GUI wiring (M4.8): the block entity is its own MenuProvider
    // ---------------------------------------------------------------------

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FillerMenu(containerId, playerInventory, this, this.guiData);
    }

    @Override
    public Component getDisplayName() {
        // The block's overridden description id = the frozen legacy key ("tile.fillerBlock.name"), see BcLangKeys.
        return this.getBlockState().getBlock().getName();
    }

    // ---------------------------------------------------------------------
    // Container (the 27 resource slots = the legacy invResources, M4.8)
    // ---------------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return RESOURCE_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return hasNoResources();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < RESOURCE_SLOTS ? this.resourceItems.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack result = ContainerHelper.removeItem(this.resourceItems, slot, count);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.resourceItems, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < RESOURCE_SLOTS) {
            this.resourceItems.set(slot, stack);
            stack.limitSize(this.getMaxStackSize(stack));
            setChanged();
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        // The legacy invResources slot predicate: only placeable block items.
        return slot >= 0 && slot < RESOURCE_SLOTS && stack.getItem() instanceof BlockItem;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.resourceItems.clear();
    }
}
