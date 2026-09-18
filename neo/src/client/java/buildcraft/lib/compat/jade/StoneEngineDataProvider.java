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
import buildcraft.core.blockentity.StoneEngineBlockEntity;

/**
 * Jade panel for {@link StoneEngineBlockEntity} ({@code buildcraftcore:engine_stone}): the buffer line
 * {@code "12,345 / 100,000 μJ"} plus, while a fuel item is burning, {@code "Burn 45%"} (percent of the current item
 * already consumed).
 *
 * <p>Two halves of Jade's server data channel (the slice BE keeps its energy/burn state server-side only; the client
 * BE copy is stale between update-tag pushes, so the tooltip never reads it): the server half
 * {@link #appendServerData} runs on the (integrated or dedicated) server and copies the raw numbers into the request
 * response tag; the client half ({@link Client}) formats whatever arrived. If the server half never ran (tag lacks
 * our key) the tooltip stays silent instead of lying with zeros.
 *
 * <p>Class shape (Jade's own {@code EnergyStorageProvider} pattern, enforced by Jade's
 * {@code WailaCommonRegistration#checkDataProvider} since MC 1.21.6): the server provider must not implement
 * {@code IComponentProvider}, so the client half is a nested subclass instead of the same class.
 */
public class StoneEngineDataProvider implements IServerDataProvider<BlockAccessor> {

    public static final StoneEngineDataProvider INSTANCE = new StoneEngineDataProvider();

    /** Server-data keys; prefixed and provider-unique because Jade merges every provider into one tag. */
    static final String KEY_ENERGY = "bc_engine_energy";
    static final String KEY_BURN_REMAIN = "bc_engine_burn_remain";
    static final String KEY_BURN_TOTAL = "bc_engine_burn_total";

    private StoneEngineDataProvider() {
    }

    @Override
    public Identifier getUid() {
        return Identifier.fromNamespaceAndPath("buildcraftlib", "engine_stone");
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof StoneEngineBlockEntity engine) {
            data.putLong(KEY_ENERGY, engine.getEnergyStored());
            data.putInt(KEY_BURN_REMAIN, engine.getBurnRemain());
            data.putInt(KEY_BURN_TOTAL, engine.getBurnTotal());
        }
    }

    /** The client half: formats the server data into tooltip lines. */
    public static final class Client extends StoneEngineDataProvider implements IBlockComponentProvider {

        public static final Client INSTANCE = new Client();

        private Client() {
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains(KEY_ENERGY)) {
                return;
            }
            tooltip.add(BcJadeFormat.energy(data.getLongOr(KEY_ENERGY, 0), StoneEngineBlockEntity.CAPACITY));
            int remain = data.getIntOr(KEY_BURN_REMAIN, 0);
            int total = data.getIntOr(KEY_BURN_TOTAL, 0);
            if (remain > 0 && total > 0) {
                int percent = Math.max(0, Math.min(100, (total - remain) * 100 / total));
                tooltip.add(BcJadeFormat.line("Burn " + percent + "%"));
            }
        }
    }
}
