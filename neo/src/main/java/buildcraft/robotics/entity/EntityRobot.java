/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.robotics.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import buildcraft.robotics.zone.BoxZone;

/**
 * Minimal robot entity for the M2.13 slice of buildcraftrobotics (replaces the M2.4c {@link PlaceholderEntity} under
 * the unchanged id {@code buildcraftrobotics:robot_miner} &mdash; zero new registry ids). Deliberately reduced
 * stand-in for the legacy {@code buildcraft.robotics.entity.EntityRobot} (1,562 lines) and its AI tree:
 *
 * <p><b>Legacy audit (frozen 1.20.1 tree) and how the slice maps to it:</b>
 * <ul>
 * <li><b>Entity model</b> &mdash; legacy: {@code EntityRobot extends EntityRobotBase extends CreatureEntity}, a
 * flying {@code MobCategory.MISC} entity ({@code setNoGravity(true)}, {@code noPhysics = true},
 * {@code sized(0.5F, 0.5F)}, fire immune) whose 17 per-board entity types share one class parameterised by a
 * {@code RedstoneBoardRobotNBT}. Slice: a plain {@link Entity} with the same size/category/immunities (the M2.4c
 * placeholders established that a plain MISC entity needs no attribute registration); health/hurt semantics stay
 * with the placeholder's invulnerable stance. Only the {@code robot_miner} id carries this class &mdash; the other
 * 16 board ids stay placeholders until the full boards migration (M2.5+).</li>
 * <li><b>Task execution path</b> &mdash; legacy: {@code EntityRobot.tick} runs {@code AIRobotMain.cycle()}, which
 * delegates to the board ({@code BoardRobotMiner} &rarr; {@code BoardRobotGenericBreakBlock}), which spawns the AI
 * tree {@code AIRobotSearchAndGotoBlock} (zone scan via {@code BlockScannerZoneRandom}) &rarr;
 * {@code AIRobotStraightMoveTo} (flight) &rarr; {@code AIRobotBreak} (per-tick break damage + crack overlay)
 * &rarr; {@code AIRobotGotoSleep}. Each running AI charges {@code battery.extractPower(1, getPowerCost())} per
 * tick from {@code AIRobot#cycle}. Slice: the tree collapses to the explicit state machine
 * {@link RobotTaskState} (IDLE/SEARCH/MOVE/BREAK/DONE) in {@link #tickTask} &mdash; every state documents its
 * legacy AI counterpart, each charging the same per-tick AI cost on the M2.2 &times;10&#8315;&#8304; slice scale.</li>
 * <li><b>Movement model</b> &mdash; legacy: straight-line flight. {@code AIRobotGoto.setDestination} sets
 * {@code deltaMovement = normalized(dir) / 10} (a 0.1 blocks/tick flight speed, gravity/friction disabled) and
 * {@code AIRobotStraightMoveTo.update} terminates once the distance stops shrinking. The generic
 * {@code AIRobotGotoBlock} adds A* pathfinding (96 range) for obstacle avoidance; robots flying with
 * {@code noPhysics = true} make the straight leg the essence, so the slice flies the straight leg only
 * (speed 0.1/tick, snapping into the hover point on arrival &mdash; the same "close enough &rarr; arrive"
 * semantics without the decreasing-distance heuristic). Pathfinding does not migrate with the slice.</li>
 * <li><b>Zone ("作业区域")</b> &mdash; legacy: {@code IZone#getZoneToWork} fed by the zone planner bitmap
 * ({@code ZonePlan}/{@code ZoneChunk}, edited in the {@code TileZonePlanner} GUI, attached through gate statements).
 * Slice: a {@link BoxZone} carried directly on the robot ({@link #setWorkZone}, persisted); the scan inside the
 * zone is a fixed x/z/y order instead of legacy's random {@code BlockScannerZoneRandom} (determinism for tests;
 * same trade-off as the M2.12 quarry scan).</li>
 * <li><b>Board = target filter</b> &mdash; legacy: {@code BoardRobotMiner#isExpectedBlock} =
 * {@code state.is(Tags.Blocks.ORES) && TierSortingRegistry.isCorrectTierForDrops}. Slice:
 * {@link #isExpectedBlock} = the vanilla {@code #minecraft:iron_ores} tag (26.1.2 no longer ships the generic
 * {@code minecraft:ores} block tag, so the slice pins the one ore family its tests exercise; full ore coverage
 * migrates with the boards). The legacy tool-fetch leg ({@code AIRobotFetchAndEquipItemStack} equipping a pickaxe)
 * is replaced by the constant {@link #EQUIPPED_TOOL} diamond pickaxe used for break speed and drops &mdash; the
 * M2.12 quarry slice established the same "legacy tool parity = diamond pickaxe" stand-in.</li>
 * <li><b>Breaking</b> &mdash; legacy: {@code AIRobotBreak} accumulates
 * {@code blockDamage += toolSpeed / hardness / 30} per tick, drives {@code destroyBlockProgress} crack stages and
 * harvests through {@code BlockUtil.harvestBlock} (drops fall to the ground; a picker flow collects them). Slice:
 * the identical damage formula and crack overlay; drops go straight into the robot's carried list
 * ({@link #getCarried} &mdash; the slice stand-in for legacy's drop-to-ground + collection flow).</li>
 * <li><b>Power</b> &mdash; legacy: 5,000 MJ battery ({@code EntityRobotBase.MAX_POWER}) recharged at docking
 * stations ({@code DockingStation} on robot-station pipe plugs, {@code AIRobotRecharge}, per-world
 * {@code IRobotRegistry}). The neo placeholder id pool has <b>no station id</b> (robotics blocks are
 * {@code requester}/{@code zone_planner}; legacy stations are transport pipe plugs), so the slice takes the
 * entity-direct-power closed loop instead of activating a station: {@link #getPowerRequested()}/{@link
 * #receivePower(long, boolean)} mirror the legacy {@code IMjReceiver} semantics already established by
 * {@code buildcraft.core.blockentity.MjReceiver} (request {@code capacity - stored}, return the excess), callable
 * programmatically until a real charging anchor migrates. Battery capacity 500,000 &micro;MJ = legacy
 * 5,000 MJ &times;10&#8315;&#8304;; the battery-empty stall (state IDLE) mirrors legacy
 * {@code AIRobotShutdown} at {@code SHUTDOWN_POWER = 0}.</li>
 * <li><b>Per-tick AI costs</b> (legacy {@code AIRobot#getPowerCost}, &times;10&#8315;&#8304;):
 * search {@code AIRobotSearchBlock} 0.2 MJ/t &rarr; {@link #COST_SEARCH} = 20 &micro;MJ/t; move
 * {@code AIRobotGoto} 0.3 MJ/t &rarr; {@link #COST_MOVE} = 30 &micro;MJ/t; break {@code AIRobotBreak}
 * {@code ceil(16 MJ * 2 / 30)} &asymp; 1.067 MJ/t &rarr; {@link #COST_BREAK} = 107 &micro;MJ/t. Legacy charges
 * {@code extractPower(1, cost)} (partial drain allowed); the slice drains {@code min(cost, stored)}.</li>
 * </ul>
 *
 * <p><b>Client sync</b> uses {@link EntityDataAccessor}s (the vanilla entity channel): the task state byte drives
 * the renderer's body tint and the optional target position marks the robot as busy; the battery/carry/zone stay
 * server-only (the slice renderer does not display them). Persistence goes through the standard entity NBT
 * ({@code bc_*} keys, same prefix convention as the M2.12 BE slices), which also makes
 * {@code /summon buildcraftrobotics:robot_miner {bc_...}} configure a robot in one command (m213 evidence rig).
 */
