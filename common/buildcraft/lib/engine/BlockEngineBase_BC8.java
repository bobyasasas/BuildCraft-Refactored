/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.engine;


import buildcraft.api.blocks.ICustomRotationHandler;
import buildcraft.api.core.IEngineType;
import buildcraft.lib.block.BlockBCTile_Neptune;
import buildcraft.lib.block.IBlockWithTickableTE;
import buildcraft.lib.client.model.ModelHolderVariable;
import buildcraft.lib.client.model.MutableQuad;
import buildcraft.lib.misc.SpriteUtil;
import buildcraft.lib.tile.TileBC_Neptune;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LazyLoadedValue;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientBlockExtensions;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public abstract class BlockEngineBase_BC8<E extends Enum<E> & IEngineType> extends BlockBCTile_Neptune<TileEngineBase_BC8> implements ICustomRotationHandler, IBlockWithTickableTE<TileEngineBase_BC8> {
    public final E engineType;

    private final BiFunction<BlockPos, BlockState, ? extends TileEngineBase_BC8> engineTileConstructor;

    public BlockEngineBase_BC8(String idBC, BlockBehaviour.Properties props, E type, BiFunction<BlockPos, BlockState, ? extends TileEngineBase_BC8> engineTileConstructor) {
        super(idBC, props);
        this.engineType = type;
        this.engineTileConstructor = engineTileConstructor;
    }

    // Engine directly related methods



    @Nonnull
    public ItemStack getStack() {
        return new ItemStack(this, 1);
    }




    // BlockState

//    @Override

//    @Override

//    @Override

    // Misc Block Overrides

//    @Override

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter world, BlockPos pos) {
        return true;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter world, BlockPos pos) {
        return 1.0F;
    }

//    @Override

//    @Override

//    @Override

//    @Override

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    private static final VoxelShape BASE_U = Block.box(0, 0, 0, 16, 4, 16);
    private static final VoxelShape TRUNK_U = Block.box(4, 4, 4, 12, 16, 12);
    private static final VoxelShape UP = Shapes.or(BASE_U, TRUNK_U);
    private static final VoxelShape BASE_D = Block.box(0, 12, 0, 16, 16, 16);
    private static final VoxelShape TRUNK_D = Block.box(4, 0, 4, 12, 12, 12);
    private static final VoxelShape DOWN = Shapes.or(BASE_D, TRUNK_D);
    private static final VoxelShape BASE_E = Block.box(0, 0, 0, 4, 16, 16);
    private static final VoxelShape TRUNK_E = Block.box(4, 4, 4, 16, 12, 12);
    private static final VoxelShape EAST = Shapes.or(BASE_E, TRUNK_E);
    private static final VoxelShape BASE_W = Block.box(12, 0, 0, 16, 16, 16);
    private static final VoxelShape TRUNK_W = Block.box(0, 4, 4, 12, 12, 12);
    private static final VoxelShape WEST = Shapes.or(BASE_W, TRUNK_W);
    private static final VoxelShape BASE_N = Block.box(0, 0, 12, 16, 16, 16);
    private static final VoxelShape TRUNK_N = Block.box(4, 4, 0, 12, 12, 12);
    private static final VoxelShape NORTH = Shapes.or(BASE_N, TRUNK_N);
    private static final VoxelShape BASE_S = Block.box(0, 0, 0, 16, 16, 4);
    private static final VoxelShape TRUNK_S = Block.box(4, 4, 4, 12, 12, 16);
    private static final VoxelShape SOUTH = Shapes.or(BASE_S, TRUNK_S);

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (world.getBlockEntity(pos) instanceof TileEngineBase_BC8 engine) {
            return switch (engine.currentDirection) {
                case DOWN -> DOWN;
                case UP -> UP;
                case WEST -> WEST;
                case EAST -> EAST;
                case SOUTH -> SOUTH;
                case NORTH -> NORTH;
            };
        }
        return Shapes.block();
    }

    @Override
    public TileBC_Neptune newBlockEntity(BlockPos pos, BlockState state) {
        BiFunction<BlockPos, BlockState, ? extends TileEngineBase_BC8> constructor = this.engineTileConstructor;
        if (constructor == null) {
            return null;
        }
        TileEngineBase_BC8 tile = constructor.apply(pos, state);
        return tile;
    }

