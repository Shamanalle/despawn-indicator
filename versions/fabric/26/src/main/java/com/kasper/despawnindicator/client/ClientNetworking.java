package com.kasper.despawnindicator.client;

import com.kasper.despawnindicator.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** The client's end of the two channels ({@link ModNetworking}), Fabric 26.x. */
public final class ClientNetworking {

    private ClientNetworking() {
    }

    /** Registers the client's receiver. */
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.Clocks.TYPE,
                (payload, context) -> ClientTicker.receive(payload.data()));
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