public class EntityRobot extends Entity {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Battery capacity in &micro;MJ (slice value: legacy {@code EntityRobotBase.MAX_POWER} = 5,000 MJ &times;10&#8315;&#8304;). */
    public static final long BATTERY_CAPACITY = 500_000;
    /** Legacy {@code AIRobotSearchBlock#getPowerCost} (0.2 MJ/t) on the slice scale, in &micro;MJ/t. */
    public static final long COST_SEARCH = 20;
    /** Legacy {@code AIRobotGoto#getPowerCost} (0.3 MJ/t) on the slice scale, in &micro;MJ/t. */
    public static final long COST_MOVE = 30;
    /** Legacy {@code AIRobotBreak#getPowerCost} ({@code ceil(16 MJ * 2 / 30)}) on the slice scale, in &micro;MJ/t. */
    public static final long COST_BREAK = 107;
    /** Legacy flight speed: {@code AIRobotGoto#setDestination} normalized direction / 10, blocks per tick. */
    public static final double MOVE_SPEED = 0.1;
    /** Distance from the target block centre the robot parks at while breaking (keeps it outside the block). */
    private static final double HOVER_DISTANCE = 0.8;
    /**
     * The tool the slice robot behaves as if it had equipped (legacy {@code AIRobotFetchAndEquipItemStack} fetches a
     * real pickaxe from the station inventory; the M2.12 slice tool-parity precedent stands in for it).
     */
    private static final ItemStack EQUIPPED_TOOL = new ItemStack(Items.DIAMOND_PICKAXE);

    private static final EntityDataAccessor<Byte> DATA_TASK_STATE = SynchedEntityData
            .defineId(EntityRobot.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Optional<BlockPos>> DATA_TARGET = SynchedEntityData
            .defineId(EntityRobot.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);

    /**
     * The collapsed slice form of the legacy AI tree. {@code SEARCH} = {@code AIRobotSearchBlock},
     * {@code MOVE} = {@code AIRobotStraightMoveTo}, {@code BREAK} = {@code AIRobotBreak}, {@code IDLE} =
     * {@code AIRobotShutdown}/{@code AIRobotGotoSleep} (no power / nothing to do), {@code DONE} = the slice
     * "zone exhausted" affordance (legacy robots sleep at their dock forever; no dock exists in the slice).
     */
    public enum RobotTaskState {
        IDLE,
        SEARCH,
        MOVE,
        BREAK,
        DONE
    }

    /** Battery charge in &micro;MJ (server side; see {@link #receivePower}). */
    private long energyStored;
    /** The work area ("作业区域") this robot mines inside; null = no task (legacy: {@code getZoneToWork()}). */
    @Nullable
    private BoxZone workZone;
    /** Scan cursor inside {@link #workZone} (absolute coords); null = start of the scan. */
    @Nullable
    private BlockPos scanCursor;
    /** The block currently being flown to / broken (absolute coords); null between tasks. */
    @Nullable
    private BlockPos target;
    /** Break progress on {@link #target}, 0..1 (legacy {@code AIRobotBreak#blockDamage}). */
    private float breakProgress;
    /** Mined drops waiting for delivery (slice stand-in for legacy's ground drops + collection; see class javadoc). */
    private final List<ItemStack> carried = new ArrayList<>();

    public EntityRobot(EntityType<? extends EntityRobot> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TASK_STATE, (byte) RobotTaskState.IDLE.ordinal());
        builder.define(DATA_TARGET, Optional.empty());
    }

    // ---------------------------------------------------------------------
    // Slice setup API (gametests / m213 evidence rig; legacy gets these from the
    // zone planner + docking stations)
    // ---------------------------------------------------------------------

    /** Sets the work area (legacy: zone planner + {@code ActionRobotWorkInArea}) and re-arms the task. */
    public void setWorkZone(@Nullable BoxZone zone) {
        if (this.level().isClientSide()) {
            return;
        }
        this.workZone = zone;
        this.scanCursor = null;
        this.target = null;
        this.breakProgress = 0;
        this.syncTarget();
        this.setTaskState(zone != null && this.energyStored > 0 ? RobotTaskState.SEARCH : RobotTaskState.IDLE);
    }

    @Nullable
    public BoxZone getWorkZone() {
        return this.workZone;
    }

    public long getEnergyStored() {
        return this.energyStored;
    }

    /** The mined-drops buffer (legacy: drops fall to the ground; the slice parks them here). Read-only view. */
    public List<ItemStack> getCarried() {
        return List.copyOf(this.carried);
    }

    /** The live task state (server side truth). */
    public RobotTaskState getTaskState() {
        return RobotTaskState.values()[this.entityData.get(DATA_TASK_STATE)];
    }

    /** The block currently being worked (client-visible via the synced data; null while idle/done). */
    @Nullable
    public BlockPos getTarget() {
        return this.entityData.get(DATA_TARGET).orElse(null);
    }

    // ---------------------------------------------------------------------
    // Entity-direct power (the station-less minimal closed loop, see class javadoc)
    // ---------------------------------------------------------------------

    /** Legacy {@code IMjReceiver#getPowerRequested}: {@code capacity - stored}; 0 when full. */
    public long getPowerRequested() {
        return BATTERY_CAPACITY - this.energyStored;
    }

    /**
     * Legacy {@code IMjReceiver#receivePower}: accepts up to the requested amount into the battery (no-op with
     * {@code simulate}) and returns the excess. Recharging wakes an idle robot back into {@code SEARCH}.
     */
    public long receivePower(long microJoules, boolean simulate) {
        long accepted = Math.min(microJoules, BATTERY_CAPACITY - this.energyStored);
        if (!simulate && accepted > 0) {
            this.energyStored += accepted;
            if (this.getTaskState() == RobotTaskState.IDLE && this.workZone != null) {
                this.setTaskState(RobotTaskState.SEARCH);
            }
        }
        return microJoules - accepted;
    }

    // ---------------------------------------------------------------------
    // Server tick: the collapsed AI tree
    // ---------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        this.tickTask((ServerLevel) this.level());
    }

