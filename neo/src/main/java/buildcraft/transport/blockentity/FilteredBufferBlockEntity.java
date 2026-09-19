/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.transport.blockentity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.slf4j.Logger;
import buildcraft.transport.BcTransportBlockEntities;

/**
 * The M4.16 filtered buffer block entity ({@code buildcrafttransport:filtered_buffer}), replacing the M2.4c
 * {@code PlaceholderBlockEntity} under the unchanged id. Minimal faithful slice of the legacy
 * {@code buildcraft.transport.tile.TileFilteredBuffer} (frozen 8.0.x-1.20.1 tree):
 * <ul>
 * <li><b>Buffer inventory</b> &mdash; legacy: a 9-slot {@code ItemHandlerSimple} ({@code invMain}, exposed through
 * {@code IBCTileMenuProvider} on all six pipe faces with both insert and extract access) whose insert checker only
 * accepts stacks matching the 9 phantom {@code invFilter} slots. Slice: the same 9-slot, all-sides, insert+extract
 * buffer as a transactional {@link ItemStacksResourceHandler} &mdash; extraction always drains slot 0 first, and
 * insertion fills slot 0 upward, so the buffer behaves as a FIFO queue. <b>Trim (flagged for the transport migration):
 * the phantom filter grid and its GUI have not migrated</b> (the legacy empty-filter default actually rejects
 * everything until the filter is stocked; the slice accepts everything instead), and there is no menu yet.</li>
 * <li><b>Item flow</b> &mdash; identical to legacy: the buffer is passive, pushing pipes/hoppers insert through the
 * block item capability and pulling automation extracts (the M4.6 item pipe treats any capability-backed neighbour as
 * an exit target; vanilla hoppers insert into the face they point at and pull from the container above).</li>
 * <li><b>Observability</b> &mdash; the slice adds a {@code [M416]} log line on every committed contents change plus a
 * periodic summary in {@link #serverTick} (legacy had no logging; this is the in-world evidence channel for the
 * item-passes-through-the-buffer scenario).</li>
 * </ul>
 *
 * <p>Persistence rides the handler's own {@code ValueIO} codec under {@code bc_buffer} (shape: the vanilla
 * {@code ContainerHelper} item-list compound).
 */
public class FilteredBufferBlockEntity extends BlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Buffer size: the legacy 9-slot {@code invMain} (same slot count as the 9-slot phantom filter). */
    public static final int SLOTS = 9;

    /** Periodic summary cadence in ticks (2 s), only while the buffer holds items. */
    public static final int LOG_INTERVAL = 40;

    /**
     * The FIFO buffer. Every committed change logs one summary line (aborted automation probes never commit, so they
     * stay silent).
     */
    private final BufferHandler buffer = new BufferHandler();

    public FilteredBufferBlockEntity(BlockPos pos, BlockState state) {
        super(BcTransportBlockEntities.FILTERED_BUFFER.value(), pos, state);
    }

    /**
     * The buffer exposed to automation. Legacy handed the same {@code invMain} to every side
     * ({@code EnumPipePart.VALUES}); the side argument is ignored accordingly.
     */
    public ItemStacksResourceHandler getBuffer() {
        return this.buffer;
    }

    /** Human-readable one-line snapshot of the held items, for the log evidence. */
    public String describeContents() {
        StringBuilder builder = new StringBuilder();
        int held = 0;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = this.buffer.getResource(i).toStack((int) this.buffer.getAmountAsLong(i));
            if (!stack.isEmpty()) {
                if (held > 0) {
                    builder.append(", ");
                }
                builder.append(stack.getCount()).append('x').append(stack.getItem());
                held++;
            }
        }
        return held == 0 ? "empty" : builder.toString();
    }

    // ---------------------------------------------------------------------
    // Server tick: the periodic summary (insertion/extraction log on change)
    // ---------------------------------------------------------------------

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, FilteredBufferBlockEntity buffer) {
        if (level.getGameTime() % LOG_INTERVAL != 0) {
            return;
        }
        String description = buffer.describeContents();
        if (!description.equals("empty")) {
            LOGGER.info("[M416] filtered_buffer at {} holds: {}", pos, description);
        }
    }

    private final class BufferHandler extends ItemStacksResourceHandler {

        BufferHandler() {
            super(FilteredBufferBlockEntity.SLOTS);
        }

        @Override
        protected int getCapacity(int index, ItemResource resource) {
            // the legacy ItemHandlerSimple default stack limit
            return 64;
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            FilteredBufferBlockEntity.this.setChanged();
            if (FilteredBufferBlockEntity.this.level instanceof ServerLevel) {
                LOGGER.info("[M416] filtered_buffer at {} changed: {}", FilteredBufferBlockEntity.this.worldPosition,
                    FilteredBufferBlockEntity.this.describeContents());
            }
        }
    }

    // ---------------------------------------------------------------------
    // Persistence
    // ---------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.buffer.serialize(output.child("bc_buffer"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.buffer.deserialize(input.childOrEmpty("bc_buffer"));
    }
}
