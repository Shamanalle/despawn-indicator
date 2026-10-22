package com.kasper.despawnindicator.client;

import com.kasper.despawnindicator.Blink;
import com.kasper.despawnindicator.ClientClocks;
import com.kasper.despawnindicator.ClientSettings;
import com.kasper.despawnindicator.DespawnIndicator;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * How each item on the ground looks this tick: which items blink out of sight in their last seconds
 * ({@link ClientSettings#warnSeconds}), which ones shrink and grow back instead (the fading style), and (through
 * {@link ItemLabels}) which ones carry a time label. Worked out once a tick; the item renderer of each loader
 * only asks {@link #hidden} and {@link #scale}.
 */
public final class ItemLook {

    /** About the middle of a lying item above the ground, which it shrinks towards. */
    private static final float FLOAT_HEIGHT = 0.25F;

    private static Set<Integer> hidden = Collections.emptySet();
    /** The fading items: seconds left at the end of this tick. */
    private static Map<Integer, Double> fading = Collections.emptyMap();
    private static int warnSeconds = ClientSettings.DEFAULT.warnSeconds;
    /** Items drawn smaller so far; read by the in-game test. */
    private static long shrunk;

    private ItemLook() {
    }

    /** Whether the item is blinked out of sight now; asked by the renderer every frame. */
    public static boolean hidden(ItemEntity item) {
        return hidden.contains(item.getId());
    }

    /**
     * How large the item is drawn now, 1 unless it fades; asked by the renderer every frame. The partial tick
     * (0 - 1, the part of the next tick gone) makes the size change smoothly between ticks.
     */
    public static float scale(ItemEntity item, float partialTick) {
        Double secondsLeft = fading.get(item.getId());
        if (secondsLeft == null) {
            return 1;
        }
        return (float) Blink.scale(secondsLeft - partialTick / DespawnIndicator.TICKS_PER_SECOND, warnSeconds);
    }

    /**
     * Has the renderer draw the item at {@code scale}, around the height it floats at; at full size as it is.
     * The item is drawn as usual, only smaller, so shader packs show it too.
     */
    public static void drawScaled(PoseStack pose, float scale, Runnable draw) {
        if (scale >= 1) {
            draw.run();
            return;
        }
        shrunk++;
        pose.pushPose();
        pose.translate(0, FLOAT_HEIGHT, 0);
        pose.scale(scale, scale, scale);
        pose.translate(0, -FLOAT_HEIGHT, 0);
        draw.run();
        pose.popPose();
    }

    /** How many times an item was drawn smaller. */
    public static long shrunk() {
        return shrunk;
    }

    /** The end of a client tick in {@code level}. */
    static void tick(Minecraft mc, ClientLevel level, ClientSettings settings) {
        ItemLabels.update(mc, level, settings);
        warnSeconds = settings.warnSeconds;
        if (settings.style == ClientSettings.Style.LABEL) {
            // Labels instead of blinking (ItemLabels)
            hidden = Collections.emptySet();
            fading = Collections.emptyMap();
            return;
        }
        boolean fade = settings.style == ClientSettings.Style.FADE;
        Set<Integer> nextHidden = new HashSet<>();
        Map<Integer, Double> nextFading = new HashMap<>();
        for (Entity e : level.entitiesForRendering()) {
            if (!(e instanceof ItemEntity)) {
                continue;
            }
            ItemEntity item = (ItemEntity) e;
            // Reading starts the estimate of an item the server says nothing about
            ClientClocks.Reading r = read(item);
            if (r.unlimited || ItemLabels.labeled(item)) {
                continue;
            }
            double secondsLeft = r.ticksLeft / (double) DespawnIndicator.TICKS_PER_SECOND;
            if (fade && secondsLeft < settings.warnSeconds) {
                nextFading.put(item.getId(), secondsLeft);
            } else if (!fade && !Blink.shown(secondsLeft, settings.warnSeconds)) {
                nextHidden.add(item.getId());
            }
        }
        hidden = nextHidden;
        fading = nextFading;
    }

    static ClientClocks.Reading read(ItemEntity item) {
        return ClientTicker.CLOCKS.read(item.getId(), ClientPlatform.lifespan(item));
    }

    /** Left the world or changed dimension. */
    static void clear() {
        hidden = Collections.emptySet();
        fading = Collections.emptyMap();
        ItemLabels.clear();
    }
}