    /** One AI cycle per tick (legacy {@code AIRobotMain.cycle} + the active delegate AI's {@code update}). */
    private void tickTask(ServerLevel level) {
        RobotTaskState state = this.getTaskState();
        if (state == RobotTaskState.DONE) {
            return; // zone exhausted: parks forever (legacy: sleeps at the dock)
        }
        if (this.energyStored <= 0) {
            // legacy AIRobotShutdown at SHUTDOWN_POWER = 0; a recharge via receivePower resumes the task
            this.setTaskState(RobotTaskState.IDLE);
            return;
        }
        switch (state) {
            case SEARCH -> this.tickSearch(level);
            case MOVE -> this.tickMove();
            case BREAK -> this.tickBreak(level);
            default -> this.setTaskState(this.workZone != null ? RobotTaskState.SEARCH : RobotTaskState.IDLE);
        }
    }

    /** Legacy {@code AIRobotSearchBlock}: find the next minable ore cell inside the zone, charging the search AI cost. */
    private void tickSearch(ServerLevel level) {
        this.energyStored -= Math.min(COST_SEARCH, this.energyStored);
        if (this.workZone == null) {
            this.setTaskState(RobotTaskState.IDLE);
            return;
        }
        BlockPos cursor = this.scanCursor;
        while (true) {
            BlockPos next = this.workZone.advanceScan(cursor);
            if (next == null) {
                // legacy: AIRobotGotoSleep when the search finds nothing; the slice latches DONE instead
                this.setTaskState(RobotTaskState.DONE);
                return;
            }
            cursor = next;
            this.scanCursor = next;
            if (this.isExpectedBlock(level.getBlockState(next)) && canMine(level, next)) {
                this.target = next.immutable();
                this.breakProgress = 0;
                this.syncTarget();
                this.setTaskState(RobotTaskState.MOVE);
                return;
            }
        }
    }

