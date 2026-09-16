/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.energy.generation.structure;

import buildcraft.core.BcBlocks;
import buildcraft.energy.generation.structure.OilGenStructurePart.GenByPredicate;
import buildcraft.energy.generation.structure.OilGenStructurePart.ReplaceType;
import buildcraft.lib.misc.VecUtil;
import buildcraft.lib.misc.data.Box;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * M2.8 port of legacy {@code buildcraft.energy.generation.structure.OilGenerator} (1.20.1): decides per chunk
 * whether an oil well generates and assembles its pieces. All generation numbers below are the <em>legacy default
 * config values</em> (legacy {@code BCEnergyConfig} defaults, see the audit in {@code neo/docs/worldgen-26.1.2.md}
 * §6.2):
 * <ul>
 * <li>probabilities per chunk: large 0.04 %, medium 0.1 %, lake 2 % (lake only in {@code surfaceDepositBiomes},
 * which defaults to empty — so lakes never generate with defaults);</li>
 * <li>bonus = ×3 (surface deposit biome) × ×30 (excessive biome, defaulting to the dead oil_biome ids)
 * × generationRate 1.0;</li>
 * <li>excluded biomes default to {@code minecraft:hell}/{@code minecraft:sky}, ids that do not exist in modern
 * versions, so nothing is actually excluded there; the end is protected within 1200 blocks of the origin;</li>
 * <li>spouts enabled, small 6..12 tall, large 10..20 tall.</li>
 * </ul>
 * The legacy BC config system itself has not migrated, so these are frozen constants; if/when energy config
 * migrates, replace them with config reads (task for the energy content migration).
 */
public class OilGenerator {
    /** Random number, used to differentiate generators */
    public static final long MAGIC_GEN_NUMBER = 0xD0_46_B4_E4_0C_7D_07_CFL;

    /**
     * The distance that oil generation will be checked to see if their structures overlap with the currently
     * generating chunk. This should be large enough that all oil generation can fit inside this radius. If this
     * number is too big then oil generation will be slightly slower
     */
    public static final int MAX_CHUNK_RADIUS = 5;

    // --- Legacy BCEnergyConfig defaults (frozen; see class javadoc) ---
    public static final double OIL_WELL_GENERATION_RATE = 1.0;
    /** Percentage probability values, divided by 100 exactly like legacy {@code BCEnergyConfig#reloadConfig}. */
    public static final double SMALL_OIL_GEN_PROB = 2.0 / 100;
    public static final double MEDIUM_OIL_GEN_PROB = 0.1 / 100;
    public static final double LARGE_OIL_GEN_PROB = 0.04 / 100;
    public static final boolean ENABLE_OIL_SPOUTS = true;
    public static final int SMALL_SPOUT_MIN_HEIGHT = 6;
    public static final int SMALL_SPOUT_MAX_HEIGHT = 12;
    public static final int LARGE_SPOUT_MIN_HEIGHT = 10;
    public static final int LARGE_SPOUT_MAX_HEIGHT = 20;

    public static final Set<Identifier> EXCESSIVE_BIOMES = Set.of(
        Identifier.fromNamespaceAndPath("buildcraftenergy", "oil_desert"),//
        Identifier.fromNamespaceAndPath("buildcraftenergy", "oil_ocean"));
    /** Legacy default is empty — surface (lake) deposits never generate with defaults. */
    public static final Set<Identifier> SURFACE_DEPOSIT_BIOMES = Set.of();
    /** Legacy default; both ids stopped existing after 1.12, kept for behavioural parity. */
    public static final Set<Identifier> EXCLUDED_BIOMES = Set.of(
        Identifier.fromNamespaceAndPath("minecraft", "hell"),//
        Identifier.fromNamespaceAndPath("minecraft", "sky"));

    public enum GenType {
        LARGE,
        MEDIUM,
        LAKE,
        NONE
    }

    /** Per-chunk generation data handed from {@link OilStructureFeature} into {@link #generatePieces}. */
    public record Info(GenType type, WorldgenRandom oilRand, int xForGen, int zForGen) {}

    public static void generatePieces(
        StructurePiecesBuilder piecesBuilder,
        Structure.GenerationContext context,
        OilGenerator.Info info
    ) {
        LevelHeightAccessor heightAccessor = context.heightAccessor();
        int minHeight = heightAccessor.getMinY();
        int maxHeight = heightAccessor.getMaxY();
        ChunkPos chunkPos = context.chunkPos();
        int chunkX = chunkPos.x();
        int chunkZ = chunkPos.z();

        int x = chunkX * 16 + 8;
        int z = chunkZ * 16 + 8;
        if (info == null) {
            throw new IllegalStateException(
                "Tried to gen oil structure pieces in chunk [" + chunkPos + "], but no info was given. Bug!");
        }
        GenType type = info.type();
        WorldgenRandom rand = info.oilRand();
        int xForGen = info.xForGen();
        int zForGen = info.zForGen();
        BlockPos min = new BlockPos(x - 16 * MAX_CHUNK_RADIUS, minHeight, z - 16 * MAX_CHUNK_RADIUS);
        Box box = new Box(min, min.offset(2 * 16 * MAX_CHUNK_RADIUS, maxHeight - minHeight, 2 * 16 * MAX_CHUNK_RADIUS));

        OilStructure structure = createStructureByType(type, rand, xForGen, zForGen, minHeight, maxHeight, box);
        // type == NONE -> null
        if (structure != null) {
            piecesBuilder.addPiece(structure);
        }
    }

