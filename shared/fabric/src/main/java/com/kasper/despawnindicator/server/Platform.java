package com.kasper.despawnindicator.server;

import com.kasper.despawnindicator.DespawnIndicator;
import com.kasper.despawnindicator.ModNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;

import java.nio.file.Path;

/** What the shared server code needs from the loader (Fabric). The channels are the band's own ({@link ModNetworking}). */
public final class Platform {

    private Platform() {
    }

    /** Ticks the item lives in all: always 6000 on Fabric. */
    public static int lifespan(ItemEntity item) {
        return DespawnIndicator.VANILLA_LIFETIME;
    }

    /** The player's game language ("ru_ru"), as their client reported it; empty where the server does not keep it. */
    public static String language(ServerPlayer player) {
        return ModNetworking.language(player);
    }

    public static void send(ServerPlayer player, byte[] message) {
        ModNetworking.sendClocks(player, message);
    }

    /** The game's config folder. */
    public static Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
