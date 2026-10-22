package com.kasper.despawnindicator;

import com.kasper.despawnindicator.server.ServerClocks;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * The two channels on Fabric 1.20.2 - 1.20.4 (channels by name, before payloads). A packet is the message's bytes
 * as they are, the same bytes as on Forge, NeoForge and the Bukkit plugin.
 */
public final class ModNetworking {

    public static final ResourceLocation HELLO = new ResourceLocation(DespawnIndicator.MOD_ID, DespawnIndicator.CHANNEL_HELLO);
    public static final ResourceLocation CLOCKS = new ResourceLocation(DespawnIndicator.MOD_ID, DespawnIndicator.CHANNEL_CLOCKS);

    private ModNetworking() {
    }

    /** Registers the server's receiver; runs on both sides. */
    static void register() {
        // Packets arrive on the network thread; the hello is answered on the server thread
        ServerPlayNetworking.registerGlobalReceiver(HELLO, (server, player, handler, buf, sender) -> {
            byte[] data = read(buf);
            server.execute(() -> ServerClocks.hello(player, data));
        });
    }

    /** Sends a message on the clocks channel, if the player's client listens on it. */
    public static void sendClocks(ServerPlayer player, byte[] message) {
        if (ServerPlayNetworking.canSend(player, CLOCKS)) {
            ServerPlayNetworking.send(player, CLOCKS, write(message));
        }
    }

    /** The player's game language ("ru_ru"), as their client reported it. */
    public static String language(ServerPlayer player) {
        try {
            return player.clientInformation().language();
        } catch (Throwable t) {
            return "";
        }
    }

    /** The rest of the packet, as it is. */
    public static byte[] read(FriendlyByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return data;
    }

    public static FriendlyByteBuf write(byte[] data) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeBytes(data);
        return buf;
    }
}