//    @Override

//    @Override

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean p_60514_) {
        super.neighborChanged(state, world, pos, block, fromPos, p_60514_);
        if (world.isClientSide) return;
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof TileEngineBase_BC8) {
            TileEngineBase_BC8 engine = (TileEngineBase_BC8) tile;
            engine.rotateIfInvalid();
        }
    }

    // ICustomRotationHandler

    @Override
    public InteractionResult attemptRotation(Level world, BlockPos pos, BlockState state, Direction sideWrenched) {
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof TileEngineBase_BC8) {
            TileEngineBase_BC8 engine = (TileEngineBase_BC8) tile;
            return engine.attemptRotation();
        }
        return InteractionResult.FAIL;
    }

    public static final Map<IEngineType, ModelHolderVariable> engineModels = new HashMap<>();

    @OnlyIn(Dist.CLIENT)
    public static void setModel(IEngineType engineType, ModelHolderVariable model) {
        engineModels.put(engineType, model);
    }

    private static final Map<IEngineType, LazyLoadedValue<TextureAtlasSprite>> engineParticles = new HashMap<>();

    @OnlyIn(Dist.CLIENT)
    private TextureAtlasSprite getEngineParticle(IEngineType engineType) {
        return engineParticles.computeIfAbsent(engineType, (e) ->
                new LazyLoadedValue<>(
                        () ->
                        {
                            for (MutableQuad quad : engineModels.get(e).getCutoutQuads()) {
                                if (quad.getFace() == Direction.DOWN) {
                                    return quad.getSprite();
                                }
                            }
                            return SpriteUtil.missingSprite().get();
                        }
                )

        ).get();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initializeClient(Consumer<IClientBlockExtensions> consumer) {
        consumer.accept(new IClientBlockExtensions() {
            @Override
            public boolean addHitEffects(BlockState state, Level worldIn, HitResult hitIn, ParticleEngine manager) {
                if (hitIn.getType() != HitResult.Type.BLOCK) {
                    return false;
                }
                BlockHitResult target = (BlockHitResult) hitIn;
                ClientLevel world = (ClientLevel) worldIn;
                BlockEntity te = world.getBlockEntity(target.getBlockPos());
                if (te instanceof TileEngineBase_BC8) {
                    double x = Math.random();
                    double y = Math.random();
                    double z = Math.random();

                    x += target.getLocation().x;
                    y += target.getLocation().y;
                    z += target.getLocation().z;

                    TerrainParticle particle = new TerrainParticle(world, x, y, z, 0, 0, 0, state);
                    particle.setPos(x, y, z);
                    TextureAtlasSprite texture = getEngineParticle(engineType);
                    if (texture == null) {
                        return false;
                    }
                    particle.setSprite(texture);
                    particle.setPower(0.2F);
                    particle.scale(0.6F);
                    manager.add(particle);

                    return true;
                }

                return false;
            }

            @Override
            public boolean addDestroyEffects(BlockState state, Level worldIn, BlockPos pos, ParticleEngine manager) {
                ClientLevel world = (ClientLevel) worldIn;
                BlockEntity te = world.getBlockEntity(pos);
                if (te instanceof TileEngineBase_BC8) {
                    int countX = 2;
                    int countY = 2;
                    int countZ = 2;

                    TextureAtlasSprite texture = getEngineParticle(engineType);
                    if (texture == null) {
                        return false;
                    }

                    for (int x = 0; x < countX; x++) {
                        for (int y = 0; y < countY; y++) {
                            for (int z = 0; z < countZ; z++) {
                                double _x = pos.getX() + 0.5;
                                double _y = pos.getY() + 0.5;
                                double _z = pos.getZ() + 0.5;

                                TerrainParticle particle = new TerrainParticle(world, _x, _y, _z, 0, 0, 0, state);
                                particle.setPos(_x, _y, _z);
                                particle.setSprite(texture);
                                manager.add(particle);
                            }
                        }
                    }
                    return true;
                }
                return false;
            }
        });
    }
}
