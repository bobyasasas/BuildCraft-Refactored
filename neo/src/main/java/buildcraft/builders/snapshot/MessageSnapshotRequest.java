/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.builders.snapshot;

import java.util.UUID;
import javax.annotation.Nullable;
import buildcraft.lib.BCLib;
import buildcraft.lib.net.MessageManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Legacy counterpart: {@code buildcraft.builders.snapshot.MessageSnapshotRequest} (1.20.1). Client asks the server for
 * the full snapshot behind a snapshot key (blueprint/library scanning).
 *
 * <p>Wire format field set (identical to legacy {@code Snapshot.Key#writeToByteBuf}):
 * <ul>
 * <li>VARINT-length-prefixed byte[] - hash</li>
 * <li>BOOL - header present</li>
 * <li>if header: recursive key (hash + header flag), LONG+LONG owner uuid, LONG created, VARINT-length UTF name</li>
 * </ul>
 * The {@link Key}/{@link Header} records mirror the legacy {@code Snapshot.Key}/{@code Snapshot.Header} fields; the
 * full snapshot classes migrate in a later task.
 */
public class MessageSnapshotRequest implements CustomPacketPayload {

    public static final Type<MessageSnapshotRequest> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(BCLib.MOD_ID, "message_snapshot_request"));

    public static final StreamCodec<FriendlyByteBuf, MessageSnapshotRequest> STREAM_CODEC = CustomPacketPayload.codec(
            MessageSnapshotRequest::write, MessageSnapshotRequest::read);

    /** Mirrors legacy {@code Snapshot.Key}: hash + optional recursive header. */
    public record Key(byte[] hash, @Nullable Header header) {
        static void write(Key key, FriendlyByteBuf buf) {
            buf.writeByteArray(key.hash);
            buf.writeBoolean(key.header != null);
            if (key.header != null) {
                Header.write(key.header, buf);
            }
        }

        static Key read(FriendlyByteBuf buf) {
            byte[] hash = buf.readByteArray();
            Header header = buf.readBoolean() ? Header.read(buf) : null;
            return new Key(hash, header);
        }
    }

    /** Mirrors legacy {@code Snapshot.Header}: recursive key, owner uuid, creation timestamp, display name. */
    public record Header(Key key, UUID owner, long created, String name) {
        static void write(Header header, FriendlyByteBuf buf) {
            Key.write(header.key, buf);
            buf.writeUUID(header.owner);
            buf.writeLong(header.created);
            buf.writeUtf(header.name);
        }

        static Header read(FriendlyByteBuf buf) {
            // Legacy decoded the name with a raw varint-length byte read (PacketBufferBC#readString); 26.1.2's
            // readUtf consumes the same varint-length layout.
            return new Header(Key.read(buf), buf.readUUID(), buf.readLong(), buf.readUtf());
        }
    }

    public final Key key;

    public MessageSnapshotRequest(Key key) {
        this.key = key;
    }

    private static void write(MessageSnapshotRequest message, FriendlyByteBuf buf) {
        Key.write(message.key, buf);
    }

    private static MessageSnapshotRequest read(FriendlyByteBuf buf) {
        return new MessageSnapshotRequest(Key.read(buf));
    }

    /** Server-side snapshot lookup. */
    public static void handleServer(MessageSnapshotRequest message, IPayloadContext context) {
        // TODO(M2.6+): port GlobalSavedDataSnapshots + Snapshot (server-side snapshot storage), then replay the legacy
        // handler (reply with MessageSnapshotResponse for the found snapshot, nothing when absent).
        MessageManager.LOGGER.info(
                "[lib.messages] Received MessageSnapshotRequest (key hash {}) - snapshot storage not migrated yet",
                java.util.Arrays.toString(message.key.hash()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
