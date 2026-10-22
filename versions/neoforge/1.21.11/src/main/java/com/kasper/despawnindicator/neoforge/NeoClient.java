package com.kasper.despawnindicator.neoforge;

import com.kasper.despawnindicator.DespawnIndicator;
import com.kasper.despawnindicator.client.BlinkingItemRenderer;
import com.kasper.despawnindicator.client.ClientTicker;
import com.kasper.despawnindicator.client.GuiCanvas;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;

/** The client half on NeoForge 1.21.11. */
final class NeoClient {

    private NeoClient() {
    }

    static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> ClientTicker.tick(Minecraft.getInstance()));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) ->
                e.registerEntityRenderer(EntityType.ITEM, BlinkingItemRenderer::new));
        modBus.addListener((RegisterGuiLayersEvent e) ->
                e.registerAboveAll(Identifier.fromNamespaceAndPath(DespawnIndicator.MOD_ID, "death_loot"),
                        (g, tick) -> GuiCanvas.drawHud(g)));
    }
}
