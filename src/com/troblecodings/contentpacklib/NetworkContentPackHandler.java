package com.troblecodings.contentpacklib;

import java.nio.ByteBuffer;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CCustomPayloadPacket;
import net.minecraft.network.play.server.SCustomPayloadPlayPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.network.NetworkEvent.ServerCustomPayloadEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.event.EventNetworkChannel;

public class NetworkContentPackHandler {
    private final EventNetworkChannel channel;
    private final ResourceLocation channelName;
    private final ContentPackHandler handler;

    public NetworkContentPackHandler(final String modid,
            final ContentPackHandler contentPackHandler) {
        this.channelName = new ResourceLocation(modid, "contentpackhandler");
        this.channel = NetworkRegistry.newEventChannel(channelName, () -> modid,
                modid::equalsIgnoreCase, modid::equalsIgnoreCase);
        this.handler = contentPackHandler;
        channel.registerObject(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void serverEvent(final ServerCustomPayloadEvent event) {
        final ByteBuffer buffer = event.getPayload().nioBuffer();
        final long serverHash = buffer.getLong();
        if (serverHash != handler.getHash()) {
            throw new IllegalArgumentException("Server and Client Hash are not equal!"
                    + " Please check that you have got the same ContentPacks on Client and Server!"
                    + " Server Hash: [" + serverHash + "], Client Hash: [" + handler.getHash()
                    + "]");
        }
        event.getSource().get().setPacketHandled(true);
    }

    @SubscribeEvent
    public void onPlayerJoin(final PlayerLoggedInEvent event) {
        final ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(handler.getHash());
        sendTo(event.getPlayer(), buffer);
    }

    private void sendTo(final PlayerEntity player, final ByteBuffer buf) {
        final PacketBuffer buffer = new PacketBuffer(Unpooled.copiedBuffer(buf.array()));
        if (player instanceof ServerPlayerEntity) {
            final ServerPlayerEntity server = (ServerPlayerEntity) player;
            server.connection.send(new SCustomPayloadPlayPacket(channelName, buffer));
        } else {
            final Minecraft mc = Minecraft.getInstance();
            mc.getConnection().send(new CCustomPayloadPacket(channelName, buffer));
        }
    }
}
