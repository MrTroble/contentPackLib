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

public class NetworkContentPackHandler {

    private static final String PROTOCOL_VERSION = "1";

    private final ContentPackHandler handler;
    private final SimpleChannel channel;

    public NetworkContentPackHandler(final String modid, final ContentPackHandler handler) {
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
        ctx.get().enqueueWork(() -> {
            if (msg.hash != handler.getHash()) {
                throw new ContentPackException("Server and Client Hash are not equal!"
                        + " Please check that you have got the same ContentPacks on Client and"
                        + " Server! Server Hash: [" + msg.hash + "], Client Hash: ["
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
