package com.kasper.despawnindicator.server;

import com.kasper.despawnindicator.DespawnIndicator;
import com.kasper.despawnindicator.ModNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;

/** What the shared server code needs from the loader (Fabric). */
public final class Platform {

    private Platform() {
    }

    /** Ticks the item lives in all: always 6000 on Fabric. */
    public static int lifespan(ItemEntity item) {
        return DespawnIndicator.VANILLA_LIFETIME;
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
        if (ServerPlayNetworking.canSend(player, ModNetworking.Clocks.TYPE)) {
            ServerPlayNetworking.send(player, new ModNetworking.Clocks(message));
        }
    }
}
