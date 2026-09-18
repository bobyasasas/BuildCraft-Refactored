/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.compat.jade;

import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Shared formatting for the BuildCraft Jade providers: thousands-grouped numbers, the slice energy unit (&micro;MJ) and
 * block position rendering. Locale-independent on purpose ({@link Locale#ROOT}) so tooltips look the same no matter
 * what JVM locale the game runs with.
 */
final class BcJadeFormat {

    private BcJadeFormat() {
    }

    /** {@code 12345} &rarr; {@code "12,345"} (grouping separator fixed by {@link Locale#ROOT}). */
    static String number(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    /** The standard buffer line: {@code "12,345 / 100,000 μJ"} (gray, like Jade's own body lines). */
    static Component energy(long stored, long capacity) {
        return line(number(stored) + " / " + number(capacity) + " μJ");
    }

    /** The lifetime-throughput line of the energy meter: {@code "Total 1,234,567 μJ"}. */
    static Component energyTotal(long total) {
        return line("Total " + number(total) + " μJ");
    }

    /** {@code [12, 64, -3]} &mdash; absolute world coordinates of a work cell / mining target. */
    static String coords(int x, int y, int z) {
        return "[" + x + ", " + y + ", " + z + "]";
    }

    /** A plain gray body line; BuildCraft lines stay visually quiet next to Jade's vanilla ones. */
    static Component line(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GRAY);
    }
}
