package com.kasper.despawnindicator.neoforge;

import com.kasper.despawnindicator.BuildInfo;
import com.kasper.despawnindicator.DespawnIndicator;
import com.kasper.despawnindicator.ModLog;
import com.kasper.despawnindicator.server.ServerClocks;
import com.kasper.despawnindicator.server.StatusCommand;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NeoForge entrypoint (Minecraft 1.21.9 - 1.21.10): the server half, which also runs in singleplayer, and on the
 * client the client half. The shared code does the work; only the hooks into NeoForge live in this package.
 */
@Mod(DespawnIndicator.MOD_ID)
public final class DespawnIndicatorNeoForge {

    public DespawnIndicatorNeoForge(IEventBus modBus, ModContainer container, Dist dist) {
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
        modBus.addListener(NeoNetworking::register);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.StartTracking e) -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                ServerClocks.startTracking(e.getTarget(), player);
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> ServerClocks.endTick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer player) {
                ServerClocks.left(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> ServerClocks.stopped());
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> StatusCommand.register(e.getDispatcher()));
        if (dist.isClient()) {
            NeoClient.init(modBus);
        }
        ModLog.info(DespawnIndicator.NAME + " " + BuildInfo.version() + " loaded");
    }
}
