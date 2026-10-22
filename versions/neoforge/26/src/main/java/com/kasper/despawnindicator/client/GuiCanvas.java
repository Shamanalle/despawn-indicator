package com.kasper.despawnindicator.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/** The {@link Canvas} over vanilla's GuiGraphicsExtractor, Minecraft 26.x. */
public final class GuiCanvas implements Canvas {

    /** The {@code hideGui} option of 26.1, absent from 26.2 on, where the HUD keeps it ({@code Hud.isHidden()}). */
    private static final MethodHandle HIDE_GUI = findHideGui();

    private final GuiGraphicsExtractor g;

    public GuiCanvas(GuiGraphicsExtractor g) {
        this.g = g;
    }

    /** Draws the HUD of the mod, unless the player hid the HUD (F1). */
    public static void drawHud(GuiGraphicsExtractor g) {
        if (!hudHidden()) {
            LootOverlay.draw(new GuiCanvas(g));
        }
    }

    private static boolean hudHidden() {
        Minecraft minecraft = Minecraft.getInstance();
        if (HIDE_GUI == null) {
            return minecraft.gui.hud.isHidden();
        }
        try {
            return (boolean) HIDE_GUI.invokeExact(minecraft.options);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    private static MethodHandle findHideGui() {
        try {
            return MethodHandles.publicLookup().findGetter(Options.class, "hideGui", boolean.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
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
