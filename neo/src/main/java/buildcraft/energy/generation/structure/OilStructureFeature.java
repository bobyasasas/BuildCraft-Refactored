/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.energy.generation.structure;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * M2.8 port of legacy {@code buildcraft.energy.generation.structure.OilStructureFeature} (1.20.1): the
 * {@link Structure} behind {@code buildcraftenergy:oil_spout}. The per-chunk dice roll and biome gating live in
 * {@link OilGenerator#getPieceTypeByRand}; only the 26.1.2 API shape differs from legacy (codec is a
 * {@link MapCodec} since {@link StructureType#codec()} changed, and this version's biome holders use
 * {@code Holder.is(TagKey)}). See {@code neo/docs/worldgen-26.1.2.md} §2/§3 for the API evidence.
 *
 * <p>The instance itself is data: {@code data/buildcraftenergy/worldgen/structure/oil_spout.json} references this
 * class via the code-registered {@link #type() structure type}, with biomes
 * {@code #buildcraftenergy:oil_gen} (= {@code #minecraft:is_overworld}) and step {@code fluid_springs} — the exact
 * JSON the legacy 1.20.1 datagen produced.</p>
 */
public class OilStructureFeature extends Structure {
    public static final MapCodec<OilStructureFeature> CODEC = simpleCodec(OilStructureFeature::new);

    public OilStructureFeature(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int chunkX = chunkPos.x();
        int chunkZ = chunkPos.z();

        int cx = chunkX;
        int cz = chunkZ;

        // Chunk Rand, seeded with the oil-specific magic number so the roll is independent of every other generator
        // (identical streams to the legacy generator for a given world seed).
        WorldgenRandom rand = new WorldgenRandom(new LegacyRandomSource(OilGenerator.MAGIC_GEN_NUMBER));
        rand.setLargeFeatureSeed(context.seed(), cx, cz);
        // shift to world coordinates
        int xForGen = cx * 16 + 8 + rand.nextInt(16);
        int zForGen = cz * 16 + 8 + rand.nextInt(16);
        Holder<Biome> biome = context.chunkGenerator().getBiomeSource().getNoiseBiome(
            QuartPos.fromBlock(xForGen),
            QuartPos.fromBlock(63),
            QuartPos.fromBlock(zForGen),
            context.randomState().sampler());
        OilGenerator.GenType type = OilGenerator.getPieceTypeByRand(rand, biome, cx, cz, xForGen, zForGen, true);
        if (type == OilGenerator.GenType.NONE) {
            return Optional.empty();
        }
        OilGenerator.Info info = new OilGenerator.Info(type, rand, xForGen, zForGen);
        return onTopOfChunkCenter(context, Heightmap.Types.WORLD_SURFACE_WG, structurePiecesBuilder -> {
            OilGenerator.generatePieces(structurePiecesBuilder, context, info);
        });
    }

    @Override
    public StructureType<?> type() {
        return OilStructureRegistry.OIL_SPOUT_TYPE.value();
    }
}
