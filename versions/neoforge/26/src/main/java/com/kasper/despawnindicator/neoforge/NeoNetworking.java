package com.kasper.despawnindicator.neoforge;

import com.kasper.despawnindicator.DespawnIndicator;
import com.kasper.despawnindicator.client.ClientTicker;
import com.kasper.despawnindicator.server.ServerClocks;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.function.Function;

/**
 * The two channels on NeoForge: the same names and bytes as on Fabric, Forge and Paper. Registered as optional,
 * so either side may lack the mod.
 */
public final class NeoNetworking {

    /** Client to server: the hello. */
    public record Hello(byte[] data) implements CustomPacketPayload {
        public static final Type<Hello> TYPE = new Type<>(id(DespawnIndicator.CHANNEL_HELLO));
        public static final StreamCodec<ByteBuf, Hello> CODEC = raw(Hello::new, Hello::data);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server to client: the clocks. */
    public record Clocks(byte[] data) implements CustomPacketPayload {
        public static final Type<Clocks> TYPE = new Type<>(id(DespawnIndicator.CHANNEL_CLOCKS));
        public static final StreamCodec<ByteBuf, Clocks> CODEC = raw(Clocks::new, Clocks::data);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private NeoNetworking() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(DespawnIndicator.MOD_ID, path);
    }

    /** The rest of the packet, as it is. */
    private static <T> StreamCodec<ByteBuf, T> raw(Function<byte[], T> make, Function<T, byte[]> bytes) {
        return StreamCodec.of((buf, payload) -> buf.writeBytes(bytes.apply(payload)), buf -> {
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            return make.apply(data);
        });
    }

    static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToServer(Hello.TYPE, Hello.CODEC, (payload, context) ->
                context.enqueueWork(() -> ServerClocks.hello((ServerPlayer) context.player(), payload.data())));
        registrar.playToClient(Clocks.TYPE, Clocks.CODEC, (payload, context) ->
                context.enqueueWork(() -> ClientTicker.receive(payload.data())));
    }
}
