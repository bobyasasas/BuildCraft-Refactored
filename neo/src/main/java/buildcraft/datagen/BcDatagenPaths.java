/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.datagen;

import java.nio.file.Path;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

/**
 * M3.4: resolves datagen output paths for one mod namespace. {@code GatherDataEvent} hands each mod its own
 * {@link PackOutput} rooted at {@code <output>/<modid>}, so providers writing only under
 * {@code <root>/assets/<namespace>/...} produce a per-mod tree that can be diffed directly against
 * {@code neo/src/main/resources/assets/<modid>}.
 */
public final class BcDatagenPaths {

    private final Path assetsDir;
    private final Path dataDir;

    public BcDatagenPaths(PackOutput output) {
        this.assetsDir = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK);
        this.dataDir = output.getOutputFolder(PackOutput.Target.DATA_PACK);
    }

    /** {@code assets/<namespace>/<folder>/<name>.json} — folder is e.g. {@code blockstates}, {@code items}, {@code models/item} or {@code lang}. */
    public Path asset(String namespace, String folder, String name) {
        return assetsDir.resolve(namespace).resolve(folder).resolve(name + ".json");
    }

    /** {@code assets/<namespace>/<folder>/<fileName>} with the full file name (e.g. {@code engine_base.jsonbc}) —
     * the M4.4 tile jsonbc emitter needs a non-{@code .json} extension. */
    public Path assetFile(String namespace, String folder, String fileName) {
        return assetsDir.resolve(namespace).resolve(folder).resolve(fileName);
    }

    /** Same layout as {@code PackOutput.PathProvider#json(Identifier)} for a full id, rooted under assets. */
    public Path asset(Identifier id, String folder) {
        return asset(id.getNamespace(), folder, id.getPath());
    }

    /** {@code data/<namespace>/<folder>/<name>.json} (unused by the M3.4 providers, kept symmetric for future ones). */
    public Path data(String namespace, String folder, String name) {
        return dataDir.resolve(namespace).resolve(folder).resolve(name + ".json");
    }
}
