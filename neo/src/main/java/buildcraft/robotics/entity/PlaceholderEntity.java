/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.robotics.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.syncher.SynchedEntityData;

/**
 * Shared placeholder entity for the M2.4c registry parity (task M2.4c): carries no behaviour, it only makes every
 * baseline {@code buildcraftrobotics} robot entity id (see
 * {@code migration/snapshots/registry-baseline.json}) a registered {@link EntityType}. The registrations using this
 * class live in {@code BcRoboticsEntities} and are replaced by the real {@code EntityRobot} behaviour in M2.5+.
 *
 * <p>Deliberately a plain {@link Entity} (not a {@code LivingEntity}): the 26.1.2 attribute system
 * ({@code EntityAttributeCreationEvent}) only binds attributes to living entity types, so a plain placeholder needs no
 * attribute registration (vanilla precedent: {@code AreaEffectCloud}, {@code Arrow} register as plain entities in
 * {@code MobCategory.MISC} without attributes). Renderers are likewise only resolved when an entity is actually
 * spawned and rendered, which never happens for these placeholders.
 */
public class PlaceholderEntity extends Entity {

    public PlaceholderEntity(EntityType<? extends PlaceholderEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // placeholder: no synced data; EntityRobot syncs its board and energy in M2.5+
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        // placeholder: nothing to read; EntityRobot restores its board in M2.5+
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        // placeholder: nothing to write; EntityRobot persists its board in M2.5+
    }
}
