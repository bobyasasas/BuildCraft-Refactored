/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.datagen;

import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * M3.4: base class for the per-mod BuildCraft datagen providers. A provider owns exactly one mod namespace: it is
 * created from that mod's {@code GatherDataEvent} {@link PackOutput} and only ever writes files under it, matching the
 * per-mod event contract (one event per mod, each with its own DataGenerator).
 *
 * <p>Serialisation goes through {@link BcDatagenJson#save(CachedOutput, JsonObject, Path, java.util.Comparator)}, the
 * byte-faithful variant of stock {@code DataProvider.saveStable}; see that class for the two deliberate deltas
 * (html-safe off, trailing newline).
 */
public abstract class BcDatagenProvider implements DataProvider {

    protected final String modid;
    protected final BcDatagenPaths paths;

    protected BcDatagenProvider(String modid, PackOutput output) {
        this.modid = modid;
        this.paths = new BcDatagenPaths(output);
    }

    /** Enqueues one asset JSON file with stock datagen key ordering ("type"/"parent" first, then natural). */
    protected CompletableFuture<?> saveAsset(CachedOutput cache, JsonObject json, String idPath, String folder) {
        return BcDatagenJson.save(cache, json, paths.asset(modid, folder, idPath), DataProvider.KEY_COMPARATOR);
    }

    @Override
    public String getName() {
        // must be unique per provider within a DataGenerator pack (stock addProvider rejects duplicate names)
        return "BuildCraft datagen: " + getClass().getSimpleName() + " (" + modid + ")";
    }
}
