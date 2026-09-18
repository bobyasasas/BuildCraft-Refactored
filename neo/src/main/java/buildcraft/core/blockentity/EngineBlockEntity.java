/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.core.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import buildcraft.core.block.EngineBlock;

/**
 * M4.4 engine-family block entity (wood / iron / rf / creative / mj_dynamo): the render-facing slice of the legacy
 * {@code TileEngineBase_BC8} — enough simulation to drive the jsonbc BER (piston progress, power stage, burning
 * flag), not the full MJ network/heat/explosion behaviour (that migrates with the engine module, M2.4/M2.9).
 *
 * <p>Slice model (mirrors {@link StoneEngineBlockEntity} where the legacy behaviour allowed it):
 * <ul>
 * <li>Non-creative engines hold one fuel item ({@link #insertFuel(ItemStack, FuelValues)}, reachable by right-clicking
 * the block with a fuel item, see {@link EngineBlock#useItemOn}); while burning they fill the buffer by
 * {@link #POWER_PER_TICK} per tick and are "pumping". The legacy wood engine burned solid fuel exactly like this; the
 * iron/rf/dynamo fuel/fluid sources are slice stand-ins.</li>
 * <li>The creative engine never burns anything: it pumps while redstone-powered and always keeps a full buffer
 * (legacy {@code TileEngineCreative}: {@code isBurning() == isRedstonePowered}).</li>
 * <li>The power stage is the legacy {@code computePowerStage} threshold table applied to the buffer fill ratio
 * (the legacy heat level tracked the power level one to one, see {@code updateHeatLevel}); the creative engine is
 * always {@link EnumPowerStage#BLACK}.</li>
 * </ul>
 *
 * <p><b>Client sync</b> is the {@link StoneEngineBlockEntity} pattern: {@link #getUpdateTag} = {@code saveCustomOnly}
 * and {@link #serverTick} calls {@code sendBlockUpdated} on ignition/burn-out/every {@link #SYNC_INTERVAL} ticks and
 * whenever the pumping flag or power stage changes. The client derives the visible state from the synced fields and
 * advances the piston locally (see {@link #clientTick}), exactly like the legacy client block of
 * {@code TileEngineBase_BC8#update()}.
 */
public class EngineBlockEntity extends BlockEntity implements EngineVisual {

    /** Constant power output while burning, in &micro;MJ per tick (slice value, matching {@code StoneEngineBlockEntity}). */
    public static final long POWER_PER_TICK = 100;
    /** Internal energy buffer size in &micro;MJ (slice value, matching {@code StoneEngineBlockEntity}). */
    public static final long CAPACITY = 100_000;
    /** While pumping, the render state is re-synced to clients every this many ticks. */
    public static final int SYNC_INTERVAL = 40;
    /** The fixed creative-engine piston speed (legacy interpolated 0.01..0.08 by output index; no output GUI here). */
    public static final double CREATIVE_PISTON_SPEED = 0.04;
    /** How fast the piston retracts when the engine stops pumping (legacy {@code TileEngineBase_BC8#update()}). */
    public static final float RETRACT_SPEED = 0.01f;

    private final boolean creative;
    private int burnRemain;
    private int burnTotal;
    private long energyStored;
    private ItemStack fuel = ItemStack.EMPTY;
    /** Server truth (and, for the creative engine, derived from redstone on the client too). */
    private boolean pumping;
    /** Client-side piston animation, advanced by {@link #clientTick} (legacy {@code progress}/{@code lastProgress}). */
    private float progress, lastProgress;
    private EnumPowerStage powerStage = EnumPowerStage.BLUE;

