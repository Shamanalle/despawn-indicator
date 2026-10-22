package com.kasper.despawnindicator.neoforge;

import com.kasper.despawnindicator.DespawnIndicator;
import com.kasper.despawnindicator.client.BlinkingItemRenderer;
import com.kasper.despawnindicator.client.ClientTicker;
import com.kasper.despawnindicator.client.GuiCanvas;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;

/** The client half on NeoForge 26.x. */
final class NeoClient {

    private NeoClient() {
    }

    static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> ClientTicker.tick(Minecraft.getInstance()));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) ->
                e.registerEntityRenderer(BlinkingItemRenderer.itemType(), BlinkingItemRenderer::new));
        modBus.addListener((RegisterGuiLayersEvent e) ->
                e.registerAboveAll(Identifier.fromNamespaceAndPath(DespawnIndicator.MOD_ID, "death_loot"),
                        (g, tick) -> GuiCanvas.drawHud(g)));
    }
}
