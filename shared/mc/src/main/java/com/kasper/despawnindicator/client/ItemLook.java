package com.kasper.despawnindicator.client;

import com.kasper.despawnindicator.Blink;
import com.kasper.despawnindicator.ClientClocks;
import com.kasper.despawnindicator.DespawnIndicator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * How each item on the ground looks this tick: which items blink out of sight, and (through
 * {@link ItemLabels}) which ones carry a time label. Worked out once a tick; the item renderer of each loader
 * only asks {@link #hidden}.
 */
public final class ItemLook {

    /** The blinking covers the last 20 seconds. */
    public static final double WARN_SECONDS = 20;

    private static Set<Integer> hidden = Collections.emptySet();

    private ItemLook() {
    }

    /** Whether the item is blinked out of sight now; asked by the renderer every frame. */
    public static boolean hidden(ItemEntity item) {
        return hidden.contains(item.getId());
    }

    /** The end of a client tick in {@code level}. */
    static void tick(Minecraft mc, ClientLevel level) {
        ItemLabels.update(mc, level);
        Set<Integer> next = new HashSet<>();
        for (Entity e : level.entitiesForRendering()) {
            if (!(e instanceof ItemEntity)) {
                continue;
            }
            ItemEntity item = (ItemEntity) e;
            // Reading starts the estimate of an item the server says nothing about
            ClientClocks.Reading r = read(item);
            if (!r.unlimited && !ItemLabels.labeled(item)
                    && !Blink.shown(r.ticksLeft / (double) DespawnIndicator.TICKS_PER_SECOND, WARN_SECONDS)) {
                next.add(item.getId());
            }
        }
        hidden = next;
    }

    static ClientClocks.Reading read(ItemEntity item) {
        return ClientTicker.CLOCKS.read(item.getId(), ClientPlatform.lifespan(item));
    }

    /** Left the world or changed dimension. */
    static void clear() {
        hidden = Collections.emptySet();
        ItemLabels.clear();
    }
}