    public EngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean creative) {
        super(type, pos, state);
        this.creative = creative;
    }

    /** True for the creative engine (redstone-driven, never burns, always {@link EnumPowerStage#BLACK}). */
    public boolean isCreative() {
        return this.creative;
    }

    // ---------------------------------------------------------------------
    // Fuel (non-creative engines)
    // ---------------------------------------------------------------------

    /**
     * Inserts one item of {@code stack} into the fuel slot, resolving its burn length through the vanilla fuel table.
     * Returns false if the stack is empty, has no fuel value, or the slot already holds a different item; the caller
     * owns stack mutation. Same contract as {@code StoneEngineBlockEntity#insertFuel}.
     */
    public boolean insertFuel(ItemStack stack, FuelValues fuelValues) {
        if (this.creative || stack.isEmpty() || fuelValues == null) {
            return false;
        }
        if (stack.getBurnTime(RecipeType.SMELTING, fuelValues) <= 0) {
            return false;
        }
        if (this.fuel.isEmpty()) {
            this.fuel = stack.copyWithCount(1);
            this.setChanged();
            return true;
        }
        if (ItemStack.isSameItemSameComponents(this.fuel, stack) && this.fuel.getCount() < this.fuel.getMaxStackSize()) {
            this.fuel.grow(1);
            this.setChanged();
            return true;
        }
        return false;
    }

    public ItemStack getFuel() {
        return this.fuel;
    }

    // ---------------------------------------------------------------------
    // Render state (the BER reads these through the synced fields)
    // ---------------------------------------------------------------------

    /** Legacy {@code isPumping}: the piston is currently cycling (burning for real engines, redstone for creative). */
    public boolean isPumping() {
        return this.pumping;
    }

    /** True while this engine is visibly "running" (burning fuel, or redstone-powered for the creative engine). */
    public boolean isBurning() {
        return this.creative ? this.pumping : this.burnRemain > 0;
    }

    public long getEnergyStored() {
        return this.energyStored;
    }

    /** The buffer fill ratio, 0..1 (the legacy {@code getPowerLevel}). */
    public double getPowerLevel() {
        return this.energyStored / (double) CAPACITY;
    }

    /** The current power stage (server: computed; client: derived from the synced buffer in {@link #clientTick}). */
    public EnumPowerStage getPowerStage() {
        return this.powerStage;
    }

    public int getBurnRemain() {
        return this.burnRemain;
    }

    public int getBurnTotal() {
        return this.burnTotal;
    }

    /** Legacy {@code getPistonSpeed}: progress gained per tick for the current stage. */
    public double getPistonSpeed() {
        if (this.creative) {
            return CREATIVE_PISTON_SPEED;
        }
        return this.powerStage.getPistonSpeed();
    }

    /** Legacy {@code getProgressClient}: the piston position interpolated between the last two client ticks, handling
     * the 1&rarr;0 wrap so the stroke stays continuous (the math lives on {@link EngineVisual}). */
    public float getProgressClient(float partialTicks) {
        return EngineVisual.interpolateClientProgress(this.lastProgress, this.progress, partialTicks);
    }

    /** The output face = the block state's facing (the {@code EngineBlock} pattern). */
    @Override
    public net.minecraft.core.Direction getOutputFacing() {
        return this.getBlockState().getValue(EngineBlock.FACING);
    }

    // ---------------------------------------------------------------------
    // Ticking
    // ---------------------------------------------------------------------

    /** Server tick (wired through {@link EngineBlock#getTicker}): the slice burn/creative logic plus render sync. */
    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, EngineBlockEntity engine) {
        boolean changed = false;
        boolean syncToClients = false;

        if (engine.creative) {
            boolean powered = level.hasNeighborSignal(pos);
            long newStored = powered ? CAPACITY : 0;
            if (powered != engine.pumping || newStored != engine.energyStored) {
                engine.pumping = powered;
                engine.energyStored = newStored;
                changed = true;
                syncToClients = true;
            }
        } else {
            if (engine.burnRemain > 0) {
                engine.burnRemain--;
                if (engine.energyStored < CAPACITY) {
                    engine.energyStored = Math.min(CAPACITY, engine.energyStored + POWER_PER_TICK);
                }
                changed = true;
                // Re-sync periodically while pumping so clients keep animating even after a missed update
                syncToClients = engine.burnRemain % SYNC_INTERVAL == 0;
                if (engine.burnRemain == 0) {
                    // burn-out: the client must see pumping = false
                    syncToClients = true;
                }
            } else if (engine.energyStored < CAPACITY && !engine.fuel.isEmpty()) {
                // ignite the next fuel item (insertFuel guarantees it actually has a burn value)
                int burnTicks = engine.fuel.getBurnTime(RecipeType.SMELTING, level.fuelValues());
                if (burnTicks > 0) {
                    engine.fuel.shrink(1);
                    if (engine.fuel.isEmpty()) {
                        engine.fuel = ItemStack.EMPTY;
                    }
                    engine.burnTotal = burnTicks;
                    engine.burnRemain = burnTicks;
                    changed = true;
                    // ignition: the client must see pumping = true
                    syncToClients = true;
                }
            }
            boolean pumping = engine.burnRemain > 0;
            if (pumping != engine.pumping) {
                engine.pumping = pumping;
                syncToClients = true;
            }
        }

        EnumPowerStage stage = engine.computePowerStage();
        if (stage != engine.powerStage) {
            engine.powerStage = stage;
            // the trunk texture changes with the stage (legacy sent NET_RENDER_DATA for this)
            syncToClients = true;
        }
        if (changed) {
            engine.setChanged();
        }
        if (syncToClients) {
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /** Client tick (wired through {@link EngineBlock#getTicker}): derives the visible state from the synced fields and
     * advances the piston locally, exactly like the legacy client block of {@code TileEngineBase_BC8#update()}. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, EngineBlockEntity engine) {
        engine.lastProgress = engine.progress;
        engine.pumping = engine.isBurning();
        engine.progress = EngineVisual.advanceClientProgress(//
            engine.progress, engine.pumping, engine.getPistonSpeed(), RETRACT_SPEED);

        if (engine.pumping && engine.creative) {
            spawnCreativeParticles(level, pos);
        }

        engine.powerStage = engine.computePowerStage();
    }

    private static void spawnCreativeParticles(Level level, BlockPos pos) {
        // A light visual cue that the creative engine is running (the legacy one spat flame particles from its
        // chimney through ParticleUtil / smoke; a single flame keeps the slice honest).
        if (level.getRandom().nextFloat() < 0.25f) {
            double x = pos.getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.3;
            double y = pos.getY() + 0.55;
            double z = pos.getZ() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.3;
            level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0.02, 0);
        }
    }

    /** The legacy {@code computePowerStage} (thresholds on the buffer fill ratio; creative is always BLACK). */
    private EnumPowerStage computePowerStage() {
        if (this.creative) {
            return EnumPowerStage.BLACK;
        }
        return EnumPowerStage.fromLevel(this.getPowerLevel());
    }

    // ---------------------------------------------------------------------
    // Persistence + client sync (Beacon pattern, see the class javadoc)
    // ---------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("bc_burn_remain", this.burnRemain);
        output.putInt("bc_burn_total", this.burnTotal);
        output.putLong("bc_energy_stored", this.energyStored);
        output.putBoolean("bc_pumping", this.pumping);
        output.putFloat("bc_progress", this.progress);
        if (!this.fuel.isEmpty()) {
            output.store("bc_fuel", ItemStack.CODEC, this.fuel);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.burnRemain = input.getIntOr("bc_burn_remain", 0);
        this.burnTotal = input.getIntOr("bc_burn_total", 0);
        this.energyStored = input.getLongOr("bc_energy_stored", 0L);
        this.pumping = input.getBooleanOr("bc_pumping", false);
        this.progress = input.getFloatOr("bc_progress", 0.0f);
        this.lastProgress = this.progress;
        this.fuel = input.read("bc_fuel", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    public @Nullable ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
