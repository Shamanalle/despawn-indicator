package com.kasper.despawnindicator.client;

import com.kasper.despawnindicator.DespawnIndicator;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;

/** Puts the death loot timer ({@link LootOverlay}) on the HUD. Fabric, Minecraft 1.21.9 - 1.21.11. */
public final class LootHudHook {

    private LootHudHook() {
    }

    public static void register() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(DespawnIndicator.MOD_ID, "death_loot"),
                (g, tick) -> GuiCanvas.drawHud(g));
    }
}
