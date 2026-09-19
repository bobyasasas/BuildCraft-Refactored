/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.robotics.blockentity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import buildcraft.robotics.BcRoboticsBlockEntities;
import buildcraft.robotics.zone.BoxZone;

/**
 * M4.17 zone planner block entity: a minimal port of the legacy {@code TileZonePlanner} under the unchanged id
 * {@code buildcraftrobotics:zone_planner}. The machine's legacy job is defining robot work zones &mdash; 16 layers of
 * {@code ZonePlan} chunk bitmaps edited through the planner GUI (paintbrushes + map locations in, zones out). The v1
 * slice keeps the "define + persist + publish" spine and carries the zone as the M2.13 {@link BoxZone} stand-in: one
 * axis-aligned box defined by {@link #setZone} (single-radius form through {@link ZonePlannerLogic#around} or raw
 * corners), stored in the block entity NBT ({@code bc_zone} child) and readable through {@link #getZone()} &mdash; the
 * same zone type {@code EntityRobot#setWorkZone} consumes, so the robot-network linkage is a getter away once the v2
 * robot side lands.
 *
 * <p><b>v1 trims (all javadoc-tracked; the trim boundary of this slice):</b> the 16-layer {@code ZonePlan} bitmap and
 * its per-chunk granularity are reduced to one {@link BoxZone} (the M2.13-sanctioned zone stand-in); the whole
 * paintbrush/map-location processing pair (legacy {@code update()}: map location &rarr; layer at 200-tick pace, layer
 * &rarr; map location) is not carried, nor its six inventories, the {@code ContainerZonePlanner} GUI, the client
 * render-data sync ({@code NET_RENDER_DATA}/{@code NET_PLAN_CHANGE}) or the chunk-map network layer
 * ({@code ZonePlannerMapChunkKey}/{@code MessageZoneMap*} placeholders stay as they are). No ticker: the v1 planner has
 * no per-tick processing (legacy ticked only for the map-location progress bars).
 */
public class ZonePlannerBlockEntity extends BlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** The planned zone ({@code null} until defined; legacy started with 16 empty {@code ZonePlan} layers). */
    @Nullable
    private BoxZone zone;
    /** One-shot guard for the lazy restore announce (see {@link #getZone()}). */
    private boolean announced;

    public ZonePlannerBlockEntity(BlockPos pos, BlockState state) {
        super(BcRoboticsBlockEntities.ZONE_PLANNER.value(), pos, state);
    }

    /**
     * The planned zone, or {@code null} while undefined. The first read of a zone that came back from the save announces
     * it (the {@code [M417]} restore evidence); freshly defined machines never double-log because {@link #setZone}
     * arms the guard.
     */
    @Nullable
    public BoxZone getZone() {
        if (!this.announced && this.zone != null) {
            this.announced = true;
            LOGGER.info("[M417] zone planner at {}: zone restored from save: {} ({} cells)", this.worldPosition,
                this.zone, ZonePlannerLogic.volume(this.zone));
        }
        return this.zone;
    }

    /**
     * Defines (or redefines) the zone from raw corners (the corners are normalised by {@link BoxZone} exactly like the
     * legacy map-location loads, which could produce swapped bounds).
     */
    public void setZone(BlockPos cornerA, BlockPos cornerB) {
        this.zone = new BoxZone(cornerA, cornerB);
        this.announced = true;
        this.setChanged();
        LOGGER.info("[M417] zone planner at {}: zone defined {} ({} cells)", this.worldPosition, this.zone,
            ZonePlannerLogic.volume(this.zone));
    }

    /** The single-radius definition form (see {@link ZonePlannerLogic#around} for the clamping). */
    public void setZone(BlockPos center, int radius) {
        BoxZone planned = ZonePlannerLogic.around(center, radius);
        this.setZone(planned.min(), planned.max());
    }

    // ---------------------------------------------------------------------
    // Persistence
    // ---------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.zone != null) {
            ValueOutput stored = output.child("bc_zone");
            stored.putLong("min", this.zone.min().asLong());
            stored.putLong("max", this.zone.max().asLong());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("bc_zone").ifPresent(stored -> this.zone = new BoxZone(
            BlockPos.of(stored.getLongOr("min", this.worldPosition.asLong())),
            BlockPos.of(stored.getLongOr("max", this.worldPosition.asLong()))));
    }

    /** The evidence-rig read side for the {@code [M417]} zone log (server side only; the zone has no client sync). */
    public void logZoneState() {
        BoxZone current = this.getZone();
        if (current == null) {
            LOGGER.info("[M417] zone planner at {}: no zone defined", this.worldPosition);
        } else {
            boolean selfInside = ZonePlannerLogic.contains(current, this.worldPosition);
            LOGGER.info("[M417] zone planner at {}: zone {} ({} cells, planner inside: {})", this.worldPosition,
                current, ZonePlannerLogic.volume(current), selfInside);
        }
    }
}
