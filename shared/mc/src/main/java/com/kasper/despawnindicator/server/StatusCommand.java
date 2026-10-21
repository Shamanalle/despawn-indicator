package com.kasper.despawnindicator.server;

import com.kasper.despawnindicator.BuildInfo;
import com.kasper.despawnindicator.DespawnIndicator;
import com.kasper.despawnindicator.ServerText;
import com.kasper.despawnindicator.TimeText;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** {@code /despawnindicator}: the mod's status on this server, in the language of whoever asks. */
public final class StatusCommand {

    private StatusCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(DespawnIndicator.MOD_ID).executes(StatusCommand::status));
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String lang = source.getEntity() instanceof ServerPlayer ? Platform.language((ServerPlayer) source.getEntity()) : "";
        int online = source.getServer().getPlayerList().getPlayerCount();
        reply(source, ServerText.get(lang, "status.title", BuildInfo.version()));
        reply(source, ServerText.get(lang, "status.players", ServerClocks.SENDER.playersWithMod(), online));
        reply(source, ServerText.get(lang, "status.lifetime", TimeText.format(DespawnIndicator.VANILLA_LIFETIME, false)));
        return 1;
    }

    private static void reply(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
    }
}
