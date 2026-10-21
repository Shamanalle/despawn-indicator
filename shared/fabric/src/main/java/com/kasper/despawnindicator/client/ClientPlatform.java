package com.kasper.despawnindicator.client;

import com.kasper.despawnindicator.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** What the shared client code needs from the loader (Fabric). */
public final class ClientPlatform {

    private ClientPlatform() {
    }

    /** @return whether the hello went out: false while the server has not (yet) said it has the mod */
    public static boolean trySendHello(byte[] hello) {
        if (!ClientPlayNetworking.canSend(ModNetworking.Hello.TYPE)) {
            return false;
        }
        ClientPlayNetworking.send(new ModNetworking.Hello(hello));
        return true;
    }
}