    /** Legacy {@code AIRobotStraightMoveTo}: fly the straight leg to the hover point in front of the target. */
    private void tickMove() {
        this.energyStored -= Math.min(COST_MOVE, this.energyStored);
        BlockPos target = this.target;
        if (target == null) {
            this.setTaskState(RobotTaskState.SEARCH);
            return;
        }
        Vec3 centre = Vec3.atCenterOf(target);
        Vec3 position = this.position();
        Vec3 away = position.subtract(centre);
        // park HOVER_DISTANCE from the centre, on the line we came in on (never inside the block)
        Vec3 hover = away.lengthSqr() < 1.0E-4 ? centre.add(0, HOVER_DISTANCE, 0)
                : centre.add(away.normalize().scale(HOVER_DISTANCE));
        Vec3 toHover = hover.subtract(position);
        double distance = toHover.length();
        if (distance <= MOVE_SPEED) {
            // legacy AIRobotStraightMoveTo terminates on arrival and snaps to the destination
            this.setPos(hover.x, hover.y, hover.z);
            this.setDeltaMovement(Vec3.ZERO);
            this.setTaskState(RobotTaskState.BREAK);
        } else {
            Vec3 step = toHover.normalize().scale(MOVE_SPEED);
            this.setPos(position.x + step.x, position.y + step.y, position.z + step.z);
        }
    }

