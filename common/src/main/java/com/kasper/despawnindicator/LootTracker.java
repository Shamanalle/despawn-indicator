package com.kasper.despawnindicator;

/**
 * The client side of the death loot timer. A server with the mod says where the loot is and how long it has
 * left ({@link LootMessage}); the client counts that down. On other servers the client guesses: it notices
 * its player die, looks for items at that spot a moment later, and counts the item lifetime down from the
 * death, marked as an estimate (the items' clock stops while nobody is near them, which the client cannot
 * see).
 */
public final class LootTracker {

    /** The timer at the current tick. */
    public static final class Reading {
        public final int ticksLeft;
        public final boolean unlimited;
        public final boolean frozen;
        public final boolean estimate;
        public final String dimension;
        public final int x;
        public final int y;
        public final int z;
        public final int items;

        Reading(int ticksLeft, boolean unlimited, boolean frozen, boolean estimate, LootMessage at) {
            this.ticksLeft = ticksLeft;
            this.unlimited = unlimited;
            this.frozen = frozen;
            this.estimate = estimate;
            this.dimension = at.dimension;
            this.x = at.x;
            this.y = at.y;
            this.z = at.z;
            this.items = at.items;
        }
    }

    /** Where the player died, waiting for the items to show up there (only when guessing). */
    public static final class Death {
        public final String dimension;
        public final int x;
        public final int y;
        public final int z;
        public final long tick;

        Death(String dimension, int x, int y, int z, long tick) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.tick = tick;
        }
    }

    private LootMessage loot;
    private long since;
    private boolean estimate;
    private boolean serverTells;
    private Death death;
    private long now;

    /** One client tick passed (not while the game is paused). */
    public synchronized void tick() {
        now++;
    }

    public synchronized long now() {
        return now;
    }

    /** A message from the server: from now on it is the one to say. */
    public synchronized void receive(LootMessage m) {
        serverTells = true;
        death = null;
        estimate = false;
        loot = m.present ? m : null;
        since = now;
    }

    /** Whether the server keeps track, so the client does not guess. */
    public synchronized boolean serverTells() {
        return serverTells;
    }

    /** The player just died here; when guessing, the items are looked for later ({@link #death()}). */
    public synchronized void died(String dimension, int x, int y, int z) {
        if (!serverTells) {
            death = new Death(dimension, x, y, z, now);
        }
    }

    /** The death whose items are still to be looked for, or null. */
    public synchronized Death death() {
        return death;
    }

    /** {@code items} stacks lie where the player died: they despawn {@code lifespan} ticks after the death. */
    public synchronized void found(int items, int lifespan) {
        if (death == null) {
            return;
        }
        loot = new LootMessage(death.dimension, death.x, death.y, death.z, items, 0, lifespan, lifespan);
        since = death.tick;
        estimate = true;
        death = null;
    }

    /** Nothing lies where the player died (kept their inventory, a grave took it): no timer. */
    public synchronized void notFound() {
        death = null;
    }

    /** The guessed loot is gone (picked up, or the player saw it is not there any more). */
    public synchronized void gone() {
        if (estimate) {
            loot = null;
        }
    }

    /** The guessed loot, still counted; for the client to check whether it is there. */
    public synchronized boolean guessing() {
        return estimate && loot != null;
    }

    /** The timer now, or null when there is none. */
    public synchronized Reading read() {
        if (loot == null) {
            return null;
        }
        if (loot.has(ItemClock.UNLIMITED)) {
            return new Reading(0, true, false, estimate, loot);
        }
        boolean frozen = loot.has(ItemClock.FROZEN);
        long left = frozen ? loot.remaining : loot.remaining - (now - since);
        return new Reading((int) Math.max(0, Math.min(Integer.MAX_VALUE, left)), false, frozen, estimate, loot);
    }

    /** Left the server: the next one may or may not keep track. */
    public synchronized void clear() {
        loot = null;
        death = null;
        estimate = false;
        serverTells = false;
    }
}
