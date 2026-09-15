/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.builders.snapshot;

import buildcraft.lib.BCLib;
import buildcraft.lib.net.MessageManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Legacy counterpart: {@code buildcraft.builders.snapshot.MessageSnapshotResponse} (1.20.1). Delivers the full NBT of a
 * requested snapshot to the client.
 *
 * <p>Wire format field set (identical to legacy): one compound NBT tag holding the whole snapshot
 * ({@code Snapshot.writeToNBT} structure on the sender side).
 */
public class MessageSnapshotResponse implements CustomPacketPayload {

    public static final Type<MessageSnapshotResponse> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(BCLib.MOD_ID, "message_snapshot_response"));

    public static final StreamCodec<FriendlyByteBuf, MessageSnapshotResponse> STREAM_CODEC = CustomPacketPayload.codec(
            MessageSnapshotResponse::write, MessageSnapshotResponse::read);

    /** The full snapshot NBT (legacy built this via {@code Snapshot.writeToNBT(snapshot)}). */
    public final CompoundTag snapshotNbt;

    public MessageSnapshotResponse(CompoundTag snapshotNbt) {
        this.snapshotNbt = snapshotNbt;
    }

    private static void write(MessageSnapshotResponse message, FriendlyByteBuf buf) {
        buf.writeNbt(message.snapshotNbt);
    }

    private static MessageSnapshotResponse read(FriendlyByteBuf buf) {
        CompoundTag nbt = buf.readNbt();
        if (nbt == null) {
            throw new IllegalStateException("MessageSnapshotResponse was sent without a snapshot NBT root");
        }
        return new MessageSnapshotResponse(nbt);
    }

    /** Client-side snapshot cache population. */
    public static void handleClient(MessageSnapshotResponse message, IPayloadContext context) {
        // TODO(M2.6+): port Snapshot#readFromNBT + ClientSnapshots, then replay the legacy handler (decode the NBT into
        // a Snapshot and feed it into the client snapshot cache).
        MessageManager.LOGGER.info(
                "[lib.messages] Received MessageSnapshotResponse ({} NBT entries) - client snapshot cache not migrated yet",
                message.snapshotNbt.size());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
