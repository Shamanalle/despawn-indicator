package com.kasper.despawnindicator.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** The {@link Canvas} over vanilla's GuiGraphics, Minecraft 1.20.1 - 1.21.11. */
public final class GuiCanvas implements Canvas {

    private final GuiGraphics g;

    public GuiCanvas(GuiGraphics g) {
        this.g = g;
    }

    /** Draws the HUD of the mod, unless the player hid the HUD (F1). */
    public static void drawHud(GuiGraphics g) {
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
        // Not drawString, whose return type changed within 1.21
        g.drawCenteredString(Minecraft.getInstance().font, text, cx, y, argb);
    }
}
