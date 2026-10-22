package com.kasper.despawnindicator;

import com.kasper.despawnindicator.client.BlinkingItemRenderer;
import com.kasper.despawnindicator.client.ClientTicker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client entrypoint: receives the clocks, counts them down and draws the items with their blinking. */
public class DespawnIndicatorClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.Clocks.TYPE,
                (payload, context) -> ClientTicker.receive(payload.data()));
        ClientTickEvents.END_CLIENT_TICK.register(ClientTicker::tick);
        BlinkingItemRenderer.register();
    }
}
