package com.kasper.despawnindicator;

import com.kasper.despawnindicator.client.ClientTicker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client entrypoint: receives the clocks and counts them down. */
public class DespawnIndicatorClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.Clocks.TYPE,
                (payload, context) -> ClientTicker.receive(payload.data()));
        ClientTickEvents.END_CLIENT_TICK.register(ClientTicker::tick);
    }
}
