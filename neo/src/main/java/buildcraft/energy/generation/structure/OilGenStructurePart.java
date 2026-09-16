/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.energy.generation.structure;

import buildcraft.energy.BcEnergyFluids;
import buildcraft.core.BcBlocks;
import buildcraft.lib.misc.VecUtil;
import buildcraft.lib.misc.data.Box;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.SpringFeature;
import net.minecraft.world.level.material.Fluid;

import java.util.function.Predicate;

/**
 * M2.8 port of legacy {@code buildcraft.energy.generation.structure.OilGenStructurePart} (1.20.1): the primitive
 * shapes an oil well is built from. Semantics and defaults are the legacy ones — see
 * {@code neo/docs/worldgen-26.1.2.md} §6 for the audit. Adaptations for 26.1.2:
 * <ul>
 * <li>oil fluid = {@link BcEnergyFluids#OIL_HEAT_0} ({@code buildcraftenergy:oil_heat_0}, the baseline crude oil —
 * the legacy tree has no plain {@code oil} fluid), looked up lazily so this class can be loaded during registry
 * phases;</li>
 * <li>fluid detection in {@link Spout} keeps the legacy block-identity test (water/lava/fluid block instance)
 * instead of a blanket fluid-state check, so waterlogged decorations do not stop a spout where legacy ones
 * didn't;</li>
 * <li>{@link Spring} places {@link BcBlocks#SPRING_OIL}; the {@code TileSpringOil} behaviour (totalSources,
 * infinite oil) migrates with the energy tile tasks — the worldgen-side block identity is already the baseline
 * one.</li>
 * </ul>
 */
public abstract class OilGenStructurePart {
    public final Box box;
    public final ReplaceType replaceType;

    public OilGenStructurePart(Box containingBox, ReplaceType replaceType) {
        this.box = containingBox;
        this.replaceType = replaceType;
    }

    /** Generates this structure in the world, but only between the given coordinates. */
    public final void generate(LevelAccessor world, Box within) {
        Box intersect = box.getIntersect(within);
        if (intersect != null) {
            generateWithin(world, intersect);
        }
    }

    /** Generates this structure in the world, but only between the given coordinates. */
    protected abstract void generateWithin(LevelAccessor world, Box intersect);

    /**
     * @return The number of oil blocks that this structure will set. Note that this is called *after*
     *         {@link #generateWithin(LevelAccessor, Box)}, by the Spring type, so this can store the number set.
     */
    public abstract int countOilBlocks();

    public void setOilIfCanReplace(LevelAccessor world, BlockPos pos) {
        if (canReplaceForOil(world, pos)) {
            setOil(world, pos);
        }
    }

    public boolean canReplaceForOil(LevelAccessor world, BlockPos pos) {
        return replaceType.canReplace(world, pos);
    }

    // Lazily computed (worldgen time, long after the fluid/block registries fill) — see the class javadoc.
    private static BlockState oilBlockState = null;
    private static Fluid oilFluid = null;

    private static BlockState getOilBlockState() {
        if (oilBlockState == null) {
            oilBlockState = BcEnergyFluids.OIL_HEAT_0.get().defaultFluidState().createLegacyBlock();
            oilFluid = BcEnergyFluids.OIL_HEAT_0.get();
        }
        return oilBlockState;
    }

    /**
     * Set the block to crude oil.
     * We should call {@link LevelAccessor#scheduleTick(BlockPos, Fluid, int)} to make oil flow in
     * {@link WorldGenRegion}. Just like {@link SpringFeature#place(FeaturePlaceContext)}
     *
     * @param world This will be {@link net.minecraft.server.level.ServerLevel} when a player makes an oil well,
     *            while {@link WorldGenRegion} when an oil well generates naturally
     * @param pos The block pos we set to oil block
     */
    public static void setOil(LevelAccessor world, BlockPos pos) {
        world.setBlock(pos, getOilBlockState(), Block.UPDATE_ALL);
        world.scheduleTick(pos, oilFluid, 0);
    }

    /** Legacy {@code BlockUtil.getFluidWithFlowing(Block)} semantics: only water, lava or BC {@link LiquidBlock}s. */
    private static boolean isFluidBlockState(BlockState state) {
        return state.is(Blocks.WATER) || state.is(Blocks.LAVA) || state.getBlock() instanceof LiquidBlock;
    }

