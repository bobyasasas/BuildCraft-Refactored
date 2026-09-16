/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders.blockentity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import buildcraft.builders.BcBuildersBlockEntities;
import buildcraft.core.blockentity.MjReceiver;

/**
 * Minimal quarry block entity for the M2.12 vertical slice of buildcraftbuilders (replaces the M2.4c
 * {@code PlaceholderBlockEntity} under the unchanged id {@code buildcraftbuilders:quarry}). Deliberately reduced
 * stand-in for the legacy {@code buildcraft.builders.tile.TileQuarry}: no frame blocks ({@code TaskAddFrame}), no
 * laser/advancement integration, no chunk-load management, no drill entity or collision boxes, no marker/volume box
 * discovery and no randomized iteration order &mdash; those arrive with the full builders module migration.
 *
 * <p><b>Legacy audit (frozen 1.20.1 tree) and how the slice maps to it:</b>
 * <ul>
 * <li><b>Power input</b> &mdash; legacy: {@code MjBattery(24000 * MjAPI.MJ)} fed through
 * {@code MjCapabilityHelper(new MjBatteryReceiver(battery))}, i.e. {@code IMjReceiver#getPowerRequested} =
 * {@code capacity - stored}, {@code receivePower} returns the excess. Slice: the same receiver semantics through
 * {@link MjReceiver} (the {@code M2.2} micro-MJ economy, quantities scaled &times;10&#8315;&#8308; to match the slice
 * engine's 100&nbsp;&micro;MJ/t vs the legacy stone engine's 1&nbsp;MJ/t): battery capacity
 * {@link #CAPACITY} = 2,400,000&nbsp;&micro;MJ; the M2.2c kinesis pipe pushes into it every tick.</li>
 * <li><b>Mining area</b> &mdash; legacy: {@code miningBox} discovered from volume markers ({@code TileMarkerVolume} /
 * {@code IAreaProvider}), frame blocks built around it first. Slice: the flat {@code bc_min_*}/{@code bc_max_*} BE
 * fields set programmatically via {@link #setMiningArea} (gametests / the m212 evidence rig); the marker and frame
 * systems have not migrated.</li>
 * <li><b>Mining loop</b> &mdash; legacy: a {@code BoxIterator} over the box (axis order XZY/ZXY and x/z inversions
 * randomised per session) picks cells with {@code canMine}, and the active {@code TaskBreakBlock} accumulates power
 * every tick ({@code battery.extractPower(0, min(max, required))}, {@code Task#addPower}) until it reaches
 * {@code getTarget()}; leftover power is refunded to the battery. Slice: a fixed deterministic scan
 * (x, then z, then y <em>descending</em> &mdash; the drill works top-down) drives {@link #tickWork}, which spends the
 * battery on the current target at the legacy pace until {@code taskPower} reaches the block's cost.</li>
 * <li><b>Per-block power cost</b> &mdash; legacy {@code TaskBreakBlock#getTarget()} =
 * {@code BlockUtil.computeBlockBreakPower} = {@code 16 MJ * (hardness + 1) * 2} (stone: 80&nbsp;MJ). Slice: the same
 * formula shape on the &times;10&#8315;&#8308; scale, {@link #blockWorkCost} = 3,200&nbsp;&micro;MJ &times;
 * (hardness&nbsp;+&nbsp;1) (stone: 8,000&nbsp;&micro;MJ). The filler reuses this exact cost for placement, restoring
 * the 7.x-era powered building behaviour that the 8.x {@code TemplateBuilder} dropped (the frozen tree's builder loop
 * drains no MJ per block).</li>
 * <li><b>Breaking &amp; drops</b> &mdash; legacy:
 * {@code BlockUtil.breakBlockAndGetDrops(level, pos, DIAMOND_PICKAXE, owner, true)} with the drops inserted into the
 * best acceptor ({@code InventoryUtil.addToBestAcceptor}). Slice: {@link Block#getDrops} with a diamond pickaxe tool
 * (same tool parity) into the small internal output buffer {@link #output} ("产物进入内部缓冲" &mdash; the legacy
 * quarry itself has no inventory, the buffer is the slice's stand-in until real inventories migrate).</li>
 * <li><b>canMine guards</b> &mdash; legacy: skips air, fluids and unbreakable blocks (negative destroy speed).
 * Slice: identical three guards in {@link #canMine}.</li>
 * </ul>
 *
 * <p><b>Client sync (M2.12, the M2.7b Beacon pattern):</b> {@link #getUpdatePacket()} returns
 * {@code ClientboundBlockEntityDataPacket.create(this)} and {@link #getUpdateTag} returns {@link #saveCustomOnly}
 * (exactly the {@link #saveAdditional} keys), so the client renderer sees the mining area, the current target cell and
 * the finished flag through {@code loadAdditional} &mdash; the same channel the engine/pipe/gate slices already use.
 * {@link #serverTick} pushes an update on every visible change (target picked, block broken, finished).
 */
