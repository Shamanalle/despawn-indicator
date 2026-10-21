package com.kasper.despawnindicator.neoforge;

import com.kasper.despawnindicator.client.ClientTicker;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** The client half on NeoForge 1.21 - 1.21.1. */
final class NeoClient {

    private NeoClient() {
    }

    static void init() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> ClientTicker.tick(Minecraft.getInstance()));
    }
}
