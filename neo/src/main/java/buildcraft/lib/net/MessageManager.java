/* Copyright (c) 2026 BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.net;

import buildcraft.builders.snapshot.MessageSnapshotRequest;
import buildcraft.builders.snapshot.MessageSnapshotResponse;
import buildcraft.core.marker.volume.MessageVolumeBoxes;
import buildcraft.lib.net.cache.MessageObjectCacheRequest;
import buildcraft.lib.net.cache.MessageObjectCacheResponse;
import buildcraft.robotics.zone.MessageZoneMapRequest;
import buildcraft.robotics.zone.MessageZoneMapResponse;
import buildcraft.transport.net.MessageMultiPipeItem;
import buildcraft.transport.wire.MessageWireSystems;
import buildcraft.transport.wire.MessageWireSystemsPowered;
import com.mojang.logging.LogUtils;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

/**
 * BuildCraft network hub for NeoForge 26.1.2 (task M2.5), replacing the legacy 1.20.1 SimpleChannel-based
 * {@code buildcraft.lib.net.MessageManager}.
 *
 * <p><b>Registration mechanism (verified against neoforge-26.1.2.109-sources.jar):</b> the mod-bus event
 * {@link RegisterPayloadHandlersEvent} hands out a {@link PayloadRegistrar} via {@code registrar(String version)}; each
 * payload type is registered with {@code playToClient}/{@code playToServer}/{@code playBidirectional}
 * {@code (CustomPacketPayload.Type, StreamCodec<? super RegistryFriendlyByteBuf, T>, IPayloadHandler<T>)} (see
 * {@code PayloadRegistrar} lines 44-82). The version string participates in the Neo-Neo connection handshake.
 *
 * <p><b>Architectural decision (M2.5):</b> unlike legacy (one SimpleChannel per BuildCraft mod, numeric message ids),
 * all 16 payload types share a single <b>buildcraftlib</b> protocol version ("1") and name their
 * {@link CustomPacketPayload.Type} ids in the {@code buildcraftlib} namespace, mirroring how legacy registered the
 * whole message set through the lib-owned MessageManager.
 *
 * <p>Send helpers keep the legacy entry point names ({@link #sendToAll}, {@link #sendTo}, {@link #sendToServer}, ...)
 * on top of the 26.1.2 static {@link PacketDistributor}/{@link ClientPacketDistributor} API.
 */
public final class MessageManager {

    /** The single network protocol version for the whole BuildCraft suite (legacy used one channel per mod). */
    public static final String NET_VERSION = "1";

    public static final Logger LOGGER = LogUtils.getLogger();

    private MessageManager() {}

    /** Registers every BuildCraft payload type. Wired from {@code BCLib}'s mod event bus constructor. */
    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(NET_VERSION);

        // --- buildcraft.lib base messages (8) ---
        registrar.playToClient(MessageContainer.TYPE, MessageContainer.STREAM_CODEC, MessageContainer::handleClient);
        // Bidirectional for the M2.5 handshake demo: legacy only sent requests server-bound (debugger tool), but the
        // login handshake sends it client-bound so the client can echo a MessageDebugResponse back.
        registrar.playBidirectional(
                MessageDebugRequest.TYPE, MessageDebugRequest.STREAM_CODEC,
                MessageDebugRequest::handleServer, MessageDebugRequest::handleClient);
        registrar.playBidirectional(
                MessageDebugResponse.TYPE, MessageDebugResponse.STREAM_CODEC,
                MessageDebugResponse::handleServer, MessageDebugResponse::handleClient);
        registrar.playToClient(MessageMarker.TYPE, MessageMarker.STREAM_CODEC, MessageMarker::handleClient);
        registrar.playBidirectional(
                MessageUpdateEntity.TYPE, MessageUpdateEntity.STREAM_CODEC,
                MessageUpdateEntity::handleServer, MessageUpdateEntity::handleClient);
        registrar.playBidirectional(
                MessageUpdateTile.TYPE, MessageUpdateTile.STREAM_CODEC,
                MessageUpdateTile::handleServer, MessageUpdateTile::handleClient);
        registrar.playToServer(
                MessageObjectCacheRequest.TYPE, MessageObjectCacheRequest.STREAM_CODEC,
                MessageObjectCacheRequest::handleServer);
        registrar.playToClient(
                MessageObjectCacheResponse.TYPE, MessageObjectCacheResponse.STREAM_CODEC,
                MessageObjectCacheResponse::handleClient);

