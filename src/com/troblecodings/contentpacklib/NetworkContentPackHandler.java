package com.troblecodings.contentpacklib;

import java.nio.ByteBuffer;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkEvent.ServerCustomPayloadEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.event.EventNetworkChannel;

public class NetworkContentPackHandler {

    private final EventNetworkChannel channel;
    private final ResourceLocation channelName;
    private final ContentPackHandler handler;

    public NetworkContentPackHandler(final String modid, final ContentPackHandler handler) {
        this.channelName = new ResourceLocation(modid, "contentpackhandler");
        this.channel = NetworkRegistry.newEventChannel(channelName, () -> modid,
                modid::equalsIgnoreCase, modid::equalsIgnoreCase);
        this.handler = handler;
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
        sendTo(event.getEntity(), buffer);
    }

    private void sendTo(final Player player, final ByteBuffer buf) {
        final FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.copiedBuffer(buf.position(0)));
        if (player instanceof ServerPlayer) {
            final ServerPlayer server = (ServerPlayer) player;
            server.connection.send(new ClientboundCustomPayloadPacket(channelName, buffer));
        } else {
            final Minecraft mc = Minecraft.getInstance();
            mc.getConnection().send(new ServerboundCustomPayloadPacket(channelName, buffer));
        }
    }
}
