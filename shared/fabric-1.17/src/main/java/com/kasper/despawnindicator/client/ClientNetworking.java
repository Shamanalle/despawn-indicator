package com.kasper.despawnindicator.client;

import com.kasper.despawnindicator.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** The client's end of the two channels ({@link ModNetworking}), Fabric, Minecraft 1.17.1 - 1.19.4. */
public final class ClientNetworking {

    private ClientNetworking() {
    }

    /** Registers the client's receiver. */
    public static void register() {
        // Packets arrive on the network thread; the clocks are read on the client thread
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.CLOCKS, (client, handler, buf, sender) -> {
            byte[] data = ModNetworking.read(buf);
            client.execute(() -> ClientTicker.receive(data));
        });
    }

    /** @return whether the hello went out: false while the server has not (yet) said it has the mod */
    public static boolean trySendHello(byte[] hello) {
        if (!ClientPlayNetworking.canSend(ModNetworking.HELLO)) {
            return false;
        }
        ClientPlayNetworking.send(ModNetworking.HELLO, ModNetworking.write(hello));
        return true;
    }
}
