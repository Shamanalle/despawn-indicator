package com.kasper.despawnindicator.client;

import com.kasper.despawnindicator.neoforge.NeoNetworking;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;

/** What the shared client code needs from the loader (NeoForge, Minecraft 1.21.6 - 1.21.8). */
public final class ClientPlatform {

    /** PacketDistributor.sendToServer of NeoForge 21.6, which has no ClientPacketDistributor yet (21.7 on) */
    private static final MethodHandle SEND_TO_SERVER_21_6 = findSendToServer21_6();

    private ClientPlatform() {
    }

    /** @return whether the hello went out: false while the server has not said it has the mod */
    public static boolean trySendHello(byte[] hello) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null || !connection.hasChannel(NeoNetworking.Hello.TYPE)) {
            return false;
        }
        NeoNetworking.Hello payload = new NeoNetworking.Hello(hello);
        if (SEND_TO_SERVER_21_6 == null) {
            ClientPacketDistributor.sendToServer(payload);
            return true;
        }
        try {
            SEND_TO_SERVER_21_6.invokeExact((CustomPacketPayload) payload, new CustomPacketPayload[0]);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
        return true;
    }

    /** Ticks the item lives in all, as far as this client knows: the server's word replaces it. */
    public static int lifespan(ItemEntity item) {
        return item.lifespan;
    }

    private static MethodHandle findSendToServer21_6() {
        try {
            Class.forName("net.neoforged.neoforge.client.network.ClientPacketDistributor", false,
                    ClientPlatform.class.getClassLoader());
            return null;
        } catch (ClassNotFoundException absent) {
            // NeoForge 21.6
        }
        try {
            return MethodHandles.publicLookup().findStatic(
                    PacketDistributor.class, "sendToServer",
                    MethodType.methodType(void.class, CustomPacketPayload.class, CustomPacketPayload[].class));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("No way to send to the server on this NeoForge", e);
        }
    }
}
