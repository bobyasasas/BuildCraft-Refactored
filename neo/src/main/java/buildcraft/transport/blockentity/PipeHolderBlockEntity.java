/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.transport.blockentity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;
import buildcraft.core.BcBlocks;
import buildcraft.core.block.KinesisPipeBlock;
import buildcraft.core.blockentity.EnergyMeterBlockEntity;
import buildcraft.core.blockentity.MjReceiver;
import buildcraft.core.blockentity.StoneEngineBlockEntity;
import buildcraft.transport.BcTransportBlockEntities;
import buildcraft.transport.BcTransportBlocks;
import buildcraft.transport.net.MessageMultiPipeItem;
import buildcraft.transport.net.PipeItemMessageQueue;
import buildcraft.transport.pipe.BcPipeFamilies;
import buildcraft.transport.pipe.BcPipeFamilies.Family;

/**
 * M4.6 block entity for the single {@code buildcrafttransport:pipe_holder} block (the legacy
 * {@code buildcraft.transport.tile.TilePipeHolder} visual slice). One BE type carries every pipe family, exactly like
 * legacy: the family and dye colour are stored here (set by {@code ItemPipeHolder} on placement) and drive the world
 * renderer.
 *
 * <p><b>What is simulated (deliberately small, the real transport module graph is a later milestone):</b>
 * <ul>
 * <li><b>Connections</b> are rescanned every server tick (six neighbour lookups, the
 * {@code KinesisPipeBlockEntity} slice pattern): pipe-to-pipe (with the legacy wooden rule), item-capability
 * neighbours for item pipes, liquid sources for fluid pipes, engines/mj receivers/meters for power pipes. The set is
 * pushed to the client through the update tag and drives both the renderer's arms and the block's shape.</li>
 * <li><b>Items</b>: automation can push stacks into the pipe through the {@code item_handler} capability
 * (per-face inboxes, drained each tick); each stack becomes a travelling item that moves edge &rarr; centre &rarr; next
 * pipe or neighbour inventory. Travelling items are synced to clients with the legacy
 * {@link MessageMultiPipeItem} wire format (baseline {@code PipeItemMessageQueue}). Items that cannot exit are dropped
 * back into the world after a grace period, like legacy's return-to-world fallback.</li>
 * <li><b>Fluids</b>: a visual-only single-bucket buffer, filled/drained by bucket use
 * ({@link #interactWithBucket}); there is no fluid network yet.</li>
 * <li><b>Power</b>: the {@code KinesisPipeBlockEntity} diffusion slice (pull from engines and adjacent power pipes,
 * push into mj receivers/meters) drives the pulse renderer; no path finding.</li>
 * </ul>
 *
 * <p><b>Client sync (update tag, the vanilla {@code BeaconBlockEntity} pattern used since M2.7b):</b> the update tag
 * is {@link #saveCustomOnly}, and the client applies it through {@code loadWithComponents} &rarr;
 * {@link #loadAdditional}.
 */
public class PipeHolderBlockEntity extends BlockEntity {

    /** Ticks an item takes to travel from one pipe edge to the centre (and centre to edge). */
    public static final int HALF_TRAVEL_TICKS = 2;
    /** Ticks an item waits at the centre when no exit accepts it, before re-trying. */
    public static final int RETRY_TICKS = 10;
    /** Power slice buffer size in &micro;MJ ({@code KinesisPipeBlockEntity.CAPACITY}). */
    public static final long POWER_CAPACITY = 10_000;
    /** Power slice max transfer per connection and tick, in &micro;MJ ({@code KinesisPipeBlockEntity.RATE}). */
    public static final long POWER_RATE = 1_000;
    /** Fluid slice capacity, in mB (legacy pipes held one bucket per segment). */
    public static final int FLUID_CAPACITY = 1000;

    /** No pluggable on the face ({@link #attachPlug}). */
    public static final byte PLUG_NONE = 0;
    /** A structural blocker plug (baseline {@code buildcrafttransport:blocker} pluggable). */
    public static final byte PLUG_BLOCKER = 1;
    /** A power adaptor plug (baseline {@code buildcrafttransport:power_adapter} pluggable). */
    public static final byte PLUG_POWER_ADAPTOR = 2;

    /** The pipe family (index into {@link BcPipeFamilies#FAMILIES}); -1 while unset. */
    private int familyIndex = -1;
    /** The pipe colour, or null for the colourless variant. */
    @Nullable
    private DyeColor colour;

    /** Pluggable type codes per face ordinal (update-tag + renderer state, see {@link #attachPlug}). */
    private final byte[] plugs = new byte[6];

