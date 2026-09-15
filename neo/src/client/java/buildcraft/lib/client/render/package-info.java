/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

/**
 * Minimal dynamic-quad toolkit for the 26.1.2 render pipeline (task M2.7a, the foundation for the M2.7b+ block entity
 * renderers).
 *
 * <h2>What was ported from legacy — and what was deliberately left out</h2>
 *
 * <p>Legacy BuildCraft (1.20.1, {@code common/buildcraft/lib/client/model/}) shipped a home-grown model system:
 * {@code MutableQuad}/{@code MutableVertex} (mutable quad authoring), plus {@code ModelHolderRegistry},
 * {@code AdvModelCache}, {@code ModelCache}, {@code ResourceLoaderContext} and friends (custom baking/caching/reload
 * infrastructure on top of Forge's old {@code IBakedModel} extension points). <b>Only the quad/vertex authoring core
 * has a counterpart here: {@link buildcraft.lib.client.render.BcQuad}/{@link buildcraft.lib.client.render.BcVertex}.
 * The whole legacy model cache/loader stack is NOT ported — 26.1.2 replaces it with native facilities
 * ({@code RegisterBlockModelsEvent}, {@code BlockStateModel}, the extractRenderState/submit pipeline), so a
 * home-grown cache would duplicate what the engine now does better.</b>
 *
 * <h2>How BcQuad fits the 26.1.2 pipeline</h2>
 *
 * <p>The vanilla 26.1+ renderer works in two phases: {@code extract} (build an immutable {@code ...RenderState} from
 * live world objects, once per frame) and {@code submit} (read that state and emit vertices, possibly multiple
 * passes). {@link buildcraft.lib.client.render.BcQuad} is a value object — instances can be held directly as fields
 * of a {@code BlockEntityRenderState} subclass, built during extract, and emitted during submit via
 * {@link buildcraft.lib.client.render.BcQuad#emit}. That "render state holding" is the modern replacement for the
 * legacy mutable-quad {@code ModelHolder} caches.
 *
 * <p>See {@code neo/docs/render-pipeline-26.1.2.md} for the full pipeline walkthrough.
 */
package buildcraft.lib.client.render;
