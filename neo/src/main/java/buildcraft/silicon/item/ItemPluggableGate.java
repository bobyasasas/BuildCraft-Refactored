/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.silicon.item;

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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import buildcraft.core.blockentity.KinesisPipeBlockEntity;
import buildcraft.lib.datacomponent.gate.EnumGateMaterial;
import buildcraft.lib.datacomponent.gate.GateVariantData;

/**
 * The M2.11 gate item: right-clicking a slice kinesis pipe attaches this item's gate variant to the clicked face.
 * Legacy counterpart: {@code buildcraft.silicon.item.ItemPluggableGate} (implements {@code IItemPluggable}; the pipe
 * calls {@code #onPlace} when the player uses a pluggable item on a pipe face, creating a
 * {@code buildcraft.silicon.plug.PluggableGate}). The real pipe-holder pluggable system has not migrated, so this
 * slice item attaches the minimal gate ({@code buildcraft.core.gate.BcGateLogic}) directly to the
 * {@link KinesisPipeBlockEntity} — same interaction surface (use item on pipe face), one gate per pipe.
 *
 * <p>The registered id stays exactly {@code buildcraftsilicon:plug_gate_iron_and_no_modifier} (the placeholder item of
 * M2.4 was swapped for this class, M2.11); the remaining 24 gate variant ids are still placeholders. Like legacy, the
 * display name is the plain item translation key — the variant-composed {@code gate.name} keys are GUI/lang content
 * and are not wired here (the lang key set is frozen, M3.5).
 */
public class ItemPluggableGate extends Item {

    private final GateVariantData variant;

    public ItemPluggableGate(Properties properties, GateVariantData variant) {
        super(properties);
        this.variant = variant;
    }

    public GateVariantData getVariant() {
        return this.variant;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.getBlockEntity(pos) instanceof KinesisPipeBlockEntity pipe) {
            if (!level.isClientSide()) {
                Direction face = context.getClickedFace();
                if (pipe.attachGate(face, this.variant)) {
                    Player player = context.getPlayer();
                    if (player == null || !player.getAbilities().instabuild) {
                        context.getItemInHand().shrink(1);
                    }
                    // legacy SoundUtil#playBlockPlace: the gate material block's place sound
                    SoundType soundType = this.materialBlock().defaultBlockState().getSoundType();
                    level.playSound(null, pos, soundType.getPlaceSound(), SoundSource.BLOCKS,
                            (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /** The vanilla block visually associated with the gate material (legacy {@code EnumGateMaterial#block}). */
    private Block materialBlock() {
        return switch (this.variant.material()) {
            case CLAY_BRICK -> Blocks.BRICKS;
            case IRON -> Blocks.IRON_BLOCK;
            case NETHER_BRICK -> Blocks.NETHER_BRICKS;
            case GOLD -> Blocks.GOLD_BLOCK;
        };
    }
}
