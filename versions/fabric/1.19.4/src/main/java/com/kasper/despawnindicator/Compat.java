package com.kasper.despawnindicator;

import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

/** The Minecraft calls of the shared code that changed in 1.20, as they are on 1.19.4. */
public final class Compat {

    private Compat() {
    }

    public static MutableComponent literal(String text) {
        return Component.literal(text);
    }

    public static ServerLevel level(ServerPlayer player) {
        return player.getLevel();
    }

    /** Tells the player who ran a command, and not the operators. */
    public static void reply(CommandSourceStack source, Component text) {
        source.sendSuccess(text, false);
    }

    /** Who dropped the item, by hand or when they died; null if nobody did or they are not in the world. */
    public static UUID ownerId(ItemEntity item) {
        Entity owner = item.getOwner();
        return owner == null ? null : owner.getUUID();
    }
}
