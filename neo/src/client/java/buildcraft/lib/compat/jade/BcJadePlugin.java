/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.compat.jade;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import buildcraft.builders.block.FillerBlock;
import buildcraft.builders.block.QuarryBlock;
import buildcraft.builders.blockentity.FillerBlockEntity;
import buildcraft.builders.blockentity.QuarryBlockEntity;
import buildcraft.core.block.EnergyMeterBlock;
import buildcraft.core.block.KinesisPipeBlock;
import buildcraft.core.block.StoneEngineBlock;
import buildcraft.core.blockentity.EnergyMeterBlockEntity;
import buildcraft.core.blockentity.KinesisPipeBlockEntity;
import buildcraft.core.blockentity.StoneEngineBlockEntity;

/**
 * Jade compat entry point (Jade 26.1.x for MC 26.1.2, compileOnly integration).
 * <p>
 * Discovery: Jade scans every loaded mod file for classes annotated with
 * {@link WailaPlugin} via FML's {@code ModFileScanData.getAnnotatedBy} (no
 * ServiceLoader file, no {@code jade_plugins.json}), then instantiates them
 * itself when Jade is present. The annotation's {@code value} is a required
 * mod id gate; the empty default loads the plugin whenever its jar is present,
 * which is the same pattern Jade's own plugins use. Nothing in BuildCraft
 * references this class, so with Jade absent it is never loaded — which is
 * what keeps the compileOnly integration safe at runtime.
 * <p>
 * Registration is the standard two-phase shape: {@link #register} hooks the
 * {@code IServerDataProvider<BlockAccessor>} halves against the block entity
 * classes (energy/progress state lives in the server-side BE only, so the
 * tooltip values must ride Jade's server data request/response tag), and
 * {@link #registerClient} hooks the nested {@code Client} halves
 * ({@code IBlockComponentProvider} — Jade rejects data providers that
 * implement it themselves since MC 1.21.6) against the block classes. The 21
 * placeholder block entities deliberately get no provider: Jade shows just
 * the block name for them.
 */
@WailaPlugin
public class BcJadePlugin implements IWailaPlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(StoneEngineDataProvider.INSTANCE, StoneEngineBlockEntity.class);
        registration.registerBlockDataProvider(KinesisPipeDataProvider.INSTANCE, KinesisPipeBlockEntity.class);
        registration.registerBlockDataProvider(EnergyMeterDataProvider.INSTANCE, EnergyMeterBlockEntity.class);
        registration.registerBlockDataProvider(FillerDataProvider.INSTANCE, FillerBlockEntity.class);
        registration.registerBlockDataProvider(QuarryDataProvider.INSTANCE, QuarryBlockEntity.class);
        LOGGER.info("BuildCraft Jade compat registered");
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(StoneEngineDataProvider.Client.INSTANCE, StoneEngineBlock.class);
        registration.registerBlockComponent(KinesisPipeDataProvider.Client.INSTANCE, KinesisPipeBlock.class);
        registration.registerBlockComponent(EnergyMeterDataProvider.Client.INSTANCE, EnergyMeterBlock.class);
        registration.registerBlockComponent(FillerDataProvider.Client.INSTANCE, FillerBlock.class);
        registration.registerBlockComponent(QuarryDataProvider.Client.INSTANCE, QuarryBlock.class);
    }
}
