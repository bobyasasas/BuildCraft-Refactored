/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.energy.generation.structure;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

/**
 * M2.8 port of legacy {@code buildcraft.energy.generation.structure.OilStructureRegistry} (1.20.1): code-side
 * registration of the {@code buildcraftenergy:oil_spout} structure type and piece type. In 26.1.2 these are the
 * only two oil-worldgen things that can register in code — the {@link Structure} instance itself and its
 * {@code structure_set} are datapack entries shipped as JSON resources ({@code data/buildcraftenergy/worldgen/...},
 * identical to the legacy datagen output); see {@code neo/docs/worldgen-26.1.2.md} §1-§4.
 */
public class OilStructureRegistry {
    public static final String STRUCTURE_OIL_SPOUT = "oil_spout";

    private static final Logger LOG_INTERNAL = LogUtils.getLogger();

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister
        .create(BuiltInRegistries.STRUCTURE_TYPE, "buildcraftenergy");
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES = DeferredRegister
        .create(BuiltInRegistries.STRUCTURE_PIECE, "buildcraftenergy");

    public static final DeferredHolder<StructureType<?>, StructureType<OilStructureFeature>> OIL_SPOUT_TYPE = STRUCTURE_TYPES
        .register(STRUCTURE_OIL_SPOUT, OilStructureRegistry::makeOilSpoutType);

    /** Legacy: {@code StructurePieceType.setPieceId(OilStructure::deserialize, id)} — a contextless piece loader. */
    public static final DeferredHolder<StructurePieceType, StructurePieceType> OIL_SPOUT_PIECE_TYPE = STRUCTURE_PIECES
        .register(STRUCTURE_OIL_SPOUT, () -> (StructurePieceType.ContextlessType) OilStructure::deserialize);

    /** Legacy debug gates ({@code BCDebugging.shouldDebugLog/Complex("energy.oilgen")}), off unless enabled. */
    public static final boolean DEBUG_OILGEN_BASIC = Boolean.getBoolean("buildcraft.debug.oilgen");
    public static final boolean DEBUG_OILGEN_ALL = DEBUG_OILGEN_BASIC && Boolean.getBoolean("buildcraft.debug.oilgen.all");

    /** Shared logger for the oil generation debug output. */
    public static final Logger LOG = LOG_INTERNAL;

    private static StructureType<OilStructureFeature> makeOilSpoutType() {
        // StructureType is a SAM interface over its MapCodec (26.1.2); the legacy vanilla helper
        // StructureType.register(id, codec) did exactly this Registry.register call internally.
        return () -> OilStructureFeature.CODEC;
    }

    public static void clinit() {
        // Kept from legacy BCEnergy#preInit ordering; both DeferredRegisters self-register via the mod bus.
    }
}
