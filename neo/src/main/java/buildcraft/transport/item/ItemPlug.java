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
import net.minecraft.world.level.block.SoundType;
import buildcraft.core.block.KinesisPipeBlock;
import buildcraft.core.blockentity.KinesisPipeBlockEntity;
import buildcraft.transport.blockentity.PipeHolderBlockEntity;

/**
 * The plug attach items (legacy {@code buildcraft.transport.item.ItemPluggableSimple} slice):
 * {@code plug_blocker} and {@code plug_power_adaptor}. Right-clicking a pipe attaches the plug to the clicked face
 * (legacy {@code ItemPluggableSimple#onItemUseFirst} &rarr; {@code Pipe#addPluggable}); the plug replaces whatever
 * occupied the face before, exactly like legacy. The plug is rendered by the pipe BER (the static cutout layer),
 * so no separate block or block entity is created.
 */
public class ItemPlug extends Item {

    /** The pluggable type this item attaches ({@code PipeHolderBlockEntity#PLUG_*}). */
    private final byte plugType;

    public ItemPlug(Properties properties, byte plugType) {
        super(properties);
        this.plugType = plugType;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        if (level.getBlockEntity(pos) instanceof PipeHolderBlockEntity pipe) {
            if (pipe.getPlug(face) == this.plugType) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                pipe.attachPlug(face, this.plugType);
                if (player == null || !player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                SoundType sound = pipe.getBlockState().getSoundType();
                level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS,
                    (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
            }
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof KinesisPipeBlockEntity kinesis
                && level.getBlockState(pos).getBlock() instanceof KinesisPipeBlock) {
            // the kinesis slice pipe keeps its single-slot gate host; plugs there are not migrated
            return InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }
}
