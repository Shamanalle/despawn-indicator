package com.kasper.despawnindicator.forge;

import com.kasper.despawnindicator.client.ClientTicker;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/** The client half on Forge 1.20.1. */
final class ForgeClient {

    private ForgeClient() {
    }

    static void init() {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END) {
                ClientTicker.tick(Minecraft.getInstance());
            }
        });
    }
}
