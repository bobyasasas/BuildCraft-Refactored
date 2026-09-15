/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

/**
 * Shared client-side infrastructure for the BuildCraft neo port (lib-level client code). Everything here lives in the
 * {@code client} source set ({@code neo/src/client/java}).
 *
 * <p><b>Dependency direction contract (M2.7a, closes the M1.2 deferral on the new tree):</b>
 * <ul>
 * <li>{@code client} source set may depend on {@code main} — the build wires {@code main.output} onto the client
 * compile/runtime classpaths.</li>
 * <li>{@code main} must NEVER reference anything in {@code buildcraft.lib.client.*} (or any other client-only
 * package): it cannot, since {@code src/client} output is not on main's classpath. Client entry points that need to
 * hook events register through classes like {@code buildcraft.energy.client.BuildCraftEnergyClient}
 * ({@code @Mod(dist = Dist.CLIENT)}); no hand-written {@code Dist} guards anywhere.</li>
 * </ul>
 *
 * <p>Sub-packages: {@link buildcraft.lib.client.render} — the minimal dynamic-quad toolkit that the M2.7+ block entity
 * renderers build on (26.1.2 extractRenderState/submit pipeline).
 */
package buildcraft.lib.client;
