package com.troblecodings.contentpacklib;

import java.nio.ByteBuffer;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.EventNetworkChannel;
import net.minecraftforge.network.PacketDistributor;

/**
 * In 1.20.4 ist der Custom-Payload-Stack komplett umgebaut. {@code NetworkRegistry.newEventChannel}
 * existiert nicht mehr -- Channels werden via {@link ChannelBuilder} aufgebaut, das Event heisst
 * {@link CustomPayloadEvent} (Server-/Client-Spaltung entfaellt) und versendet wird ueber
 * {@code channel.send(buf, PacketDistributor.PLAYER.with(...))}. Der Inhalt bleibt unveraendert
 * ein 8-Byte-Hash, der beim Login vom Server zum Client geschickt und auf Gleichheit geprueft wird.
 */
public class NetworkContentPackHandler {

    private final EventNetworkChannel channel;
    private final ContentPackHandler handler;

    public NetworkContentPackHandler(final String modid, final ContentPackHandler handler) {
        final ResourceLocation channelName = new ResourceLocation(modid, "contentpackhandler");
        this.channel = ChannelBuilder.named(channelName)
                .optional()
                .eventNetworkChannel();
        this.handler = handler;
        channel.registerObject(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onPayload(final CustomPayloadEvent event) {
        final ByteBuffer buffer = event.getPayload().nioBuffer();
        final long serverHash = buffer.getLong();
        if (serverHash != handler.getHash()) {
            throw new IllegalArgumentException("Server and Client Hash are not equal!"
                    + " Please check that you have got the same ContentPacks on Client and Server!"
                    + " Server Hash: [" + serverHash + "], Client Hash: [" + handler.getHash()
                    + "]");
        }
        event.getSource().setPacketHandled(true);
    }

    @SubscribeEvent
    public void onPlayerJoin(final PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer server)) {
            return;
        }
        final ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(handler.getHash());
        final FriendlyByteBuf friendly = new FriendlyByteBuf(
                Unpooled.copiedBuffer(buffer.position(0)));
        channel.send(friendly, PacketDistributor.PLAYER.with(server));
    }
}