public class QuarryBlockEntity extends BlockEntity implements MjReceiver {

    /** Internal energy buffer size in &micro;MJ (slice value: legacy 24,000 MJ &times;10&#8315;&#8308;, see javadoc). */
    public static final long CAPACITY = 2_400_000;
    /**
     * Slice per-block work cost base in &micro;MJ: legacy {@code BlockUtil.computeBlockBreakPower} charges
     * {@code 16 MJ * (hardness + 1) * 2} per broken block; scaled &times;10&#8315;&#8308; that is
     * {@code 3,200 * (hardness + 1)} &micro;MJ. {@link #blockWorkCost} applies the hardness term; the filler slice
     * prices placement with the same formula ({@link FillerBlockEntity} cross-reference).
     */
    public static final long WORK_COST_BASE = 3_200;
    /** Legacy {@code TileQuarry.MAX_POWER_PER_TICK} (512 MJ) on the slice scale, in &micro;MJ per tick. */
    public static final long MAX_POWER_PER_TICK = 51_200;
    /** Output buffer cap in stacks; the quarry stalls (and refuses power) while the buffer is full. */
    public static final int MAX_OUTPUT_STACKS = 8;
    /** Legacy tool parity: the slice breaks blocks with the same diamond-pickaxe loot context as {@code TaskBreakBlock}. */
    private static final ItemStack BREAK_TOOL = new ItemStack(Items.DIAMOND_PICKAXE);

    /** Internal energy buffer, &micro;MJ (see class javadoc). */
    private long energyStored;
    /** Lifetime energy received through {@link #receivePower}, in &micro;MJ (evidence/test assertion counter). */
    private long totalReceived;
    /** Mining area corners (absolute coords); unset (null) means "no area &mdash; no work". */
    @Nullable
    private BlockPos areaMin;
    @Nullable
    private BlockPos areaMax;
    /** Next cell the area scan will look at (absolute coords); null while no area is set. */
    @Nullable
    private BlockPos scanCursor;
    /** The cell currently being drilled, or null between tasks (legacy {@code TileQuarry.currentTask}). */
    @Nullable
    private BlockPos currentTarget;
    /** Power accumulated onto {@link #currentTarget} so far, in &micro;MJ (legacy {@code Task#power}). */
    private long taskPower;
    /** Mined drops waiting to leave the machine (legacy: pushed to acceptors; slice: internal buffer). */
    private final List<ItemStack> output = new ArrayList<>();
    /** True once every cell of the area has been mined (legacy has no finished flag; slice test/evidence affordance). */
    private boolean finished;

    public QuarryBlockEntity(BlockPos pos, BlockState state) {
        super(BcBuildersBlockEntities.QUARRY.value(), pos, state);
    }

    // ---------------------------------------------------------------------
    // Slice setup API (gametests / m212 evidence rig; legacy gets these from markers)
    // ---------------------------------------------------------------------

