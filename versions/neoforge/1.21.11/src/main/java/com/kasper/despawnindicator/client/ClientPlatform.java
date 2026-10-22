package com.kasper.despawnindicator.client;

import com.kasper.despawnindicator.neoforge.NeoNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** What the shared client code needs from the loader (NeoForge, Minecraft 1.21.11). */
public final class ClientPlatform {

    private ClientPlatform() {
    }

    /** @return whether the hello went out: false while the server has not said it has the mod */
    public static boolean trySendHello(byte[] hello) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null || !connection.hasChannel(NeoNetworking.Hello.TYPE)) {
            return false;
        }
        ClientPacketDistributor.sendToServer(new NeoNetworking.Hello(hello));
        return true;
    }

    /** Ticks the item lives in all, as far as this client knows: the server's word replaces it. */
    public static int lifespan(ItemEntity item) {
        return item.lifespan;
    }
}
