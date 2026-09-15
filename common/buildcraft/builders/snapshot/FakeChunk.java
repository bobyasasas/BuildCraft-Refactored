package buildcraft.builders.snapshot;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

public class FakeChunk extends LevelChunk {
    final Level level;

    public FakeChunk(Level world, ChunkPos chunkPos) {
        super(world, chunkPos);
        this.level = world;
    }

    @Override
    public void addAndRegisterBlockEntity(BlockEntity p_156391_) {
        this.setBlockEntity(p_156391_);
    }

    @Override
    public void setBlockEntity(BlockEntity p_156374_) {
        BlockPos blockpos = p_156374_.getBlockPos();
        if (this.getBlockState(blockpos).hasBlockEntity()) {
            p_156374_.setLevel(this.level);
            BlockEntity blockentity = this.blockEntities.put(blockpos.immutable(), p_156374_);
        }
    }
}
