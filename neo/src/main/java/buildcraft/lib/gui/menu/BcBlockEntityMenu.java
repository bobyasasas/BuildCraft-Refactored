/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.lib.gui.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * Minimal 26.1.2 GUI framework base (task M4.8) for the machine menus that are bound to one block entity: the menu
 * carries the {@linkplain #pos menu position} that the NeoForge {@code player.openMenu(MenuProvider, BlockPos)} extra
 * data wrote, plus the block entity itself &mdash; always present on the server side, and re-resolved through
 * {@code level().getBlockEntity(pos)} on the client side (so it can be null before the chunk finishes loading, or after
 * the block was broken mid-session).
 *
 * <p>This is the 26.1.2 replacement for the legacy 1.20.1 direct-drawing GUI stack's
 * {@code ContainerBCTile<TileBC_Neptune>}: only the small capability surface the migrated machine menus actually share
 * (position plumbing, the still-valid check and the server/client constructor split) is provided here, deliberately
 * without the legacy widget/ledger machinery. Menus add their own slots with the vanilla helpers
 * ({@link AbstractContainerMenu#addStandardInventorySlots}) and sync their display state with {@code ContainerData}.
 */
public abstract class BcBlockEntityMenu<B extends BlockEntity> extends AbstractContainerMenu {

    /** The bound block entity; never null on the server side, possibly null on the client side (see class javadoc). */
    private final @Nullable B blockEntity;
    /** The position the bound block entity sits at (immutable copy of the position sent through the menu extra data). */
    public final BlockPos pos;

    protected BcBlockEntityMenu(MenuType<?> type, int id, @Nullable B blockEntity, BlockPos pos) {
        super(type, id);
        this.blockEntity = blockEntity;
        this.pos = pos.immutable();
    }

    /** The bound block entity, or null on the client when it could not be (yet) re-resolved from {@link #pos}. */
    public final @Nullable B blockEntityOrNull() {
        return this.blockEntity;
    }

    /** The bound block entity; only call from code paths guaranteed to have it (server side, or after a null check). */
    public final B blockEntity() {
        if (this.blockEntity == null) {
            throw new IllegalStateException("Menu " + this.getType() + " lost its block entity at " + this.pos);
        }
        return this.blockEntity;
    }

    /** The vanilla distance check ({@code Container#stillValidBlockEntity}) against the bound block entity. */
    protected final boolean stillValidBlockEntity(Player player) {
        return this.blockEntity != null && Container.stillValidBlockEntity(this.blockEntity, player);
    }
}
