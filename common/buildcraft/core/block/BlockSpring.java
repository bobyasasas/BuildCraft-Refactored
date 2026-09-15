package buildcraft.core.block;

import buildcraft.api.blocks.ISpring;
import buildcraft.api.enums.EnumSpring;
import buildcraft.lib.block.BlockBCBase_Neptune;
import buildcraft.lib.misc.data.XorShift128Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiFunction;

public class BlockSpring extends BlockBCBase_Neptune implements EntityBlock, ISpring {
    public final EnumSpring springType;

    public static final XorShift128Random rand = new XorShift128Random();

    public BlockSpring(String idBC, BlockBehaviour.Properties properties, EnumSpring springType) {
        super(idBC, properties);

        this.springType = springType;
    }

    // 1.18.2: no meta

//    @Override

    // BlockState

//    @Override

//    @Override

//    @Override

    // Other

//    @Override

//    @Override

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        generateSpringBlock(world, pos, state);
    }

//    @Override

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        BiFunction<BlockPos, BlockState, BlockEntity> constructor = this.springType.tileConstructor;
        if (constructor != null) {
            return constructor.apply(pos, state);
        }
        return null;
    }

    // @Override

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity p_52509_, ItemStack p_52510_) {
        super.setPlacedBy(world, pos, state, p_52509_, p_52510_);
        world.scheduleTick(pos, this, this.springType.tickRate);
    }

    private void generateSpringBlock(Level world, BlockPos pos, BlockState state) {
        EnumSpring spring = this.springType;
        world.scheduleTick(pos, this, spring.tickRate);
        if (!spring.canGen || spring.liquidBlock == null) {
            return;
        }
        if (!world.isEmptyBlock(pos.above())) {
            return;
        }
        if (spring.chance != -1 && rand.nextInt(spring.chance) != 0) {
            return;
        }
        world.setBlock(pos.above(), spring.liquidBlock, Block.UPDATE_ALL);
    }

    // Prevents updates on chunk generation
    // @Override

    // ISpring

    @Override
    public EnumSpring getType() {
        return springType;
    }
}
