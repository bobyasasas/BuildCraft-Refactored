/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.datagen;

import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;

/**
 * M3.4: regenerates the shipped en_us lang files. {@code buildcraftcore} carries the frozen 857-key table (the M3.5
 * mechanical copy of the BuildCraft-Localization {@code en_us.json}); the other seven modids ship an empty {@code {}}
 * file (runtime lang is one flat cross-namespace KV map, M3.5 ruling).
 *
 * <p>Deliberately NOT stock {@code net.neoforged.neoforge.common.data.LanguageProvider}: that class sorts keys into a
 * TreeMap and saves via stock {@code saveStable}, which would reorder the frozen table and strip the trailing newline.
 * Here the entries are emitted in committed insertion order (null comparator in {@link BcDatagenJson#save}) so the
 * file comes out byte-identical to the shipped asset; the diff gate re-proves the frozen key set on every run.
 */
public final class BcLangProvider extends BcDatagenProvider {

    private final Map<String, String> translations = new LinkedHashMap<>();

    public BcLangProvider(String modid, PackOutput output) {
        super(modid, output);
        if ("buildcraftcore".equals(modid)) {
            BcLangData.addTo(translations);
        }
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        JsonObject json = new JsonObject();
        translations.forEach(json::addProperty);
        // null comparator: insertion order, exactly like the committed file (stock saveStable would sort)
        return BcDatagenJson.save(cache, json, paths.asset(modid, "lang", "en_us"), null);
    }
}
