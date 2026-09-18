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
import buildcraft.core.blockentity.KinesisPipeBlockEntity;

/**
 * Jade panel for {@link KinesisPipeBlockEntity} ({@code buildcraftcore:pipe_kinesis_wood}): the buffer line
 * {@code "1,234 / 10,000 μJ"}. Server/client two halves like {@link StoneEngineDataProvider} &mdash; the pipe's energy
 * lives server-side only, so the value rides Jade's request/response tag, not the client BE copy.
 */
public class KinesisPipeDataProvider implements IServerDataProvider<BlockAccessor> {

    public static final KinesisPipeDataProvider INSTANCE = new KinesisPipeDataProvider();

    static final String KEY_ENERGY = "bc_pipe_energy";

    private KinesisPipeDataProvider() {
    }

    @Override
    public Identifier getUid() {
        return Identifier.fromNamespaceAndPath("buildcraftlib", "pipe_kinesis_wood");
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof KinesisPipeBlockEntity pipe) {
            data.putLong(KEY_ENERGY, pipe.getEnergyStored());
        }
    }

    /** The client half: formats the server data into tooltip lines. */
    public static final class Client extends KinesisPipeDataProvider implements IBlockComponentProvider {

        public static final Client INSTANCE = new Client();

        private Client() {
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains(KEY_ENERGY)) {
                return;
            }
            tooltip.add(BcJadeFormat.energy(data.getLongOr(KEY_ENERGY, 0), KinesisPipeBlockEntity.CAPACITY));
        }
    }
}
