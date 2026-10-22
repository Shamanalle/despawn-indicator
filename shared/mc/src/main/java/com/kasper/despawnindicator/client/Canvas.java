package com.kasper.despawnindicator.client;

/**
 * Where the mod draws on the screen. Each loader hands one over from its own HUD event, so the drawing code
 * stays the same while the vanilla classes behind it change between Minecraft versions. Colours are ARGB.
 */
public interface Canvas {

    /** The screen size in GUI pixels. */
    int width();

    int height();

    void fill(int x0, int y0, int x1, int y1, int argb);

    /** One line of text with a shadow, centred on {@code cx}. */
    void centeredText(String text, int cx, int y, int argb);
}
