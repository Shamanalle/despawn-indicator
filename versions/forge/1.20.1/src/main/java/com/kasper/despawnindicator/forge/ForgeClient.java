package com.kasper.despawnindicator.forge;

import com.kasper.despawnindicator.client.BlinkingItemRenderer;
import com.kasper.despawnindicator.client.ClientTicker;
import com.kasper.despawnindicator.client.GuiCanvas;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** The client half on Forge 1.20.1. */
final class ForgeClient {

    private ForgeClient() {
    }

    @SuppressWarnings("removal") // FMLJavaModLoadingContext.get() is how 1.20.1 hands out the mod bus
    static void init() {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END) {
                ClientTicker.tick(Minecraft.getInstance());
            }
        });
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) ->
                e.registerEntityRenderer(EntityType.ITEM, BlinkingItemRenderer::new));
        modBus.addListener((RegisterGuiOverlaysEvent e) ->
                e.registerAboveAll("death_loot", (gui, g, partialTick, width, height) -> GuiCanvas.drawHud(g)));
    }
}