    public enum ReplaceType {
        ALWAYS {
            @Override
            public boolean canReplace(LevelAccessor world, BlockPos pos) {
                return true;
            }
        },
        IS_FOR_LAKE {
            @Override
            public boolean canReplace(LevelAccessor world, BlockPos pos) {
                return ALWAYS.canReplace(world, pos);
            }
        };

        public abstract boolean canReplace(LevelAccessor world, BlockPos pos);
    }

    /** Fills every position matching a serialisable predicate (a sphere or a cylinder in practice). */
    public static class GenByPredicate extends OilGenStructurePart {
        public final Predicate<BlockPos> predicate;
        // Only for NBT serialisation of the predicate (see OilStructure).
        public final Object[] predicateArgs;

        public GenByPredicate(Box containingBox, ReplaceType replaceType, Object... predicateArgs) {
            super(containingBox, replaceType);
            this.predicateArgs = predicateArgs;
            Predicate<BlockPos> tester;
            if (predicateArgs.length == 2) {
                // center, radiusSq
                tester = p -> VecUtil.distanceSq(p, (BlockPos) predicateArgs[0]) <= (double) predicateArgs[1];
            } else if (predicateArgs.length == 4) {
                // axis, toReplace, center, radiusSq
                tester = p -> VecUtil.distanceSq(
                    VecUtil.replaceValue(p, (Axis) predicateArgs[0], (int) predicateArgs[1]),
                    (BlockPos) predicateArgs[2]) <= (double) predicateArgs[3];
            } else {
                throw new RuntimeException("Unexpected Predict Type! Predict Types are Limited for NBT Serialize");
            }
            this.predicate = tester;
        }

        @Override
        protected void generateWithin(LevelAccessor world, Box intersect) {
            for (BlockPos pos : BlockPos.betweenClosed(intersect.min(), intersect.max())) {
                if (predicate.test(pos)) {
                    setOilIfCanReplace(world, pos);
                }
            }
        }

        @Override
        public int countOilBlocks() {
            int count = 0;
            for (BlockPos pos : BlockPos.betweenClosed(box.min(), box.max())) {
                if (predicate.test(pos)) {
                    count++;
                }
            }
            return count;
        }
    }

    /** Flat (fixed-y) pattern, positioned absolutely. Legacy oil wells don't emit it, kept for NBT round-trip parity. */
    public static class FlatPattern extends OilGenStructurePart {
        public final boolean[][] pattern;
        public final int depth;

        public FlatPattern(Box containingBox, ReplaceType replaceType, boolean[][] pattern, int depth) {
            super(containingBox, replaceType);
            this.pattern = pattern;
            this.depth = depth;
        }

        public static FlatPattern create(BlockPos start, ReplaceType replaceType, boolean[][] pattern, int depth) {
            BlockPos min = start.offset(0, 1 - depth, 0);
            BlockPos max = start.offset(pattern.length - 1, 0, pattern.length == 0 ? 0 : pattern[0].length - 1);
            Box box = new Box(min, max);
            return new FlatPattern(box, replaceType, pattern, depth);
        }

        @Override
        protected void generateWithin(LevelAccessor world, Box intersect) {
            BlockPos start = box.min();
            for (BlockPos pos : BlockPos.betweenClosed(intersect.min(), intersect.max())) {
                int x = pos.getX() - start.getX();
                int z = pos.getZ() - start.getZ();
                if (pattern[x][z]) {
                    setOilIfCanReplace(world, pos);
                }
            }
        }

        @Override
        public int countOilBlocks() {
            int count = 0;
            for (boolean[] row : pattern) {
                for (int z = 0; z < row.length; z++) {
                    if (row[z]) {
                        count++;
                    }
                }
            }
            return count * depth;
        }
    }

    /** Pattern placed at the terrain height (the surface oil lake + tendrils). */
    public static class PatternTerrainHeight extends OilGenStructurePart {
        public final boolean[][] pattern;
        public final int depth;

        public PatternTerrainHeight(Box containingBox, ReplaceType replaceType, boolean[][] pattern, int depth) {
            super(containingBox, replaceType);
            this.pattern = pattern;
            this.depth = depth;
        }

        public static PatternTerrainHeight create(BlockPos start, ReplaceType replaceType, boolean[][] pattern, int depth) {
            BlockPos min = VecUtil.replaceValue(start, Axis.Y, 1);
            BlockPos max = min.offset(pattern.length - 1, 255, pattern.length == 0 ? 0 : pattern[0].length - 1);
            Box box = new Box(min, max);
            return new PatternTerrainHeight(box, replaceType, pattern, depth);
        }

