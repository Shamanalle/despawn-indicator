package com.kasper.despawnindicator;

import com.kasper.despawnindicator.server.ServerClocks;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/** Common entrypoint (client and server): the server half, which also runs in singleplayer. */
public class DespawnIndicatorMod implements ModInitializer {

    @Override
    public void onInitialize() {
        ModLog.use(new LogSink());
        ModNetworking.register();
        EntityTrackingEvents.START_TRACKING.register(ServerClocks::startTracking);
        ServerTickEvents.END_SERVER_TICK.register(ServerClocks::endTick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ServerClocks.left(handler.player));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ServerClocks.stopped());
        ModCommands.register();
        ModLog.info(DespawnIndicator.NAME + " " + BuildInfo.version() + " loaded");
    }
}