        // --- module messages (8) ---
        registrar.playToServer(
                MessageSnapshotRequest.TYPE, MessageSnapshotRequest.STREAM_CODEC,
                MessageSnapshotRequest::handleServer);
        registrar.playToClient(
                MessageSnapshotResponse.TYPE, MessageSnapshotResponse.STREAM_CODEC,
                MessageSnapshotResponse::handleClient);
        registrar.playToClient(MessageWireSystems.TYPE, MessageWireSystems.STREAM_CODEC, MessageWireSystems::handleClient);
        registrar.playToClient(
                MessageWireSystemsPowered.TYPE, MessageWireSystemsPowered.STREAM_CODEC,
                MessageWireSystemsPowered::handleClient);
        registrar.playToClient(
                MessageMultiPipeItem.TYPE, MessageMultiPipeItem.STREAM_CODEC, MessageMultiPipeItem::handleClient);
        registrar.playToClient(MessageVolumeBoxes.TYPE, MessageVolumeBoxes.STREAM_CODEC, MessageVolumeBoxes::handleClient);
        registrar.playToServer(
                MessageZoneMapRequest.TYPE, MessageZoneMapRequest.STREAM_CODEC, MessageZoneMapRequest::handleServer);
        registrar.playToClient(
                MessageZoneMapResponse.TYPE, MessageZoneMapResponse.STREAM_CODEC, MessageZoneMapResponse::handleClient);

        LOGGER.info("[lib.messages] Registered all 16 BuildCraft payload types (protocol version {})", NET_VERSION);

        // M2.5 handshake demo: server greets every joining player, the client echoes back. Both log lines together
        // prove a full client<->server round trip through the 26.1.2 payload API.
        NeoForge.EVENT_BUS.addListener(MessageManager::onPlayerLoggedIn);
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LOGGER.info("[lib.messages][handshake] server sending MessageDebugRequest to player {}", player.getName().getString());
            MessageManager.sendTo(MessageDebugRequest.forEntity(player.getUUID()), player);
        }
    }

    /** Send this message to everyone. The handler for this message type should be on the CLIENT side. */
    public static void sendToAll(CustomPacketPayload message) {
        PacketDistributor.sendToAllPlayers(message);
    }

    /** Send this message to the specified player. The handler for this message type should be on the CLIENT side. */
    public static void sendTo(CustomPacketPayload message, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, message);
    }

    /**
     * Send this message to everyone within a certain range of a point (legacy
     * {@code PacketDistributor.TargetPoint} overload re-expressed on the 26.1.2 static API).
     */
    public static void sendToAllAround(
            CustomPacketPayload message, ServerLevel level, ServerPlayer excluded,
            double x, double y, double z, double radius) {
        PacketDistributor.sendToPlayersNear(level, excluded, x, y, z, radius, message);
    }

    /** Send this message to everyone within the supplied dimension. */
    public static void sendToDimension(CustomPacketPayload message, ServerLevel level) {
        PacketDistributor.sendToPlayersInDimension(level, message);
    }

    /** Send this message to the server. The handler for this message type should be on the SERVER side. */
    public static void sendToServer(CustomPacketPayload message) {
        // ClientPacketDistributor is client-dist only: callers must invoke this from client code, exactly like the
        // legacy SimpleChannel#sendToServer contract.
        ClientPacketDistributor.sendToServer(message);
    }

    /** Send this message to everyone tracking the given entity. */
    public static void sendToEntity(CustomPacketPayload message, Entity entity) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, message);
    }
}
