package com.kasper.despawnindicator.client;

import com.kasper.despawnindicator.DespawnIndicator;
import net.minecraft.world.entity.item.ItemEntity;

/** What the shared client code needs from the loader (Fabric). */
public final class ClientPlatform {

    private ClientPlatform() {
    }

    /** @return whether the hello went out: false while the server has not (yet) said it has the mod */
    public static boolean trySendHello(byte[] hello) {
        return ClientNetworking.trySendHello(hello);
    }

    /** Ticks the item lives in all: the same for every item on Fabric. */
    public static int lifespan(ItemEntity item) {
        return DespawnIndicator.VANILLA_LIFETIME;
    }
}