    /** Legacy {@code AIRobotBreak}: per-tick break damage + crack overlay, paid with the break AI cost. */
    private void tickBreak(ServerLevel level) {
        this.energyStored -= Math.min(COST_BREAK, this.energyStored);
        BlockPos target = this.target;
        if (target == null) {
            this.setTaskState(RobotTaskState.SEARCH);
            return;
        }
        BlockState state = level.getBlockState(target);
        if (!this.isExpectedBlock(state) || !canMine(level, target)) {
            // vanished / no longer a target mid-task: release it and search again (legacy releases the reservation)
            this.releaseTarget();
            this.setTaskState(RobotTaskState.SEARCH);
            return;
        }
        float hardness = state.getDestroySpeed(level, target);
        float speed = EQUIPPED_TOOL.getItem().getDestroySpeed(EQUIPPED_TOOL, state);
        if (hardness == 0.0F) {
            this.breakProgress = 1.1F; // legacy: instant break
        } else {
            this.breakProgress += speed / hardness / 30.0F;
        }
        if (this.breakProgress > 1.0F) {
            level.destroyBlockProgress(this.getId(), target, -1);
            this.breakBlock(level, target, state);
            this.releaseTarget();
            this.setTaskState(RobotTaskState.SEARCH);
        } else {
            level.destroyBlockProgress(this.getId(), target, (int) (this.breakProgress * 10.0F) - 1);
        }
    }

    /**
     * Breaks the target and carries its drops (legacy {@code AIRobotBreak} harvest with the equipped pickaxe; the
     * slice parks the drops in {@link #carried} instead of dropping them to the ground).
     */
    private void breakBlock(ServerLevel level, BlockPos pos, BlockState state) {
        List<ItemStack> drops = Block.getDrops(state, level, pos, null, null, EQUIPPED_TOOL);
        level.removeBlock(pos, false);
        // the break particles/sound half of vanilla destroyBlock, without its automatic drops
        level.levelEvent(2001, pos, Block.getId(state));
        for (ItemStack drop : drops) {
            if (!drop.isEmpty()) {
                this.carried.add(drop);
            }
        }
    }

    /** Clears the target (and its synced copy). */
    private void releaseTarget() {
        this.target = null;
        this.breakProgress = 0;
        this.syncTarget();
    }

    /** Sets the synced task state (the client tint driver) and logs the transition (task-path evidence;
     * a few lines per mined cell, see {@link #tickTask}). */
    private void setTaskState(RobotTaskState state) {
        RobotTaskState from = this.getTaskState();
        this.entityData.set(DATA_TASK_STATE, (byte) state.ordinal());
        if (state != from) {
            LOGGER.info("[bc robot #{}] task {} -> {}{}", this.getId(), from, state,
                    state == RobotTaskState.MOVE && this.target != null
                            ? " target=" + this.target.toShortString() : "");
        }
    }

    private void syncTarget() {
        this.entityData.set(DATA_TARGET, Optional.ofNullable(this.target));
    }

