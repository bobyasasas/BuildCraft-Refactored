/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.builders.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import buildcraft.builders.BcBuildersBlockEntities;
import buildcraft.builders.BcBuildersBlocks;
import buildcraft.builders.blueprint.BoxScan;
import buildcraft.builders.marker.MarkerPair;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The M4.17 replacer block entity (C route, replaces the {@code PlaceholderBlockEntity} under the unchanged id
 * {@code buildcraftbuilders:replacer}): it swaps every cell of one block kind inside a marker-pair box for the resource
 * block in its "to" buffer, one cell per tick, consuming one resource per replacement. Legacy counterpart:
 * {@code buildcraft.builders.tile.TileReplacer} (frozen 1.20.1 tree).
 *
 * <p><b>Legacy audit and the v1 mapping:</b>
 * <ul>
 * <li><b>Scope</b> &mdash; legacy: pure data surgery on a stored blueprint ({@code Blueprint#replace(from, to)}, a new
 * snapshot hash written back to {@code GlobalSavedDataSnapshots}); the world is never touched. v1: the same
 * from/to shape decision applied in place to the marker-pair box
 * ({@link ConstructionMarkerBlockEntity#findNearestPairedMarker}, the architect's lookup; the machine's own cell must
 * stay outside the box). The blueprint-editing form returns with the full snapshot system.</li>
 * <li><b>From/to</b> &mdash; legacy: two {@code ItemSchematicSingle} "used" items (one block each, free). v1: slot 0
 * is the from block and slots 1..{@link #SLOTS}-1 the to buffer ({@code BlockItem}s, one consumed per replacement
 * &mdash; the task's slice charges the resource instead of the legacy free data edit; flagged).</li>
 * <li><b>Loop</b> &mdash; one {@link BoxScan} cell per tick (ascending x, z, y; the filler slice's order), skipping
 * marker cells and cells that are not the from block; the [M417] line reports every {@link #PROGRESS_EVERY}
 * replacements and the walk total. No MJ (the baseline replacer has no battery either).</li>
 * </ul>
 *
 * <p>Client sync is the quarry's Beacon pattern. No GUI in v1 (the legacy {@code ContainerReplacer} migrates with the
 * menu framework).
 */
public class ReplacerBlockEntity extends BlockEntity {

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    /** Slot 0 = the from block, slots 1.. = the to buffer. */
    public static final int SLOTS = 9;
    /** Progress log pace in replaced cells. */
    public static final int PROGRESS_EVERY = 4;

    /** The from/to inventory ({@code invSchematicFrom}/{@code invSchematicTo} collapsed into one buffer). */
    private final ItemStacksResourceHandler inv = new ItemStacksResourceHandler(SLOTS) {
        @Override
        public boolean isValid(int index, ItemResource resource) {
            return resource.toStack(1).getItem() instanceof BlockItem;
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previous) {
            // new fuel for the walk: drop the throttle and restart from the box corner
            ReplacerBlockEntity.this.idleCooldown = 0;
            ReplacerBlockEntity.this.cursor = null;
            ReplacerBlockEntity.this.syncToClients();
        }
    };

    /** The box being worked (and its marker anchor), null while no pair is in range. */
    @Nullable
    private MarkerPair box;
    @Nullable
    private BlockPos boxAnchor;
    /** The walk cursor (null while idle or exhausted). */
    @Nullable
    private BlockPos cursor;
    /** Lifetime replacements (the evidence counter). */
    private long replacedTotal;
    /** Replacements since the last progress line. */
    private int replacedSinceLog;
    /** Ticks to wait before re-walking an exhausted box (the walk costs a full box scan per tick otherwise). */
    private int idleCooldown;

    public ReplacerBlockEntity(BlockPos pos, BlockState state) {
        super(BcBuildersBlockEntities.REPLACER.value(), pos, state);
    }

    // ----------------------------------------------------------------- access (probe / rig / capability)

    public ResourceHandler<ItemResource> getInv() {
        return this.inv;
    }

    public long getReplacedTotal() {
        return this.replacedTotal;
    }

    public @Nullable MarkerPair getBox() {
        return this.box;
    }

    /** Sets the from block programmatically (slot 0; the probe/rig twin of the capability insert). */
    public void setFrom(BlockItem blockItem) {
        this.inv.set(0, ItemResource.of(new ItemStack(blockItem)), 1);
    }

    /** Inserts one stack into the to buffer, returning the rejected remainder. */
    public ItemStack insertTo(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem)) {
            return stack;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = 0;
            for (int slot = 1; slot < SLOTS && inserted < stack.getCount(); slot++) {
                inserted += this.inv.insert(slot, ItemResource.of(stack), stack.getCount() - inserted, transaction);
            }
            transaction.commit();
            ItemStack remainder = stack.copy();
            remainder.setCount(stack.getCount() - inserted);
            return remainder;
        }
    }

    /** Right-click/empty-hand status line (the [M417] evidence). */
    public void logStatus() {
        LOGGER.info("[M417] replacer at {}: from={}, to-buffer={}, box={}, replaced={}",//
            this.worldPosition, this.fromBlockId() == null ? "none" : this.fromBlockId(), this.toBufferCount(),//
            this.box == null ? "none" : this.box.sizeSummary(), this.replacedTotal);
    }

    /** The from block's id (slot 0), or null when the slot is empty. */
    private @Nullable String fromBlockId() {
        if (this.inv.getAmountAsLong(0) <= 0) {
            return null;
        }
        ItemStack stack = this.inv.getResource(0).toStack(1);
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return null;
        }
        return BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).toString();
    }

    /** The replacement block (first non-empty to slot), or null when the buffer is empty. */
    private @Nullable Block toBlock() {
        for (int slot = 1; slot < SLOTS; slot++) {
            if (this.inv.getAmountAsLong(slot) <= 0) {
                continue;
            }
            ItemStack stack = this.inv.getResource(slot).toStack(1);
            if (stack.getItem() instanceof BlockItem blockItem) {
                return blockItem.getBlock();
            }
        }
        return null;
    }

    private long toBufferCount() {
        long total = 0;
        for (int slot = 1; slot < SLOTS; slot++) {
            total += this.inv.getAmountAsLong(slot);
        }
        return total;
    }

    /** Takes one replacement item out of the to buffer (the per-replacement consumption). */
    private boolean consumeOneTo(Block block) {
        ItemResource resource = ItemResource.of(block.asItem());
        try (Transaction transaction = Transaction.openRoot()) {
            for (int slot = 1; slot < SLOTS; slot++) {
                if (this.inv.extract(slot, resource, 1, transaction) > 0) {
                    transaction.commit();
                    return true;
                }
            }
        }
        return false;
    }

    // ----------------------------------------------------------------- server tick

    /** Per-tick logic, wired through {@code ReplacerBlock#getTicker} (vanilla furnace static-tick pattern). */
    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, ReplacerBlockEntity replacer) {
        replacer.tickWork(level);
    }

    private void tickWork(ServerLevel level) {
        // keep the box live: a live anchor refreshes it, a broken one drops both
        if (this.boxAnchor != null) {
            if (level.getBlockEntity(this.boxAnchor) instanceof ConstructionMarkerBlockEntity marker
                && marker.getPair() != null) {
                this.box = marker.getPair();
            } else {
                this.box = null;
                this.boxAnchor = null;
                this.cursor = null;
            }
        } else if ((level.getGameTime() % ArchitectBlockEntity.BOX_RECHECK_TICKS) == 0) {
            ConstructionMarkerBlockEntity marker = ConstructionMarkerBlockEntity.findNearestPairedMarker(
                level, this.worldPosition);
            if (marker != null && marker.getPair() != null) {
                this.box = marker.getPair();
                this.boxAnchor = marker.getBlockPos();
                this.cursor = null;
                LOGGER.info("[M417] replacer at {} linked to marker box {} at {}",//
                    this.worldPosition, this.box.sizeSummary(), this.boxAnchor);
            }
        }
        if (this.box == null) {
            return;
        }
        if (this.idleCooldown > 0) {
            this.idleCooldown--;
            return; // the last walk found nothing; wait for new fuel or a re-arm instead of rescanning every tick
        }
        String fromId = this.fromBlockId();
        Block toBlock = this.toBlock();
        if (fromId == null || toBlock == null) {
            return; // missing from block or empty to buffer: idle (the legacy three-slot precondition)
        }
        String toId = BuiltInRegistries.BLOCK.getKey(toBlock).toString();
        if (fromId.equals(toId)) {
            return; // nothing to do (the legacy replace(from, to) is a no-op then)
        }
        MarkerPair box = this.box;
        BlockPos start = this.cursor == null ? box.min() : this.cursor;
        BlockPos found = BoxScan.nextMatch(start, box.min(), box.max(), candidate -> {
            if (level.getBlockState(candidate).getBlock() == BcBuildersBlocks.MARKER_CONSTRUCTION.value()) {
                return false; // markers are scaffolding, never content
            }
            String candidateId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(candidate).getBlock()).toString();
            return candidateId.equals(fromId);
        });
        this.cursor = found;
        if (found == null) {
            this.idleCooldown = ArchitectBlockEntity.BOX_RECHECK_TICKS;
            if (this.replacedSinceLog > 0) {
                LOGGER.info("[M417] replacer at {}: walk done, replaced {} (total {})", this.worldPosition,
                    this.replacedSinceLog, this.replacedTotal);
                this.replacedSinceLog = 0;
            }
            return;
        }
        level.setBlock(found, toBlock.defaultBlockState(), Block.UPDATE_ALL);
        this.consumeOneTo(toBlock);
        this.replacedTotal++;
        this.replacedSinceLog++;
        if (this.replacedSinceLog % PROGRESS_EVERY == 0) {
            LOGGER.info("[M417] replacer at {}: replaced {} so far (total {})", this.worldPosition,
                this.replacedSinceLog, this.replacedTotal);
        }
        this.syncToClients();
    }

    // ----------------------------------------------------------------- persistence + client sync

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.inv.serialize(output.child("bc_inv"));
        output.putLong("bc_replaced", this.replacedTotal);
        if (this.box != null) {
            output.store("bc_box", CompoundTag.CODEC, this.box.writeTo(new CompoundTag()));
        }
        if (this.boxAnchor != null) {
            output.store("bc_anchor", BlockPos.CODEC, this.boxAnchor);
        }
        if (this.cursor != null) {
            output.store("bc_cursor", BlockPos.CODEC, this.cursor);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.inv.deserialize(input.childOrEmpty("bc_inv"));
        this.replacedTotal = input.getLongOr("bc_replaced", 0L);
        this.box = input.read("bc_box", CompoundTag.CODEC).map(MarkerPair::from).orElse(null);
        this.boxAnchor = input.read("bc_anchor", BlockPos.CODEC).orElse(null);
        this.cursor = input.read("bc_cursor", BlockPos.CODEC).orElse(null);
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
