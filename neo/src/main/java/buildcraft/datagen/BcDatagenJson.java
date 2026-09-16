/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.datagen;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.util.GsonHelper;

/**
 * M3.4: byte-faithful JSON serialisation for the BuildCraft datagen providers.
 *
 * <p>This is {@link net.minecraft.data.DataProvider#saveStable(CachedOutput, JsonElement, Path)} with two deltas that
 * are required to reproduce the resources currently shipped in {@code neo/src/main/resources/assets/} byte for byte
 * (the M3.4 "no drift" gate diffs the runData output against those files, so every byte counts):
 * <ol>
 * <li>{@code JsonWriter.setHtmlSafe(false)} — the shipped lang file contains raw {@code <}, {@code >}, {@code =} and
 * {@code '} characters (e.g. {@code "Power < 25%"}), which stock saveStable would escape as {@code \u003c} etc.
 * Everything else (models, blockstates, item definitions) is unaffected by html-safety because they contain no such
 * characters;</li>
 * <li>a single {@code '\n'} appended after {@link JsonWriter#close()} — the shipped files all end with a trailing
 * newline, stock saveStable writes none.</li>
 * </ol>
 * The indentation (2 spaces), {@code setSerializeNulls(false)}, the key ordering ({@link net.minecraft.data.DataProvider#KEY_COMPARATOR}
 * puts "type" and "parent" first, everything else natural order) and the SHA-1 based {@link CachedOutput} diffing are
 * exactly the stock saveStable behaviour. Passing a {@code null} comparator keeps JSON object insertion order, which is
 * how the frozen 857-key lang table (M3.5) must be emitted.
 *
 * <p>Unlike stock saveStable, write failures are rethrown instead of logged: the datagen run must be fail-closed (same
 * philosophy as the registry/lang parity scripts), a half-written silent success would defeat the drift gate.
 */
public final class BcDatagenJson {

    private BcDatagenJson() {
    }

    /**
     * Serialises {@code json} exactly like {@code DataProvider.saveStable} (see class comment for the two deliberate
     * deltas) and hands the bytes to {@link CachedOutput#writeIfNeeded}, which skips the write when the SHA-1 matches
     * the previous run's cache entry.
     */
    public static CompletableFuture<?> save(CachedOutput cache, JsonElement json, Path path,
            Comparator<String> keyComparator) {
        return CompletableFuture.runAsync(() -> {
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                HashingOutputStream hashedBytes = new HashingOutputStream(Hashing.sha1(), bytes);

                JsonWriter jsonWriter = new JsonWriter(new OutputStreamWriter(hashedBytes, StandardCharsets.UTF_8));
                jsonWriter.setSerializeNulls(false);
                jsonWriter.setHtmlSafe(false);
                jsonWriter.setIndent("  ");
                GsonHelper.writeValue(jsonWriter, json, keyComparator);
                jsonWriter.close();
                // The shipped assets all end with a trailing newline; stock saveStable stops at the closing brace.
                hashedBytes.write('\n');

                cache.writeIfNeeded(path, bytes.toByteArray(), hashedBytes.hash());
            } catch (IOException e) {
                // fail-closed: surface the failure instead of stock saveStable's log-and-continue
                throw new UncheckedIOException("BcDatagenJson: failed to save datagen file to " + path, e);
            }
        });
    }
}
