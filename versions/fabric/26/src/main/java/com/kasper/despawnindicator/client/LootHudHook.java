package com.kasper.despawnindicator.client;

import com.kasper.despawnindicator.DespawnIndicator;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** Puts the death loot timer ({@link LootOverlay}) on the HUD. Fabric, Minecraft 26.x. */
public final class LootHudHook {

    private LootHudHook() {
    }

    public static void register() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(DespawnIndicator.MOD_ID, "death_loot"),
                (g, tick) -> LootOverlay.draw(new ExtractorCanvas(g)));
    }

    /** The {@link Canvas} over 26.x's GuiGraphicsExtractor; the HUD skips its elements while hidden (F1). */
    private static final class ExtractorCanvas implements Canvas {
        private final GuiGraphicsExtractor g;

        ExtractorCanvas(GuiGraphicsExtractor g) {
            this.g = g;
        }

        @Override
        public int width() {
            return g.guiWidth();
        }

        @Override
        public int height() {
            return g.guiHeight();
        }

        @Override
        public void fill(int x0, int y0, int x1, int y1, int argb) {
            g.fill(x0, y0, x1, y1, argb);
        }

        @Override
        public void centeredText(String text, int cx, int y, int argb) {
            g.centeredText(Minecraft.getInstance().font, text, cx, y, argb);
        }
    }
}
