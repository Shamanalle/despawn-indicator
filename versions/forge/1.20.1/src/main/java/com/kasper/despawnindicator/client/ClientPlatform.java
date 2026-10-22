package com.kasper.despawnindicator.client;

import com.kasper.despawnindicator.forge.ForgeNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.world.entity.item.ItemEntity;

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

    /** Ticks the item lives in all, as far as this client knows: the server's word replaces it. */
    public static int lifespan(ItemEntity item) {
        return item.lifespan;
    }
}