        @Override
        protected void generateWithin(LevelAccessor world, Box intersect) {
            for (int x = intersect.min().getX(); x <= intersect.max().getX(); x++) {
                int px = x - box.min().getX();

                for (int z = intersect.min().getZ(); z <= intersect.max().getZ(); z++) {
                    int pz = z - box.min().getZ();

                    if (pattern[px][pz]) {
                        BlockPos upper = world.getHeightmapPos(
                            world instanceof WorldGenRegion ? Heightmap.Types.WORLD_SURFACE_WG : Heightmap.Types.WORLD_SURFACE,
                            new BlockPos(x, 0, z)
                        ).below();
                        if (canReplaceForOil(world, upper)) {
                            for (int y = 0; y < 5; y++) {
                                world.setBlock(upper.above(y), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                            }
                            for (int y = 0; y < depth; y++) {
                                setOilIfCanReplace(world, upper.below(y));
                            }
                        }
                    }
                }
            }
        }

        @Override
        public int countOilBlocks() {
            int count = 0;
            for (boolean[] row : pattern) {
                for (int z = 0; z < row.length; z++) {
                    if (row[z]) {
                        count++;
                    }
                }
            }
            return count * depth;
        }
    }

    /** The vertical oil column from the underground reservoir up to the surface (fountains at the top). */
    public static class Spout extends OilGenStructurePart {
        public final BlockPos start;
        public final int radius;
        public final int height;
        private int count = -1;

        public Spout(BlockPos start, ReplaceType replaceType, int radius, int height) {
            super(createBox(start), replaceType);
            this.start = start;
            this.radius = radius;
            this.height = height;
        }

        private static Box createBox(BlockPos start) {
            // Only a block 1 x 256 x 1 -- that way we are only called once.
            return new Box(start, VecUtil.replaceValue(start, Axis.Y, 256));
        }

        @Override
        protected void generateWithin(LevelAccessor world, Box intersect) {
            count = 0;
            int segment = world.getChunk(start).getHighestSectionPosition();
            BlockPos worldTop = new BlockPos(start.getX(), segment + 16, start.getZ());
            for (int y = segment; y >= start.getY(); y--) {
                worldTop = worldTop.below();
                BlockState state = world.getBlockState(worldTop);
                if (world.isEmptyBlock(worldTop)) {
                    continue;
                }
                if (isFluidBlockState(state)) {
                    break;
                }
                if (state.blocksMotion()) {
                    break;
                }
            }
            OilGenStructurePart tubeY = OilGenerator.createTube(start, worldTop.getY() - start.getY(), radius, Axis.Y);
            tubeY.generate(world, tubeY.box);
            count += tubeY.countOilBlocks();
            BlockPos base = worldTop;
            for (int r = radius; r >= 0; r--) {
                OilGenStructurePart struct = OilGenerator.createTube(base, height, r, Axis.Y);
                struct.generate(world, struct.box);
                base = base.offset(0, height, 0);
                count += struct.countOilBlocks();
            }
        }

        @Override
        public int countOilBlocks() {
            if (count < 0) {
                throw new IllegalStateException("Called countOilBlocks before calling generateWithin!");
            }
            return count;
        }
    }

    /** The infinite oil source block placed at the very bottom of large wells. */
    public static class Spring extends OilGenStructurePart {
        public final BlockPos pos;

        public Spring(BlockPos pos) {
            super(new Box(pos, pos), ReplaceType.ALWAYS);
            this.pos = pos;
        }

        @Override
        protected void generateWithin(LevelAccessor world, Box intersect) {
            // Nothing to place per-chunk; OilPlacer calls generate(world, count) once the chunk containing the
            // position generates.
        }

        @Override
        public int countOilBlocks() {
            return 0;
        }

        /** Places {@link BcBlocks#SPRING_OIL}; the {@code TileSpringOil} totalSources wiring migrates with the tile. */
        public void generate(LevelAccessor world, int count) {
            BlockState state = BcBlocks.SPRING_OIL.get().defaultBlockState();
            world.setBlock(pos, state, Block.UPDATE_ALL);
            BlockEntity tile = world.getBlockEntity(pos);
            if (tile != null) {
                // The baseline block carries a buildcraftcore:spring_oil block entity; the behaviour class has not
                // migrated yet, so we can only observe the mismatch here.
                // (Legacy warned about exactly this situation when the BE did not bind.)
            }
            // totalSources = count is stored by TileSpringOil once the energy tile migrates.
        }
    }
}
