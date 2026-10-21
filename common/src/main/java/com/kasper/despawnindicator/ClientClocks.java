package com.kasper.despawnindicator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;

/**
 * The client side of the clocks: what the server said about each item, counted down tick by tick, and for
 * items the server said nothing about, an estimate from the moment this client first saw them.
 * <p>
 * The estimate is the best a client can do alone: Minecraft does not tell clients how old an item is, so an
 * item that lay on the ground for four minutes before you came near looks new. Such times are shown with "≈".
 */
public final class ClientClocks {

    /** What is known about an item's despawn, at the current tick. */
    public static final class Reading {
        public final int ticksLeft;
        public final int lifespan;
        /** It never despawns: no timer, no blinking. */
        public final boolean unlimited;
        /** Counted from when this client first saw it, not told by the server. */
        public final boolean estimate;
        /** Dropped by this player. */
        public final boolean mine;

        Reading(int ticksLeft, int lifespan, boolean unlimited, boolean estimate, boolean mine) {
            this.ticksLeft = ticksLeft;
            this.lifespan = lifespan;
            this.unlimited = unlimited;
            this.estimate = estimate;
            this.mine = mine;
        }
    }

    private static final class Received {
        final ItemClock clock;
        final long tick;

        Received(ItemClock clock, long tick) {
            this.clock = clock;
            this.tick = tick;
        }
    }

    private final Map<Integer, Received> exact = new HashMap<>();
    private final Map<Integer, Long> firstSeen = new HashMap<>();
    private long now;

    /** One client tick passed in a world (not while the game is paused). */
    public synchronized void tick() {
        now++;
    }

    public synchronized long now() {
        return now;
    }

    /** A message from the server. */
    public synchronized void receive(List<ItemClock> clocks) {
        for (ItemClock c : clocks) {
            exact.put(c.entityId, new Received(c, now));
        }
    }

    /** Whether the server told anything about the item. */
    public synchronized boolean isExact(int entityId) {
        return exact.containsKey(entityId);
    }

    /**
     * The item's clock now. {@code lifespan} is what it lives in all when the server said nothing: 6000 ticks,
     * or what a loader's item says.
     */
    public synchronized Reading read(int entityId, int lifespan) {
        Received r = exact.get(entityId);
        if (r != null) {
            ItemClock c = r.clock;
            if (c.has(ItemClock.UNLIMITED)) {
                return new Reading(0, 0, true, false, c.has(ItemClock.MINE));
            }
            long passed = c.has(ItemClock.FROZEN) ? 0 : now - r.tick;
            return new Reading(clamp(c.remaining - passed), c.lifespan, false, false, c.has(ItemClock.MINE));
        }
        Long seen = firstSeen.get(entityId);
        if (seen == null) {
            seen = now;
            firstSeen.put(entityId, seen);
        }
        return new Reading(clamp(lifespan - (now - seen)), lifespan, false, true, false);
    }

    private static int clamp(long ticks) {
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, ticks));
    }

    /** Forgets the items {@code alive} says are gone; call it now and then, not every frame. */
    public synchronized void retain(IntPredicate alive) {
        for (Iterator<Integer> i = exact.keySet().iterator(); i.hasNext(); ) {
            if (!alive.test(i.next())) {
                i.remove();
            }
        }
        for (Iterator<Integer> i = firstSeen.keySet().iterator(); i.hasNext(); ) {
            if (!alive.test(i.next())) {
                i.remove();
            }
        }
    }

    /** Forgets everything (left the world or changed dimension: entity ids start over). */
    public synchronized void clear() {
        exact.clear();
        firstSeen.clear();
    }

    /** How many items the server told about; for the status line and the tests. */
    public synchronized int exactCount() {
        return exact.size();
    }
}
