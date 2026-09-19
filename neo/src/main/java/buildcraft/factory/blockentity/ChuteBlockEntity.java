/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.factory.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import buildcraft.factory.BcFactoryBlockEntities;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * M4.16 chute block entity: a minimal port of the legacy {@code TileChute} under the unchanged id
 * {@code buildcraftfactory:chute}. The v1 chute is a directional pass-through at the vanilla hopper cadence: every
 * {@link #MOVE_INTERVAL} ticks it pulls up to {@link #MOVE_AMOUNT} items of the first occupied slot out of the container
 * <em>above</em> (through the 26.1.2 item capability, the legacy {@code ItemTransactorHelper} bridge) and inserts them
 * into the block <em>below</em> &mdash; a vanilla chest, another machine's inventory or a transport item pipe's face
 * inbox (the pipe module registers the same {@code Capabilities.Item.BLOCK} capability, so pipes need no special
 * casing). Nothing is moved when either side is missing or the target is full, and the move is transactional: items are
 * only pulled out after the insertion side has committed to accepting them.
 *
 * <p><b>v1 trims (all javadoc-tracked, the task scopes the chute to the pull-from-above/push-below link):</b> the
 * legacy internal 4-slot inventory (and its item capability) is not carried &mdash; items flow straight through; the
 * dropped-item vacuum {@code pickupItems} and the six-side shuffled ejection are not carried (fixed above/below faces);
 * and the tiny MJ battery pacing is replaced by the fixed vanilla-hopper-like interval (legacy gained free progress
 * when facing up and MJ-driven progress otherwise).
 */
public class ChuteBlockEntity extends BlockEntity {

    /** Ticks between move operations (the vanilla hopper cadence, see the class javadoc). */
    public static final int MOVE_INTERVAL = 8;
    /** Items moved per operation (a fraction of a stack, clearly visible over a few seconds). */
    public static final int MOVE_AMOUNT = 16;

    /** Lifetime moved item count (evidence-rig counter, not persisted &mdash; the chute is stateless otherwise). */
    private long movedTotal;
    /** Ticks until the next move attempt. */
    private int cooldown;

    public ChuteBlockEntity(BlockPos pos, BlockState state) {
        super(BcFactoryBlockEntities.CHUTE.value(), pos, state);
    }

    public long getMovedTotal() {
        return this.movedTotal;
    }

    /** The per-tick move driver (wired through {@code ChuteBlock#getTicker}). */
    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, ChuteBlockEntity chute) {
        if (chute.cooldown > 0) {
            chute.cooldown--;
        } else {
            chute.cooldown = MOVE_INTERVAL;
            chute.moveStack(level, pos);
        }
    }

    /**
     * One pass-through operation: the first occupied slot of the upper container donates up to {@link #MOVE_AMOUNT}
     * items (of that one resource, the vanilla hopper shape) into the lower target. Insert first, extract second, one
     * transaction: an insertion the chute cannot pull back out of the source never commits.
     */
    private void moveStack(ServerLevel level, BlockPos pos) {
        ResourceHandler<ItemResource> above = this.neighbourHandler(level, pos.above(), Direction.DOWN);
        ResourceHandler<ItemResource> below = this.neighbourHandler(level, pos.below(), Direction.UP);
        if (above == null || below == null) {
            return;
        }
        for (int i = 0; i < above.size(); i++) {
            ItemResource resource = above.getResource(i);
            long available = above.getAmountAsLong(i);
            if (resource.isEmpty() || available <= 0) {
                continue;
            }
            int want = (int) Math.min(MOVE_AMOUNT, available);
            // probe what the target accepts (the probe transaction is discarded on close, nothing moves)
            int accepted;
            try (Transaction probe = Transaction.openRoot()) {
                accepted = below.insert(resource, want, probe);
            }
            if (accepted <= 0) {
                return;
            }
            try (Transaction move = Transaction.openRoot()) {
                int inserted = below.insert(resource, accepted, move);
                if (inserted <= 0) {
                    return;
                }
                int extracted = above.extract(i, resource, inserted, move);
                if (extracted < inserted) {
                    return; // source changed mid-move: discard the whole transaction
                }
                move.commit();
                this.movedTotal += inserted;
            }
            return; // one resource per operation (the vanilla hopper shape, see the class javadoc)
        }
    }

    /** The item capability of the neighbour at {@code targetPos}, queried on the face towards this chute. */
    private static @Nullable ResourceHandler<ItemResource> neighbourHandler(ServerLevel level, BlockPos targetPos,
        Direction faceTowardsChute) {
        return level.getCapability(Capabilities.Item.BLOCK, targetPos, faceTowardsChute);
    }
}
