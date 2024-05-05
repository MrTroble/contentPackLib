package com.troblecodings.contentpacklib;

import java.nio.ByteBuffer;

import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientCustomPacketEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;

public class NetworkContentPackHandler {

    private final FMLEventChannel channel;
    private final String channelName;
    private final ContentPackHandler handler;

    public NetworkContentPackHandler(final String modid, final ContentPackHandler handler) {
        this.channelName = modid + ":CPNet";
        channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(channelName);
        this.handler = handler;
        channel.register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void serverEvent(final ClientCustomPacketEvent event) {
        final ByteBuffer buffer = event.getPacket().payload().nioBuffer();
        final long serverHash = buffer.getLong();
        if (serverHash != handler.getHash()) {
            throw new ContentPackException("Server and Client Packs are not equal!"
                    + " Please check that you have got the same ContentPacks on Client and Server!"
                    + " Server Hash: [" + serverHash + "], Client Hash: [" + handler.getHash()
                    + "]");
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(final PlayerLoggedInEvent event) {
        final ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(handler.getHash());
        sendTo(event.player, buffer);
    }

    private void sendTo(final EntityPlayer player, final ByteBuffer buf) {
        final PacketBuffer buffer = new PacketBuffer(
                Unpooled.copiedBuffer((ByteBuffer) buf.position(0)));
        if (player instanceof EntityPlayerMP) {
            final EntityPlayerMP server = (EntityPlayerMP) player;
            channel.sendTo(new FMLProxyPacket(buffer, channelName), server);
        } else {
            channel.sendToServer(new FMLProxyPacket(new CPacketCustomPayload(channelName, buffer)));
        }
    }
}