    /** Connected directions, recomputed server-side every tick and pushed via the update tag. */
    public final EnumSet<Direction> connections = EnumSet.noneOf(Direction.class);

    /** The fluid slice buffer (visual only; see class javadoc). */
    private FluidStack fluid = FluidStack.EMPTY;

    /** The power slice buffer, &micro;MJ (power pipe families only). */
    private long powerStored;

    /** One travelling item inside this pipe (server side truth; legacy {@code TravellingItem} slice). */
    static final class TravellingItem {
        final ItemStack stack;
        /** The face the item is moving along: entry face while {@link #toCenter}, exit face afterwards. */
        final Direction side;
        /** True while the item travels from the {@link #side} edge towards the centre. */
        final boolean toCenter;
        int ticksLeft;

        TravellingItem(ItemStack stack, Direction side, boolean toCenter) {
            this.stack = stack;
            this.side = side;
            this.toCenter = toCenter;
            this.ticksLeft = PipeHolderBlockEntity.HALF_TRAVEL_TICKS;
        }
    }

    /** Items currently inside this pipe (server side truth; clients get the batched mirror). */
    private final List<TravellingItem> travellingItems = new ArrayList<>();
    /** Set when the travelling item list changed this tick (triggers a {@link MessageMultiPipeItem} batch). */
    private boolean itemsDirty;

    /**
     * Per-face item inboxes for automation insertion (hoppers etc., the legacy
     * {@code PipeTransportItems#receiveItems} analogue). The inboxes are drained into travelling items at the start
     * of each server tick.
     */
    private final PipeInbox[] inboxes = new PipeInbox[6];

    public PipeHolderBlockEntity(BlockPos pos, BlockState state) {
        super(BcTransportBlockEntities.PIPE_HOLDER.value(), pos, state);
        for (int i = 0; i < this.inboxes.length; i++) {
            this.inboxes[i] = new PipeInbox();
        }
    }

    // ---------------------------------------------------------------------
    // pipe identity (family + colour + plugs)
    // ---------------------------------------------------------------------

    /** The pipe family, or null when unset (never happens for item-placed pipes). */
    @Nullable
    public Family getFamily() {
        if (this.familyIndex < 0 || this.familyIndex >= BcPipeFamilies.FAMILIES.size()) {
            return null;
        }
        return BcPipeFamilies.FAMILIES.get(this.familyIndex);
    }

    /** The pipe colour, or null for the colourless variant. */
    @Nullable
    public DyeColor getColour() {
        return this.colour;
    }

    /** Sets the pipe identity (called by {@code ItemPipeHolder} right after placing the block). */
    public void setPipe(Family family, @Nullable DyeColor colour) {
        this.familyIndex = BcPipeFamilies.FAMILIES.indexOf(family);
        this.colour = colour;
        this.pushToClients();
    }

    /** The pluggable type on the given face ({@link #PLUG_NONE}, {@link #PLUG_BLOCKER}, ...). */
    public byte getPlug(Direction face) {
        return this.plugs[face.ordinal()];
    }

    /** Attaches a plug to the given face, replacing any previous one (legacy: one pluggable slot per face). */
    public void attachPlug(Direction face, byte plugType) {
        this.plugs[face.ordinal()] = plugType;
        this.pushToClients();
    }

    /** The world texture stem for the renderer ({@code BcPipeFamilies#textureStem}). */
    public String getTextureStem() {
        Family family = this.getFamily();
        if (family == null) {
            return "items_stone";
        }
        return BcPipeFamilies.textureStem(family, this.colour);
    }

    // ---------------------------------------------------------------------
    // fluid slice (visual only)
    // ---------------------------------------------------------------------

    /** The buffered fluid for the renderer, or empty. */
    public FluidStack getFluid() {
        return this.fluid;
    }

    /**
     * Fills or drains the fluid slice (called from {@code PipeHolderBlock#useItemOn} with the bucket's content).
     *
     * @return true when the bucket's content was fully consumed (bucket should lose it).
     */
    public boolean interactWithBucket(FluidStack bucketContent) {
        if (bucketContent.isEmpty()) {
            // empty bucket: drain the whole buffer into the bucket
            if (this.fluid.isEmpty()) {
                return false;
            }
            this.fluid = FluidStack.EMPTY;
            this.pushToClients();
            return true;
        }
        if (this.fluid.isEmpty()) {
            this.fluid = bucketContent.copyWithAmount(Math.min(bucketContent.getAmount(), FLUID_CAPACITY));
        } else if (FluidStack.isSameFluidSameComponents(this.fluid, bucketContent)) {
            int filled = Math.min(bucketContent.getAmount(), FLUID_CAPACITY - this.fluid.getAmount());
            if (filled <= 0) {
                return false;
            }
            this.fluid.setAmount(this.fluid.getAmount() + filled);
        } else {
            return false;
        }
        this.pushToClients();
        return true;
    }

