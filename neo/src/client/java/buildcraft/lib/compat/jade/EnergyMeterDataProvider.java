/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import buildcraft.core.blockentity.EnergyMeterBlockEntity;

/**
 * Jade panel for {@link EnergyMeterBlockEntity} ({@code buildcraftcore:energy_meter}): the lifetime throughput line
 * {@code "Total 1,234,567 μJ"}. The meter never syncs its counter to clients at all, so the server-data channel is the
 * only way the tooltip can show a real number (see {@link StoneEngineDataProvider} for the two-half pattern).
 */
public class EnergyMeterDataProvider implements IServerDataProvider<BlockAccessor> {

    public static final EnergyMeterDataProvider INSTANCE = new EnergyMeterDataProvider();

    static final String KEY_TOTAL = "bc_meter_total";

    private EnergyMeterDataProvider() {
    }

    @Override
    public Identifier getUid() {
        return Identifier.fromNamespaceAndPath("buildcraftlib", "energy_meter");
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof EnergyMeterBlockEntity meter) {
            data.putLong(KEY_TOTAL, meter.getTotalReceived());
        }
    }

    /** The client half: formats the server data into tooltip lines. */
    public static final class Client extends EnergyMeterDataProvider implements IBlockComponentProvider {

        public static final Client INSTANCE = new Client();

        private Client() {
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains(KEY_TOTAL)) {
                return;
            }
            tooltip.add(BcJadeFormat.energyTotal(data.getLongOr(KEY_TOTAL, 0)));
        }
    }
}
