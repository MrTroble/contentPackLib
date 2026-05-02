package com.troblecodings.contentpacklib;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * NeoForge 1.21 ersetzt das Forge-Channel-System komplett: Payloads sind {@link
 * CustomPacketPayload}-Records, registriert ueber {@link RegisterPayloadHandlersEvent}.
 * Wir packen den 8-Byte-Hash in einen Payload-Record, registrieren beim Player-Login einen
 * Send vom Server an den Client und vergleichen client-seitig den Hash mit unserem lokalen.
 */
public class NetworkContentPackHandler {

    private final ContentPackHandler handler;
    private final CustomPacketPayload.Type<HashPayload> payloadType;

    public NetworkContentPackHandler(final String modid, final ContentPackHandler handler,
            final IEventBus modBus) {
        this.handler = handler;
        this.payloadType = new CustomPacketPayload.Type<>(
                Identifier.fromNamespaceAndPath(modid, "contentpackhandler"));
        modBus.register(this);
        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);
    }

    @SubscribeEvent
    public void onRegisterPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        final IPayloadHandler<HashPayload> clientHandler = (payload, ctx) -> verify(payload);
        registrar.playToClient(payloadType,
                StreamCodec.of(
                        (buf, pl) -> buf.writeLong(pl.hash),
                        buf -> new HashPayload(buf.readLong(), payloadType)),
                clientHandler);
    }

    private void verify(final HashPayload payload) {
        if (payload.hash != handler.getHash()) {
            throw new IllegalArgumentException("Server and Client Hash are not equal!"
                    + " Please check that you have got the same ContentPacks on Client and Server!"
                    + " Server Hash: [" + payload.hash + "], Client Hash: [" + handler.getHash()
                    + "]");
        }
    }

    private void onPlayerJoin(final PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer server)) {
            return;
        }
        PacketDistributor.sendToPlayer(server, new HashPayload(handler.getHash(), payloadType));
    }

    /**
     * Custom-Payload-Record mit 8-Byte-Hash. Der Type wird zur Konstruktionszeit (im
     * NetworkContentPackHandler-Ctor) gebaut, damit der modid-spezifische Identifier
     * pro Instanz korrekt ist und mehrere Mods, die contentpacklib einbinden, kollisionsfrei
     * koexistieren.
     */
    public static final class HashPayload implements CustomPacketPayload {

        private final long hash;
        private final CustomPacketPayload.Type<HashPayload> type;

        HashPayload(final long hash, final CustomPacketPayload.Type<HashPayload> type) {
            this.hash = hash;
            this.type = type;
        }

        public long hash() {
            return hash;
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return type;
        }
    }
}