    /**
     * To find out which type to gen
     * {@link GenType#NONE} means skipped and nothing for gen
     */
    public static GenType getPieceTypeByRand(RandomSource rand, Holder<Biome> biome, int cx, int cz, int x, int z, boolean log) {
        Identifier biomeRegistryName = biome.unwrapKey().map(ResourceKey::identifier).orElse(null);
        // Do not generate oil in excluded biomes
        boolean isExcludedBiome = EXCLUDED_BIOMES.contains(biomeRegistryName);
        if (isExcludedBiome) {
            return GenType.NONE;
        }

        if (biome.is(BiomeTags.IS_END) && (Math.abs(x) < 1200 || Math.abs(z) < 1200)) {
            return GenType.NONE;
        }

        boolean oilBiome = SURFACE_DEPOSIT_BIOMES.contains(biomeRegistryName);

        double bonus = oilBiome ? 3.0 : 1.0;
        bonus *= OIL_WELL_GENERATION_RATE;
        if (EXCESSIVE_BIOMES.contains(biomeRegistryName)) {
            bonus *= 30.0;
        }
        final GenType type;

        double nextDouble = rand.nextDouble();

        if (nextDouble <= LARGE_OIL_GEN_PROB * bonus) {
            // 0.04%
            type = GenType.LARGE;
        } else if (nextDouble <= MEDIUM_OIL_GEN_PROB * bonus) {
            // 0.1%
            type = GenType.MEDIUM;
        } else if (oilBiome && nextDouble <= SMALL_OIL_GEN_PROB * bonus) {
            // 2%
            type = GenType.LAKE;
        } else {
            type = GenType.NONE;
        }
        if (log) {
            logGeneration(type, cx, cz, x, z, biomeRegistryName, oilBiome, isExcludedBiome);
        }
        return type;
    }

    private static void logGeneration(GenType type, int cx, int cz, int x, int z, Identifier biome, boolean oilBiome,
                                      boolean excluded) {
        if (type == GenType.NONE) {
            if (OilStructureRegistry.DEBUG_OILGEN_BASIC) {
                String why = excluded ? "the biome we found (" + biome + ") is disabled!"
                    : OilStructureRegistry.DEBUG_OILGEN_ALL
                        ? "none of the random numbers were above the thresholds for generation"
                        : null;
                if (why != null) {
                    OilStructureRegistry.LOG.info(
                        "[energy.oilgen] Not generating oil in chunk {}, {} because {}",
                        cx, cz, why);
                }
            }
        } else if (OilStructureRegistry.DEBUG_OILGEN_BASIC) {
            OilStructureRegistry.LOG.info(
                "[energy.oilgen] Generating an oil well ({}) in chunk {}, {} at {}, {}{}",
                type.name().toLowerCase(Locale.ROOT), cx, cz, x, z,
                oilBiome ? " (surface deposit biome)" : "");
        }
    }

    public static OilStructure createStructureByType(final GenType type, RandomSource rand, int x, int z,
                                                     int worldBottomHeight, int worldTopHeight, Box box) {
        List<OilGenStructurePart> structures = new ArrayList<>();
        final int lakeRadius;
        final int tendrilRadius;
        switch (type) {
            case LARGE:
                lakeRadius = 4;
                tendrilRadius = 25 + rand.nextInt(20);
                break;
            case MEDIUM:
                lakeRadius = 2;
                tendrilRadius = 5 + rand.nextInt(10);
                break;
            case LAKE:
                lakeRadius = 6;
                tendrilRadius = 25 + rand.nextInt(20);
                break;
            default:
                return null;
        }
        structures.add(createTendril(new BlockPos(x, 62, z), lakeRadius, tendrilRadius, rand));

        int minHeight, maxHeight;

        if (type != GenType.LAKE) {
            // Generate a spherical cave deposit
            int wellY = worldBottomHeight + 20 + rand.nextInt(10);

            int radius;
            if (type == GenType.LARGE) {
                radius = 8 + rand.nextInt(9);
            } else {
                radius = 4 + rand.nextInt(4);
            }

            structures.add(createSphere(new BlockPos(x, wellY, z), radius));

            // Generate a spout
            if (ENABLE_OIL_SPOUTS) {
                if (type == GenType.LARGE) {
                    minHeight = LARGE_SPOUT_MIN_HEIGHT;
                    maxHeight = LARGE_SPOUT_MAX_HEIGHT;
                    radius = 1;
                } else {
                    minHeight = SMALL_SPOUT_MIN_HEIGHT;
                    maxHeight = SMALL_SPOUT_MAX_HEIGHT;
                    radius = 0;
                }
                final int height;
                if (maxHeight == minHeight) {
                    height = maxHeight;
                } else {
                    if (maxHeight < minHeight) {
                        int t = maxHeight;
                        maxHeight = minHeight;
                        minHeight = t;
                    }
                    height = minHeight + rand.nextInt(maxHeight - minHeight);
                }
                structures.add(createSpout(new BlockPos(x, wellY, z), height, radius));
            }

            // Generate a spring at the very bottom
            if (type == GenType.LARGE) {
                structures.add(createTube(new BlockPos(x, worldBottomHeight + 2, z), wellY - worldBottomHeight + 1, radius, Direction.Axis.Y));
                if (BcBlocks.SPRING_OIL != null) {
                    structures.add(createSpring(new BlockPos(x, worldBottomHeight + 1, z)));
                }
            }
        }
        return new OilStructure(box, structures);
    }

