package com.kasper.despawnindicator.server;

import com.kasper.despawnindicator.forge.ForgeNetworking;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;

/** What the shared server code needs from the loader (Forge, Minecraft 1.20.1). */
public final class Platform {

    private Platform() {
    }

    /** Ticks the item lives in all: Forge lets each item, and ItemExpireEvent, choose. */
    public static int lifespan(ItemEntity item) {
        return item.lifespan;
    }

    /** Minecraft 1.20.1 does not tell the server the player's language, so replies are in English. */
    public static String language(ServerPlayer player) {
        return "";
    }

    public static void send(ServerPlayer player, byte[] message) {
        player.connection.send(new ClientboundCustomPayloadPacket(ForgeNetworking.CLOCKS, ForgeNetworking.write(message)));
    }
}
