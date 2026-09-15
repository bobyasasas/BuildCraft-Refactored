/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.block;

import buildcraft.api.properties.BuildCraftProperties;
import buildcraft.lib.registry.TagManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.common.Tags;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class BlockBCBase_Neptune extends Block {
    public static final Property<Direction> PROP_FACING = BuildCraftProperties.BLOCK_FACING;
    public static final Property<Direction> BLOCK_FACING_6 = BuildCraftProperties.BLOCK_FACING_6;

    /** The tag used to identify this in the {@link TagManager}. Note that this may be empty if this block doesn't use
     * the tag system. */
    public final String idBC;

    /** @param idBC The ID that will be looked up in the {@link TagManager} when registering blocks. Pass null or the
     *            empty string to bypass the {@link TagManager} entirely. */
    public BlockBCBase_Neptune(String idBC, Properties props) {
        super(props);
        if (idBC == null) {
            idBC = "";
        }
        this.idBC = idBC;

//        // Sensible default block properties

        if (!idBC.isEmpty()) {
            // Init names from the tag manager
            String unlocalizedName = TagManager.getTag(idBC, TagManager.EnumTagType.UNLOCALIZED_NAME);
            if (unlocalizedName.startsWith("buildcraft.christmas.")) {
                unlocalizedName = unlocalizedName.replace("buildcraft.christmas.", "buildcraft.christmas.tile.") + ".name";
            } else {
                unlocalizedName = "tile." + unlocalizedName + ".name";
            }
            setUnlocalizedName(unlocalizedName);
        }

        if (this instanceof IBlockWithFacing) {
            Property<Direction> facingProp = ((IBlockWithFacing) this).getFacingProperty();
            registerDefaultState(
                    defaultBlockState()
                            .setValue(facingProp, Direction.NORTH)
            );
        }
    }

    // IBlockState

    protected void addProperties(List<Property<?>> properties) {
        if (this instanceof IBlockWithFacing) {
            properties.add(((IBlockWithFacing) this).getFacingProperty());
        }
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        List<Property<?>> properties = new ArrayList<>();
        addProperties(properties);
        builder.add(properties.toArray(new Property<?>[0]));
    }

//    @Override

//    @Override

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        if (this instanceof IBlockWithFacing) {
            Property<Direction> prop = ((IBlockWithFacing) this).getFacingProperty();
            Direction facing = state.getValue(prop);
            state = state.setValue(prop, rot.rotate(facing));
        }
        return state;
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        if (this instanceof IBlockWithFacing) {
            Property<Direction> prop = ((IBlockWithFacing) this).getFacingProperty();
            Direction facing = state.getValue(prop);
            state = state.setValue(prop, mirror.mirror(facing));
        }
        return state;
    }

    @Override
    public BlockState rotate(BlockState state, LevelAccessor world, BlockPos pos, Rotation direction) {
        if (this instanceof IBlockWithFacing) {
            if (!((IBlockWithFacing) this).canBeRotated(world, pos, world.getBlockState(pos))) {
                return state;
            }
        }
        return super.rotate(world.getBlockState(pos), world, pos, direction);
    }

    // Others

    @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        LivingEntity placer = context.getPlayer();
        BlockState state = super.getStateForPlacement(context);
        if (this instanceof IBlockWithFacing) {
            Direction orientation = placer.getDirection();
            IBlockWithFacing b = (IBlockWithFacing) this;
            if (b.canFaceVertically()) {
                if (Mth.abs((float) placer.getX() - pos.getX()) < 2.0F
                        && Mth.abs((float) placer.getZ() - pos.getZ()) < 2.0F) {
                    double y = placer.getY() + placer.getEyeHeight();

                    if (y - pos.getY() > 2.0D) {
                        orientation = Direction.DOWN;
                    }

                    if (pos.getY() - y > 0.0D) {
                        orientation = Direction.UP;
                    }
                }
            }
            state = state.setValue(b.getFacingProperty(), orientation.getOpposite());
        }
        return state;
    }

    public static boolean isExceptBlockForAttachWithPiston(Block attachBlock) {
        return isExceptionBlockForAttaching(attachBlock) || attachBlock == Blocks.PISTON || attachBlock == Blocks.STICKY_PISTON || attachBlock == Blocks.PISTON_HEAD;
    }

    protected static boolean isExceptionBlockForAttaching(Block attachBlock) {
        return attachBlock instanceof ShulkerBoxBlock || attachBlock instanceof LeavesBlock || attachBlock instanceof TrapDoorBlock || attachBlock == Blocks.BEACON || attachBlock == Blocks.CAULDRON || attachBlock == Blocks.GLASS || attachBlock == Blocks.GLOWSTONE || attachBlock == Blocks.ICE || attachBlock == Blocks.SEA_LANTERN || attachBlock.builtInRegistryHolder().is(Tags.Blocks.STAINED_GLASS);
    }

    // in 1.18.2 setUnlocalizedName setRegistryName are unvailable
    @Override
    public String getDescriptionId() {
        return this.unlocalizedName;
    }

    private String unlocalizedName;

    public void setUnlocalizedName(String unlocalizedName) {
        this.unlocalizedName = unlocalizedName;
    }

    // should be called where we want, not by mc
    public BlockState getActualState(BlockState state, LevelAccessor world, BlockPos pos, BlockEntity tile) {
        return state;
    }


    /**
     * To call {@link #getActualState(BlockState, LevelAccessor, BlockPos, BlockEntity)} and update BlockState if required.
     * @param state
     * @param world
     * @param pos
     * @param tile Whether null is allowed depends on how {@link #getActualState(BlockState, LevelAccessor, BlockPos, BlockEntity)} overrides.
     */
    public void checkActualStateAndUpdate(BlockState state, Level world, BlockPos pos, @Nullable BlockEntity tile) {
        BlockState newState = getActualState(state, world, pos, tile);
        if (!newState.equals(state)) {
            world.setBlockAndUpdate(pos, newState);
        }
    }

    public ResourceLocation getRegistryName() {
        return this.builtInRegistryHolder().key().location();
    }
}
