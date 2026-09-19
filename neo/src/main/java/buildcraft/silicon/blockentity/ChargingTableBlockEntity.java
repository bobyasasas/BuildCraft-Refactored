/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.silicon.blockentity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import buildcraft.lib.charge.BcChargeableItem;
import buildcraft.silicon.BcSiliconBlockEntities;

/**
 * M4.17 charging table block entity (legacy counterpart:
 * {@code buildcraft.silicon.tile.TileChargingTable}, id {@code buildcraftsilicon:charging_table} unchanged) &mdash;
 * the M4.16 PARTIAL slice (energy-in + visible buffer store only) completed with the real item charging:
 * <ul>
 * <li><b>Inventory</b> &mdash; legacy: a single slot ({@code ItemHandlerSimple("inv", 1)}) whose
 * {@code isItemValidForSlot} only accepts {@code IMjContainerItem} stacks. Slice: a one-slot
 * {@link ItemStacksResourceHandler} whose {@code isValid} only accepts {@link BcChargeableItem} items, exposed
 * through the 26.1.2 item capability on every face (insert and extract, the legacy {@code EnumAccess.BOTH}).</li>
 * <li><b>Target</b> &mdash; legacy {@code getTarget()}: the held item's {@code maxPowerStored - powerStored}, 0 with
 * nothing (chargeable) in the slot. The laser sees that need through the shared base rule
 * ({@code required = target - power}), so it feeds exactly while the item still has room. The M4.16
 * {@code BUFFER_CAPACITY} stand-in target is gone &mdash; with no chargeable item the table has no work and the base
 * drains the buffer (the legacy idle rule).</li>
 * <li><b>Pacing</b> &mdash; legacy {@code update()}: while the buffer holds energy and the item still needs some, the
 * whole buffer dumps into the item every tick ({@code power -= receivePower(stack, power, false)}), clamped by the
 * item's remaining room. Slice: the same per-tick dump through {@link BcChargeableItem#clampReceive}, run ahead of
 * the shared base economics (which then drain the empty buffer once the item is full).</li>
 * </ul>
 */
public class ChargingTableBlockEntity extends LaserTableBaseBlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** The single item slot (legacy {@code "inv"}). */
    public static final int SLOT = 0;

    /** The one-slot inventory (legacy {@code inv}; only chargeable items insert, see class javadoc). */
    private final ItemStacksResourceHandler inv = new ItemStacksResourceHandler(1) {
        @Override
        public boolean isValid(int index, ItemResource resource) {
            // the legacy isItemValidForSlot: only IMjContainerItem items, now only BcChargeableItem items
            return resource.getItem() instanceof BcChargeableItem;
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previous) {
            // no per-tick client sync here (charging mutates the slot every tick while it works); the base class's
            // SYNC_INTERVAL pass carries the state, this only keeps persistence honest
            ChargingTableBlockEntity.this.setChanged();
        }
    };

    public ChargingTableBlockEntity(BlockPos pos, BlockState state) {
        super(BcSiliconBlockEntities.CHARGING_TABLE.value(), pos, state);
    }

    public ItemStacksResourceHandler getInv() {
        return this.inv;
    }

    /** The capability view: every side reaches the slot (legacy {@code EnumPipePart.VALUES} routing). */
    public ResourceHandler<ItemResource> getItemHandler(@Nullable Direction side) {
        return this.inv;
    }

    /** The held stack as an item stack (one slot, so the resource carries the amount). */
    private ItemStack slotStack() {
        return this.inv.getResource(SLOT).toStack((int) this.inv.getAmountAsLong(SLOT));
    }

    @Override
    protected long currentTarget() {
        ItemStack stack = this.slotStack();
        if (!stack.isEmpty() && stack.getItem() instanceof BcChargeableItem chargeable) {
            return Math.max(0, chargeable.getMaxPowerStored(stack) - chargeable.getPowerStored(stack));
        }
        return 0;
    }

    @Override
    protected void craft(ServerLevel level) {
        // unreachable: the per-tick dump below keeps the buffer empty while an item needs charge, so the power never
        // reaches the target (the legacy dump-before-craft ordering); kept for the abstract contract
    }

    @Override
    protected void tickPower(ServerLevel level) {
        // the legacy TileChargingTable#update dump: with energy in the buffer and a chargeable item in the slot, the
        // whole buffer goes into the item every tick, clamped by the item's remaining room
        ItemStack stack = this.slotStack();
        if (this.power > 0 && !stack.isEmpty() && stack.getItem() instanceof BcChargeableItem chargeable) {
            long stored = chargeable.getPowerStored(stack);
            long max = chargeable.getMaxPowerStored(stack);
            long accepted = BcChargeableItem.clampReceive(stored, max, this.power);
            if (accepted > 0) {
                chargeable.receivePower(stack, this.power, false);
                this.power -= accepted;
                this.inv.set(SLOT, ItemResource.of(stack), (int) this.inv.getAmountAsLong(SLOT));
                if (stored < max && stored + accepted >= max) {
                    LOGGER.info("[M417] charging table @ {}: {} fully charged to {}µMJ (custom_data={})",
                        this.worldPosition, stack.getItem(), max, customDataOf(stack));
                }
            }
        }
        super.tickPower(level);
    }

    /** Per-tick driver wired through {@code ChargingTableBlock#getTicker}. */
    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, ChargingTableBlockEntity table) {
        table.tickPower(level);
    }

    /** The slot stack's {@code minecraft:custom_data} tag for the {@code [M417]} evidence logs (read-only). */
    private static String customDataOf(ItemStack stack) {
        var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return data == null ? "{}" : data.copyTag().toString();
    }

    // ---------------------------------------------------------------------
    // Persistence
    // ---------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.inv.serialize(output.child("bc_inv"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.inv.deserialize(input.childOrEmpty("bc_inv"));
    }
}