    /** Sets the mining area (legacy: marker-discovered {@code miningBox}) and resets the scan/target state. */
    public void setMiningArea(BlockPos min, BlockPos max) {
        if (this.level != null && this.level.isClientSide()) {
            return;
        }
        this.areaMin = min;
        this.areaMax = max;
        this.scanCursor = min;
        this.currentTarget = null;
        this.taskPower = 0;
        this.finished = false;
        this.syncToClients();
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

    /** The cell currently being drilled (client renderer: "当前采掘格"), or null between tasks. */
    @Nullable
    public BlockPos getCurrentTarget() {
        return this.currentTarget;
    }

    /** The mined-drops buffer (legacy pushes drops to acceptors; the slice parks them here). Read-only view. */
    public List<ItemStack> getOutput() {
        return List.copyOf(this.output);
    }

    // ---------------------------------------------------------------------
    // MjReceiver (legacy IMjReceiver semantics, see the interface javadoc)
    // ---------------------------------------------------------------------

    @Override
    public long getPowerRequested() {
        if (!this.hasWork()) {
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

    /** True while the slice quarry still has usable work: an area, room in the output buffer, and not finished. */
    private boolean hasWork() {
        if (this.finished || this.areaMin == null || this.areaMax == null) {
            return false;
        }
        return this.output.size() < MAX_OUTPUT_STACKS;
    }

    // ---------------------------------------------------------------------
    // Server tick: the legacy TaskBreakBlock power loop on the slice scan order
    // ---------------------------------------------------------------------

    /** Per-tick work logic, wired through {@code QuarryBlock#getTicker} (vanilla furnace static-tick pattern). */
    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, QuarryBlockEntity quarry) {
        quarry.tickWork(level);
    }

    /** One work step per tick: charge the current target from the battery, break it when fully paid (legacy
     * {@code Task#addPower} pace), then pick the next mineable cell from the deterministic scan. */
    private void tickWork(ServerLevel level) {
        if (!this.hasWork()) {
            return;
        }
        boolean sync = false;
        if (this.currentTarget == null) {
            BlockPos next = this.findNextMineable(level);
            if (next == null) {
                this.finished = true;
                this.syncToClients();
                return;
            }
            this.currentTarget = next;
            this.taskPower = 0;
            sync = true;
        }
        BlockState targetState = level.getBlockState(this.currentTarget);
        long cost = blockWorkCost(targetState);
        // legacy: battery.extractPower(0, min(max, getRequiredPowerThisTick())) fed into Task#addPower every tick
        long added = Math.min(Math.min(MAX_POWER_PER_TICK, cost - this.taskPower), this.energyStored);
        if (added > 0) {
            this.energyStored -= added;
            this.taskPower += added;
        }
        if (this.taskPower >= cost) {
            // legacy Task#addPower refunds the overshoot to the battery when the task finishes
            long leftover = this.taskPower - cost;
            if (this.breakBlock(level, this.currentTarget, targetState)) {
                this.energyStored = Math.min(CAPACITY, this.energyStored + leftover);
                this.currentTarget = null;
                this.taskPower = 0;
                sync = true;
            }
        }
        if (sync) {
            this.syncToClients();
        }
    }

    /**
     * Breaks the target and parks its drops in the output buffer (legacy {@code TaskBreakBlock#finish} +
     * {@code breakBlockAndGetDrops(DIAMOND_PICKAXE)}). Returns false when the cell is no longer minable (legacy checks
     * {@code canMine} again at finish time too).
     */
    private boolean breakBlock(ServerLevel level, BlockPos pos, BlockState state) {
        if (!canMine(level, pos)) {
            return true; // vanished mid-task: treat as done (legacy TaskBreakBlock#finish returns true)
        }
        List<ItemStack> drops = Block.getDrops(state, level, pos, null, null, BREAK_TOOL);
        level.removeBlock(pos, false);
        // the break particles/sound half of vanilla destroyBlock, without its automatic drops
        level.levelEvent(2001, pos, Block.getId(state));
        if (this.output.size() < MAX_OUTPUT_STACKS) {
            for (ItemStack drop : drops) {
                if (!drop.isEmpty() && this.output.size() < MAX_OUTPUT_STACKS) {
                    this.output.add(drop);
                }
            }
        }
        return true;
    }

    /**
     * Deterministic slice scan (legacy randomises XZY/ZXY and x/z inversions per session): x, then z, then y
     * <em>descending</em> &mdash; the drill works top-down. Advances/resumes from {@link #scanCursor}.
     */
    @Nullable
    private BlockPos findNextMineable(ServerLevel level) {
        if (this.areaMin == null || this.areaMax == null) {
            return null;
        }
        BlockPos cursor = this.scanCursor != null ? this.scanCursor : this.areaMin;
        BlockPos pos = cursor;
        while (pos.getY() >= this.areaMin.getY()) {
            if (canMine(level, pos)) {
                this.scanCursor = pos;
                return pos;
            }
            BlockPos next = this.nextScanPos(pos);
            if (next == null) {
                break;
            }
            pos = next;
        }
        this.scanCursor = null;
        return null;
    }

    /** Scan order helper: x ascending, then z ascending, then y descending (see {@link #findNextMineable}). */
    private BlockPos nextScanPos(BlockPos pos) {
        BlockPos min = this.areaMin;
        BlockPos max = this.areaMax;
        if (min == null || max == null) {
            return null;
        }
        if (pos.getX() < max.getX()) {
            return pos.offset(1, 0, 0);
        }
        if (pos.getZ() < max.getZ()) {
            return new BlockPos(min.getX(), pos.getY(), pos.getZ() + 1);
        }
        return new BlockPos(min.getX(), pos.getY() - 1, min.getZ());
    }

    // ---------------------------------------------------------------------
    // Guards + cost (legacy canMine / BlockUtil.computeBlockBreakPower)
    // ---------------------------------------------------------------------

    /** Legacy {@code TileQuarry#canMine} guards: solid, breakable, non-fluid cells only. */
    public static boolean canMine(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.liquid()) {
            return false;
        }
        return state.getDestroySpeed(level, pos) >= 0;
    }

    /**
     * The slice power cost of one block interaction (break or place), in &micro;MJ: legacy
     * {@code 16 MJ * (hardness + 1) * 2} scaled &times;10&#8315;&#8308; (see {@link #WORK_COST_BASE}).
     */
    public static long blockWorkCost(BlockState state) {
        // getDestroySpeed ignores its (legacy world/pos) arguments and returns the cached hardness; the legacy
        // formula's *2 is already folded into WORK_COST_BASE (1,600 * 2 = 3,200) on the *10^-4 scale
        float hardness = state.getDestroySpeed(null, BlockPos.ZERO);
        return (long) Math.floor(WORK_COST_BASE * (hardness + 1));
    }
    // ---------------------------------------------------------------------
    // Persistence + client sync (Beacon pattern, see class javadoc)
    // ---------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("bc_energy_stored", this.energyStored);
        output.putLong("bc_total_received", this.totalReceived);
        output.putLong("bc_task_power", this.taskPower);
        output.putBoolean("bc_finished", this.finished);
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
        if (this.currentTarget != null) {
            output.putInt("bc_target_x", this.currentTarget.getX());
            output.putInt("bc_target_y", this.currentTarget.getY());
            output.putInt("bc_target_z", this.currentTarget.getZ());
        }
        if (!this.output.isEmpty()) {
            output.store("bc_output", ItemStack.CODEC.listOf(), this.output);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energyStored = input.getLongOr("bc_energy_stored", 0L);
        this.totalReceived = input.getLongOr("bc_total_received", 0L);
        this.taskPower = input.getLongOr("bc_task_power", 0L);
        this.finished = input.getBooleanOr("bc_finished", false);
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
        if (input.getIntOr("bc_target_x", Integer.MIN_VALUE) != Integer.MIN_VALUE) {
            this.currentTarget = new BlockPos(input.getIntOr("bc_target_x", 0), input.getIntOr("bc_target_y", 0),
                    input.getIntOr("bc_target_z", 0));
        } else {
            this.currentTarget = null;
        }
        this.output.clear();
        input.read("bc_output", ItemStack.CODEC.listOf()).ifPresent(list -> this.output.addAll(list));
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
}
