package com.kasper.despawnindicator;

import com.kasper.despawnindicator.server.ServerClocks;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * The two channels on Fabric 1.20.5 - 1.20.6 (payload API). A payload is the message's bytes as they are, the same
 * bytes as on Forge, NeoForge and the Bukkit plugin.
 */
public final class ModNetworking {

    /** Client to server: {@link HelloMessage}. */
    public record Hello(byte[] data) implements CustomPacketPayload {
        public static final Type<Hello> TYPE = new Type<>(id(DespawnIndicator.CHANNEL_HELLO));
        public static final StreamCodec<ByteBuf, Hello> CODEC = raw(Hello::new, Hello::data);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server to client: {@link ClockMessage}. */
    public record Clocks(byte[] data) implements CustomPacketPayload {
        public static final Type<Clocks> TYPE = new Type<>(id(DespawnIndicator.CHANNEL_CLOCKS));
        public static final StreamCodec<ByteBuf, Clocks> CODEC = raw(Clocks::new, Clocks::data);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private ModNetworking() {
    }

    /** Registers the payload types and the server's receiver; runs on both sides. */
    static void register() {
        PayloadTypeRegistry.playC2S().register(Hello.TYPE, Hello.CODEC);
        PayloadTypeRegistry.playS2C().register(Clocks.TYPE, Clocks.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Hello.TYPE,
                (payload, context) -> ServerClocks.hello(context.player(), payload.data()));
    }

    /** Sends a message on the clocks channel, if the player's client listens on it. */
    public static void sendClocks(ServerPlayer player, byte[] message) {
        if (ServerPlayNetworking.canSend(player, Clocks.TYPE)) {
            ServerPlayNetworking.send(player, new Clocks(message));
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

    private static ResourceLocation id(String path) {
        return new ResourceLocation(DespawnIndicator.MOD_ID, path);
    }

    /** The rest of the packet, as it is. */
    private static <T> StreamCodec<ByteBuf, T> raw(java.util.function.Function<byte[], T> make, java.util.function.Function<T, byte[]> bytes) {
        return StreamCodec.of((buf, payload) -> buf.writeBytes(bytes.apply(payload)), buf -> {
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            return make.apply(data);
        });
    }
}
