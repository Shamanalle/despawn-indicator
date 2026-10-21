package com.kasper.despawnindicator.forge;

import com.kasper.despawnindicator.BuildInfo;
import com.kasper.despawnindicator.DespawnIndicator;
import com.kasper.despawnindicator.ModLog;
import com.kasper.despawnindicator.server.ServerClocks;
import com.kasper.despawnindicator.server.StatusCommand;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forge entrypoint (Minecraft 1.20.1): the server half, which also runs in singleplayer, and on the client the
 * client half. The shared code does the work; only the hooks into Forge live in this package.
 */
@Mod(DespawnIndicator.MOD_ID)
public final class DespawnIndicatorForge {

    public DespawnIndicatorForge() {
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
        ForgeNetworking.register();
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.StartTracking e) -> {
            if (e.getEntity() instanceof ServerPlayer) {
                ServerClocks.startTracking(e.getTarget(), (ServerPlayer) e.getEntity());
            }
        });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent e) -> {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (e.phase == TickEvent.Phase.END && server != null) {
                ServerClocks.endTick(server);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer) {
                ServerClocks.left((ServerPlayer) e.getEntity());
            }
        });
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> ServerClocks.stopped());
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> StatusCommand.register(e.getDispatcher()));
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ForgeClient.init();
        }
        ModLog.info(DespawnIndicator.NAME + " " + BuildInfo.version() + " loaded");
    }
}