    public static OilGenStructurePart createSpout(BlockPos start, int height, int radius) {
        return new OilGenStructurePart.Spout(start, OilGenStructurePart.ReplaceType.ALWAYS, radius, height);
    }

    public static OilGenStructurePart createTubeY(BlockPos base, int height, int radius) {
        return createTube(base, height, radius, Axis.Y);
    }

    public static OilGenStructurePart createSpring(BlockPos at) {
        return new OilGenStructurePart.Spring(at);
    }

    public static OilGenStructurePart createTube(BlockPos center, int length, int radius, Axis axis) {
        int valForAxis = VecUtil.getValue(center, axis);
        BlockPos min = VecUtil.replaceValue(center.offset(-radius, -radius, -radius), axis, valForAxis);
        BlockPos max = VecUtil.replaceValue(center.offset(radius, radius, radius), axis, valForAxis + length);
        double radiusSq = (double) radius * radius;
        int toReplace = valForAxis;
        return new GenByPredicate(new Box(min, max), ReplaceType.ALWAYS, new Object[] { axis, toReplace, center, radiusSq });
    }

    public static OilGenStructurePart createSphere(BlockPos center, int radius) {
        Box box = new Box(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius));
        double radiusSq = (double) radius * radius + 0.01;
        return new OilGenStructurePart.GenByPredicate(box, OilGenStructurePart.ReplaceType.ALWAYS,
            new Object[] { center, radiusSq });
    }

    public static OilGenStructurePart createTendril(BlockPos center, int lakeRadius, int radius, RandomSource rand) {
        BlockPos start = center.offset(-radius, 0, -radius);
        int diameter = radius * 2 + 1;
        boolean[][] pattern = new boolean[diameter][diameter];

        int x = radius;
        int z = radius;
        for (int dx = -lakeRadius; dx <= lakeRadius; dx++) {
            for (int dz = -lakeRadius; dz <= lakeRadius; dz++) {
                pattern[x + dx][z + dz] = (double) dx * dx + (double) dz * dz <= (double) lakeRadius * lakeRadius;
            }
        }

        for (int w = 1; w < radius; w++) {
            float proba = (float) (radius - w + 4) / (float) (radius + 4);

            fillPatternIfProba(rand, proba, x, z + w, pattern);
            fillPatternIfProba(rand, proba, x, z - w, pattern);
            fillPatternIfProba(rand, proba, x + w, z, pattern);
            fillPatternIfProba(rand, proba, x - w, z, pattern);

            for (int i = 1; i <= w; i++) {
                fillPatternIfProba(rand, proba, x + i, z + w, pattern);
                fillPatternIfProba(rand, proba, x + i, z - w, pattern);
                fillPatternIfProba(rand, proba, x + w, z + i, pattern);
                fillPatternIfProba(rand, proba, x - w, z + i, pattern);

                fillPatternIfProba(rand, proba, x - i, z + w, pattern);
                fillPatternIfProba(rand, proba, x - i, z - w, pattern);
                fillPatternIfProba(rand, proba, x + w, z - i, pattern);
                fillPatternIfProba(rand, proba, x - w, z - i, pattern);
            }
        }

        int depth = rand.nextDouble() < 0.5 ? 1 : 2;
        return OilGenStructurePart.PatternTerrainHeight.create(start, OilGenStructurePart.ReplaceType.IS_FOR_LAKE, pattern, depth);
    }

    private static void fillPatternIfProba(RandomSource rand, float proba, int x, int z, boolean[][] pattern) {
        if (rand.nextFloat() <= proba) {
            pattern[x][z] = isSet(pattern, x, z - 1) | isSet(pattern, x, z + 1) //
                | isSet(pattern, x - 1, z) | isSet(pattern, x + 1, z);
        }
    }

    private static boolean isSet(boolean[][] pattern, int x, int z) {
        if (x < 0 || x >= pattern.length) return false;
        if (z < 0 || z >= pattern[x].length) return false;
        return pattern[x][z];
    }
}
