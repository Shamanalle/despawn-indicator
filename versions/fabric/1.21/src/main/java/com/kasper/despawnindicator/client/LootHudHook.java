package com.kasper.despawnindicator.client;

import com.kasper.despawnindicator.DespawnIndicator;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.ResourceLocation;

/**
 * Puts the death loot timer ({@link LootOverlay}) on the HUD. Fabric, Minecraft 1.21 - 1.21.11: the HUD
 * element registry came with Fabric API for 1.21.6 and the older callback went away later, so the registry
 * is used where it exists. Each way lives in its own class, loaded only when used.
 */
public final class LootHudHook {

    private LootHudHook() {
    }

    public static void register() {
        if (hasRegistry()) {
            Modern.register();
        } else {
            Old.register();
        }
    }

    private static boolean hasRegistry() {
        try {
            Class.forName("net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static final class Modern {
        static void register() {
            HudElementRegistry.addLast(ResourceLocation.fromNamespaceAndPath(DespawnIndicator.MOD_ID, "death_loot"),
                    (g, tick) -> GuiCanvas.drawHud(g));
        }
    }

    @SuppressWarnings("deprecation")
    private static final class Old {
        static void register() {
            HudRenderCallback.EVENT.register((g, tick) -> GuiCanvas.drawHud(g));
        }
    }
}
