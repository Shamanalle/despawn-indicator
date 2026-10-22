package com.kasper.despawnindicator;

import com.kasper.despawnindicator.server.StatusCommand;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;

/** Registers the mod's command with Fabric API's command API v1, the only one for Minecraft 1.18.2. */
public final class ModCommands {

    private ModCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> StatusCommand.register(dispatcher));
    }
}
