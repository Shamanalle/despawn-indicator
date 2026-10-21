package com.kasper.despawnindicator.forge;

import com.kasper.despawnindicator.DespawnIndicator;
import com.kasper.despawnindicator.client.ClientTicker;
import com.kasper.despawnindicator.server.ServerClocks;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.event.EventNetworkChannel;

/**
 * The two channels on Forge: the same names and bytes as on Fabric, NeoForge and Paper, so a Forge client works
 * with any of those servers and the other way round. Either side may lack the mod.
 */
@SuppressWarnings("removal") // new ResourceLocation(..), which is what Forge 1.20.1 has
public final class ForgeNetworking {

    public static final ResourceLocation HELLO = new ResourceLocation(DespawnIndicator.MOD_ID, DespawnIndicator.CHANNEL_HELLO);
    public static final ResourceLocation CLOCKS = new ResourceLocation(DespawnIndicator.MOD_ID, DespawnIndicator.CHANNEL_CLOCKS);

    private ForgeNetworking() {
    }

    private static EventNetworkChannel channel(ResourceLocation name) {
        return NetworkRegistry.newEventChannel(name, () -> "1", version -> true, version -> true);
    }

    static void register() {
        channel(HELLO).addListener((NetworkEvent.ClientCustomPayloadEvent e) -> {
            NetworkEvent.Context context = e.getSource().get();
            ServerPlayer player = context.getSender();
            byte[] data = read(e.getPayload());
            if (player != null && data != null) {
                context.enqueueWork(() -> ServerClocks.hello(player, data));
            }
            context.setPacketHandled(true);
        });
        channel(CLOCKS).addListener((NetworkEvent.ServerCustomPayloadEvent e) -> {
            NetworkEvent.Context context = e.getSource().get();
            byte[] data = read(e.getPayload());
            if (data != null) {
                context.enqueueWork(() -> ClientTicker.receive(data));
            }
            context.setPacketHandled(true);
        });
    }

    private static byte[] read(FriendlyByteBuf buf) {
        if (buf == null) {
            return null;
        }
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return data;
    }

    public static FriendlyByteBuf write(byte[] data) {
        return new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
    }
}
