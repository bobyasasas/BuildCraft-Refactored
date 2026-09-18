/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package buildcraft.transport.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import buildcraft.transport.net.MessageMultiPipeItem;

/**
 * Client-side mirror of the items travelling through item pipes (M4.6 "流动可见" slice), the counterpart of legacy
 * {@code PipeFlowItems}' client state. {@code MessageMultiPipeItem} batches arrive from
 * {@code MessageMultiPipeItem#clientHandler} (installed by {@code BuildCraftTransportClient}); every entry in a batch
 * <b>replaces</b> the mirror for that pipe position, exactly like the legacy handler — a pipe that went empty sends one
 * empty list, clearing its mirror.
 *
 * <p>Animation is local: when a batch arrives the visuals for that pipe are stamped with the current client game time,
 * and the renderer interpolates each item along its current half-leg ({@link #LEG_TICKS}, matching the server's
 * 2-tick half travel plus one tick of network slack) between the face centre and the pipe centre, so movement stays
 * smooth between the every-leg sync batches.
 *
 * <p>Simplification vs legacy ({@code BuildCraftObjectCaches}): the wire format carries the vanilla registry id of the
 * item (both ends agree on the numeric ids), so travelling items lose their data components — plain items travel
 * pixel-perfect, component-carrying stacks arrive as their plain form.
 */
public final class PipeItemFlowClient {

    /** Client-side half-leg duration, in ticks (server {@code HALF_TRAVEL_TICKS} + network slack). */
    public static final float LEG_TICKS = 3.0f;

    /** One travelling item on the client: what to draw and where it is along its half-leg. */
    public static final class ItemVisual {
        public final ItemStack stack;
        /** The face the item travels along (entry face while {@link #toCenter}, exit face afterwards). */
        public final Direction side;
        /** True while the item moves from the {@link #side} face towards the pipe centre. */
        public final boolean toCenter;
        /** The client game time the visual was received at (interpolation start). */
        public final long receivedTick;

        ItemVisual(ItemStack stack, Direction side, boolean toCenter, long receivedTick) {
            this.stack = stack;
            this.side = side;
            this.toCenter = toCenter;
            this.receivedTick = receivedTick;
        }
    }

    private static final Map<BlockPos, List<ItemVisual>> ITEMS = new HashMap<>();

    private PipeItemFlowClient() {}

    /** The current travelling-item visuals for one pipe (never null; empty when the pipe holds nothing). */
    public static List<ItemVisual> get(BlockPos pos) {
        List<ItemVisual> visuals = ITEMS.get(pos);
        return visuals == null ? List.of() : visuals;
    }

    /** Applies one batch (the {@code MessageMultiPipeItem#clientHandler} target, client thread). */
    public static void handle(MessageMultiPipeItem message, @Nullable ClientLevel level) {
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        for (Map.Entry<BlockPos, List<MessageMultiPipeItem.TravellingItemData>> entry : message.items.entrySet()) {
            List<ItemVisual> visuals = new ArrayList<>(entry.getValue().size());
            for (MessageMultiPipeItem.TravellingItemData data : entry.getValue()) {
                ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.byId(data.stackId), data.stackCount);
                visuals.add(new ItemVisual(stack, data.side, data.toCenter, now));
            }
            ITEMS.put(entry.getKey(), visuals);
        }
    }
}
