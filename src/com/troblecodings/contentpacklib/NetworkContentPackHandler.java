package com.troblecodings.contentpacklib;

import java.util.function.Supplier;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

/**
 * Schickt beim Login des Spielers den Content-Pack-Hash vom Server zum
 * Client. Hat der Client einen abweichenden Hash (z.B. anderes Pack
 * installiert), wird die Verbindung mit einer aussagekraeftigen
 * IllegalArgumentException abgebrochen.
 *
 * 1.14.4-Port der 1.12.2-{@code FMLEventChannel}-Variante; jetzt mit
 * Forge {@link SimpleChannel} und {@link PacketDistributor}.
 */
public class NetworkContentPackHandler {

    private static final String PROTOCOL_VERSION = "1";

    private final FileReader handler;
    private final SimpleChannel channel;

    public NetworkContentPackHandler(final String modid, final FileReader handler) {
        this.handler = handler;
        this.channel = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(modid, "cpnet"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals);
        this.channel.registerMessage(0, HashPacket.class, HashPacket::encode, HashPacket::decode,
                this::handleHash);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            final ServerPlayerEntity sp = (ServerPlayerEntity) event.getPlayer();
            channel.send(PacketDistributor.PLAYER.with(() -> sp), new HashPacket(handler.getHash()));
        }
    }

    private void handleHash(final HashPacket msg, final Supplier<NetworkEvent.Context> ctx) {
        // Auf der Client-Seite: vergleichen mit lokalem Hash.
        ctx.get().enqueueWork(() -> {
            if (msg.hash != handler.getHash()) {
                throw new IllegalArgumentException(
                        "Server and Client content-pack hashes are not equal! "
                                + "Please ensure that you have the same Content Packs on Client "
                                + "and Server. Server hash: [" + msg.hash + "], Client hash: ["
                                + handler.getHash() + "]");
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static final class HashPacket {
        public final long hash;

        public HashPacket(final long hash) {
            this.hash = hash;
        }

        public static void encode(final HashPacket msg, final PacketBuffer buf) {
            buf.writeLong(msg.hash);
        }

        public static HashPacket decode(final PacketBuffer buf) {
            return new HashPacket(buf.readLong());
        }
    }
}