    /** Legacy {@code BoardRobotMiner#isExpectedBlock}: the miner board targets ores (see class javadoc for the tag narrowing). */
    public boolean isExpectedBlock(BlockState state) {
        return state.is(BlockTags.IRON_ORES);
    }

    /** Legacy mining guards (quarry slice parity): solid, breakable, non-fluid cells only. */
    private static boolean canMine(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.liquid()) {
            return false;
        }
        return state.getDestroySpeed(level, pos) >= 0;
    }

    // ---------------------------------------------------------------------
    // Other entity basics
    // ---------------------------------------------------------------------

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // the placeholder's invulnerable stance (see PlaceholderEntity); legacy robot health/damage migrates later
        return false;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    // ---------------------------------------------------------------------
    // Persistence (standard entity NBT; also the /summon configuration surface)
    // ---------------------------------------------------------------------

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.energyStored = input.getLongOr("bc_energy_stored", 0L);
        this.breakProgress = input.getFloatOr("bc_break_progress", 0.0F);
        byte state = input.getByteOr("bc_task_state", (byte) RobotTaskState.IDLE.ordinal());
        RobotTaskState[] states = RobotTaskState.values();
        this.entityData.set(DATA_TASK_STATE,
                (byte) states[Math.min(Math.max(state, 0), states.length - 1)].ordinal());
        if (input.getIntOr("bc_min_x", Integer.MIN_VALUE) != Integer.MIN_VALUE
                && input.getIntOr("bc_max_x", Integer.MAX_VALUE) != Integer.MAX_VALUE) {
            this.workZone = new BoxZone(
                    new BlockPos(input.getIntOr("bc_min_x", 0), input.getIntOr("bc_min_y", 0),
                            input.getIntOr("bc_min_z", 0)),
                    new BlockPos(input.getIntOr("bc_max_x", 0), input.getIntOr("bc_max_y", 0),
                            input.getIntOr("bc_max_z", 0)));
        }
        if (input.getIntOr("bc_scan_x", Integer.MIN_VALUE) != Integer.MIN_VALUE) {
            this.scanCursor = new BlockPos(input.getIntOr("bc_scan_x", 0), input.getIntOr("bc_scan_y", 0),
                    input.getIntOr("bc_scan_z", 0));
        }
        if (input.getIntOr("bc_target_x", Integer.MIN_VALUE) != Integer.MIN_VALUE) {
            this.target = new BlockPos(input.getIntOr("bc_target_x", 0), input.getIntOr("bc_target_y", 0),
                    input.getIntOr("bc_target_z", 0));
        }
        this.syncTarget();
        this.carried.clear();
        input.read("bc_carry", ItemStack.CODEC.listOf()).ifPresent(this.carried::addAll);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putLong("bc_energy_stored", this.energyStored);
        output.putFloat("bc_break_progress", this.breakProgress);
        output.putByte("bc_task_state", (byte) this.getTaskState().ordinal());
        if (this.workZone != null) {
            BlockPos min = this.workZone.min();
            BlockPos max = this.workZone.max();
            output.putInt("bc_min_x", min.getX());
            output.putInt("bc_min_y", min.getY());
            output.putInt("bc_min_z", min.getZ());
            output.putInt("bc_max_x", max.getX());
            output.putInt("bc_max_y", max.getY());
            output.putInt("bc_max_z", max.getZ());
        }
        if (this.scanCursor != null) {
            output.putInt("bc_scan_x", this.scanCursor.getX());
            output.putInt("bc_scan_y", this.scanCursor.getY());
            output.putInt("bc_scan_z", this.scanCursor.getZ());
        }
        if (this.target != null) {
            output.putInt("bc_target_x", this.target.getX());
            output.putInt("bc_target_y", this.target.getY());
            output.putInt("bc_target_z", this.target.getZ());
        }
        if (!this.carried.isEmpty()) {
            output.store("bc_carry", ItemStack.CODEC.listOf(), this.carried);
        }
    }
}
