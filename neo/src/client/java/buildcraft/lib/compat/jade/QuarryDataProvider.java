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
import buildcraft.builders.blockentity.QuarryBlockEntity;

/**
 * Jade panel for {@link QuarryBlockEntity} ({@code buildcraftbuilders:quarry}): the battery line
 * {@code "456,789 / 2,400,000 μJ"}, then either {@code "Done"} (whole area mined) or the working state &mdash; the cell
 * currently being drilled ({@code "Target: [12, 64, -3]"}) and, when mined drops are parked in the internal buffer,
 * {@code "Buffer: 7 items"}. Server/client two halves like {@link StoneEngineDataProvider}.
 */
public class QuarryDataProvider implements IServerDataProvider<BlockAccessor> {

    public static final QuarryDataProvider INSTANCE = new QuarryDataProvider();

    static final String KEY_ENERGY = "bc_quarry_energy";
    static final String KEY_FINISHED = "bc_quarry_finished";
    /** Present only while a drill target is active (three ints, absolute coords). */
    static final String KEY_TARGET_X = "bc_quarry_target_x";
    static final String KEY_TARGET_Y = "bc_quarry_target_y";
    static final String KEY_TARGET_Z = "bc_quarry_target_z";
    /** Total item count of the mined-drops buffer; present only when the buffer is non-empty. */
    static final String KEY_BUFFER = "bc_quarry_buffer";

    private QuarryDataProvider() {
    }

    @Override
    public Identifier getUid() {
        return Identifier.fromNamespaceAndPath("buildcraftlib", "quarry");
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof QuarryBlockEntity quarry) {
            data.putLong(KEY_ENERGY, quarry.getEnergyStored());
            data.putBoolean(KEY_FINISHED, quarry.isFinished());
            if (quarry.getCurrentTarget() != null) {
                data.putInt(KEY_TARGET_X, quarry.getCurrentTarget().getX());
                data.putInt(KEY_TARGET_Y, quarry.getCurrentTarget().getY());
                data.putInt(KEY_TARGET_Z, quarry.getCurrentTarget().getZ());
            }
            int items = 0;
            for (var stack : quarry.getOutput()) {
                items += stack.getCount();
            }
            if (items > 0) {
                data.putInt(KEY_BUFFER, items);
            }
        }
    }

    /** The client half: formats the server data into tooltip lines. */
    public static final class Client extends QuarryDataProvider implements IBlockComponentProvider {

        public static final Client INSTANCE = new Client();

        private Client() {
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains(KEY_ENERGY)) {
                return;
            }
            tooltip.add(BcJadeFormat.energy(data.getLongOr(KEY_ENERGY, 0), QuarryBlockEntity.CAPACITY));
            if (data.getBooleanOr(KEY_FINISHED, false)) {
                tooltip.add(BcJadeFormat.line("Done"));
                return;
            }
            if (data.contains(KEY_TARGET_X)) {
                String target = BcJadeFormat.coords(data.getIntOr(KEY_TARGET_X, 0), data.getIntOr(KEY_TARGET_Y, 0),
                        data.getIntOr(KEY_TARGET_Z, 0));
                tooltip.add(BcJadeFormat.line("Target: " + target));
            }
            if (data.contains(KEY_BUFFER)) {
                tooltip.add(
                        BcJadeFormat.line("Buffer: " + BcJadeFormat.number(data.getIntOr(KEY_BUFFER, 0)) + " items"));
            }
        }
    }
}
