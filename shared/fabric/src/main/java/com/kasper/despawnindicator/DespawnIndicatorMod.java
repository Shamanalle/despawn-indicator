package com.kasper.despawnindicator;

import com.kasper.despawnindicator.server.ServerClocks;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Common entrypoint (client and server): the server half, which also runs in singleplayer. */
public class DespawnIndicatorMod implements ModInitializer {

    @Override
    public void onInitialize() {
        Logger log = LoggerFactory.getLogger(DespawnIndicator.NAME);
        ModLog.use(new ModLog.Sink() {
            @Override
            public void info(String message) {
                log.info(message);
            }

            @Override
            public void warn(String message) {
                log.warn(message);
            }
        });
        ModNetworking.register();
        EntityTrackingEvents.START_TRACKING.register(ServerClocks::startTracking);
        ServerTickEvents.END_SERVER_TICK.register(ServerClocks::endTick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ServerClocks.left(handler.player));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ServerClocks.stopped());
        ModCommands.register();
        ModLog.info(DespawnIndicator.NAME + " " + BuildInfo.version() + " loaded");
    }
}
