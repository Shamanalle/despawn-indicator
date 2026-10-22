package com.kasper.despawnindicator;

import com.kasper.despawnindicator.server.StatusCommand;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/** Registers the mod's command with Fabric API's command API v2 (Minecraft 1.19 and later). */
public final class ModCommands {

    private ModCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> StatusCommand.register(dispatcher));
    }
}
