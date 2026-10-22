package com.kasper.despawnindicator.neoforge;

import com.kasper.despawnindicator.client.BlinkingItemRenderer;
import com.kasper.despawnindicator.client.ClientTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;

/** The client half on NeoForge 1.21 - 1.21.1. */
final class NeoClient {

    private NeoClient() {
    }

    static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> ClientTicker.tick(Minecraft.getInstance()));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) ->
                e.registerEntityRenderer(EntityType.ITEM, BlinkingItemRenderer::new));
    }
}
