/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.robotics.blockentity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import buildcraft.robotics.BcRoboticsBlockEntities;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * M4.17 requester block entity: a minimal port of the legacy {@code TileRequester} under the unchanged id
 * {@code buildcraftrobotics:requester}. The machine holds the legacy pair of inventories &mdash; the 20 phantom request
 * template slots (legacy {@code requests}) and the 20-slot buffer (legacy {@code inv}), the buffer slot {@code i} only
 * ever accepting stacks that match template {@code i} (legacy {@code isItemValid}) &mdash; and fulfils its requests
 * itself: every {@link #PULL_INTERVAL} ticks it pulls matching items out of the six adjacent containers through the
 * 26.1.2 item capability (legacy {@code EnumPipePart.VALUES} side layout) into the buffer, up to each template's
 * outstanding count ({@link RequesterLogic}, the count form of the legacy
 * {@code getRequest}/{@code offerItem} robot contract).
 *
 * <p><b>v1 trims (all javadoc-tracked; the task scopes the requester to list + self-pull):</b> the legacy
 * {@code IRequestProvider} robot network delivery is replaced by the direct neighbour pull (the robot AI that consumed
 * it is v2 and untouched); the {@code ContainerRequester} GUI is not carried &mdash; the template slots are set
 * programmatically through {@link #setRequestTemplate} (the server branch of the legacy {@code setRequest}, without its
 * client-to-server message half); and the comparator analog output of {@code BlockRequester} is not carried. The buffer
 * keeps the legacy both-ways capability exposure so pipes/hoppers can still serve or drain it.
 *
 * <p><b>Evidence logging</b> is the task's dual-evidence duty: every pull lands a {@code [M417]} line, shortfall is
 * logged on change (and cleared once when everything is fulfilled), and the first tick after a world reload announces
 * what was restored from the save (the persistence check).
 */
public class RequesterBlockEntity extends BlockEntity {

    /** The legacy {@code TileRequester.NB_ITEMS}: 20 template slots over 20 buffer slots. */
    public static final int NB_ITEMS = 20;
    /** Ticks between pull passes (v1 pacing standing in for the legacy robot delivery cadence). */
    public static final int PULL_INTERVAL = 20;
    /** Per-slot stack cap (the legacy {@code ItemHandlerSimple} 64 capacities). */
    public static final int SLOT_CAPACITY = 64;

    private static final Logger LOGGER = LogUtils.getLogger();

    /** The phantom request templates (legacy {@code requests}); slot {@code i} configures buffer slot {@code i}. */
    private final NonNullList<ItemStack> requests = NonNullList.withSize(NB_ITEMS, ItemStack.EMPTY);

    /**
     * The buffer (legacy {@code inv}): slot {@code i} accepts only stacks matching template {@code i} and keeps the
     * legacy both-ways access (automation may insert into and extract from it).
     */
    private final ItemStacksResourceHandler inv = new ItemStacksResourceHandler(NB_ITEMS) {
        @Override
        protected int getCapacity(int index, ItemResource resource) {
            return SLOT_CAPACITY;
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            ItemStack template = RequesterBlockEntity.this.requests.get(index);
            return !template.isEmpty() && !resource.isEmpty()
                && ItemStack.isSameItemSameComponents(template, resource.toStack(1));
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            RequesterBlockEntity.this.setChanged();
        }
    };

    /** Ticks until the next pull pass. */
    private int cooldown;
    /** Lifetime pulled item count (the evidence counter). */
    private long pulledTotal;
    /** One-shot guard for the restore announce; a template write marks the BE as freshly configured, not restored. */
    private boolean announced;
    /** The last logged shortfall signature (shortfall lines fire on change only). */
    @Nullable
    private String lastShortfall;

    public RequesterBlockEntity(BlockPos pos, BlockState state) {
        super(BcRoboticsBlockEntities.REQUESTER.value(), pos, state);
    }

    /** The full buffer view (capability target and evidence-rig read side). */
    public ItemStacksResourceHandler getInv() {
        return this.inv;
    }

    public long getPulledTotal() {
        return this.pulledTotal;
    }

    /** The template stack of slot {@code i} (legacy {@code getRequestTemplate}, unguarded like legacy). */
    public ItemStack getRequestTemplate(int index) {
        return this.requests.get(index);
    }

    /**
     * Sets one request template (the server branch of the legacy {@code setRequest}; the client message half died with
     * the v1 GUI trim). Pass an empty stack to clear the slot.
     */
    public void setRequestTemplate(int index, ItemStack template) {
        this.requests.set(index, template.copy());
        this.announced = true;
        this.setChanged();
    }

    /** The per-tick pull driver (wired through {@code RequesterBlock#getTicker}). */
    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, RequesterBlockEntity requester) {
        requester.announceRestored();
        if (requester.cooldown > 0) {
            requester.cooldown--;
        } else {
            requester.cooldown = PULL_INTERVAL;
            requester.pullFromNeighbours(level);
        }
    }

    /**
     * First-tick persistence announce: a BE that came back from the save with templates or buffer content says so (the
     * run-B half of the persistence evidence). Freshly configured machines never announce &mdash;
     * {@link #setRequestTemplate} arms {@link #announced} first.
     */
    private void announceRestored() {
        if (this.announced) {
            return;
        }
        this.announced = true;
        long buffered = this.bufferedTotal();
        long templates = this.requests.stream().filter(template -> !template.isEmpty()).count();
        if (buffered > 0 || templates > 0) {
            LOGGER.info("[M417] requester at {}: restored from save: {} request template(s), {} item(s) buffered"
                + " (lifetime pulled {})", this.worldPosition, templates, buffered, this.pulledTotal);
        }
    }

    /**
     * One pull pass: every pending template scans the six neighbour faces for a matching stack and pulls its deficit
     * (transactionally &mdash; the buffer insert only commits once the source extraction succeeded, the chute move
     * shape). Afterwards the shortfall bookkeeping runs (see {@link #logShortfall}).
     */
    private void pullFromNeighbours(ServerLevel level) {
        for (int i = 0; i < NB_ITEMS; i++) {
            ItemStack template = this.requests.get(i);
            if (template.isEmpty()) {
                continue;
            }
            int deficit = RequesterLogic.deficit(template.getCount(), (int) Math.min(this.inv.getAmountAsLong(i),
                Integer.MAX_VALUE));
            if (deficit <= 0) {
                continue;
            }
            ItemResource want = ItemResource.of(template);
            for (Direction side : Direction.values()) {
                ResourceHandler<ItemResource> source = level.getCapability(Capabilities.Item.BLOCK,
                    this.worldPosition.relative(side), side.getOpposite());
                if (source == null) {
                    continue;
                }
                for (int s = 0; s < source.size() && deficit > 0; s++) {
                    ItemResource offer = source.getResource(s);
                    long available = source.getAmountAsLong(s);
                    if (offer.isEmpty() || available <= 0
                        || !ItemStack.isSameItemSameComponents(template, offer.toStack(1))) {
                        continue;
                    }
                    int plan = RequesterLogic.planPull(deficit, available);
                    if (plan <= 0) {
                        continue;
                    }
                    try (Transaction move = Transaction.openRoot()) {
                        int inserted = this.inv.insert(want, plan, move);
                        if (inserted <= 0) {
                            continue;
                        }
                        int extracted = source.extract(s, offer, inserted, move);
                        if (extracted < inserted) {
                            continue; // source changed mid-move: discard the whole transaction
                        }
                        move.commit();
                        this.pulledTotal += extracted;
                        this.setChanged();
                        deficit -= extracted;
                        LOGGER.info("[M417] requester at {}: pulled {} x {} from {} into buffer slot {} (slot now"
                            + " {}/{})", this.worldPosition, extracted, BuiltInRegistries.ITEM.getKey(offer.getItem()),
                            side, i, this.inv.getAmountAsLong(i), template.getCount());
                    }
                }
                if (deficit <= 0) {
                    break;
                }
            }
        }
        this.logShortfall();
    }

    /**
     * The shortfall log: one {@code [M417]} line describing every pending slot, fired only when the description
     * changes, plus a one-shot clear line when the last shortfall is fulfilled (legacy {@code isFulfilled} per slot,
     * aggregated for the evidence log).
     */
    private void logShortfall() {
        StringBuilder pending = new StringBuilder();
        for (int i = 0; i < NB_ITEMS; i++) {
            ItemStack template = this.requests.get(i);
            if (template.isEmpty()) {
                continue;
            }
            long buffered = this.inv.getAmountAsLong(i);
            boolean matches = buffered > 0
                && ItemStack.isSameItemSameComponents(template, this.inv.getResource(i).toStack(1));
            if (RequesterLogic.classify(template.isEmpty(), buffered == 0, matches, (int) Math.min(buffered,
                Integer.MAX_VALUE), template.getCount()) == RequesterLogic.Fulfilment.FULFILLED) {
                continue;
            }
            if (!pending.isEmpty()) {
                pending.append("; ");
            }
            int bufferedCount = (int) Math.min(buffered, Integer.MAX_VALUE);
            pending.append("slot ").append(i).append(": want ").append(template.getCount()).append("x ")
                .append(BuiltInRegistries.ITEM.getKey(template.getItem())).append(", buffered ").append(buffered)
                .append(" (missing ").append(RequesterLogic.deficit(template.getCount(), bufferedCount)).append(")");
        }
        String signature = pending.toString();
        if (signature.isEmpty()) {
            if (this.lastShortfall != null && !this.lastShortfall.isEmpty()) {
                LOGGER.info("[M417] requester at {}: all requests fulfilled (lifetime pulled {})", this.worldPosition,
                    this.pulledTotal);
            }
            this.lastShortfall = "";
        } else if (!signature.equals(this.lastShortfall)) {
            LOGGER.info("[M417] requester at {}: request shortfall [{}]", this.worldPosition, signature);
            this.lastShortfall = signature;
        }
    }

    private long bufferedTotal() {
        long total = 0;
        for (int i = 0; i < NB_ITEMS; i++) {
            total += this.inv.getAmountAsLong(i);
        }
        return total;
    }

    // ---------------------------------------------------------------------
    // Persistence (no client sync in v1: nothing renders from the buffer or the templates, the trim is javadoc-tracked)
    // ---------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.inv.serialize(output.child("bc_inv"));
        // the legacy slot-tagged shape (empty slots are simply absent; the slot index rides along), see
        // ContainerHelper: a raw ItemStack.CODEC.listOf() cannot encode the 18 empty template slots at all
        ContainerHelper.saveAllItems(output, this.requests);
        output.putLong("bc_pulled_total", this.pulledTotal);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.inv.deserialize(input.childOrEmpty("bc_inv"));
        ContainerHelper.loadAllItems(input, this.requests);
        this.pulledTotal = input.getLongOr("bc_pulled_total", 0L);
    }
}
