/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory.blockentity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import buildcraft.core.blockentity.MjReceiver;
import buildcraft.factory.BcFactoryBlockEntities;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * M4.16 mining well block entity: a minimal port of the legacy {@code TileMiningWell} (+ its
 * {@code TileMiner} base) under the unchanged id {@code buildcraftfactory:mining_well}. The well is a real
 * {@link MjReceiver} (the legacy {@code MjBatteryReceiver} on a 500&nbsp;MJ battery): it drills the 1x1 column
 * straight below itself (the legacy {@code TileMiningWell#nextPos} walk, see {@link MiningWellLogic}), charging the
 * current target from its internal battery every tick until the block's break cost is paid, then breaks it and moves
 * on &mdash; down to bedrock, the build limit or {@link #MAX_DEPTH}.
 *
 * <p><b>Power:</b> the kinesis pipe slice pushes &micro;MJ into the battery through {@link #receivePower} every tick
 * (the legacy {@code PipeTransportPower} &rarr; {@code IMjReceiver} push). Per-block costs use the legacy
 * {@code BlockUtil#computeBlockBreakPower} formula shape on the &times;10&#8315;&#8308; slice scale
 * ({@code MiningWellLogic#blockWorkCost}, identical to the builders-slice quarry's economy: stone 8,000&nbsp;&micro;MJ).
 *
 * <p><b>Drops</b> (the legacy {@code breakBlockAndGetDrops(DIAMOND_PICKAXE)} + {@code addToBestAcceptor}):
 * blocks break with the same diamond-pickaxe loot context; every drop is offered to the item capability of the block
 * <em>above</em> the well (a chest, a machine, a transport item pipe face), and whatever nothing accepts pops into the
 * world as a regular item entity with a small upward kick (the legacy "eject" half).
 *
 * <p><b>v1 trims (all javadoc-tracked):</b> no {@code tube} filler blocks in the drilled column (the legacy drill
 * shaft visual, the {@code buildcraftfactory:tube} behaviour class has not migrated), no break-progress
 * {@code destroyBlockProgress} overlay, no block-update subscription (the scan rescans on demand instead), and
 * low-viscosity fluids are passed through rather than mined out (the legacy dried ponds at {@code viscosity <= 1000}).
 *
 * <p><b>Client sync</b> is the milestone's {@code BeaconBlockEntity} pattern: {@link #getUpdateTag} returns
 * {@link #saveCustomOnly}; the well pushes an update whenever the target changes or a block breaks (the drill is a
 * static cube, so the sync exists for state parity and future renderers).
 */
public class MiningWellBlockEntity extends BlockEntity implements MjReceiver {

    /**
     * Internal energy buffer size in &micro;MJ (slice value: the legacy {@code TileMiner} 500&nbsp;MJ battery
     * &times;10&#8315;&#8308;, see the class javadoc).
     */
    public static final long CAPACITY = 50_000;
    /** The drill depth limit in blocks (the legacy {@code BCCoreConfig.miningMaxDepth} default of 512). */
    public static final int MAX_DEPTH = 512;
    /** Legacy tool parity: the slice breaks blocks with the same diamond-pickaxe loot context as {@code TileMiningWell}. */
    private static final ItemStack BREAK_TOOL = new ItemStack(Items.DIAMOND_PICKAXE);

    /** Internal energy buffer, &micro;MJ (see class javadoc). */
    private long energyStored;
    /** Lifetime energy received through {@link #receivePower}, in &micro;MJ (evidence/test assertion counter). */
    private long totalReceived;
    /** The cell currently being drilled, or null when the well is complete/idle (legacy {@code currentPos}). */
    @Nullable
    private BlockPos currentTarget;
    /** Power accumulated onto {@link #currentTarget} so far, in &micro;MJ (legacy {@code progress}). */
    private long taskProgress;
    /** Lifetime broken block count (evidence counter). */
    private long blocksBroken;

    public MiningWellBlockEntity(BlockPos pos, BlockState state) {
        super(BcFactoryBlockEntities.MINING_WELL.value(), pos, state);
    }

    public long getEnergyStored() {
        return this.energyStored;
    }

    public long getTotalReceived() {
        return this.totalReceived;
    }

    public long getBlocksBroken() {
        return this.blocksBroken;
    }

    /** The cell currently being drilled, or null when the well is complete (evidence-rig read side). */
    @Nullable
    public BlockPos getCurrentTarget() {
        return this.currentTarget;
    }

    // ---------------------------------------------------------------------
    // MjReceiver (legacy IMjReceiver semantics, see the interface javadoc)
    // ---------------------------------------------------------------------

    @Override
    public long getPowerRequested() {
        if (this.currentTarget == null) {
            return 0; // legacy: refuse all power once the drill is done
        }
        return Math.max(0, CAPACITY - this.energyStored);
    }

    @Override
    public long receivePower(long microJoules, boolean simulate) {
        long accepted = Math.min(microJoules, Math.max(0, CAPACITY - this.energyStored));
        if (!simulate && accepted > 0) {
            this.energyStored += accepted;
            this.totalReceived += accepted;
            this.setChanged();
        }
        return microJoules - accepted;
    }

    // ---------------------------------------------------------------------
    // Server tick: the legacy TileMiningWell#mine loop on the slice scan
    // ---------------------------------------------------------------------

    /** The per-tick drill driver (wired through {@code MiningWellBlock#getTicker}). */
    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, MiningWellBlockEntity well) {
        well.tickWork(level);
    }

    /** One work step per tick: charge the current target from the battery, break it when fully paid, then rescan. */
    private void tickWork(ServerLevel level) {
        if (this.currentTarget == null) {
            BlockPos next = this.findTarget(level);
            if (next == null) {
                return; // complete: no target, no power request (see getPowerRequested)
            }
            this.currentTarget = next;
            this.taskProgress = 0;
            this.syncToClients();
        }
        BlockState targetState = level.getBlockState(this.currentTarget);
        if (!canBreak(level, this.currentTarget)) {
            // the target vanished or turned unbreakable mid-task: rescan next tick (legacy shouldCheck half)
            this.currentTarget = null;
            this.taskProgress = 0;
            this.syncToClients();
            return;
        }
        long cost = MiningWellLogic.blockWorkCost(targetState.getDestroySpeed(null, BlockPos.ZERO));
        long added = Math.min(cost - this.taskProgress, this.energyStored);
        if (added > 0) {
            this.energyStored -= added;
            this.taskProgress += added;
        }
        if (this.taskProgress >= cost) {
            long leftover = this.taskProgress - cost;
            this.breakBlock(level, this.currentTarget, targetState);
            // the (legacy TaskBreakBlock) overshoot refund; capped at the battery
            this.energyStored = Math.min(CAPACITY, this.energyStored + leftover);
            this.currentTarget = null;
            this.taskProgress = 0;
            this.syncToClients();
        }
    }

    /** The first breakable block straight below (the legacy {@code nextPos} walk, via {@link MiningWellLogic}). */
    @Nullable
    private BlockPos findTarget(ServerLevel level) {
        BlockPos wellPos = this.worldPosition;
        int offset = MiningWellLogic.findTarget(MAX_DEPTH, o -> {
            BlockPos cell = wellPos.offset(0, -o, 0);
            if (level.isOutsideBuildHeight(cell)) {
                return MiningWellLogic.Cell.UNBREAKABLE;
            }
            BlockState state = level.getBlockState(cell);
            if (state.isAir() || state.liquid()) {
                return MiningWellLogic.Cell.PASSABLE; // v1 trim: fluids passed through, see the class javadoc
            }
            return state.getDestroySpeed(level, cell) >= 0
                    ? MiningWellLogic.Cell.BREAKABLE
                    : MiningWellLogic.Cell.UNBREAKABLE;
        });
        return offset < 0 ? null : wellPos.offset(0, -offset, 0);
    }

    /** Legacy {@code TileQuarry#canMine} shape: solid, breakable, non-fluid cells only. */
    private static boolean canBreak(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.liquid()) {
            return false;
        }
        return state.getDestroySpeed(level, pos) >= 0;
    }

    /**
     * Breaks the target with the diamond-pickaxe loot context and ejects the drops upwards
     * (see the class javadoc): capability into the block above first, item entity fallback second.
     */
    private void breakBlock(ServerLevel level, BlockPos pos, BlockState state) {
        List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), null, BREAK_TOOL);
        level.removeBlock(pos, false);
        // the break particles/sound half of vanilla destroyBlock, without its automatic drops
        level.levelEvent(2001, pos, Block.getId(state));
        this.blocksBroken++;
        this.ejectUp(level, drops);
    }

    /** Offers every drop to the block above the well; leftovers pop into the world with an upward kick. */
    private void ejectUp(ServerLevel level, List<ItemStack> drops) {
        ResourceHandler<ItemResource> acceptor = level.getCapability(
                Capabilities.Item.BLOCK, this.worldPosition.above(), Direction.DOWN);
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }
            if (acceptor != null) {
                ItemResource resource = ItemResource.of(drop);
                try (Transaction transaction = Transaction.openRoot()) {
                    int accepted = acceptor.insert(resource, drop.getCount(), transaction);
                    if (accepted > 0) {
                        transaction.commit();
                        drop.shrink(accepted);
                    }
                }
            }
            if (!drop.isEmpty()) {
                ItemEntity entity = new ItemEntity(level,
                        this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.5, this.worldPosition.getZ() + 0.5,
                        drop);
                entity.setDeltaMovement(0.0, 0.2, 0.0);
                entity.setPickUpDelay(20);
                level.addFreshEntity(entity);
            }
        }
    }

    // ---------------------------------------------------------------------
    // Persistence + client sync (Beacon pattern, see class javadoc)
    // ---------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("bc_energy_stored", this.energyStored);
        output.putLong("bc_total_received", this.totalReceived);
        output.putLong("bc_task_power", this.taskProgress);
        output.putLong("bc_blocks_broken", this.blocksBroken);
        if (this.currentTarget != null) {
            output.putInt("bc_target_x", this.currentTarget.getX());
            output.putInt("bc_target_y", this.currentTarget.getY());
            output.putInt("bc_target_z", this.currentTarget.getZ());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energyStored = input.getLongOr("bc_energy_stored", 0L);
        this.totalReceived = input.getLongOr("bc_total_received", 0L);
        this.taskProgress = input.getLongOr("bc_task_power", 0L);
        this.blocksBroken = input.getLongOr("bc_blocks_broken", 0L);
        if (input.getIntOr("bc_target_x", Integer.MIN_VALUE) != Integer.MIN_VALUE) {
            this.currentTarget = new BlockPos(input.getIntOr("bc_target_x", 0), input.getIntOr("bc_target_y", 0),
                    input.getIntOr("bc_target_z", 0));
        } else {
            this.currentTarget = null;
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

    /** Marks the state dirty and pushes it to clients (Beacon-pattern update tag, see class javadoc). */
    private void syncToClients() {
        this.setChanged();
        if (this.level instanceof ServerLevel serverLevel) {
            BlockState state = this.getBlockState();
            serverLevel.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }
}