    /** The buffered power, &micro;MJ (drives the pulse renderer). */
    public long getPowerStored() {
        return this.powerStored;
    }

    // ---------------------------------------------------------------------
    // server tick
    // ---------------------------------------------------------------------

    /** The per-tick entry point, wired through {@code PipeHolderBlock#getTicker}. */
    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, PipeHolderBlockEntity pipe) {
        pipe.recomputeConnections(level, pos);
        pipe.drainInboxes();
        pipe.tickItems(level, pos);
        pipe.tickPower(level, pos);
        if (pipe.itemsDirty) {
            pipe.itemsDirty = false;
            PipeItemMessageQueue.appendPipe(pipe);
        }
    }

    /**
     * The connection scan (see class javadoc). Runs server-side so the result is authoritative for the renderer (via
     * the update tag) and for the item routing.
     */
    private void recomputeConnections(Level level, BlockPos pos) {
        Family family = this.getFamily();
        if (family == null) {
            if (!this.connections.isEmpty()) {
                this.connections.clear();
                this.pushToClients();
            }
            return;
        }
        EnumSet<Direction> previous = EnumSet.copyOf(this.connections);
        this.connections.clear();
        for (Direction direction : Direction.values()) {
            if (this.isConnected(level, pos, family, direction)) {
                this.connections.add(direction);
            }
        }
        if (!previous.equals(this.connections)) {
            this.pushToClients();
        }
    }

    private boolean isConnected(Level level, BlockPos pos, Family family, Direction direction) {
        BlockPos neighbourPos = pos.relative(direction);
        BlockState neighbour = level.getBlockState(neighbourPos);
        // pipe-to-pipe, with the legacy wooden rule (wooden pipes never connect to each other)
        if (neighbour.getBlock() == BcTransportBlocks.PIPE_HOLDER.value()
                && level.getBlockEntity(neighbourPos) instanceof PipeHolderBlockEntity other) {
            Family otherFamily = other.getFamily();
            if (otherFamily == null) {
                return false;
            }
            return !(family.wooden && otherFamily.wooden);
        }
        if (neighbour.getBlock() instanceof KinesisPipeBlock) {
            // the core kinesis slice pipes connect to power families (and only those, like legacy)
            return family.flow == BcPipeFamilies.FlowKind.POWER;
        }
        switch (family.flow) {
            case ITEMS: {
                // anything exposing an item target: vanilla/modded inventories (chests, hoppers, furnaces...)
                return level.getCapability(Capabilities.Item.BLOCK, neighbourPos, direction.getOpposite()) != null;
            }
            case FLUIDS: {
                // liquid source blocks (water etc.); fluid tanks have not migrated yet
                return neighbour.getFluidState().isSource();
            }
            case POWER: {
                return neighbour.getBlock() == BcBlocks.ENERGY_METER.value()
                        || level.getBlockEntity(neighbourPos) instanceof MjReceiver
                        || level.getBlockEntity(neighbourPos) instanceof StoneEngineBlockEntity;
            }
            default: {
                // structure pipes only ever connect to pipes
                return false;
            }
        }
    }

    // ---------------------------------------------------------------------
    // item slice
    // ---------------------------------------------------------------------

    /** One face's insertion inbox (new-API resource handler keeps the automation plumbing transactional). */
    private final class PipeInbox extends ItemStacksResourceHandler {
        PipeInbox() {
            super(1);
        }

        @Override
        protected int getCapacity(int index, ItemResource resource) {
            return 64;
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            PipeHolderBlockEntity.this.itemsDirty = true;
            PipeHolderBlockEntity.this.setChanged();
        }
    }

    /** The inbox automation inserts into from the given face ({@code null} face = generic access). */
    public ResourceHandler<ItemResource> getInbox(@Nullable Direction face) {
        int index = face == null ? 0 : face.ordinal();
        return this.inboxes[index];
    }

    private void drainInboxes() {
        for (int i = 0; i < this.inboxes.length; i++) {
            PipeInbox inbox = this.inboxes[i];
            ItemResource resource = inbox.getResource(0);
            long amount = inbox.getAmountAsLong(0);
            if (amount <= 0) {
                continue;
            }
            inbox.set(0, ItemResource.EMPTY, 0);
            this.travellingItems.add(new TravellingItem(resource.toStack((int) amount), Direction.values()[i], true));
            this.itemsDirty = true;
        }
    }

    private void tickItems(ServerLevel level, BlockPos pos) {
        if (this.travellingItems.isEmpty()) {
            return;
        }
        // backward index loop: removals during the loop are safe, items appended by receiveItem()/exitTowards()
        // bounce-backs sit above the starting index and are handled next tick
        for (int i = this.travellingItems.size() - 1; i >= 0; i--) {
            TravellingItem item = this.travellingItems.get(i);
            item.ticksLeft--;
            if (item.ticksLeft > 0) {
                continue;
            }
            this.itemsDirty = true;
            if (!item.toCenter) {
                // reached the exit edge but nothing accepted it: drop into the world (legacy's fallback)
                this.travellingItems.remove(i);
                this.dropItem(level, pos, item.stack);
                continue;
            }
            // reached the centre: pick the next leg
            Direction exit = this.pickExit(level, pos, item);
            if (exit == null) {
                item.ticksLeft = RETRY_TICKS; // wait and retry
                continue;
            }
            this.travellingItems.remove(i);
            this.exitTowards(level, pos, item, exit);
        }
    }

    /** Chooses the exit face at the pipe centre: machines that accept first, then other pipes as last resort. */
    @Nullable
    private Direction pickExit(Level level, BlockPos pos, TravellingItem item) {
        Direction pipeExit = null;
        for (Direction direction : this.connections) {
            if (direction == item.side) {
                continue;
            }
            BlockPos neighbourPos = pos.relative(direction);
            if (level.getBlockEntity(neighbourPos) instanceof PipeHolderBlockEntity) {
                pipeExit = direction; // pipes are a last-resort exit; keep scanning for machines
                continue;
            }
            ResourceHandler<ItemResource> target = level.getCapability(
                    Capabilities.Item.BLOCK, neighbourPos, direction.getOpposite());
            if (target != null && this.canInsert(target, item.stack)) {
                return direction;
            }
        }
        return pipeExit;
    }

    /** Insertion probe (transaction is aborted on close, so nothing moves). */
    private boolean canInsert(ResourceHandler<ItemResource> target, ItemStack stack) {
        try (Transaction transaction = Transaction.openRoot()) {
            return target.insert(ItemResource.of(stack), 1, transaction) > 0;
        }
    }

    /** Hands a centre item over to the given exit face; leftovers that nothing accepts drop into the world. */
    private void exitTowards(ServerLevel level, BlockPos pos, TravellingItem item, Direction exit) {
        BlockPos neighbourPos = pos.relative(exit);
        if (level.getBlockEntity(neighbourPos) instanceof PipeHolderBlockEntity other) {
            other.receiveItem(item.stack, exit.getOpposite());
            return;
        }
        ResourceHandler<ItemResource> target = level.getCapability(
                Capabilities.Item.BLOCK, neighbourPos, exit.getOpposite());
        if (target != null) {
            try (Transaction transaction = Transaction.openRoot()) {
                int accepted = target.insert(ItemResource.of(item.stack), item.stack.getCount(), transaction);
                if (accepted > 0) {
                    transaction.commit();
                }
                if (accepted >= item.stack.getCount()) {
                    return;
                }
                item.stack.shrink(accepted);
            }
        }
        // nothing (or not everything) accepted: drop the rest into the world at the pipe centre
        this.dropItem(level, pos, item.stack);
    }

    /** Receives an item from the neighbouring pipe ({@code entryFace} is the face it arrives through). */
    private void receiveItem(ItemStack stack, Direction entryFace) {
        this.travellingItems.add(new TravellingItem(stack, entryFace, true));
        this.itemsDirty = true;
    }

    private void dropItem(ServerLevel level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack.copy());
        level.addFreshEntity(entity);
    }

    /** Appends this pipe's travelling items to the client sync batch (called by {@code PipeItemMessageQueue}). */
    public void appendItemData(MessageMultiPipeItem message) {
        for (TravellingItem item : this.travellingItems) {
            int stackId = BuiltInRegistries.ITEM.getId(item.stack.getItem());
            message.append(this.worldPosition, stackId, (byte) Math.min(64, item.stack.getCount()),
                    item.toCenter, item.side, null, (byte) Math.max(1, item.ticksLeft));
        }
    }

    // ---------------------------------------------------------------------
    // power slice (KinesisPipeBlockEntity diffusion pattern)
    // ---------------------------------------------------------------------

    private void tickPower(ServerLevel level, BlockPos pos) {
        Family family = this.getFamily();
        if (family == null || family.flow != BcPipeFamilies.FlowKind.POWER) {
            return;
        }
        // pull phase: engines pointing at this pipe, then higher adjacent power pipes (half-difference)
        for (Direction direction : Direction.values()) {
            if (this.powerStored >= POWER_CAPACITY) {
                break;
            }
            BlockEntity neighbour = level.getBlockEntity(pos.relative(direction));
            if (neighbour instanceof StoneEngineBlockEntity engine
                    && engine.getOutputFacing() == direction.getOpposite()) {
                this.powerStored += engine.extractEnergy(Math.min(POWER_RATE, POWER_CAPACITY - this.powerStored), false);
            } else if (neighbour instanceof PipeHolderBlockEntity other
                    && other.getFamily() != null
                    && other.getFamily().flow == BcPipeFamilies.FlowKind.POWER
                    && other.powerStored > this.powerStored) {
                long halfDifference = (other.powerStored - this.powerStored) / 2;
                long pulled = other.extractPower(Math.min(POWER_RATE, Math.min(halfDifference,
                        POWER_CAPACITY - this.powerStored)));
                this.powerStored += pulled;
            }
        }
        // push phase: meters and mj receivers, exactly like the kinesis slice
        for (Direction direction : Direction.values()) {
            if (this.powerStored <= 0) {
                break;
            }
            BlockEntity neighbour = level.getBlockEntity(pos.relative(direction));
            if (neighbour instanceof EnergyMeterBlockEntity meter) {
                long accepted = meter.receiveEnergy(Math.min(POWER_RATE, this.powerStored), false);
                if (accepted > 0) {
                    this.powerStored -= accepted;
                    this.setChanged();
                }
            } else if (neighbour instanceof MjReceiver receiver) {
                long wanted = receiver.getPowerRequested();
                if (wanted <= 0) {
                    continue;
                }
                long offer = Math.min(POWER_RATE, Math.min(wanted, this.powerStored));
                long excess = receiver.receivePower(offer, false);
                long accepted = offer - excess;
                if (accepted > 0) {
                    this.powerStored -= accepted;
                    this.setChanged();
                }
            }
        }
    }

    /** Power output interface of the slice (same contract as {@code KinesisPipeBlockEntity#extractEnergy}). */
    public long extractPower(long max) {
        long extracted = Math.min(max, this.powerStored);
        if (extracted > 0) {
            this.powerStored -= extracted;
            this.setChanged();
        }
        return extracted;
    }

    // ---------------------------------------------------------------------
    // persistence + client sync
    // ---------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("bc_family", this.familyIndex);
        output.putInt("bc_colour", this.colour == null ? -1 : this.colour.ordinal());
        int plugBits = 0;
        for (int i = 0; i < this.plugs.length; i++) {
            plugBits |= (this.plugs[i] & 0x3) << (i * 2);
        }
        output.putInt("bc_plugs", plugBits);
        int connectionBits = 0;
        for (Direction direction : this.connections) {
            connectionBits |= 1 << direction.ordinal();
        }
        output.putInt("bc_connections", connectionBits);
        if (!this.fluid.isEmpty()) {
            output.store("bc_fluid", FluidStack.CODEC, this.fluid);
        }
        output.putLong("bc_power", this.powerStored);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.familyIndex = input.getIntOr("bc_family", -1);
        int colourOrdinal = input.getIntOr("bc_colour", -1);
        DyeColor[] colours = DyeColor.values();
        this.colour = colourOrdinal >= 0 && colourOrdinal < colours.length ? colours[colourOrdinal] : null;
        int plugBits = input.getIntOr("bc_plugs", 0);
        for (int i = 0; i < this.plugs.length; i++) {
            this.plugs[i] = (byte) ((plugBits >> (i * 2)) & 0x3);
        }
        this.connections.clear();
        int connectionBits = input.getIntOr("bc_connections", 0);
        for (Direction direction : Direction.values()) {
            if ((connectionBits & (1 << direction.ordinal())) != 0) {
                this.connections.add(direction);
            }
        }
        this.fluid = input.read("bc_fluid", FluidStack.CODEC).orElse(FluidStack.EMPTY);
        this.powerStored = input.getLongOr("bc_power", 0L);
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
    private void pushToClients() {
        this.setChanged();
        if (this.level instanceof ServerLevel serverLevel) {
            BlockState state = this.getBlockState();
            serverLevel.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }
}
