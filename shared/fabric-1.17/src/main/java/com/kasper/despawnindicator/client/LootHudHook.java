package com.kasper.despawnindicator.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

/** Puts the death loot timer ({@link LootOverlay}) on the HUD. Fabric, Minecraft 1.17.1 - 1.19.4. */
public final class LootHudHook {

    private LootHudHook() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register((pose, tickDelta) -> GuiCanvas.drawHud(pose));
    }
}
