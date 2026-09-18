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
import buildcraft.builders.blockentity.FillerBlockEntity;

/**
 * Jade panel for {@link FillerBlockEntity} ({@code buildcraftbuilders:filler}): the battery line
 * {@code "123,456 / 1,600,000 μJ"}, then either {@code "Done"} (box matches the pattern) or the working state
 * &mdash; the pattern's legacy unique tag ({@code "Pattern: buildcraft:fill"}) and the cell currently being placed
 * ({@code "Cell: [12, 64, -3]"}). Server/client two halves like {@link StoneEngineDataProvider}.
 */
public class FillerDataProvider implements IServerDataProvider<BlockAccessor> {

    public static final FillerDataProvider INSTANCE = new FillerDataProvider();

    static final String KEY_ENERGY = "bc_filler_energy";
    static final String KEY_FINISHED = "bc_filler_finished";
    /** Present only when a pattern is selected; the value is the legacy unique tag (e.g. {@code buildcraft:fill}). */
    static final String KEY_PATTERN = "bc_filler_pattern";
    /** Present only while a placement cell is active (three ints, absolute coords). */
    static final String KEY_CELL_X = "bc_filler_cell_x";
    static final String KEY_CELL_Y = "bc_filler_cell_y";
    static final String KEY_CELL_Z = "bc_filler_cell_z";

    private FillerDataProvider() {
    }

    @Override
    public Identifier getUid() {
        return Identifier.fromNamespaceAndPath("buildcraftlib", "filler");
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof FillerBlockEntity filler) {
            data.putLong(KEY_ENERGY, filler.getEnergyStored());
            data.putBoolean(KEY_FINISHED, filler.isFinished());
            FillerBlockEntity.Pattern pattern = filler.getPattern();
            if (pattern != null) {
                data.putString(KEY_PATTERN, pattern.uniqueTag);
            }
            // currentCell is transient (null between placements): encode presence, not sentinel coordinates
            if (filler.getCurrentCell() != null) {
                data.putInt(KEY_CELL_X, filler.getCurrentCell().getX());
                data.putInt(KEY_CELL_Y, filler.getCurrentCell().getY());
                data.putInt(KEY_CELL_Z, filler.getCurrentCell().getZ());
            }
        }
    }

    /** The client half: formats the server data into tooltip lines. */
    public static final class Client extends FillerDataProvider implements IBlockComponentProvider {

        public static final Client INSTANCE = new Client();

        private Client() {
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains(KEY_ENERGY)) {
                return;
            }
            tooltip.add(BcJadeFormat.energy(data.getLongOr(KEY_ENERGY, 0), FillerBlockEntity.CAPACITY));
            if (data.getBooleanOr(KEY_FINISHED, false)) {
                tooltip.add(BcJadeFormat.line("Done"));
                return;
            }
            if (data.contains(KEY_PATTERN)) {
                tooltip.add(BcJadeFormat.line("Pattern: " + data.getStringOr(KEY_PATTERN, "?")));
            }
            if (data.contains(KEY_CELL_X)) {
                String cell = BcJadeFormat.coords(data.getIntOr(KEY_CELL_X, 0), data.getIntOr(KEY_CELL_Y, 0),
                        data.getIntOr(KEY_CELL_Z, 0));
                tooltip.add(BcJadeFormat.line("Cell: " + cell));
            }
        }
    }
}
