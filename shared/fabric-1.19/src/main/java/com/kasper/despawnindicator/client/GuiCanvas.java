package com.kasper.despawnindicator.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;

/** The {@link Canvas} over a PoseStack and vanilla's GuiComponent, Minecraft 1.19 - 1.19.4 (before GuiGraphics). */
public final class GuiCanvas implements Canvas {

    private final PoseStack pose;

    public GuiCanvas(PoseStack pose) {
        this.pose = pose;
    }

    /** Draws the HUD of the mod, unless the player hid the HUD (F1). */
    public static void drawHud(PoseStack pose) {
        if (!Minecraft.getInstance().options.hideGui) {
            LootOverlay.draw(new GuiCanvas(pose));
        }
    }

    @Override
    public int width() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    @Override
    public int height() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    @Override
    public void fill(int x0, int y0, int x1, int y1, int argb) {
        GuiComponent.fill(pose, x0, y0, x1, y1, argb);
    }

    @Override
    public void centeredText(String text, int cx, int y, int argb) {
        GuiComponent.drawCenteredString(pose, Minecraft.getInstance().font, text, cx, y, argb);
    }
}
