package com.kasper.despawnindicator.client;

import com.kasper.despawnindicator.forge.ForgeNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;

/** What the shared client code needs from the loader (Forge, Minecraft 1.20.1). */
public final class ClientPlatform {

    private ClientPlatform() {
    }

    /** Sends the hello once: a server without the mod ignores it. */
    public static boolean trySendHello(byte[] hello) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null || Minecraft.getInstance().player == null) {
            return false;
        }
        connection.send(new ServerboundCustomPayloadPacket(ForgeNetworking.HELLO, ForgeNetworking.write(hello)));
        return true;
    }
}
