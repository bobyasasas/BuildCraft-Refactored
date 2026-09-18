/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.transport.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import buildcraft.transport.BcTransportBlocks;
import buildcraft.transport.block.PipeHolderBlock;
import buildcraft.transport.blockentity.PipeHolderBlockEntity;
import buildcraft.transport.pipe.BcPipeFamilies.Family;
import net.minecraft.world.item.DyeColor;

/**
 * The placed pipe item (legacy {@code buildcraft.transport.item.ItemPipeHolder} slice). Legacy's item was a
 * {@code BlockItem} over the shared {@code pipe_holder} block; here it stays a plain {@link Item} (the M4.3 item model
 * datagen decision: {@code instanceof BlockItem} items without an override would flip to the nonexistent
 * {@code block/<path>} model) and performs the placement itself: it puts down the shared
 * {@link PipeHolderBlock} and then stamps the item's family + colour onto the new
 * {@link PipeHolderBlockEntity} ({@code ItemPipeHolder#onPlace} &rarr; {@code Pipe#createPipe} analogue).
 */
public class ItemPipeHolder extends Item {

    /** This variant's pipe family (immutable, parsed from the registry id at registration). */
    private final Family family;
    /** This variant's colour, or null for the {@code _colorless} variant. */
    @Nullable
    private final DyeColor colour;

    public ItemPipeHolder(Properties properties, Family family, @Nullable DyeColor colour) {
        super(properties);
        this.family = family;
        this.colour = colour;
    }

    public Family getFamily() {
        return this.family;
    }

    @Nullable
    public DyeColor getColour() {
        return this.colour;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockState clicked = level.getBlockState(clickedPos);
        // vanilla BlockItem placement target: replaceable clicked blocks (grass etc.) are replaced, everything else
        // places against the clicked face
        BlockPos placePos = clicked.canBeReplaced() ? clickedPos : clickedPos.relative(face);
        Player player = context.getPlayer();
        if (player != null && !player.mayUseItemAt(placePos, face, context.getItemInHand())) {
            return InteractionResult.FAIL;
        }
        if (!level.getBlockState(placePos).canBeReplaced()) {
            return InteractionResult.FAIL;
        }
        BlockState pipeState = BcTransportBlocks.PIPE_HOLDER.value().defaultBlockState();
        level.setBlock(placePos, pipeState, Block.UPDATE_ALL);
        if (level.getBlockEntity(placePos) instanceof PipeHolderBlockEntity pipe) {
            pipe.setPipe(this.family, this.colour);
        }
        if (!level.isClientSide() && player != null && !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        SoundType sound = pipeState.getSoundType();
        level.playSound(player, placePos, sound.getPlaceSound(), SoundSource.BLOCKS,
            (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        return InteractionResult.SUCCESS;
    }
}
