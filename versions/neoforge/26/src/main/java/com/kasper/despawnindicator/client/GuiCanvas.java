package com.kasper.despawnindicator.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** The {@link Canvas} over vanilla's GuiGraphicsExtractor, Minecraft 26.x. */
public final class GuiCanvas implements Canvas {

    private final GuiGraphicsExtractor g;

    public GuiCanvas(GuiGraphicsExtractor g) {
        this.g = g;
    }

    /** Draws the HUD of the mod, unless the player hid the HUD (F1). */
    public static void drawHud(GuiGraphicsExtractor g) {
        if (!Minecraft.getInstance().options.hideGui) {
            LootOverlay.draw(new GuiCanvas(g));
        }
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
