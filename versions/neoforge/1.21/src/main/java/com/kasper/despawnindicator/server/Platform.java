package com.kasper.despawnindicator.server;

import com.kasper.despawnindicator.neoforge.NeoNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.neoforge.network.PacketDistributor;

/** What the shared server code needs from the loader (NeoForge, Minecraft 1.21 - 1.21.1). */
public final class Platform {

    private Platform() {
    }

    /** Ticks the item lives in all: NeoForge lets each item, and ItemExpireEvent, choose. */
    public static int lifespan(ItemEntity item) {
        return item.lifespan;
    }

    /** The player's game language ("ru_ru"), as their client reported it. */
    public static String language(ServerPlayer player) {
        try {
            return player.clientInformation().language();
        } catch (Throwable t) {
            return "";
        }
    }

    public static void send(ServerPlayer player, byte[] message) {
        if (player.connection.hasChannel(NeoNetworking.Clocks.TYPE)) {
            PacketDistributor.sendToPlayer(player, new NeoNetworking.Clocks(message));
        }
    }
}
